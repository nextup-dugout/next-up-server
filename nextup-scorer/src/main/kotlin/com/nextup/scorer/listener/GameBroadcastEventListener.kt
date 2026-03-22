package com.nextup.scorer.listener

import com.nextup.core.domain.event.GameEndedEvent
import com.nextup.core.domain.event.GameStartedEvent
import com.nextup.core.domain.event.HalfInningAdvancedEvent
import com.nextup.core.domain.event.PitchCountWarningEvent
import com.nextup.core.domain.event.PitchCountWarningType
import com.nextup.core.domain.event.PlateAppearanceRecordedEvent
import com.nextup.core.domain.event.PlayerSubstitutedEvent
import com.nextup.core.domain.event.PositionChangedEvent
import com.nextup.core.domain.event.RecordCorrectedEvent
import com.nextup.core.domain.event.TimeLimitWarningEvent
import com.nextup.core.domain.event.TimeLimitWarningType
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.scorer.dto.websocket.GameEventMessage
import com.nextup.scorer.dto.websocket.GameStateMessage
import com.nextup.scorer.dto.websocket.InningScoresDto
import com.nextup.scorer.dto.websocket.PlayerBriefDto
import com.nextup.scorer.dto.websocket.RunnersDto
import com.nextup.scorer.dto.websocket.ScoreboardMessage
import com.nextup.scorer.dto.websocket.TeamScoreDto
import com.nextup.scorer.dto.websocket.WarningMessage
import com.nextup.scorer.dto.websocket.WarningSeverity
import com.nextup.scorer.dto.websocket.WarningType
import com.nextup.scorer.service.websocket.GameBroadcastService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.Instant

/**
 * 경기 브로드캐스트 이벤트 리스너
 *
 * 경기 도메인 이벤트를 수신하여 WebSocket으로 실시간 브로드캐스트합니다.
 * AFTER_COMMIT 단계에서 실행되므로 DB 커밋 이후 별도 트랜잭션으로 게임 상태를 재조회합니다.
 *
 * 각 핸들러는 try-catch로 감싸져 WebSocket 전송 실패가 원본 트랜잭션에 영향을 주지 않습니다.
 */
@Component
class GameBroadcastEventListener(
    private val gameRepository: GameRepositoryPort,
    private val gameTeamRepository: GameTeamRepositoryPort,
    private val gamePlayerRepository: GamePlayerRepositoryPort,
    private val gameEventRepository: GameEventRepositoryPort,
    private val gameBroadcastService: GameBroadcastService,
) {
    private val log = LoggerFactory.getLogger(GameBroadcastEventListener::class.java)

    /**
     * 경기 시작 이벤트를 처리합니다.
     *
     * broadcastEvent(GAME_START) + broadcastState + broadcastScoreboard
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onGameStarted(event: GameStartedEvent) {
        try {
            val game =
                gameRepository.findByIdOrNull(event.gameId)
                    ?: run {
                        log.warn("GameStartedEvent: 경기를 찾을 수 없음 (gameId={})", event.gameId)
                        return
                    }
            val gameTeams = gameTeamRepository.findAllByGameId(event.gameId)

            val startEvent =
                GameEventMessage(
                    eventId = 0L,
                    eventType = "GAME_START",
                    inning = game.currentInning,
                    isTopInning = game.isTopInning,
                    description = "경기 시작",
                    batter = null,
                    pitcher = null,
                    result = null,
                    runsScored = 0,
                    timestamp = Instant.now(),
                )
            gameBroadcastService.broadcastEvent(event.gameId, startEvent)
            gameBroadcastService.broadcastState(event.gameId, buildGameStateMessage(event.gameId, game, gameTeams))
            gameBroadcastService.broadcastScoreboard(
                event.gameId,
                buildScoreboardMessage(event.gameId, game, gameTeams)
            )

            log.debug("경기 시작 브로드캐스트 완료 (gameId={})", event.gameId)
        } catch (ex: Exception) {
            log.error("GameStartedEvent 브로드캐스트 실패 (gameId={}): {}", event.gameId, ex.message, ex)
        }
    }

    /**
     * 타석 결과 기록 이벤트를 처리합니다.
     *
     * broadcastState + broadcastScoreboard
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onPlateAppearanceRecorded(event: PlateAppearanceRecordedEvent) {
        try {
            val game =
                gameRepository.findByIdOrNull(event.gameId)
                    ?: run {
                        log.warn("PlateAppearanceRecordedEvent: 경기를 찾을 수 없음 (gameId={})", event.gameId)
                        return
                    }
            val gameTeams = gameTeamRepository.findAllByGameId(event.gameId)

            gameBroadcastService.broadcastState(event.gameId, buildGameStateMessage(event.gameId, game, gameTeams))
            gameBroadcastService.broadcastScoreboard(
                event.gameId,
                buildScoreboardMessage(event.gameId, game, gameTeams)
            )

            log.debug("타석 결과 브로드캐스트 완료 (gameId={}, result={})", event.gameId, event.result)
        } catch (ex: Exception) {
            log.error("PlateAppearanceRecordedEvent 브로드캐스트 실패 (gameId={}): {}", event.gameId, ex.message, ex)
        }
    }

    /**
     * 이닝 진행 이벤트를 처리합니다.
     *
     * broadcastEvent(INNING_CHANGE) + broadcastState + broadcastScoreboard
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onHalfInningAdvanced(event: HalfInningAdvancedEvent) {
        try {
            val game =
                gameRepository.findByIdOrNull(event.gameId)
                    ?: run {
                        log.warn("HalfInningAdvancedEvent: 경기를 찾을 수 없음 (gameId={})", event.gameId)
                        return
                    }
            val gameTeams = gameTeamRepository.findAllByGameId(event.gameId)

            val halfText = if (event.newIsTopInning) "초" else "말"
            val inningChangeEvent =
                GameEventMessage(
                    eventId = 0L,
                    eventType = "INNING_CHANGE",
                    inning = event.newInning,
                    isTopInning = event.newIsTopInning,
                    description = "${event.newInning}회$halfText 시작",
                    batter = null,
                    pitcher = null,
                    result = null,
                    runsScored = 0,
                    timestamp = Instant.now(),
                )
            gameBroadcastService.broadcastEvent(event.gameId, inningChangeEvent)
            gameBroadcastService.broadcastState(event.gameId, buildGameStateMessage(event.gameId, game, gameTeams))
            gameBroadcastService.broadcastScoreboard(
                event.gameId,
                buildScoreboardMessage(event.gameId, game, gameTeams)
            )

            log.debug(
                "이닝 변경 브로드캐스트 완료 (gameId={}, inning={}, isTop={})",
                event.gameId,
                event.newInning,
                event.newIsTopInning,
            )
        } catch (ex: Exception) {
            log.error("HalfInningAdvancedEvent 브로드캐스트 실패 (gameId={}): {}", event.gameId, ex.message, ex)
        }
    }

    /**
     * 경기 종료 이벤트를 처리합니다.
     *
     * broadcastEvent(GAME_END) + broadcastScoreboard
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onGameEnded(event: GameEndedEvent) {
        try {
            val game =
                gameRepository.findByIdOrNull(event.gameId)
                    ?: run {
                        log.warn("GameEndedEvent: 경기를 찾을 수 없음 (gameId={})", event.gameId)
                        return
                    }
            val gameTeams = gameTeamRepository.findAllByGameId(event.gameId)

            val endEvent =
                GameEventMessage(
                    eventId = 0L,
                    eventType = "GAME_END",
                    inning = game.currentInning,
                    isTopInning = game.isTopInning,
                    description = "경기 종료 (${event.finalStatus})",
                    batter = null,
                    pitcher = null,
                    result = null,
                    runsScored = 0,
                    timestamp = Instant.now(),
                )
            gameBroadcastService.broadcastEvent(event.gameId, endEvent)
            gameBroadcastService.broadcastScoreboard(
                event.gameId,
                buildScoreboardMessage(event.gameId, game, gameTeams)
            )

            log.debug("경기 종료 브로드캐스트 완료 (gameId={}, status={})", event.gameId, event.finalStatus)
        } catch (ex: Exception) {
            log.error("GameEndedEvent 브로드캐스트 실패 (gameId={}): {}", event.gameId, ex.message, ex)
        }
    }

    /**
     * 선수 교체 이벤트를 처리합니다.
     *
     * broadcastEvent(SUBSTITUTION) + broadcastState
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onPlayerSubstituted(event: PlayerSubstitutedEvent) {
        try {
            val game =
                gameRepository.findByIdOrNull(event.gameId)
                    ?: run {
                        log.warn("PlayerSubstitutedEvent: 경기를 찾을 수 없음 (gameId={})", event.gameId)
                        return
                    }
            val gameTeams = gameTeamRepository.findAllByGameId(event.gameId)
            val gameEvent = gameEventRepository.findByIdOrNull(event.gameEventId)

            val substitutionEvent =
                GameEventMessage(
                    eventId = event.gameEventId,
                    eventType = "SUBSTITUTION",
                    inning = game.currentInning,
                    isTopInning = game.isTopInning,
                    description = gameEvent?.description ?: "선수 교체",
                    batter = null,
                    pitcher = null,
                    result = null,
                    runsScored = 0,
                    timestamp = Instant.now(),
                )
            gameBroadcastService.broadcastEvent(event.gameId, substitutionEvent)
            gameBroadcastService.broadcastState(event.gameId, buildGameStateMessage(event.gameId, game, gameTeams))

            log.debug("선수 교체 브로드캐스트 완료 (gameId={}, gameEventId={})", event.gameId, event.gameEventId)
        } catch (ex: Exception) {
            log.error("PlayerSubstitutedEvent 브로드캐스트 실패 (gameId={}): {}", event.gameId, ex.message, ex)
        }
    }

    /**
     * 포지션 변경 이벤트를 처리합니다.
     *
     * broadcastEvent(POSITION_CHANGE) + broadcastState
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onPositionChanged(event: PositionChangedEvent) {
        try {
            val game =
                gameRepository.findByIdOrNull(event.gameId)
                    ?: run {
                        log.warn("PositionChangedEvent: 경기를 찾을 수 없음 (gameId={})", event.gameId)
                        return
                    }
            val gameTeams = gameTeamRepository.findAllByGameId(event.gameId)
            val gameEvent = gameEventRepository.findByIdOrNull(event.gameEventId)

            val positionChangeEvent =
                GameEventMessage(
                    eventId = event.gameEventId,
                    eventType = "POSITION_CHANGE",
                    inning = game.currentInning,
                    isTopInning = game.isTopInning,
                    description = gameEvent?.description ?: "포지션 변경",
                    batter = null,
                    pitcher = null,
                    result = null,
                    runsScored = 0,
                    timestamp = Instant.now(),
                )
            gameBroadcastService.broadcastEvent(event.gameId, positionChangeEvent)
            gameBroadcastService.broadcastState(event.gameId, buildGameStateMessage(event.gameId, game, gameTeams))

            log.debug("포지션 변경 브로드캐스트 완료 (gameId={}, gameEventId={})", event.gameId, event.gameEventId)
        } catch (ex: Exception) {
            log.error("PositionChangedEvent 브로드캐스트 실패 (gameId={}): {}", event.gameId, ex.message, ex)
        }
    }

    /**
     * 기록 정정 이벤트를 처리합니다.
     *
     * broadcastState + broadcastScoreboard
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onRecordCorrected(event: RecordCorrectedEvent) {
        try {
            val game =
                gameRepository.findByIdOrNull(event.gameId)
                    ?: run {
                        log.warn("RecordCorrectedEvent: 경기를 찾을 수 없음 (gameId={})", event.gameId)
                        return
                    }
            val gameTeams = gameTeamRepository.findAllByGameId(event.gameId)

            gameBroadcastService.broadcastState(event.gameId, buildGameStateMessage(event.gameId, game, gameTeams))
            gameBroadcastService.broadcastScoreboard(
                event.gameId,
                buildScoreboardMessage(event.gameId, game, gameTeams)
            )

            log.debug(
                "기록 정정 브로드캐스트 완료 (gameId={}, correctionType={})",
                event.gameId,
                event.correctionType,
            )
        } catch (ex: Exception) {
            log.error("RecordCorrectedEvent 브로드캐스트 실패 (gameId={}): {}", event.gameId, ex.message, ex)
        }
    }

    /**
     * 투구수 제한 경고 이벤트를 처리합니다.
     *
     * broadcastWarning(PITCH_COUNT)
     * LIMIT_REACHED 시 투수 교체 권고 메시지를 포함합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onPitchCountWarning(event: PitchCountWarningEvent) {
        try {
            val severity =
                when (event.warningType) {
                    PitchCountWarningType.LIMIT_REACHED -> WarningSeverity.CRITICAL
                    PitchCountWarningType.APPROACHING_LIMIT -> WarningSeverity.WARNING
                }
            val title = event.warningType.displayName
            val message =
                when (event.warningType) {
                    PitchCountWarningType.LIMIT_REACHED ->
                        "투수가 투구 수 제한(${event.pitchCountLimit}구)에 도달했습니다" +
                            "(현재 ${event.pitchesThrown}구). 투수 교체를 권고합니다."
                    PitchCountWarningType.APPROACHING_LIMIT ->
                        "투수가 ${event.pitchesThrown}구를 던졌습니다. " +
                            "제한(${event.pitchCountLimit}구)까지 ${event.remainingPitches}구 남았습니다."
                }
            val details =
                mapOf(
                    "gamePlayerId" to event.gamePlayerId,
                    "playerId" to event.playerId,
                    "pitchesThrown" to event.pitchesThrown,
                    "pitchCountLimit" to event.pitchCountLimit,
                    "remainingPitches" to event.remainingPitches,
                    "isSubstitutionRecommended" to event.isSubstitutionRecommended,
                )

            val warningMessage =
                WarningMessage(
                    gameId = event.gameId,
                    warningType = WarningType.PITCH_COUNT,
                    severity = severity,
                    title = title,
                    message = message,
                    details = details,
                    timestamp = Instant.now(),
                )
            gameBroadcastService.broadcastWarning(event.gameId, warningMessage)

            log.debug(
                "투구수 경고 브로드캐스트 완료 (gameId={}, pitchesThrown={}, limit={}, type={})",
                event.gameId,
                event.pitchesThrown,
                event.pitchCountLimit,
                event.warningType,
            )
        } catch (ex: Exception) {
            log.error("PitchCountWarningEvent 브로드캐스트 실패 (gameId={}): {}", event.gameId, ex.message, ex)
        }
    }

    /**
     * 시간 제한 경고 이벤트를 처리합니다.
     *
     * broadcastWarning(TIME_LIMIT)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onTimeLimitWarning(event: TimeLimitWarningEvent) {
        try {
            val severity =
                when (event.warningType) {
                    TimeLimitWarningType.LIMIT_REACHED -> WarningSeverity.CRITICAL
                    TimeLimitWarningType.APPROACHING_LIMIT -> WarningSeverity.WARNING
                }
            val title = event.warningType.displayName
            val message =
                when (event.warningType) {
                    TimeLimitWarningType.LIMIT_REACHED ->
                        "경기 시간 제한(${event.timeLimitMinutes}분)에 도달했습니다" +
                            "(경과 시간: ${event.elapsedMinutes}분)."
                    TimeLimitWarningType.APPROACHING_LIMIT ->
                        "경기 시간 제한에 임박했습니다. " +
                            "남은 시간: ${event.remainingMinutes}분 " +
                            "(제한: ${event.timeLimitMinutes}분, 경과: ${event.elapsedMinutes}분)."
                }
            val details =
                mapOf<String, Any?>(
                    "startedAt" to event.startedAt.toString(),
                    "timeLimitMinutes" to event.timeLimitMinutes,
                    "elapsedMinutes" to event.elapsedMinutes,
                    "remainingMinutes" to event.remainingMinutes,
                )

            val warningMessage =
                WarningMessage(
                    gameId = event.gameId,
                    warningType = WarningType.TIME_LIMIT,
                    severity = severity,
                    title = title,
                    message = message,
                    details = details,
                    timestamp = Instant.now(),
                )
            gameBroadcastService.broadcastWarning(event.gameId, warningMessage)

            log.debug(
                "시간 제한 경고 브로드캐스트 완료 (gameId={}, elapsed={}분, limit={}분, type={})",
                event.gameId,
                event.elapsedMinutes,
                event.timeLimitMinutes,
                event.warningType,
            )
        } catch (ex: Exception) {
            log.error("TimeLimitWarningEvent 브로드캐스트 실패 (gameId={}): {}", event.gameId, ex.message, ex)
        }
    }

    // ===== 내부 헬퍼 메서드 =====

    private fun buildGameStateMessage(
        gameId: Long,
        game: com.nextup.core.domain.game.Game,
        gameTeams: List<com.nextup.core.domain.game.GameTeam>,
    ): GameStateMessage {
        val gameState = game.gameState
        val currentBatter =
            gameState.currentBatterId?.let { gamePlayerRepository.findByIdOrNull(it) }
        val currentPitcher =
            gameState.currentPitcherId?.let { gamePlayerRepository.findByIdOrNull(it) }
        val runnerOnFirst =
            gameState.runnerOnFirstId?.let { gamePlayerRepository.findByIdOrNull(it) }
        val runnerOnSecond =
            gameState.runnerOnSecondId?.let { gamePlayerRepository.findByIdOrNull(it) }
        val runnerOnThird =
            gameState.runnerOnThirdId?.let { gamePlayerRepository.findByIdOrNull(it) }

        return GameStateMessage(
            gameId = gameId,
            inning = game.currentInning,
            isTopInning = game.isTopInning,
            outs = gameState.outs,
            balls = gameState.balls,
            strikes = gameState.strikes,
            runners =
                RunnersDto(
                    first = runnerOnFirst?.let { toPlayerBriefDto(it) },
                    second = runnerOnSecond?.let { toPlayerBriefDto(it) },
                    third = runnerOnThird?.let { toPlayerBriefDto(it) },
                ),
            currentBatter = currentBatter?.let { toPlayerBriefDto(it) },
            currentPitcher = currentPitcher?.let { toPlayerBriefDto(it) },
        )
    }

    private fun buildScoreboardMessage(
        gameId: Long,
        game: com.nextup.core.domain.game.Game,
        gameTeams: List<com.nextup.core.domain.game.GameTeam>,
    ): ScoreboardMessage {
        val homeTeam = gameTeams.find { it.homeAway == HomeAway.HOME }
        val awayTeam = gameTeams.find { it.homeAway == HomeAway.AWAY }
        val maxInning = maxOf(game.totalInnings, game.currentInning)

        val homeScores =
            (1..maxInning).map { inning ->
                homeTeam?.getInningScore(inning) ?: 0
            }
        val awayScores =
            (1..maxInning).map { inning ->
                awayTeam?.getInningScore(inning) ?: 0
            }

        return ScoreboardMessage(
            gameId = gameId,
            homeTeam =
                TeamScoreDto(
                    teamId = homeTeam?.team?.id ?: 0L,
                    teamName = homeTeam?.team?.name ?: "",
                    logoUrl = null,
                    runs = homeTeam?.totalScore ?: 0,
                    hits = homeTeam?.totalHits ?: 0,
                    errors = homeTeam?.totalErrors ?: 0,
                ),
            awayTeam =
                TeamScoreDto(
                    teamId = awayTeam?.team?.id ?: 0L,
                    teamName = awayTeam?.team?.name ?: "",
                    logoUrl = null,
                    runs = awayTeam?.totalScore ?: 0,
                    hits = awayTeam?.totalHits ?: 0,
                    errors = awayTeam?.totalErrors ?: 0,
                ),
            inningScores =
                InningScoresDto(
                    homeScores = homeScores,
                    awayScores = awayScores,
                ),
            currentInning = game.currentInning,
            isTopInning = game.isTopInning,
        )
    }

    private fun toPlayerBriefDto(gamePlayer: com.nextup.core.domain.game.GamePlayer): PlayerBriefDto =
        PlayerBriefDto(
            id = gamePlayer.player.id,
            name = gamePlayer.player.name,
            backNumber = gamePlayer.backNumber,
        )
}
