package com.nextup.api.controller.eventgame

import com.nextup.api.dto.eventgame.AssignTeamApiRequest
import com.nextup.api.dto.eventgame.CancelEventGameApiRequest
import com.nextup.api.dto.eventgame.CreateEventGameApiRequest
import com.nextup.api.dto.eventgame.FinishEventGameApiRequest
import com.nextup.api.dto.eventgame.JoinEventGameApiRequest
import com.nextup.core.domain.eventgame.EventGame
import com.nextup.core.domain.eventgame.EventGameParticipant
import com.nextup.core.domain.eventgame.TeamAssignment
import com.nextup.core.service.eventgame.EventGameService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("EventGameController 테스트")
class EventGameControllerTest {
    private lateinit var eventGameService: EventGameService
    private lateinit var controller: EventGameController

    @BeforeEach
    fun setUp() {
        eventGameService = mockk()
        controller = EventGameController(eventGameService)
    }

    private fun createMockEventGame(): EventGame {
        val game =
            EventGame.create(
                organizerId = 100L,
                title = "주말 픽업 게임",
                description = "누구나 참가 가능",
                scheduledAt = LocalDateTime.now().plusDays(7),
                location = "잠실 야구장",
                fieldName = "A구장",
                maxParticipants = 20,
                innings = 7,
                teamAName = "Team A",
                teamBName = "Team B",
            )
        return game
    }

    private fun createMockParticipant(game: EventGame): EventGameParticipant =
        EventGameParticipant.create(
            eventGame = game,
            playerId = 10L,
            message = "참가합니다",
        )

    @Nested
    @DisplayName("POST /api/v1/event-games")
    inner class CreateEventGame {
        @Test
        fun `이벤트 게임 생성 성공`() {
            val game = createMockEventGame()
            every { eventGameService.createEventGame(any()) } returns game

            val request =
                CreateEventGameApiRequest(
                    title = "주말 픽업 게임",
                    description = "누구나 참가 가능",
                    scheduledAt = LocalDateTime.now().plusDays(7),
                    location = "잠실 야구장",
                    fieldName = "A구장",
                    maxParticipants = 20,
                    innings = 7,
                    teamAName = "Team A",
                    teamBName = "Team B",
                )

            val response = controller.createEventGame(100L, request)

            assertThat(response.success).isTrue()
            assertThat(response.data).isNotNull
            assertThat(response.data!!.title).isEqualTo("주말 픽업 게임")
            assertThat(response.data!!.status).isEqualTo("RECRUITING")
            assertThat(response.data!!.organizerId).isEqualTo(100L)
            assertThat(response.data!!.maxParticipants).isEqualTo(20)
            assertThat(response.data!!.innings).isEqualTo(7)
            assertThat(response.data!!.description).isEqualTo("누구나 참가 가능")
            assertThat(response.data!!.location).isEqualTo("잠실 야구장")
            assertThat(response.data!!.fieldName).isEqualTo("A구장")
            assertThat(response.data!!.teamAName).isEqualTo("Team A")
            assertThat(response.data!!.teamBName).isEqualTo("Team B")
            verify { eventGameService.createEventGame(any()) }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/event-games")
    inner class GetRecruitingEventGames {
        @Test
        fun `모집 중인 이벤트 게임 목록 조회 성공`() {
            val games = listOf(createMockEventGame(), createMockEventGame())
            every { eventGameService.getRecruitingEventGames() } returns games

            val response = controller.getRecruitingEventGames()

            assertThat(response.success).isTrue()
            assertThat(response.data).hasSize(2)
            verify { eventGameService.getRecruitingEventGames() }
        }

        @Test
        fun `빈 목록 조회`() {
            every { eventGameService.getRecruitingEventGames() } returns emptyList()

            val response = controller.getRecruitingEventGames()

            assertThat(response.success).isTrue()
            assertThat(response.data).isEmpty()
        }
    }

    @Nested
    @DisplayName("GET /api/v1/event-games/{id}")
    inner class GetEventGame {
        @Test
        fun `이벤트 게임 상세 조회 성공`() {
            val game = createMockEventGame()
            every { eventGameService.getEventGame(1L) } returns game

            val response = controller.getEventGame(1L)

            assertThat(response.success).isTrue()
            assertThat(response.data).isNotNull
            assertThat(response.data!!.title).isEqualTo("주말 픽업 게임")
            verify { eventGameService.getEventGame(1L) }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/event-games/me")
    inner class GetMyEventGames {
        @Test
        fun `내가 주최한 이벤트 게임 목록 조회 성공`() {
            val games = listOf(createMockEventGame())
            every { eventGameService.getMyEventGames(100L) } returns games

            val response = controller.getMyEventGames(100L)

            assertThat(response.success).isTrue()
            assertThat(response.data).hasSize(1)
            verify { eventGameService.getMyEventGames(100L) }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/event-games/{id}/join")
    inner class JoinEventGame {
        @Test
        fun `이벤트 게임 참가 신청 성공`() {
            val game = createMockEventGame()
            val participant = createMockParticipant(game)
            every { eventGameService.joinEventGame(1L, 10L, "참가합니다") } returns participant

            val request = JoinEventGameApiRequest(message = "참가합니다")
            val response = controller.joinEventGame(1L, 10L, request)

            assertThat(response.success).isTrue()
            assertThat(response.data).isNotNull
            assertThat(response.data!!.playerId).isEqualTo(10L)
            assertThat(response.data!!.status).isEqualTo("APPLIED")
            assertThat(response.data!!.message).isEqualTo("참가합니다")
            verify { eventGameService.joinEventGame(1L, 10L, "참가합니다") }
        }

        @Test
        fun `메시지 없이 참가 신청`() {
            val game = createMockEventGame()
            val participant =
                EventGameParticipant.create(
                    eventGame = game,
                    playerId = 10L,
                    message = null,
                )
            every { eventGameService.joinEventGame(1L, 10L, null) } returns participant

            val request = JoinEventGameApiRequest(message = null)
            val response = controller.joinEventGame(1L, 10L, request)

            assertThat(response.success).isTrue()
            assertThat(response.data!!.message).isNull()
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/event-games/{id}/participants/{participantId}/confirm")
    inner class ConfirmParticipant {
        @Test
        fun `참가자 확정 성공`() {
            val game = createMockEventGame()
            val participant = createMockParticipant(game)
            participant.confirm()
            every { eventGameService.confirmParticipant(1L, 5L) } returns participant

            val response = controller.confirmParticipant(1L, 5L, 100L)

            assertThat(response.success).isTrue()
            assertThat(response.data!!.status).isEqualTo("CONFIRMED")
            verify { eventGameService.confirmParticipant(1L, 5L) }
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/event-games/{id}/participants/{participantId}")
    inner class CancelParticipation {
        @Test
        fun `참가 취소 성공`() {
            val game = createMockEventGame()
            val participant = createMockParticipant(game)
            participant.cancel()
            every { eventGameService.cancelParticipation(1L, 5L) } returns participant

            val response = controller.cancelParticipation(1L, 5L, 100L)

            assertThat(response.success).isTrue()
            assertThat(response.data!!.status).isEqualTo("CANCELLED")
            verify { eventGameService.cancelParticipation(1L, 5L) }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/event-games/{id}/participants")
    inner class GetParticipants {
        @Test
        fun `참가자 목록 조회 성공`() {
            val game = createMockEventGame()
            val participants =
                listOf(
                    createMockParticipant(game),
                    EventGameParticipant.create(game, 20L, "저도 참가"),
                )
            every { eventGameService.getParticipants(1L) } returns participants

            val response = controller.getParticipants(1L)

            assertThat(response.success).isTrue()
            assertThat(response.data).hasSize(2)
            verify { eventGameService.getParticipants(1L) }
        }

        @Test
        fun `참가자 없는 경우 빈 목록 반환`() {
            every { eventGameService.getParticipants(1L) } returns emptyList()

            val response = controller.getParticipants(1L)

            assertThat(response.success).isTrue()
            assertThat(response.data).isEmpty()
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/event-games/{id}/close")
    inner class CloseRecruitment {
        @Test
        fun `모집 마감 성공`() {
            val game = createMockEventGame()
            game.closeRecruitment()
            every { eventGameService.closeRecruitment(1L) } returns game

            val response = controller.closeRecruitment(1L, 100L)

            assertThat(response.success).isTrue()
            assertThat(response.data!!.status).isEqualTo("CLOSED")
            verify { eventGameService.closeRecruitment(1L) }
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/event-games/{id}/participants/{participantId}/assign-team")
    inner class AssignTeam {
        @Test
        fun `팀 수동 배정 성공 - TEAM_A`() {
            val game = createMockEventGame()
            val participant = createMockParticipant(game)
            participant.confirm()
            participant.assignTeam(TeamAssignment.TEAM_A)
            every {
                eventGameService.assignTeam(1L, 5L, TeamAssignment.TEAM_A)
            } returns participant

            val request = AssignTeamApiRequest(team = "TEAM_A")
            val response = controller.assignTeam(1L, 5L, 100L, request)

            assertThat(response.success).isTrue()
            assertThat(response.data!!.teamAssignment).isEqualTo("TEAM_A")
            verify { eventGameService.assignTeam(1L, 5L, TeamAssignment.TEAM_A) }
        }

        @Test
        fun `팀 수동 배정 성공 - TEAM_B`() {
            val game = createMockEventGame()
            val participant = createMockParticipant(game)
            participant.confirm()
            participant.assignTeam(TeamAssignment.TEAM_B)
            every {
                eventGameService.assignTeam(1L, 5L, TeamAssignment.TEAM_B)
            } returns participant

            val request = AssignTeamApiRequest(team = "TEAM_B")
            val response = controller.assignTeam(1L, 5L, 100L, request)

            assertThat(response.success).isTrue()
            assertThat(response.data!!.teamAssignment).isEqualTo("TEAM_B")
        }
    }

    @Nested
    @DisplayName("POST /api/v1/event-games/{id}/auto-assign")
    inner class AutoAssignTeams {
        @Test
        fun `자동 팀 배정 성공`() {
            val game = createMockEventGame()
            every { eventGameService.autoAssignTeams(1L) } returns game

            val response = controller.autoAssignTeams(1L, 100L)

            assertThat(response.success).isTrue()
            assertThat(response.data).isNotNull
            verify { eventGameService.autoAssignTeams(1L) }
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/event-games/{id}/complete-assignment")
    inner class CompleteTeamAssignment {
        @Test
        fun `팀 배정 완료 성공`() {
            val game = createMockEventGame()
            val p1 = EventGameParticipant.create(game, 10L)
            val p2 = EventGameParticipant.create(game, 20L)
            game.addParticipant(p1)
            game.addParticipant(p2)
            p1.confirm()
            p2.confirm()
            p1.assignTeam(TeamAssignment.TEAM_A)
            p2.assignTeam(TeamAssignment.TEAM_B)
            game.closeRecruitment()
            game.completeTeamAssignment()
            every { eventGameService.completeTeamAssignment(1L) } returns game

            val response = controller.completeTeamAssignment(1L, 100L)

            assertThat(response.success).isTrue()
            assertThat(response.data!!.status).isEqualTo("TEAM_ASSIGNED")
            verify { eventGameService.completeTeamAssignment(1L) }
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/event-games/{id}/start")
    inner class StartGame {
        @Test
        fun `경기 시작 성공`() {
            val game = createMockEventGame()
            val p1 = EventGameParticipant.create(game, 10L)
            val p2 = EventGameParticipant.create(game, 20L)
            game.addParticipant(p1)
            game.addParticipant(p2)
            p1.confirm()
            p2.confirm()
            p1.assignTeam(TeamAssignment.TEAM_A)
            p2.assignTeam(TeamAssignment.TEAM_B)
            game.closeRecruitment()
            game.completeTeamAssignment()
            game.start()
            every { eventGameService.startGame(1L) } returns game

            val response = controller.startGame(1L, 100L)

            assertThat(response.success).isTrue()
            assertThat(response.data!!.status).isEqualTo("IN_PROGRESS")
            assertThat(response.data!!.startedAt).isNotNull()
            verify { eventGameService.startGame(1L) }
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/event-games/{id}/finish")
    inner class FinishGame {
        @Test
        fun `경기 종료 성공`() {
            val game = createMockEventGame()
            val p1 = EventGameParticipant.create(game, 10L)
            val p2 = EventGameParticipant.create(game, 20L)
            game.addParticipant(p1)
            game.addParticipant(p2)
            p1.confirm()
            p2.confirm()
            p1.assignTeam(TeamAssignment.TEAM_A)
            p2.assignTeam(TeamAssignment.TEAM_B)
            game.closeRecruitment()
            game.completeTeamAssignment()
            game.start()
            game.finish(5, 3)
            every { eventGameService.finishGame(1L, 5, 3) } returns game

            val request = FinishEventGameApiRequest(teamAScore = 5, teamBScore = 3)
            val response = controller.finishGame(1L, 100L, request)

            assertThat(response.success).isTrue()
            assertThat(response.data!!.status).isEqualTo("FINISHED")
            assertThat(response.data!!.teamAScore).isEqualTo(5)
            assertThat(response.data!!.teamBScore).isEqualTo(3)
            assertThat(response.data!!.endedAt).isNotNull()
            verify { eventGameService.finishGame(1L, 5, 3) }
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/event-games/{id}/cancel")
    inner class CancelGame {
        @Test
        fun `경기 취소 성공`() {
            val game = createMockEventGame()
            game.cancel("우천 취소")
            every { eventGameService.cancelGame(1L, "우천 취소") } returns game

            val request = CancelEventGameApiRequest(reason = "우천 취소")
            val response = controller.cancelGame(1L, 100L, request)

            assertThat(response.success).isTrue()
            assertThat(response.data!!.status).isEqualTo("CANCELLED")
            assertThat(response.data!!.cancelReason).isEqualTo("우천 취소")
            verify { eventGameService.cancelGame(1L, "우천 취소") }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/event-games/me/history")
    inner class GetMyHistory {
        @Test
        fun `내 참가 이력 조회 성공`() {
            val game = createMockEventGame()
            val game2 = createMockEventGame()
            val participant1 = createMockParticipant(game)
            val participant2 = EventGameParticipant.create(game2, 10L)
            every { eventGameService.getPlayerHistory(10L) } returns
                listOf(participant1, participant2)

            val response = controller.getMyHistory(10L)

            assertThat(response.success).isTrue()
            assertThat(response.data).hasSize(2)
            verify { eventGameService.getPlayerHistory(10L) }
        }

        @Test
        fun `참가 이력 없는 경우 빈 목록 반환`() {
            every { eventGameService.getPlayerHistory(10L) } returns emptyList()

            val response = controller.getMyHistory(10L)

            assertThat(response.success).isTrue()
            assertThat(response.data).isEmpty()
        }
    }
}
