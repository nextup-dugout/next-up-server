package com.nextup.infrastructure.service.eventgame

import com.nextup.common.exception.EventGameNotFoundException
import com.nextup.common.exception.PlayerNotLinkedException
import com.nextup.common.exception.UserNotFoundException
import com.nextup.core.domain.eventgame.EventGame
import com.nextup.core.domain.eventgame.EventGameParticipant
import com.nextup.core.domain.eventgame.EventGameParticipantStatus
import com.nextup.core.domain.eventgame.EventGameStatus
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.user.User
import com.nextup.core.port.repository.EventGameParticipantRepositoryPort
import com.nextup.core.port.repository.EventGameRepositoryPort
import com.nextup.core.port.repository.UserRepositoryPort
import com.nextup.core.service.eventgame.CreateEventGameCommand
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
    private lateinit var userRepository: UserRepositoryPort
    private lateinit var service: EventGameServiceImpl

    @BeforeEach
    fun setUp() {
        eventGameRepository = mockk()
        participantRepository = mockk()
        userRepository = mockk()
        service = EventGameServiceImpl(eventGameRepository, participantRepository, userRepository)
    }

    private fun createPlayer(id: Long = 10L): Player {
        val player =
            Player(
                name = "테스트 선수",
                primaryPosition = Position.CENTER_FIELD,
            )
        val idField = Player::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(player, id)
        return player
    }

    private fun createUserWithPlayer(
        userId: Long = 1L,
        player: Player,
    ): User {
        val user =
            User.createLocalUser(
                email = "test@example.com",
                encodedPassword = "encoded",
                nickname = "테스터",
            )
        val idField = user.javaClass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(user, userId)
        user.linkPlayer(player)
        return user
    }

    private fun createUserWithoutPlayer(userId: Long = 1L): User {
        val user =
            User.createLocalUser(
                email = "test@example.com",
                encodedPassword = "encoded",
                nickname = "테스터",
            )
        val idField = user.javaClass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(user, userId)
        return user
    }

    private fun createEventGame(
        id: Long = 1L,
        maxParticipants: Int = 20,
    ): EventGame {
        val game =
            EventGame.create(
                organizerId = 100L,
                title = "주말 픽업 게임",
                scheduledAt = LocalDateTime.now().plusDays(7),
                maxParticipants = maxParticipants,
            )
        return game
    }

    /**
     * CLOSED 상태이고, 확정 참가자 2명에게 팀이 배정된 게임을 생성합니다.
     * completeTeamAssignment() 호출이 가능한 상태.
     */
    private fun createEventGameWithTeamAssigned(): EventGame {
        val game = createEventGame()
        val p1 = EventGameParticipant.create(game, 10L)
        val p2 = EventGameParticipant.create(game, 20L)
        game.addParticipant(p1)
        game.addParticipant(p2)
        p1.confirm()
        p2.confirm()
        game.closeRecruitment()
        p1.assignTeam(com.nextup.core.domain.eventgame.TeamAssignment.TEAM_A)
        p2.assignTeam(com.nextup.core.domain.eventgame.TeamAssignment.TEAM_B)
        return game
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
    }

    @Nested
    @DisplayName("getEventGame")
    inner class GetEventGameTest {
        @Test
        fun `존재하지 않는 이벤트 게임 조회 시 예외`() {
            every { eventGameRepository.findByIdOrNull(999L) } returns null

            assertThatThrownBy { service.getEventGame(999L) }
                .isInstanceOf(EventGameNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("joinEventGame")
    inner class JoinEventGameTest {
        @Test
        fun `참가 신청 성공`() {
            val player = createPlayer(id = 10L)
            val user = createUserWithPlayer(userId = 1L, player = player)
            val game = createEventGame()
            every { userRepository.findByIdOrNull(1L) } returns user
            every { eventGameRepository.findByIdOrNull(any()) } returns game
            every { eventGameRepository.save(any()) } answers { firstArg() }

            val result = service.joinEventGame(1L, 1L, "참가합니다")

            assertThat(result.playerId).isEqualTo(10L)
            assertThat(result.status).isEqualTo(EventGameParticipantStatus.APPLIED)
            assertThat(result.message).isEqualTo("참가합니다")
        }

        @Test
        fun `사용자를 찾을 수 없으면 예외 발생`() {
            every { userRepository.findByIdOrNull(999L) } returns null

            assertThatThrownBy { service.joinEventGame(1L, 999L) }
                .isInstanceOf(UserNotFoundException::class.java)
        }

        @Test
        fun `선수 프로필이 없으면 예외 발생`() {
            val user = createUserWithoutPlayer(userId = 1L)
            every { userRepository.findByIdOrNull(1L) } returns user

            assertThatThrownBy { service.joinEventGame(1L, 1L) }
                .isInstanceOf(PlayerNotLinkedException::class.java)
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
        }
    }

    @Nested
    @DisplayName("cancelParticipation")
    inner class CancelParticipationTest {
        @Test
        fun `참가 취소 성공`() {
            val game = createEventGame()
            val participant = EventGameParticipant.create(game, 10L)
            participant.confirm()
            every { eventGameRepository.findByIdOrNull(any()) } returns game
            every { participantRepository.findByIdOrNull(any()) } returns participant
            every { participantRepository.save(any()) } answers { firstArg() }

            val result = service.cancelParticipation(1L, 1L)

            assertThat(result.status).isEqualTo(EventGameParticipantStatus.CANCELLED)
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
        fun `팀 배정 성공`() {
            val game = createEventGame()
            val participant = EventGameParticipant.create(game, 10L)
            participant.confirm()
            every { eventGameRepository.findByIdOrNull(any()) } returns game
            every { participantRepository.findByIdOrNull(any()) } returns participant
            every { participantRepository.save(any()) } answers { firstArg() }

            val result =
                service.assignTeam(1L, 1L, com.nextup.core.domain.eventgame.TeamAssignment.TEAM_A)

            assertThat(result.teamAssignment)
                .isEqualTo(com.nextup.core.domain.eventgame.TeamAssignment.TEAM_A)
        }
    }

    @Nested
    @DisplayName("completeTeamAssignment")
    inner class CompleteTeamAssignmentTest {
        @Test
        fun `팀 배정 완료 성공`() {
            val game = createEventGameWithTeamAssigned()
            // status = CLOSED (team assigned 직전 상태)
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
            val game = createEventGameWithTeamAssigned()
            game.completeTeamAssignment()
            every { eventGameRepository.findByIdOrNull(any()) } returns game
            every { eventGameRepository.save(any()) } answers { firstArg() }

            val result = service.startGame(1L)

            assertThat(result.status).isEqualTo(EventGameStatus.IN_PROGRESS)
        }
    }

    @Nested
    @DisplayName("finishGame")
    inner class FinishGameTest {
        @Test
        fun `경기 종료 성공`() {
            val game = createEventGameWithTeamAssigned()
            game.completeTeamAssignment()
            game.start()
            every { eventGameRepository.findByIdOrNull(any()) } returns game
            every { eventGameRepository.save(any()) } answers { firstArg() }

            val result = service.finishGame(1L, 5, 3)

            assertThat(result.status).isEqualTo(EventGameStatus.FINISHED)
            assertThat(result.teamAScore).isEqualTo(5)
            assertThat(result.teamBScore).isEqualTo(3)
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

            val result = service.cancelGame(1L, "우천으로 취소")

            assertThat(result.status).isEqualTo(EventGameStatus.CANCELLED)
            assertThat(result.cancelReason).isEqualTo("우천으로 취소")
        }
    }

    @Nested
    @DisplayName("getParticipants")
    inner class GetParticipantsTest {
        @Test
        fun `참가자 목록 조회`() {
            val game = createEventGame()
            val participants =
                listOf(
                    EventGameParticipant.create(game, 10L),
                    EventGameParticipant.create(game, 20L),
                )
            every { eventGameRepository.findByIdOrNull(any()) } returns game
            every { participantRepository.findByEventGameId(any()) } returns participants

            val result = service.getParticipants(1L)

            assertThat(result).hasSize(2)
        }
    }

    @Nested
    @DisplayName("getPlayerHistory")
    inner class GetPlayerHistoryTest {
        @Test
        fun `선수 참가 이력 조회`() {
            val game = createEventGame()
            val participations = listOf(EventGameParticipant.create(game, 10L))
            every { participantRepository.findByPlayerId(10L) } returns participations

            val result = service.getPlayerHistory(10L)

            assertThat(result).hasSize(1)
        }
    }

    @Nested
    @DisplayName("autoAssignTeams")
    inner class AutoAssignTeamsTest {
        @Test
        fun `자동 팀 배정 성공`() {
            val game = createEventGame()
            // 참가자는 모집 중일 때 추가
            val p1 = EventGameParticipant.create(game, 10L)
            val p2 = EventGameParticipant.create(game, 20L)
            game.addParticipant(p1)
            game.addParticipant(p2)
            p1.confirm()
            p2.confirm()
            // 모집 마감
            game.closeRecruitment()

            every { eventGameRepository.findByIdOrNull(any()) } returns game
            every { eventGameRepository.save(any()) } answers { firstArg() }
            every { participantRepository.save(any()) } answers { firstArg() }

            val result = service.autoAssignTeams(1L)

            assertThat(result).isNotNull()
            val assignedParticipants =
                result.participants.filter { it.teamAssignment != null }
            assertThat(assignedParticipants).hasSize(2)
        }
    }
}
