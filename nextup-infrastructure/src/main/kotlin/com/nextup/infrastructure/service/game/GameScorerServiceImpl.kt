package com.nextup.infrastructure.service.game

import com.nextup.common.exception.BattingRecordNotFoundException
import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.GamePlayerNotFoundException
import com.nextup.common.exception.InvalidGameStateException
import com.nextup.common.exception.NoEventToUndoException
import com.nextup.common.exception.PitchingRecordNotFoundException
import com.nextup.common.exception.UndoNotAvailableException
import com.nextup.core.domain.event.PlateAppearanceRecordedEvent
import com.nextup.core.domain.event.PlateAppearanceUndoneEvent
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameEvent
import com.nextup.core.domain.game.GameEventType
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.service.game.BoxScoreService
import com.nextup.core.service.game.GameScorerService
import com.nextup.core.service.game.dto.GameEndReason
import com.nextup.core.service.game.dto.PlateAppearanceRequest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 기록원 전용 경기 기록 서비스 구현
 */
@Service
@Transactional(readOnly = true)
class GameScorerServiceImpl(
    private val gameRepository: GameRepositoryPort,
    private val gamePlayerRepository: GamePlayerRepositoryPort,
    private val gameTeamRepository: GameTeamRepositoryPort,
    private val boxScoreService: BoxScoreService,
    private val gameEventRepository: GameEventRepositoryPort,
    private val battingRecordRepository: BattingRecordRepositoryPort,
    private val pitchingRecordRepository: PitchingRecordRepositoryPort,
    private val eventPublisher: ApplicationEventPublisher,
) : GameScorerService {
    @Transactional
    override fun startGame(gameId: Long): Game {
        val game = findGame(gameId)

        // 경기 시작
        game.start()

        return gameRepository.save(game)
    }

    @Transactional
    override fun recordPlateAppearance(
        gameId: Long,
        request: PlateAppearanceRequest,
    ): Game {
        val game = findGame(gameId)

        // 경기가 진행 중인지 확인
        if (game.status != GameStatus.IN_PROGRESS) {
            throw InvalidGameStateException("진행 중인 경기만 타석 기록을 입력할 수 있습니다. 현재 상태: ${game.status.displayName}")
        }

        // 타자와 투수 조회
        val batter =
            gamePlayerRepository.findByIdOrNull(request.batterId)
                ?: throw GamePlayerNotFoundException(request.batterId)
        val pitcher =
            gamePlayerRepository.findByIdOrNull(request.pitcherId)
                ?: throw GamePlayerNotFoundException(request.pitcherId)

        // 주자 이동 처리
        val scoredRunnerIds = mutableListOf<Long>()
        request.runnerMovements.forEach { movement ->
            if (movement.isScored) {
                scoredRunnerIds.add(movement.runnerId)
            }

            // 베이스 상태 업데이트
            if (movement.isOut) {
                game.recordOut()
            } else if (!movement.isScored) {
                game.setRunner(movement.toBase, movement.runnerId)
            }
        }

        // 타자의 출루 처리
        if (request.result.isOnBase) {
            val advanceToBase =
                when {
                    request.result.isSingle -> com.nextup.core.domain.game.Base.FIRST
                    request.result.isDouble -> com.nextup.core.domain.game.Base.SECOND
                    request.result.isTriple -> com.nextup.core.domain.game.Base.THIRD
                    request.result.isHomeRun -> {
                        // 홈런인 경우 타자도 득점
                        scoredRunnerIds.add(batter.id)
                        null
                    }
                    else -> com.nextup.core.domain.game.Base.FIRST // 볼넷, 사구 등
                }

            advanceToBase?.let { game.setRunner(it, batter.id) }
        } else if (!request.result.isOnBase) {
            // 아웃인 경우
            game.recordOut()
        }

        // 박스스코어 갱신
        boxScoreService.updateOnPlateAppearance(
            gameId = gameId,
            batter = batter,
            pitcher = pitcher,
            result = request.result,
            rbis = request.getActualRbis(),
            runsScored = scoredRunnerIds,
            inning = game.currentInning,
        )

        // 실시간 통계 갱신 이벤트 발행
        eventPublisher.publishEvent(
            PlateAppearanceRecordedEvent(
                gameId = gameId,
                playerId = batter.player.id,
                result = request.result,
            ),
        )

        // 다음 타자로 진행
        game.advanceBatter()
        game.resetCount()

        return gameRepository.save(game)
    }

    @Transactional
    override fun advanceHalfInning(gameId: Long): Game {
        val game = findGame(gameId)

        // 경기가 진행 중인지 확인
        if (game.status != GameStatus.IN_PROGRESS) {
            throw InvalidGameStateException("진행 중인 경기만 이닝을 진행할 수 있습니다. 현재 상태: ${game.status.displayName}")
        }

        // 다음 반 이닝으로 진행
        game.nextHalfInning()

        return gameRepository.save(game)
    }

    @Transactional
    override fun endGame(
        gameId: Long,
        reason: GameEndReason,
    ): Game {
        val game = findGame(gameId)

        // 경기가 진행 중인지 확인
        if (game.status != GameStatus.IN_PROGRESS) {
            throw InvalidGameStateException("진행 중인 경기만 종료할 수 있습니다. 현재 상태: ${game.status.displayName}")
        }

        when (reason) {
            GameEndReason.REGULATION -> game.finish()
            GameEndReason.MERCY_RULE -> game.callGame("콜드게임 (점수차)")
            GameEndReason.WEATHER -> game.callGame("콜드게임 (기상 조건)")
            GameEndReason.FORFEIT -> throw InvalidGameStateException(
                "몰수 처리는 전용 API를 사용해주세요.",
            )
            GameEndReason.OTHER -> game.callGame("기타 사유")
        }

        return gameRepository.save(game)
    }

    @Transactional
    override fun undoLastEvent(gameId: Long): GameEvent {
        val game = findGame(gameId)

        // 경기가 진행 중인지 확인
        if (!game.canUndo()) {
            throw UndoNotAvailableException(
                "진행 중인 경기만 Undo할 수 있습니다. 현재 상태: ${game.status.displayName}",
            )
        }

        // 마지막 활성 이벤트 조회
        val lastEvent =
            gameEventRepository.findLastActiveEvent(gameId)
                ?: throw NoEventToUndoException()

        // 이벤트 타입에 따른 롤백 처리
        when (lastEvent.eventType) {
            GameEventType.PLATE_APPEARANCE -> undoPlateAppearance(game, lastEvent)
            GameEventType.INNING_CHANGE -> undoInningChange(game, lastEvent)
            else -> {
                // GAME_STATUS, BASE_RUNNING 등은 단순 마킹만 처리
            }
        }

        // 이벤트를 undone으로 마킹
        lastEvent.markUndone()
        gameEventRepository.save(lastEvent)

        // 게임 상태 저장
        gameRepository.save(game)

        // 타석 결과 Undo인 경우 실시간 통계 역산 이벤트 발행
        if (lastEvent.eventType == GameEventType.PLATE_APPEARANCE) {
            lastEvent.plateAppearanceResult?.let { result ->
                lastEvent.batter?.let { batter ->
                    eventPublisher.publishEvent(
                        PlateAppearanceUndoneEvent(
                            gameId = gameId,
                            playerId = batter.player.id,
                            result = result,
                        ),
                    )
                }
            }
        }

        return lastEvent
    }

    private fun undoPlateAppearance(
        game: Game,
        event: GameEvent,
    ) {
        val result = event.plateAppearanceResult ?: return

        // 1. GameState 복원 - 아웃 카운트와 주자 상태
        game.restoreInningState(
            inning = event.inning,
            isTop = event.isTopInning,
            outs = event.outCountBefore,
        )
        game.gameState.restoreRunners(event.runnersBeforeJson)
        game.gameState.resetCount()

        // 2. 타순 롤백
        game.revertBatter()

        // 3. 타격 기록 롤백
        event.batter?.let { batter ->
            val battingRecord =
                battingRecordRepository.findByGamePlayer(batter)
                    ?: throw BattingRecordNotFoundException(batter.id)

            battingRecord.revertPlateAppearanceResult(result, event.rbis)
        }

        // 4. 득점한 주자들의 득점 기록 롤백
        if (event.runsScored > 0) {
            // 홈런인 경우 타자 자신의 득점도 롤백 (revertPlateAppearanceResult에서 처리됨)
            // 다른 주자들의 득점 롤백은 runnersBeforeJson/runnersAfterJson 비교로 처리
            // runsScored 수만큼 팀 점수 차감
            event.batter?.let { batter ->
                batter.gameTeam.subtractRunInInning(event.inning, event.runsScored)
            }
        }

        // 5. 투수 기록 롤백
        event.pitcher?.let { pitcher ->
            val pitchingRecord =
                pitchingRecordRepository.findByGamePlayer(pitcher)
                    ?: throw PitchingRecordNotFoundException(pitcher.id)

            pitchingRecord.revertBatterFaced(result)

            // 아웃이었으면 아웃 카운트도 롤백
            if (!result.isOnBase) {
                pitchingRecord.revertOut()
            }

            // 득점이 있었으면 투수 실점도 롤백
            if (event.runsScored > 0) {
                pitchingRecord.revertEarnedRun(event.runsScored)
            }
        }

        // 6. 안타였으면 팀 안타 수 차감
        if (result.isHit) {
            event.batter?.let { batter ->
                batter.gameTeam.subtractHit()
            }
        }
    }

    private fun undoInningChange(
        game: Game,
        event: GameEvent,
    ) {
        // 이닝 전환 이벤트 롤백: 이전 이닝/상태로 복원
        game.restoreInningState(
            inning = event.inning,
            isTop = event.isTopInning,
            outs = event.outCountBefore,
        )
        game.gameState.restoreRunners(event.runnersBeforeJson)
    }

    @Transactional
    override fun forfeitGame(
        gameId: Long,
        winnerTeamId: Long,
        reason: String,
    ): Game {
        val game = findGame(gameId)

        // 경기 상태 확인 (예정 또는 진행 중인 경기만 몰수 처리 가능)
        if (game.status != GameStatus.SCHEDULED && game.status != GameStatus.IN_PROGRESS) {
            throw InvalidGameStateException(
                "예정 또는 진행 중인 경기만 몰수 처리할 수 있습니다. 현재 상태: ${game.status.displayName}",
            )
        }

        // 해당 경기에 참여하는 GameTeam 조회
        val gameTeams = gameTeamRepository.findAllByGameId(gameId)

        if (gameTeams.size != 2) {
            throw InvalidGameStateException(
                "몰수 처리를 위해서는 정확히 2개의 팀이 필요합니다. 현재 팀 수: ${gameTeams.size}",
            )
        }

        // 몰수 처리 (7:0 점수 자동 반영)
        game.forfeit(
            winnerTeamId = winnerTeamId,
            reason = reason,
            gameTeams = gameTeams,
        )

        return gameRepository.save(game)
    }

    private fun findGame(id: Long): Game =
        gameRepository.findByIdOrNull(id)
            ?: throw GameNotFoundException(id)
}
