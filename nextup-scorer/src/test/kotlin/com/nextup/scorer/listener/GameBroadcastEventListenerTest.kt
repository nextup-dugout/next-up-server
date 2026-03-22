package com.nextup.scorer.listener

import com.nextup.core.domain.event.GameEndedEvent
import com.nextup.core.domain.event.GameStartedEvent
import com.nextup.core.domain.event.HalfInningAdvancedEvent
import com.nextup.core.domain.event.PitchCountWarningEvent
import com.nextup.core.domain.event.PitchCountWarningType
import com.nextup.core.domain.event.PlateAppearanceRecordedEvent
import com.nextup.core.domain.event.PlayerSubstitutedEvent
import com.nextup.core.domain.event.RecordCorrectedEvent
import com.nextup.core.domain.event.TimeLimitWarningEvent
import com.nextup.core.domain.event.TimeLimitWarningType
import com.nextup.core.domain.game.CorrectionType
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameEvent
import com.nextup.core.domain.game.GameState
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.scorer.service.websocket.GameBroadcastService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("GameBroadcastEventListener 테스트")
class GameBroadcastEventListenerTest {

    private val gameRepository = mockk<GameRepositoryPort>()
    private val gameTeamRepository = mockk<GameTeamRepositoryPort>()
    private val gamePlayerRepository = mockk<GamePlayerRepositoryPort>()
    private val gameEventRepository = mockk<GameEventRepositoryPort>()
    private val gameBroadcastService = mockk<GameBroadcastService>(relaxed = true)

    private val listener =
        GameBroadcastEventListener(
            gameRepository = gameRepository,
            gameTeamRepository = gameTeamRepository,
            gamePlayerRepository = gamePlayerRepository,
            gameEventRepository = gameEventRepository,
            gameBroadcastService = gameBroadcastService,
        )

    private val mockGame = mockk<Game>()
    private val mockGameState = mockk<GameState>()
    private val mockHomeTeam = mockk<GameTeam>()
    private val mockAwayTeam = mockk<GameTeam>()
    private val mockTeamHome = mockk<Team>()
    private val mockTeamAway = mockk<Team>()

    companion object {
        private const val GAME_ID = 1L
    }

    @BeforeEach
    fun setUp() {
        // Game mock
        every { mockGame.id } returns GAME_ID
        every { mockGame.currentInning } returns 3
        every { mockGame.isTopInning } returns true
        every { mockGame.totalInnings } returns 9
        every { mockGame.gameState } returns mockGameState

        // GameState mock
        every { mockGameState.outs } returns 1
        every { mockGameState.balls } returns 2
        every { mockGameState.strikes } returns 1
        every { mockGameState.currentBatterId } returns null
        every { mockGameState.currentPitcherId } returns null
        every { mockGameState.runnerOnFirstId } returns null
        every { mockGameState.runnerOnSecondId } returns null
        every { mockGameState.runnerOnThirdId } returns null

        // Team mock
        every { mockTeamHome.id } returns 10L
        every { mockTeamHome.name } returns "홈팀"
        every { mockTeamAway.id } returns 20L
        every { mockTeamAway.name } returns "원정팀"

        // GameTeam mock
        every { mockHomeTeam.homeAway } returns HomeAway.HOME
        every { mockHomeTeam.team } returns mockTeamHome
        every { mockHomeTeam.totalScore } returns 2
        every { mockHomeTeam.totalHits } returns 5
        every { mockHomeTeam.totalErrors } returns 0
        every { mockHomeTeam.getInningScore(any()) } returns 0

        every { mockAwayTeam.homeAway } returns HomeAway.AWAY
        every { mockAwayTeam.team } returns mockTeamAway
        every { mockAwayTeam.totalScore } returns 1
        every { mockAwayTeam.totalHits } returns 3
        every { mockAwayTeam.totalErrors } returns 1
        every { mockAwayTeam.getInningScore(any()) } returns 0

        // Repository mocks
        every { gameRepository.findByIdOrNull(GAME_ID) } returns mockGame
        every { gameTeamRepository.findAllByGameId(GAME_ID) } returns listOf(mockHomeTeam, mockAwayTeam)
    }

    @Nested
    @DisplayName("onGameStarted - 경기 시작 이벤트")
    inner class OnGameStarted {

        @Test
        @DisplayName("경기 시작 시 broadcastEvent, broadcastState, broadcastScoreboard 모두 호출")
        fun `경기 시작 시 세 가지 브로드캐스트 모두 호출`() {
            // given
            val event = GameStartedEvent(gameId = GAME_ID)

            // when
            listener.onGameStarted(event)

            // then
            verify(exactly = 1) { gameBroadcastService.broadcastEvent(eq(GAME_ID), any()) }
            verify(exactly = 1) { gameBroadcastService.broadcastState(eq(GAME_ID), any()) }
            verify(exactly = 1) { gameBroadcastService.broadcastScoreboard(eq(GAME_ID), any()) }
        }

        @Test
        @DisplayName("게임을 찾을 수 없을 때 브로드캐스트하지 않음")
        fun `게임 없을 때 브로드캐스트 없음`() {
            // given
            every { gameRepository.findByIdOrNull(GAME_ID) } returns null
            val event = GameStartedEvent(gameId = GAME_ID)

            // when
            listener.onGameStarted(event)

            // then
            verify(exactly = 0) { gameBroadcastService.broadcastEvent(any(), any()) }
            verify(exactly = 0) { gameBroadcastService.broadcastState(any(), any()) }
            verify(exactly = 0) { gameBroadcastService.broadcastScoreboard(any(), any()) }
        }
    }

    @Nested
    @DisplayName("onPlateAppearanceRecorded - 타석 결과 기록 이벤트")
    inner class OnPlateAppearanceRecorded {

        @Test
        @DisplayName("타석 결과 기록 시 broadcastState, broadcastScoreboard 호출")
        fun `타석 결과 기록 시 두 가지 브로드캐스트 호출`() {
            // given
            val event =
                PlateAppearanceRecordedEvent(
                    gameId = GAME_ID,
                    playerId = 100L,
                    pitcherId = 200L,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then
            verify(exactly = 0) { gameBroadcastService.broadcastEvent(any(), any()) }
            verify(exactly = 1) { gameBroadcastService.broadcastState(eq(GAME_ID), any()) }
            verify(exactly = 1) { gameBroadcastService.broadcastScoreboard(eq(GAME_ID), any()) }
        }

        @Test
        @DisplayName("게임을 찾을 수 없을 때 브로드캐스트하지 않음")
        fun `게임 없을 때 브로드캐스트 없음`() {
            // given
            every { gameRepository.findByIdOrNull(GAME_ID) } returns null
            val event =
                PlateAppearanceRecordedEvent(
                    gameId = GAME_ID,
                    playerId = 100L,
                    pitcherId = 200L,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then
            verify(exactly = 0) { gameBroadcastService.broadcastState(any(), any()) }
            verify(exactly = 0) { gameBroadcastService.broadcastScoreboard(any(), any()) }
        }
    }

    @Nested
    @DisplayName("onHalfInningAdvanced - 이닝 진행 이벤트")
    inner class OnHalfInningAdvanced {

        @Test
        @DisplayName("이닝 진행 시 broadcastEvent, broadcastState, broadcastScoreboard 모두 호출")
        fun `이닝 진행 시 세 가지 브로드캐스트 모두 호출`() {
            // given
            val event =
                HalfInningAdvancedEvent(
                    gameId = GAME_ID,
                    newInning = 4,
                    newIsTopInning = false,
                )

            // when
            listener.onHalfInningAdvanced(event)

            // then
            verify(exactly = 1) { gameBroadcastService.broadcastEvent(eq(GAME_ID), any()) }
            verify(exactly = 1) { gameBroadcastService.broadcastState(eq(GAME_ID), any()) }
            verify(exactly = 1) { gameBroadcastService.broadcastScoreboard(eq(GAME_ID), any()) }
        }

        @Test
        @DisplayName("게임을 찾을 수 없을 때 브로드캐스트하지 않음")
        fun `게임 없을 때 브로드캐스트 없음`() {
            // given
            every { gameRepository.findByIdOrNull(GAME_ID) } returns null
            val event =
                HalfInningAdvancedEvent(
                    gameId = GAME_ID,
                    newInning = 4,
                    newIsTopInning = false,
                )

            // when
            listener.onHalfInningAdvanced(event)

            // then
            verify(exactly = 0) { gameBroadcastService.broadcastEvent(any(), any()) }
            verify(exactly = 0) { gameBroadcastService.broadcastState(any(), any()) }
            verify(exactly = 0) { gameBroadcastService.broadcastScoreboard(any(), any()) }
        }
    }

    @Nested
    @DisplayName("onGameEnded - 경기 종료 이벤트")
    inner class OnGameEnded {

        @Test
        @DisplayName("경기 종료 시 broadcastEvent, broadcastScoreboard 호출 (broadcastState 없음)")
        fun `경기 종료 시 두 가지 브로드캐스트 호출`() {
            // given
            val event =
                GameEndedEvent(
                    gameId = GAME_ID,
                    finalStatus = "FINISHED",
                )

            // when
            listener.onGameEnded(event)

            // then
            verify(exactly = 1) { gameBroadcastService.broadcastEvent(eq(GAME_ID), any()) }
            verify(exactly = 0) { gameBroadcastService.broadcastState(any(), any()) }
            verify(exactly = 1) { gameBroadcastService.broadcastScoreboard(eq(GAME_ID), any()) }
        }

        @Test
        @DisplayName("게임을 찾을 수 없을 때 브로드캐스트하지 않음")
        fun `게임 없을 때 브로드캐스트 없음`() {
            // given
            every { gameRepository.findByIdOrNull(GAME_ID) } returns null
            val event =
                GameEndedEvent(
                    gameId = GAME_ID,
                    finalStatus = "FINISHED",
                )

            // when
            listener.onGameEnded(event)

            // then
            verify(exactly = 0) { gameBroadcastService.broadcastEvent(any(), any()) }
            verify(exactly = 0) { gameBroadcastService.broadcastScoreboard(any(), any()) }
        }
    }

    @Nested
    @DisplayName("onPlayerSubstituted - 선수 교체 이벤트")
    inner class OnPlayerSubstituted {

        @Test
        @DisplayName("선수 교체 시 broadcastEvent, broadcastState 호출 (broadcastScoreboard 없음)")
        fun `선수 교체 시 두 가지 브로드캐스트 호출`() {
            // given
            val mockGameEvent = mockk<GameEvent>()
            every { mockGameEvent.description } returns "3회초: 홍길동 → 김대타 (지명타자)"
            every { gameEventRepository.findByIdOrNull(99L) } returns mockGameEvent

            val event =
                PlayerSubstitutedEvent(
                    gameId = GAME_ID,
                    gameEventId = 99L,
                )

            // when
            listener.onPlayerSubstituted(event)

            // then
            verify(exactly = 1) { gameBroadcastService.broadcastEvent(eq(GAME_ID), any()) }
            verify(exactly = 1) { gameBroadcastService.broadcastState(eq(GAME_ID), any()) }
            verify(exactly = 0) { gameBroadcastService.broadcastScoreboard(any(), any()) }
        }

        @Test
        @DisplayName("GameEvent 없어도 기본 설명으로 브로드캐스트")
        fun `GameEvent 없을 때 기본 설명으로 브로드캐스트`() {
            // given
            every { gameEventRepository.findByIdOrNull(99L) } returns null

            val event =
                PlayerSubstitutedEvent(
                    gameId = GAME_ID,
                    gameEventId = 99L,
                )

            // when
            listener.onPlayerSubstituted(event)

            // then
            verify(exactly = 1) { gameBroadcastService.broadcastEvent(eq(GAME_ID), any()) }
            verify(exactly = 1) { gameBroadcastService.broadcastState(eq(GAME_ID), any()) }
        }

        @Test
        @DisplayName("게임을 찾을 수 없을 때 브로드캐스트하지 않음")
        fun `게임 없을 때 브로드캐스트 없음`() {
            // given
            every { gameRepository.findByIdOrNull(GAME_ID) } returns null
            val event =
                PlayerSubstitutedEvent(
                    gameId = GAME_ID,
                    gameEventId = 99L,
                )

            // when
            listener.onPlayerSubstituted(event)

            // then
            verify(exactly = 0) { gameBroadcastService.broadcastEvent(any(), any()) }
            verify(exactly = 0) { gameBroadcastService.broadcastState(any(), any()) }
        }
    }

    @Nested
    @DisplayName("예외 처리 - 브로드캐스트 실패 시 예외를 삼키고 계속 동작")
    inner class ExceptionHandling {

        @Test
        @DisplayName("onGameStarted에서 broadcastService 예외 발생 시 예외를 삼킴")
        fun `경기 시작 브로드캐스트 실패 시 예외 전파 없음`() {
            // given
            every { gameBroadcastService.broadcastEvent(any(), any()) } throws RuntimeException("WebSocket 전송 실패")
            val event = GameStartedEvent(gameId = GAME_ID)

            // when & then - 예외가 전파되지 않음
            listener.onGameStarted(event)
        }

        @Test
        @DisplayName("onPlateAppearanceRecorded에서 broadcastService 예외 발생 시 예외를 삼킴")
        fun `타석 결과 브로드캐스트 실패 시 예외 전파 없음`() {
            // given
            every { gameBroadcastService.broadcastState(any(), any()) } throws RuntimeException("WebSocket 전송 실패")
            val event =
                PlateAppearanceRecordedEvent(
                    gameId = GAME_ID,
                    playerId = 100L,
                    pitcherId = 200L,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when & then
            listener.onPlateAppearanceRecorded(event)
        }

        @Test
        @DisplayName("onHalfInningAdvanced에서 broadcastService 예외 발생 시 예외를 삼킴")
        fun `이닝 진행 브로드캐스트 실패 시 예외 전파 없음`() {
            // given
            every { gameBroadcastService.broadcastEvent(any(), any()) } throws RuntimeException("WebSocket 전송 실패")
            val event = HalfInningAdvancedEvent(gameId = GAME_ID, newInning = 4, newIsTopInning = false)

            // when & then
            listener.onHalfInningAdvanced(event)
        }

        @Test
        @DisplayName("onGameEnded에서 broadcastService 예외 발생 시 예외를 삼킴")
        fun `경기 종료 브로드캐스트 실패 시 예외 전파 없음`() {
            // given
            every { gameBroadcastService.broadcastEvent(any(), any()) } throws RuntimeException("WebSocket 전송 실패")
            val event = GameEndedEvent(gameId = GAME_ID, finalStatus = "FINISHED")

            // when & then
            listener.onGameEnded(event)
        }

        @Test
        @DisplayName("onPlayerSubstituted에서 broadcastService 예외 발생 시 예외를 삼킴")
        fun `선수 교체 브로드캐스트 실패 시 예외 전파 없음`() {
            // given
            every { gameEventRepository.findByIdOrNull(99L) } returns null
            every { gameBroadcastService.broadcastEvent(any(), any()) } throws RuntimeException("WebSocket 전송 실패")
            val event = PlayerSubstitutedEvent(gameId = GAME_ID, gameEventId = 99L)

            // when & then
            listener.onPlayerSubstituted(event)
        }

        @Test
        @DisplayName("onRecordCorrected에서 broadcastService 예외 발생 시 예외를 삼킴")
        fun `기록 정정 브로드캐스트 실패 시 예외 전파 없음`() {
            // given
            every { gameBroadcastService.broadcastState(any(), any()) } throws RuntimeException("WebSocket 전송 실패")
            val event =
                RecordCorrectedEvent(
                    gameId = GAME_ID,
                    correctionType = CorrectionType.BATTING,
                    playerId = 100L,
                    fieldName = "result",
                    oldValue = "SINGLE",
                    newValue = "DOUBLE",
                )

            // when & then
            listener.onRecordCorrected(event)
        }
    }

    @Nested
    @DisplayName("onRecordCorrected - 기록 정정 이벤트")
    inner class OnRecordCorrected {

        @Test
        @DisplayName("기록 정정 시 broadcastState, broadcastScoreboard 호출")
        fun `기록 정정 시 두 가지 브로드캐스트 호출`() {
            // given
            val event =
                RecordCorrectedEvent(
                    gameId = GAME_ID,
                    correctionType = CorrectionType.BATTING,
                    playerId = 100L,
                    fieldName = "result",
                    oldValue = "SINGLE",
                    newValue = "DOUBLE",
                    gameEventId = 50L,
                )

            // when
            listener.onRecordCorrected(event)

            // then
            verify(exactly = 0) { gameBroadcastService.broadcastEvent(any(), any()) }
            verify(exactly = 1) { gameBroadcastService.broadcastState(eq(GAME_ID), any()) }
            verify(exactly = 1) { gameBroadcastService.broadcastScoreboard(eq(GAME_ID), any()) }
        }

        @Test
        @DisplayName("게임을 찾을 수 없을 때 브로드캐스트하지 않음")
        fun `게임 없을 때 브로드캐스트 없음`() {
            // given
            every { gameRepository.findByIdOrNull(GAME_ID) } returns null
            val event =
                RecordCorrectedEvent(
                    gameId = GAME_ID,
                    correctionType = CorrectionType.BATTING,
                    playerId = 100L,
                    fieldName = "result",
                    oldValue = "SINGLE",
                    newValue = "DOUBLE",
                )

            // when
            listener.onRecordCorrected(event)

            // then
            verify(exactly = 0) { gameBroadcastService.broadcastState(any(), any()) }
            verify(exactly = 0) { gameBroadcastService.broadcastScoreboard(any(), any()) }
        }
    }

    @Nested
    @DisplayName("onPitchCountWarning - 투구수 제한 경고 이벤트")
    inner class OnPitchCountWarning {

        @Test
        @DisplayName("LIMIT_REACHED 시 broadcastWarning 호출")
        fun `투구수 제한 도달 시 경고 브로드캐스트`() {
            // given
            val event =
                PitchCountWarningEvent(
                    gameId = GAME_ID,
                    gamePlayerId = 50L,
                    playerId = 100L,
                    pitchesThrown = 105,
                    pitchCountLimit = 100,
                    warningType = PitchCountWarningType.LIMIT_REACHED,
                )

            // when
            listener.onPitchCountWarning(event)

            // then
            verify(exactly = 1) { gameBroadcastService.broadcastWarning(eq(GAME_ID), any()) }
        }

        @Test
        @DisplayName("APPROACHING_LIMIT 시 broadcastWarning 호출")
        fun `투구수 제한 임박 시 경고 브로드캐스트`() {
            // given
            val event =
                PitchCountWarningEvent(
                    gameId = GAME_ID,
                    gamePlayerId = 50L,
                    playerId = 100L,
                    pitchesThrown = 92,
                    pitchCountLimit = 100,
                    warningType = PitchCountWarningType.APPROACHING_LIMIT,
                )

            // when
            listener.onPitchCountWarning(event)

            // then
            verify(exactly = 1) { gameBroadcastService.broadcastWarning(eq(GAME_ID), any()) }
        }

        @Test
        @DisplayName("broadcastWarning 실패 시 예외를 삼킴")
        fun `브로드캐스트 실패 시 예외 전파 없음`() {
            // given
            every {
                gameBroadcastService.broadcastWarning(any(), any())
            } throws RuntimeException("WebSocket 전송 실패")

            val event =
                PitchCountWarningEvent(
                    gameId = GAME_ID,
                    gamePlayerId = 50L,
                    playerId = 100L,
                    pitchesThrown = 105,
                    pitchCountLimit = 100,
                    warningType = PitchCountWarningType.LIMIT_REACHED,
                )

            // when & then - 예외가 전파되지 않음
            listener.onPitchCountWarning(event)
        }
    }

    @Nested
    @DisplayName("onTimeLimitWarning - 시간 제한 경고 이벤트")
    inner class OnTimeLimitWarning {

        @Test
        @DisplayName("LIMIT_REACHED 시 broadcastWarning 호출")
        fun `시간 제한 도달 시 경고 브로드캐스트`() {
            // given
            val event =
                TimeLimitWarningEvent(
                    gameId = GAME_ID,
                    startedAt = Instant.now().minusSeconds(7200),
                    timeLimitMinutes = 120,
                    elapsedMinutes = 125,
                    warningType = TimeLimitWarningType.LIMIT_REACHED,
                )

            // when
            listener.onTimeLimitWarning(event)

            // then
            verify(exactly = 1) { gameBroadcastService.broadcastWarning(eq(GAME_ID), any()) }
        }

        @Test
        @DisplayName("APPROACHING_LIMIT 시 broadcastWarning 호출")
        fun `시간 제한 임박 시 경고 브로드캐스트`() {
            // given
            val event =
                TimeLimitWarningEvent(
                    gameId = GAME_ID,
                    startedAt = Instant.now().minusSeconds(6600),
                    timeLimitMinutes = 120,
                    elapsedMinutes = 112,
                    warningType = TimeLimitWarningType.APPROACHING_LIMIT,
                )

            // when
            listener.onTimeLimitWarning(event)

            // then
            verify(exactly = 1) { gameBroadcastService.broadcastWarning(eq(GAME_ID), any()) }
        }

        @Test
        @DisplayName("broadcastWarning 실패 시 예외를 삼킴")
        fun `브로드캐스트 실패 시 예외 전파 없음`() {
            // given
            every {
                gameBroadcastService.broadcastWarning(any(), any())
            } throws RuntimeException("WebSocket 전송 실패")

            val event =
                TimeLimitWarningEvent(
                    gameId = GAME_ID,
                    startedAt = Instant.now().minusSeconds(7200),
                    timeLimitMinutes = 120,
                    elapsedMinutes = 125,
                    warningType = TimeLimitWarningType.LIMIT_REACHED,
                )

            // when & then - 예외가 전파되지 않음
            listener.onTimeLimitWarning(event)
        }
    }
}
