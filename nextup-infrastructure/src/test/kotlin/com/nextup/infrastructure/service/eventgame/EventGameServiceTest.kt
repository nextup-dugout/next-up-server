package com.nextup.infrastructure.service.eventgame

import com.nextup.common.exception.EventGameNotFoundException
import com.nextup.common.exception.EventGameParticipantNotFoundException
import com.nextup.core.domain.eventgame.EventGame
import com.nextup.core.domain.eventgame.EventGameParticipant
import com.nextup.core.domain.eventgame.EventGameParticipantStatus
import com.nextup.core.domain.eventgame.EventGameStatus
import com.nextup.core.domain.eventgame.TeamAssignment
import com.nextup.core.port.repository.EventGameParticipantRepositoryPort
import com.nextup.core.port.repository.EventGameRepositoryPort
import com.nextup.core.service.eventgame.CreateEventGameCommand
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("EventGameService 테스트")
class EventGameServiceTest {
    private lateinit var eventGameRepository: EventGameRepositoryPort
    private lateinit var participantRepository: EventGameParticipantRepositoryPort
    private lateinit var service: EventGameServiceImpl

    @BeforeEach
    fun setUp() {
        eventGameRepository = mockk()
        participantRepository = mockk()
        service = EventGameServiceImpl(eventGameRepository, participantRepository)
    }

    private fun createEventGame(maxParticipants: Int = 20): EventGame =
        EventGame.create(
            organizerId = 100L,
            title = "주말 픽업 게임",
            scheduledAt = LocalDateTime.now().plusDays(7),
            maxParticipants = maxParticipants,
        )

    private fun createGameWithConfirmedParticipants(): Triple<EventGame, EventGameParticipant, EventGameParticipant> {
        val game = createEventGame()
        val p1 = EventGameParticipant.create(game, 10L)
        val p2 = EventGameParticipant.create(game, 20L)
        game.addParticipant(p1)
        game.addParticipant(p2)
        p1.confirm()
        p2.confirm()
        return Triple(game, p1, p2)
    }

    @Nested
    @DisplayName("createEventGame")
    inner class CreateEventGameTest {
        @Test
        fun `이벤트 게임 생성 성공`() {
            val slot = slot<EventGame>()
            every { eventGameRepository.save(capture(slot)) } answers { slot.captured }

            val result =
                service.createEventGame(
                    CreateEventGameCommand(
                        organizerId = 100L,
                        title = "주말 픽업 게임",
                        scheduledAt = LocalDateTime.now().plusDays(7),
                        maxParticipants = 20,
                    ),
                )

            assertThat(result.title).isEqualTo("주말 픽업 게임")
            assertThat(result.status).isEqualTo(EventGameStatus.RECRUITING)
            assertThat(result.organizerId).isEqualTo(100L)
        }

        @Test
        fun `모든 필드를 포함하여 생성`() {
            val slot = slot<EventGame>()
            every { eventGameRepository.save(capture(slot)) } answers { slot.captured }

            val result =
                service.createEventGame(
                    CreateEventGameCommand(
                        organizerId = 100L,
                        title = "주말 픽업 게임",
                        description = "누구나 환영",
                        scheduledAt = LocalDateTime.now().plusDays(7),
                        location = "잠실 야구장",
                        fieldName = "A구장",
                        maxParticipants = 18,
                        innings = 5,
                        teamAName = "레드팀",
                        teamBName = "블루팀",
                    ),
                )

            assertThat(result.description).isEqualTo("누구나 환영")
            assertThat(result.location).isEqualTo("잠실 야구장")
            assertThat(result.fieldName).isEqualTo("A구장")
            assertThat(result.maxParticipants).isEqualTo(18)
            assertThat(result.innings).isEqualTo(5)
            assertThat(result.teamAName).isEqualTo("레드팀")
            assertThat(result.teamBName).isEqualTo("블루팀")
        }
    }

    @Nested
    @DisplayName("getEventGame")
    inner class GetEventGameTest {
        @Test
        fun `이벤트 게임 조회 성공`() {
            val game = createEventGame()
            every { eventGameRepository.findByIdOrNull(1L) } returns game

            val result = service.getEventGame(1L)

            assertThat(result).isEqualTo(game)
        }

        @Test
        fun `존재하지 않는 이벤트 게임 조회 시 예외`() {
            every { eventGameRepository.findByIdOrNull(999L) } returns null

            assertThatThrownBy { service.getEventGame(999L) }
                .isInstanceOf(EventGameNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("getRecruitingEventGames")
    inner class GetRecruitingTest {
        @Test
        fun `모집 중인 이벤트 게임 목록 조회`() {
            val games = listOf(createEventGame(), createEventGame())
            every { eventGameRepository.findByStatus(EventGameStatus.RECRUITING) } returns games

            val result = service.getRecruitingEventGames()

            assertThat(result).hasSize(2)
        }

        @Test
        fun `모집 중인 이벤트 게임 없을 때 빈 목록`() {
            every { eventGameRepository.findByStatus(EventGameStatus.RECRUITING) } returns emptyList()

            val result = service.getRecruitingEventGames()

            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("getMyEventGames")
    inner class GetMyEventGamesTest {
        @Test
        fun `내가 주최한 이벤트 게임 목록 조회`() {
            val games = listOf(createEventGame())
            every { eventGameRepository.findByOrganizerId(100L) } returns games

            val result = service.getMyEventGames(100L)

            assertThat(result).hasSize(1)
            verify { eventGameRepository.findByOrganizerId(100L) }
        }
    }

    @Nested
    @DisplayName("joinEventGame")
    inner class JoinEventGameTest {
        @Test
        fun `참가 신청 성공`() {
            val game = createEventGame()
            every { eventGameRepository.findByIdOrNull(any()) } returns game
            every { eventGameRepository.save(any()) } answers { firstArg() }

            val result = service.joinEventGame(1L, 10L, "참가합니다")

            assertThat(result.playerId).isEqualTo(10L)
            assertThat(result.status).isEqualTo(EventGameParticipantStatus.APPLIED)
            assertThat(result.message).isEqualTo("참가합니다")
        }

        @Test
        fun `메시지 없이 참가 신청`() {
            val game = createEventGame()
            every { eventGameRepository.findByIdOrNull(any()) } returns game
            every { eventGameRepository.save(any()) } answers { firstArg() }

            val result = service.joinEventGame(1L, 10L, null)

            assertThat(result.playerId).isEqualTo(10L)
            assertThat(result.message).isNull()
        }
    }

    @Nested
    @DisplayName("confirmParticipant")
    inner class ConfirmParticipantTest {
        @Test
        fun `참가 확정 성공`() {
            val game = createEventGame()
            val participant = EventGameParticipant.create(game, 10L)
            every { eventGameRepository.findByIdOrNull(any()) } returns game
            every { participantRepository.findByIdOrNull(any()) } returns participant
            every { participantRepository.save(any()) } answers { firstArg() }

            val result = service.confirmParticipant(1L, 1L)

            assertThat(result.status).isEqualTo(EventGameParticipantStatus.CONFIRMED)
        }

        @Test
        fun `존재하지 않는 참가자 확정 시 예외`() {
            val game = createEventGame()
            every { eventGameRepository.findByIdOrNull(any()) } returns game
            every { participantRepository.findByIdOrNull(any()) } returns null

            assertThatThrownBy { service.confirmParticipant(1L, 999L) }
                .isInstanceOf(EventGameParticipantNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("cancelParticipation")
    inner class CancelParticipationTest {
        @Test
        fun `참가 취소 성공`() {
            val game = createEventGame()
            val participant = EventGameParticipant.create(game, 10L)
            every { eventGameRepository.findByIdOrNull(any()) } returns game
            every { participantRepository.findByIdOrNull(any()) } returns participant
            every { participantRepository.save(any()) } answers { firstArg() }

            val result = service.cancelParticipation(1L, 1L)

            assertThat(result.status).isEqualTo(EventGameParticipantStatus.CANCELLED)
        }

        @Test
        fun `존재하지 않는 참가자 취소 시 예외`() {
            val game = createEventGame()
            every { eventGameRepository.findByIdOrNull(any()) } returns game
            every { participantRepository.findByIdOrNull(any()) } returns null

            assertThatThrownBy { service.cancelParticipation(1L, 999L) }
                .isInstanceOf(EventGameParticipantNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("closeRecruitment")
    inner class CloseRecruitmentTest {
        @Test
        fun `모집 마감 성공`() {
            val game = createEventGame()
            every { eventGameRepository.findByIdOrNull(any()) } returns game
            every { eventGameRepository.save(any()) } answers { firstArg() }

            val result = service.closeRecruitment(1L)

            assertThat(result.status).isEqualTo(EventGameStatus.CLOSED)
        }
    }

    @Nested
    @DisplayName("assignTeam")
    inner class AssignTeamTest {
        @Test
        fun `팀 수동 배정 성공`() {
            val game = createEventGame()
            val participant = EventGameParticipant.create(game, 10L)
            participant.confirm()
            every { eventGameRepository.findByIdOrNull(any()) } returns game
            every { participantRepository.findByIdOrNull(any()) } returns participant
            every { participantRepository.save(any()) } answers { firstArg() }

            val result = service.assignTeam(1L, 1L, TeamAssignment.TEAM_A)

            assertThat(result.teamAssignment).isEqualTo(TeamAssignment.TEAM_A)
        }

        @Test
        fun `존재하지 않는 참가자에 팀 배정 시 예외`() {
            val game = createEventGame()
            every { eventGameRepository.findByIdOrNull(any()) } returns game
            every { participantRepository.findByIdOrNull(any()) } returns null

            assertThatThrownBy { service.assignTeam(1L, 999L, TeamAssignment.TEAM_A) }
                .isInstanceOf(EventGameParticipantNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("autoAssignTeams")
    inner class AutoAssignTeamsTest {
        @Test
        fun `자동 팀 배정 성공 - 확정 참가자에게 팀 배정`() {
            val (game, p1, p2) = createGameWithConfirmedParticipants()
            every { eventGameRepository.findByIdOrNull(any()) } returns game
            every { participantRepository.save(any()) } answers { firstArg() }
            every { eventGameRepository.save(any()) } answers { firstArg() }

            val result = service.autoAssignTeams(1L)

            assertThat(p1.teamAssignment).isNotNull
            assertThat(p2.teamAssignment).isNotNull
            // One should be TEAM_A and the other TEAM_B (shuffled so we check both are assigned)
            val assignments = listOf(p1.teamAssignment, p2.teamAssignment)
            assertThat(assignments).containsExactlyInAnyOrder(
                TeamAssignment.TEAM_A,
                TeamAssignment.TEAM_B,
            )
            verify(exactly = 2) { participantRepository.save(any()) }
            verify { eventGameRepository.save(game) }
        }

        @Test
        fun `자동 팀 배정 - 미확정 참가자는 배정 제외`() {
            val game = createEventGame()
            val p1 = EventGameParticipant.create(game, 10L)
            val p2 = EventGameParticipant.create(game, 20L)
            game.addParticipant(p1)
            game.addParticipant(p2)
            p1.confirm()
            // p2 stays APPLIED (not confirmed)
            every { eventGameRepository.findByIdOrNull(any()) } returns game
            every { participantRepository.save(any()) } answers { firstArg() }
            every { eventGameRepository.save(any()) } answers { firstArg() }

            service.autoAssignTeams(1L)

            assertThat(p1.teamAssignment).isNotNull
            assertThat(p2.teamAssignment).isNull()
            verify(exactly = 1) { participantRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("completeTeamAssignment")
    inner class CompleteTeamAssignmentTest {
        @Test
        fun `팀 배정 완료 성공`() {
            val (game, p1, p2) = createGameWithConfirmedParticipants()
            p1.assignTeam(TeamAssignment.TEAM_A)
            p2.assignTeam(TeamAssignment.TEAM_B)
            game.closeRecruitment()
            every { eventGameRepository.findByIdOrNull(any()) } returns game
            every { eventGameRepository.save(any()) } answers { firstArg() }

            val result = service.completeTeamAssignment(1L)

            assertThat(result.status).isEqualTo(EventGameStatus.TEAM_ASSIGNED)
        }
    }

    @Nested
    @DisplayName("startGame")
    inner class StartGameTest {
        @Test
        fun `경기 시작 성공`() {
            val (game, p1, p2) = createGameWithConfirmedParticipants()
            p1.assignTeam(TeamAssignment.TEAM_A)
            p2.assignTeam(TeamAssignment.TEAM_B)
            game.closeRecruitment()
            game.completeTeamAssignment()
            every { eventGameRepository.findByIdOrNull(any()) } returns game
            every { eventGameRepository.save(any()) } answers { firstArg() }

            val result = service.startGame(1L)

            assertThat(result.status).isEqualTo(EventGameStatus.IN_PROGRESS)
            assertThat(result.startedAt).isNotNull()
        }
    }

    @Nested
    @DisplayName("finishGame")
    inner class FinishGameTest {
        @Test
        fun `경기 종료 성공`() {
            val (game, p1, p2) = createGameWithConfirmedParticipants()
            p1.assignTeam(TeamAssignment.TEAM_A)
            p2.assignTeam(TeamAssignment.TEAM_B)
            game.closeRecruitment()
            game.completeTeamAssignment()
            game.start()
            every { eventGameRepository.findByIdOrNull(any()) } returns game
            every { eventGameRepository.save(any()) } answers { firstArg() }

            val result = service.finishGame(1L, 5, 3)

            assertThat(result.status).isEqualTo(EventGameStatus.FINISHED)
            assertThat(result.teamAScore).isEqualTo(5)
            assertThat(result.teamBScore).isEqualTo(3)
            assertThat(result.endedAt).isNotNull()
        }

        @Test
        fun `0대0 경기 종료`() {
            val (game, p1, p2) = createGameWithConfirmedParticipants()
            p1.assignTeam(TeamAssignment.TEAM_A)
            p2.assignTeam(TeamAssignment.TEAM_B)
            game.closeRecruitment()
            game.completeTeamAssignment()
            game.start()
            every { eventGameRepository.findByIdOrNull(any()) } returns game
            every { eventGameRepository.save(any()) } answers { firstArg() }

            val result = service.finishGame(1L, 0, 0)

            assertThat(result.status).isEqualTo(EventGameStatus.FINISHED)
            assertThat(result.teamAScore).isEqualTo(0)
            assertThat(result.teamBScore).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("cancelGame")
    inner class CancelGameTest {
        @Test
        fun `경기 취소 성공`() {
            val game = createEventGame()
            every { eventGameRepository.findByIdOrNull(any()) } returns game
            every { eventGameRepository.save(any()) } answers { firstArg() }

            val result = service.cancelGame(1L, "우천 취소")

            assertThat(result.status).isEqualTo(EventGameStatus.CANCELLED)
            assertThat(result.cancelReason).isEqualTo("우천 취소")
        }
    }

    @Nested
    @DisplayName("getParticipants")
    inner class GetParticipantsTest {
        @Test
        fun `참가자 목록 조회 성공`() {
            val game = createEventGame()
            val participants =
                listOf(
                    EventGameParticipant.create(game, 10L),
                    EventGameParticipant.create(game, 20L),
                )
            every { eventGameRepository.findByIdOrNull(any()) } returns game
            every { participantRepository.findByEventGameId(1L) } returns participants

            val result = service.getParticipants(1L)

            assertThat(result).hasSize(2)
            verify { participantRepository.findByEventGameId(1L) }
        }

        @Test
        fun `존재하지 않는 이벤트 게임의 참가자 조회 시 예외`() {
            every { eventGameRepository.findByIdOrNull(999L) } returns null

            assertThatThrownBy { service.getParticipants(999L) }
                .isInstanceOf(EventGameNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("getPlayerHistory")
    inner class GetPlayerHistoryTest {
        @Test
        fun `플레이어 참가 이력 조회 성공`() {
            val game = createEventGame()
            val participants = listOf(EventGameParticipant.create(game, 10L))
            every { participantRepository.findByPlayerId(10L) } returns participants

            val result = service.getPlayerHistory(10L)

            assertThat(result).hasSize(1)
            verify { participantRepository.findByPlayerId(10L) }
        }

        @Test
        fun `참가 이력 없는 경우 빈 목록`() {
            every { participantRepository.findByPlayerId(999L) } returns emptyList()

            val result = service.getPlayerHistory(999L)

            assertThat(result).isEmpty()
        }
    }
}
