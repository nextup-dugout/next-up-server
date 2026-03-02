package com.nextup.infrastructure.service.game

import com.nextup.common.exception.BattingRecordNotFoundException
import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.GamePlayerNotFoundException
import com.nextup.common.exception.InvalidGameStateException
import com.nextup.common.exception.NoEventToUndoException
import com.nextup.common.exception.PitchingRecordNotFoundException
import com.nextup.common.exception.UndoNotAvailableException
import com.nextup.core.domain.event.BattingOrderViolationEvent
import com.nextup.core.domain.event.GameCancelledEvent
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.domain.event.PitchCountWarningEvent
import com.nextup.core.domain.event.PitchCountWarningType
import com.nextup.core.domain.event.PlateAppearanceRecordedEvent
import com.nextup.core.domain.event.PlateAppearanceUndoneEvent
import com.nextup.core.domain.game.Base
import com.nextup.core.domain.game.BaseRunningResult
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameEvent
import com.nextup.core.domain.game.GameEventType
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameResult
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.domain.game.PitchCountStatus
import com.nextup.core.domain.game.PitchingDecisionCalculator
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.service.game.BoxScoreService
import com.nextup.core.service.game.GameScorerService
import com.nextup.core.service.game.dto.BaseRunningRequest
import com.nextup.core.service.game.dto.GameEndReason
import com.nextup.core.service.game.dto.PlateAppearanceRecordResult
import com.nextup.core.service.game.dto.PlateAppearanceRequest
import com.nextup.core.service.game.dto.SubstitutionRequest
import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(javaClass)

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
    ): PlateAppearanceRecordResult {
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

        // 경고 수집
        val warnings = mutableListOf<String>()

        // 타순 위반 감지 (차단하지 않음 — 사회인 야구 유연성)
        detectBattingOrderViolation(game, batter, warnings)

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

        // 현재 주자 상태 스냅샷 (이벤트 기록용)
        val outCountBefore = game.gameState.outs
        val runnersBeforeJson = buildRunnersJson(game)

        // 다음 타자로 진행
        game.advanceBatter()
        game.resetCount()

        val savedGame = gameRepository.save(game)

        // D-15: 타석 이벤트 기록 (득점 주자 ID 포함)
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

        // 투구 수 경고 감지 (저장 후 현재 투수 기록 기반으로 확인) (D-20)
        detectPitchCountWarning(savedGame, pitcher, warnings)

        return PlateAppearanceRecordResult(game = savedGame, warnings = warnings)
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
            GameEndReason.MERCY_RULE -> game.callGame(reason = "콜드게임 (점수차)")
            GameEndReason.WEATHER -> game.callGame(reason = "콜드게임 (기상 조건)")
            GameEndReason.FORFEIT -> throw InvalidGameStateException(
                "몰수 처리는 전용 API를 사용해주세요.",
            )
            GameEndReason.OTHER -> game.callGame(reason = "기타 사유")
        }

        val savedGame = gameRepository.save(game)

        // 투수 승/패/세이브/홀드 자동 결정
        assignPitchingDecisions(gameId)

        publishGameResultEvent(gameId)
        return savedGame
    }

    /**
     * 경기 종료 시 투수 결정(승/패/세이브/홀드)을 자동으로 할당합니다.
     */
    private fun assignPitchingDecisions(gameId: Long) {
        val gameTeams = gameTeamRepository.findAllByGameId(gameId)
        if (gameTeams.size != 2) return

        val homeTeam = gameTeams.find { it.homeAway == HomeAway.HOME } ?: return
        val awayTeam = gameTeams.find { it.homeAway == HomeAway.AWAY } ?: return

        // 무승부 여부 판단
        val winnerTeam =
            when {
                homeTeam.totalScore > awayTeam.totalScore -> homeTeam
                awayTeam.totalScore > homeTeam.totalScore -> awayTeam
                else -> null
            }
        val loserTeam =
            when {
                homeTeam.result == GameResult.LOSS -> homeTeam
                awayTeam.result == GameResult.LOSS -> awayTeam
                winnerTeam == homeTeam -> awayTeam
                winnerTeam == awayTeam -> homeTeam
                else -> null
            }

        val allPitchingRecords = pitchingRecordRepository.findAllByGameId(gameId)
        if (allPitchingRecords.isEmpty()) return

        val decisions =
            PitchingDecisionCalculator.calculate(
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                allPitchingRecords = allPitchingRecords,
            )

        decisions.forEach { (record, decision) ->
            when (decision) {
                com.nextup.core.domain.game.PitchingDecision.WIN -> record.assignWin()
                com.nextup.core.domain.game.PitchingDecision.LOSS -> record.assignLoss()
                com.nextup.core.domain.game.PitchingDecision.SAVE -> record.assignSave()
                com.nextup.core.domain.game.PitchingDecision.HOLD -> record.assignHold()
                com.nextup.core.domain.game.PitchingDecision.BLOWN_SAVE -> record.assignBlownSave()
                com.nextup.core.domain.game.PitchingDecision.NONE -> { /* no-op */ }
            }
            pitchingRecordRepository.save(record)
        }
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
                // GAME_STATUS, BASE_RUNNING, POSITION_CHANGE 등은 단순 마킹만 처리
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
    override fun recordBaseRunning(
        gameId: Long,
        request: BaseRunningRequest,
    ): GameEvent {
        val game = findGame(gameId)

        if (game.status != GameStatus.IN_PROGRESS) {
            throw InvalidGameStateException(
                "진행 중인 경기만 주루 기록을 입력할 수 있습니다. 현재 상태: ${game.status.displayName}",
            )
        }

        val runner =
            gamePlayerRepository.findByIdOrNull(request.runnerId)
                ?: throw GamePlayerNotFoundException(request.runnerId)

        val outCountBefore = game.gameState.outs

        // 주루 플레이 전 주자 상태 스냅샷
        val runnersBeforeJson = serializeRunners(game.gameState)

        // 주루 플레이 결과에 따른 처리
        val outCountAfter: Int
        when (request.result) {
            BaseRunningResult.STOLEN_BASE,
            BaseRunningResult.ADVANCED_ON_ERROR,
            BaseRunningResult.ADVANCED_ON_WILD_PITCH,
            BaseRunningResult.ADVANCED_ON_PASSED_BALL,
            BaseRunningResult.ADVANCED_ON_BALK,
            -> {
                // 진루: 출발 베이스 비우고 도착 베이스에 주자 배치
                game.gameState.setRunner(request.fromBase, null)
                if (request.toBase != Base.HOME) {
                    game.gameState.setRunner(request.toBase, runner.id)
                }
                if (request.result == BaseRunningResult.STOLEN_BASE) {
                    val battingRecord =
                        battingRecordRepository.findByGamePlayer(runner)
                            ?: throw BattingRecordNotFoundException(runner.id)
                    battingRecord.recordStolenBase()
                }
                outCountAfter = game.gameState.outs
            }
            BaseRunningResult.CAUGHT_STEALING,
            BaseRunningResult.PICKED_OFF,
            -> {
                // 아웃: 출발 베이스 비우고 아웃 카운트 증가
                game.gameState.setRunner(request.fromBase, null)
                game.recordOut()
                if (request.result == BaseRunningResult.CAUGHT_STEALING) {
                    val battingRecord =
                        battingRecordRepository.findByGamePlayer(runner)
                            ?: throw BattingRecordNotFoundException(runner.id)
                    battingRecord.recordCaughtStealing()
                }
                outCountAfter = game.gameState.outs
            }
        }

        val runnersAfterJson = serializeRunners(game.gameState)

        val description = buildBaseRunningDescription(request)

        val event =
            GameEvent.createBaseRunning(
                game = game,
                runner = runner,
                fromBase = request.fromBase,
                toBase = request.toBase,
                result = request.result,
                description = description,
                outCountBefore = outCountBefore,
                outCountAfter = outCountAfter,
                runnersBeforeJson = runnersBeforeJson,
                runnersAfterJson = runnersAfterJson,
            )

        val savedEvent = gameEventRepository.save(event)
        gameRepository.save(game)
        return savedEvent
    }

    private fun serializeRunners(gameState: com.nextup.core.domain.game.GameState): String? {
        val entries =
            buildList {
                gameState.runnerOnFirstId?.let { add("1루:$it") }
                gameState.runnerOnSecondId?.let { add("2루:$it") }
                gameState.runnerOnThirdId?.let { add("3루:$it") }
            }
        return if (entries.isEmpty()) null else entries.joinToString(",")
    }

    private fun buildBaseRunningDescription(request: BaseRunningRequest): String {
        val fromDisplay = baseDisplay(request.fromBase)
        val toDisplay = baseDisplay(request.toBase)
        return "${request.result.displayName}: $fromDisplay → $toDisplay"
    }

    private fun baseDisplay(base: Base): String =
        when (base) {
            Base.FIRST -> "1루"
            Base.SECOND -> "2루"
            Base.THIRD -> "3루"
            Base.HOME -> "홈"
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

        val savedGame = gameRepository.save(game)
        publishGameResultEvent(gameId)
        return savedGame
    }

    @Transactional
    override fun cancelGame(
        gameId: Long,
        reason: String?,
    ): Game {
        val game = findGame(gameId)

        if (game.status != GameStatus.SCHEDULED && game.status != GameStatus.POSTPONED) {
            throw InvalidGameStateException(
                "예정 또는 연기 상태의 경기만 취소할 수 있습니다. 현재 상태: ${game.status.displayName}",
            )
        }

        game.cancel(reason)
        val savedGame = gameRepository.save(game)

        eventPublisher.publishEvent(GameCancelledEvent(gameId = gameId))

        return savedGame
    }

    @Transactional
    override fun substitutePlayer(
        gameId: Long,
        request: SubstitutionRequest,
    ): GameEvent {
        val game = findGame(gameId)

        // 경기가 진행 중인지 확인
        if (game.status != GameStatus.IN_PROGRESS) {
            throw InvalidGameStateException(
                "진행 중인 경기만 선수 교체를 할 수 있습니다. 현재 상태: ${game.status.displayName}",
            )
        }

        // 교체 나가는 선수 조회
        val outgoingPlayer =
            gamePlayerRepository.findByIdOrNull(request.outgoingPlayerId)
                ?: throw GamePlayerNotFoundException(request.outgoingPlayerId)

        // 교체 들어오는 선수 조회
        val incomingPlayer =
            gamePlayerRepository.findByIdOrNull(request.incomingPlayerId)
                ?: throw GamePlayerNotFoundException(request.incomingPlayerId)

        // 퇴장한 선수 재출전 방지 (enterAsSubstitute 호출 전 사전 검증)
        if (incomingPlayer.hasExited) {
            throw InvalidGameStateException(
                "이미 퇴장한 선수는 재출전할 수 없습니다. (GamePlayer ID: ${request.incomingPlayerId})",
            )
        }

        // 나가는 선수가 현재 출전 중인지 확인
        if (!outgoingPlayer.isCurrentlyPlaying) {
            throw InvalidGameStateException(
                "현재 출전 중인 선수만 교체할 수 있습니다. (GamePlayer ID: ${request.outgoingPlayerId})",
            )
        }

        // DH 해제 규칙 검증
        if (outgoingPlayer.isDesignatedHitter) {
            try {
                outgoingPlayer.validateDhRelease(incomingPlayer, request.newBattingOrder)
            } catch (e: IllegalArgumentException) {
                throw InvalidGameStateException(e.message ?: "DH 해제 규칙 위반")
            }
        }

        // DH 해제 처리
        val dhReleaseRequired = outgoingPlayer.isDhReleaseRequired(incomingPlayer)
        if (dhReleaseRequired) {
            outgoingPlayer.releaseDH()
            gamePlayerRepository.save(outgoingPlayer)
        }

        // 교체 나가는 선수 퇴장 처리
        outgoingPlayer.exitGame(game.currentInning)
        gamePlayerRepository.save(outgoingPlayer)

        // 교체 들어오는 선수 출전 처리
        incomingPlayer.enterAsSubstitute(
            inning = game.currentInning,
            newPosition = request.newPosition,
            newBattingOrder = request.newBattingOrder,
        )
        gamePlayerRepository.save(incomingPlayer)

        // 교체 이벤트 설명 생성
        val halfInning = if (game.isTopInning) "초" else "말"
        val dhReleaseNote = if (dhReleaseRequired) " (DH 규칙 해제)" else ""
        val description =
            "${game.currentInning}회$halfInning: ${outgoingPlayer.player.name} → ${incomingPlayer.player.name} (${request.newPosition.displayName})$dhReleaseNote"

        // 교체 이벤트 생성 및 저장
        val substitutionEvent =
            GameEvent.createSubstitution(
                game = game,
                incomingPlayer = incomingPlayer,
                outgoingPlayer = outgoingPlayer,
                description = description,
            )

        return gameEventRepository.save(substitutionEvent)
    }

    /**
     * 타순 위반을 감지하고 경고를 추가합니다 (D-17).
     *
     * 사회인 야구의 유연성을 위해 차단하지 않고 경고 이벤트를 발행합니다.
     *
     * @param game 현재 경기
     * @param batter 타자 GamePlayer
     * @param warnings 경고 메시지 수집 목록
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
     *
     * pitchesThrown이 기록된 경우에만 검사합니다.
     * GameRules의 pitchCountLimit을 우선 사용하고, 없으면 기본값을 사용합니다.
     *
     * @param game 현재 경기
     * @param pitcher 현재 투수 GamePlayer
     * @param warnings 경고 메시지 수집 목록
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

    /**
     * 현재 주자 상태를 JSON 문자열로 직렬화합니다.
     * 형식: "1루:playerId,2루:playerId,3루:playerId"
     */
    private fun buildRunnersJson(game: Game): String? {
        val parts = mutableListOf<String>()
        game.gameState.runnerOnFirstId?.let { parts.add("1루:$it") }
        game.gameState.runnerOnSecondId?.let { parts.add("2루:$it") }
        game.gameState.runnerOnThirdId?.let { parts.add("3루:$it") }
        return if (parts.isEmpty()) null else parts.joinToString(",")
    }

    /**
     * 타석 결과 이벤트 설명 문자열을 생성합니다.
     */
    private fun buildPlateAppearanceDescription(
        result: com.nextup.core.domain.game.PlateAppearanceResult,
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

    private fun publishGameResultEvent(gameId: Long) {
        val gameTeams = gameTeamRepository.findAllByGameId(gameId)
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
