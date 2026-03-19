package com.nextup.infrastructure.service.game

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.GamePlayerNotFoundException
import com.nextup.common.exception.InvalidGameStateException
import com.nextup.core.domain.event.BattingOrderViolationEvent
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.domain.event.PitchCountWarningEvent
import com.nextup.core.domain.event.PitchCountWarningType
import com.nextup.core.domain.event.PlateAppearanceRecordedEvent
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameEvent
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.domain.game.PitchCountStatus
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.service.game.BoxScoreService
import com.nextup.core.service.game.PlateAppearanceRecordService
import com.nextup.core.service.game.dto.PlateAppearanceRecordResult
import com.nextup.core.service.game.dto.PlateAppearanceRequest
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 타석 결과 기록 서비스 구현
 *
 * 타석 기록 입력과 관련 경고(투구 수, 타순 위반)를 처리합니다.
 */
@Service
@Transactional(readOnly = true)
class PlateAppearanceRecordServiceImpl(
    private val gameRepository: GameRepositoryPort,
    private val gamePlayerRepository: GamePlayerRepositoryPort,
    private val boxScoreService: BoxScoreService,
    private val gameEventRepository: GameEventRepositoryPort,
    private val battingRecordRepository: BattingRecordRepositoryPort,
    private val pitchingRecordRepository: PitchingRecordRepositoryPort,
    private val gameTeamRepository: GameTeamRepositoryPort,
    private val eventPublisher: ApplicationEventPublisher,
) : PlateAppearanceRecordService {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun recordPlateAppearance(
        gameId: Long,
        request: PlateAppearanceRequest,
    ): PlateAppearanceRecordResult {
        val game = findGame(gameId)

        if (game.status != GameStatus.IN_PROGRESS) {
            throw InvalidGameStateException("진행 중인 경기만 타석 기록을 입력할 수 있습니다. 현재 상태: ${game.status.displayName}")
        }

        val batter =
            gamePlayerRepository.findByIdOrNull(request.batterId)
                ?: throw GamePlayerNotFoundException(request.batterId)
        val pitcher =
            gamePlayerRepository.findByIdOrNull(request.pitcherId)
                ?: throw GamePlayerNotFoundException(request.pitcherId)

        val warnings = mutableListOf<String>()

        detectBattingOrderViolation(game, batter, warnings)

        val scoredRunnerIds = mutableListOf<Long>()
        request.runnerMovements.forEach { movement ->
            if (movement.isScored) {
                scoredRunnerIds.add(movement.runnerId)
            }

            if (movement.isOut) {
                game.recordOut()
            } else if (!movement.isScored) {
                game.setRunner(movement.toBase, movement.runnerId)
            }
        }

        if (request.result.isOnBase) {
            val advanceToBase =
                when {
                    request.result.isSingle -> com.nextup.core.domain.game.Base.FIRST
                    request.result.isDouble -> com.nextup.core.domain.game.Base.SECOND
                    request.result.isTriple -> com.nextup.core.domain.game.Base.THIRD
                    request.result.isHomeRun -> {
                        scoredRunnerIds.add(batter.id)
                        null
                    }
                    else -> com.nextup.core.domain.game.Base.FIRST
                }

            advanceToBase?.let { game.setRunner(it, batter.id) }
        } else if (!request.result.isOnBase) {
            game.recordOut()
        }

        boxScoreService.updateOnPlateAppearance(
            gameId = gameId,
            batter = batter,
            pitcher = pitcher,
            result = request.result,
            rbis = request.getActualRbis(),
            runsScored = scoredRunnerIds,
            inning = game.currentInning,
        )

        eventPublisher.publishEvent(
            PlateAppearanceRecordedEvent(
                gameId = gameId,
                playerId = batter.player.id,
                pitcherId = pitcher.player.id,
                result = request.result,
            ),
        )

        // 끝내기(Walk-off) 감지: 득점 발생 시 홈팀 역전/리드 확인
        var walkOff = false
        if (scoredRunnerIds.isNotEmpty()) {
            val gameTeams = gameTeamRepository.findAllByGameId(gameId)
            if (game.isWalkOff(gameTeams)) {
                log.info(
                    "끝내기 감지: 경기 ID={}, {}회말 홈팀 역전 → 경기 자동 종료",
                    gameId,
                    game.currentInning,
                )
                game.finish(gameTeams)
                walkOff = true
                publishWalkOffGameResult(gameId, gameTeams)
            }
        }

        val outCountBefore = game.gameState.outs
        val runnersBeforeJson = buildRunnersJson(game)

        // 경기가 끝내기로 종료된 경우 타순/카운트 진행 불필요
        if (!walkOff) {
            game.advanceBatter()
            game.resetCount()
        }

        val savedGame = gameRepository.save(game)

        val gameEvent =
            GameEvent.createPlateAppearance(
                game = savedGame,
                batter = batter,
                pitcher = pitcher,
                result = request.result,
                description = buildPlateAppearanceDescription(request.result, batter, scoredRunnerIds),
                outCountBefore = outCountBefore,
                outCountAfter = savedGame.gameState.outs,
                runnersBeforeJson = runnersBeforeJson,
                runnersAfterJson = buildRunnersJson(savedGame),
                runsScored = scoredRunnerIds.size,
                rbis = request.getActualRbis(),
                scoringRunnerIds = scoredRunnerIds,
            )
        gameEventRepository.save(gameEvent)

        if (!walkOff) {
            detectPitchCountWarning(savedGame, pitcher, warnings)
        }

        return PlateAppearanceRecordResult(game = savedGame, warnings = warnings)
    }

    /**
     * 타순 위반을 감지하고 경고를 추가합니다 (D-17).
     */
    private fun detectBattingOrderViolation(
        game: Game,
        batter: GamePlayer,
        warnings: MutableList<String>,
    ) {
        val isHomeTeam = !game.isTopInning
        val violation =
            game.gameState.validateBattingOrder(
                batterBattingOrder = batter.battingOrder,
                isHomeTeam = isHomeTeam,
            ) ?: return

        val message =
            "타순 위반 감지: 예상 타순 ${violation.expectedBattingOrder}번, " +
                "실제 타자 타순 ${violation.actualBattingOrder}번 (타자 GamePlayer ID: ${batter.id})"
        log.warn(message)
        warnings.add(
            "타순 위반: 예상 타순 ${violation.expectedBattingOrder}번이지만 " +
                "${violation.actualBattingOrder}번 타자가 타석에 들어섰습니다.",
        )

        eventPublisher.publishEvent(
            BattingOrderViolationEvent(
                gameId = game.id,
                gamePlayerId = batter.id,
                playerId = batter.player.id,
                expectedBattingOrder = violation.expectedBattingOrder,
                actualBattingOrder = violation.actualBattingOrder,
                inning = game.currentInning,
                isTopInning = game.isTopInning,
            ),
        )
    }

    /**
     * 현재 투수의 투구 수 경고를 감지합니다 (D-20).
     */
    private fun detectPitchCountWarning(
        game: Game,
        pitcher: GamePlayer,
        warnings: MutableList<String>,
    ) {
        val pitchingRecord =
            pitchingRecordRepository.findByGamePlayer(pitcher)
                ?: return
        val pitchesThrown = pitchingRecord.pitchesThrown ?: return

        val gameRules = game.competition.gameRules
        val limit = gameRules.pitchCountLimit ?: DEFAULT_PITCH_COUNT_WARNING_THRESHOLD
        val threshold = gameRules.pitchCountWarningThreshold

        val status = pitchingRecord.checkPitchCountStatus(limit, threshold) ?: return

        when (status) {
            PitchCountStatus.LIMIT_REACHED -> {
                val logMessage =
                    "투구 수 경고: 투수(ID: ${pitcher.id}) 투구 수 ${pitchesThrown}구 — 제한(${limit}구) 도달"
                log.warn(logMessage)
                warnings.add(
                    "투구 수 경고: 현재 투수가 ${pitchesThrown}구를 던졌습니다. 투구 수 제한(${limit}구)에 도달했습니다.",
                )
                eventPublisher.publishEvent(
                    PitchCountWarningEvent(
                        gameId = game.id,
                        gamePlayerId = pitcher.id,
                        playerId = pitcher.player.id,
                        pitchesThrown = pitchesThrown,
                        pitchCountLimit = limit,
                        warningType = PitchCountWarningType.LIMIT_REACHED,
                    ),
                )
            }
            PitchCountStatus.APPROACHING_LIMIT -> {
                val remaining = limit - pitchesThrown
                warnings.add(
                    "투구 수 주의: 현재 투수가 ${pitchesThrown}구를 던졌습니다. " +
                        "제한(${limit}구)까지 ${remaining}구 남았습니다.",
                )
                eventPublisher.publishEvent(
                    PitchCountWarningEvent(
                        gameId = game.id,
                        gamePlayerId = pitcher.id,
                        playerId = pitcher.player.id,
                        pitchesThrown = pitchesThrown,
                        pitchCountLimit = limit,
                        warningType = PitchCountWarningType.APPROACHING_LIMIT,
                    ),
                )
            }
        }
    }

    private fun buildRunnersJson(game: Game): String? {
        val parts = mutableListOf<String>()
        game.gameState.runnerOnFirstId?.let { parts.add("1루:$it") }
        game.gameState.runnerOnSecondId?.let { parts.add("2루:$it") }
        game.gameState.runnerOnThirdId?.let { parts.add("3루:$it") }
        return if (parts.isEmpty()) null else parts.joinToString(",")
    }

    private fun buildPlateAppearanceDescription(
        result: PlateAppearanceResult,
        batter: GamePlayer,
        scoredRunnerIds: List<Long>,
    ): String {
        val base = "${batter.player.name} - ${result.displayName}"
        return if (scoredRunnerIds.isNotEmpty()) {
            "$base (득점: ${scoredRunnerIds.size}점)"
        } else {
            base
        }
    }

    /**
     * 끝내기로 경기 종료 시 결과 확정 이벤트를 발행합니다.
     */
    private fun publishWalkOffGameResult(
        gameId: Long,
        gameTeams: List<GameTeam>,
    ) {
        val homeTeam = gameTeams.find { it.homeAway == HomeAway.HOME } ?: return
        val awayTeam = gameTeams.find { it.homeAway == HomeAway.AWAY } ?: return
        eventPublisher.publishEvent(
            GameResultConfirmedEvent(
                gameId = gameId,
                homeTeamId = homeTeam.team.id,
                awayTeamId = awayTeam.team.id,
                homeScore = homeTeam.totalScore,
                awayScore = awayTeam.totalScore,
            ),
        )
    }

    private fun findGame(id: Long): Game =
        gameRepository.findByIdOrNull(id)
            ?: throw GameNotFoundException(id)

    companion object {
        /** 투구 수 경고 기본 임계값 (D-20: 사회인 야구 기준 100구) */
        const val DEFAULT_PITCH_COUNT_WARNING_THRESHOLD = 100

        /** 투구 수 접근 경고 임계값 (임계값까지 이 구수 이하 남으면 주의 경고) */
        const val PITCH_COUNT_APPROACHING_THRESHOLD = 20
    }
}
