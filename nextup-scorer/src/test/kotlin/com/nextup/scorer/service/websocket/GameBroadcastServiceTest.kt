package com.nextup.scorer.service.websocket

import com.nextup.scorer.dto.websocket.*
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.time.Instant

class GameBroadcastServiceTest {

    private val messagingTemplate: SimpMessagingTemplate = mockk(relaxed = true)
    private val service = GameBroadcastService(messagingTemplate)

    @Test
    fun `should broadcast event to correct topic`() {
        // given
        val gameId = 1L
        val event =
            GameEventMessage(
                eventId = 100L,
                eventType = "PLATE_APPEARANCE",
                inning = 5,
                isTopInning = true,
                description = "안타",
                batter = PlayerBriefDto(1L, "홍길동", 10),
                pitcher = PlayerBriefDto(2L, "김투수", 1),
                result = "SINGLE",
                runsScored = 1,
                timestamp = Instant.now()
            )

        // when
        service.broadcastEvent(gameId, event)

        // then
        verify(exactly = 1) {
            messagingTemplate.convertAndSend("/topic/games/$gameId/events", event)
        }
    }

    @Test
    fun `should broadcast scoreboard to correct topic`() {
        // given
        val gameId = 1L
        val scoreboard =
            ScoreboardMessage(
                gameId = gameId,
                homeTeam = TeamScoreDto(1L, "홈팀", 5, 8, 1),
                awayTeam = TeamScoreDto(2L, "원정팀", 3, 6, 0),
                inningScores =
                    InningScoresDto(
                        homeScores = listOf(0, 1, 2, 0, 2),
                        awayScores = listOf(1, 0, 0, 2, 0)
                    ),
                currentInning = 5,
                isTopInning = false
            )

        // when
        service.broadcastScoreboard(gameId, scoreboard)

        // then
        verify(exactly = 1) {
            messagingTemplate.convertAndSend("/topic/games/$gameId/scoreboard", scoreboard)
        }
    }

    @Test
    fun `should broadcast state to correct topic`() {
        // given
        val gameId = 1L
        val state =
            GameStateMessage(
                gameId = gameId,
                inning = 5,
                isTopInning = true,
                outs = 2,
                balls = 2,
                strikes = 1,
                runners =
                    RunnersDto(
                        first = PlayerBriefDto(1L, "1루주자", 10),
                        second = null,
                        third = PlayerBriefDto(3L, "3루주자", 12)
                    ),
                currentBatter = PlayerBriefDto(4L, "현재타자", 15),
                currentPitcher = PlayerBriefDto(5L, "현재투수", 1)
            )

        // when
        service.broadcastState(gameId, state)

        // then
        verify(exactly = 1) {
            messagingTemplate.convertAndSend("/topic/games/$gameId/state", state)
        }
    }
}
