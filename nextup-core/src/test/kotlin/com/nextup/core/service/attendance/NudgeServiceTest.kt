package com.nextup.core.service.attendance

import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.domain.game.AttendanceStatus
import com.nextup.core.domain.game.AttendanceVote
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.notification.NotificationType
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.port.repository.AttendanceVoteRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.service.notification.NotificationService
import com.nextup.core.service.notification.dto.SendNotificationRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class NudgeServiceTest {
    private val gameRepository: GameRepositoryPort = mockk()
    private val attendanceVoteRepository: AttendanceVoteRepositoryPort = mockk()
    private val notificationService: NotificationService = mockk()

    private val nudgeService =
        NudgeService(
            gameRepository = gameRepository,
            attendanceVoteRepository = attendanceVoteRepository,
            notificationService = notificationService,
        )

    @Test
    fun `should send nudge notifications to non-voters`() {
        // given
        val gameId = 1L
        val game = createGame(gameId)
        val nonVoter1 = createNonVoter(userId = 10L, userName = "Player1")
        val nonVoter2 = createNonVoter(userId = 20L, userName = "Player2")

        every { gameRepository.findByIdOrNull(gameId) } returns game
        every { attendanceVoteRepository.findNonVotersByGameId(gameId) } returns
            listOf(nonVoter1, nonVoter2)
        every { notificationService.sendNotification(any()) } returns mockk()

        // when
        val result = nudgeService.nudgeNonVoters(gameId)

        // then
        assertThat(result.notifiedCount).isEqualTo(2)
        assertThat(result.nonVoterNames).containsExactlyInAnyOrder("Player1", "Player2")

        verify(exactly = 2) { notificationService.sendNotification(any()) }
    }

    @Test
    fun `should send custom message when provided`() {
        // given
        val gameId = 1L
        val customMessage = "Please vote for tomorrow's game!"
        val game = createGame(gameId)
        val nonVoter = createNonVoter(userId = 10L, userName = "Player1")

        every { gameRepository.findByIdOrNull(gameId) } returns game
        every { attendanceVoteRepository.findNonVotersByGameId(gameId) } returns listOf(nonVoter)

        val requestSlot = slot<SendNotificationRequest>()
        every { notificationService.sendNotification(capture(requestSlot)) } returns mockk()

        // when
        nudgeService.nudgeNonVoters(gameId, customMessage)

        // then
        val capturedRequest = requestSlot.captured
        assertThat(capturedRequest.body).isEqualTo(customMessage)
        assertThat(capturedRequest.title).isEqualTo("출석 투표 요청")
        assertThat(capturedRequest.type).isEqualTo(NotificationType.ATTENDANCE_NUDGE)
        assertThat(capturedRequest.data).isEqualTo("gameId=$gameId")
    }

    @Test
    fun `should use default message when custom message is not provided`() {
        // given
        val gameId = 1L
        val scheduledAt = LocalDateTime.of(2026, 3, 15, 14, 0)
        val game = createGame(gameId, scheduledAt)
        val nonVoter = createNonVoter(userId = 10L, userName = "Player1")

        every { gameRepository.findByIdOrNull(gameId) } returns game
        every { attendanceVoteRepository.findNonVotersByGameId(gameId) } returns listOf(nonVoter)

        val requestSlot = slot<SendNotificationRequest>()
        every { notificationService.sendNotification(capture(requestSlot)) } returns mockk()

        // when
        nudgeService.nudgeNonVoters(gameId)

        // then
        val capturedRequest = requestSlot.captured
        assertThat(capturedRequest.body).contains("경기($scheduledAt)")
        assertThat(capturedRequest.body).contains("출석 투표를 진행해주세요")
    }

    @Test
    fun `should return empty result when no non-voters exist`() {
        // given
        val gameId = 1L
        val game = createGame(gameId)

        every { gameRepository.findByIdOrNull(gameId) } returns game
        every { attendanceVoteRepository.findNonVotersByGameId(gameId) } returns emptyList()

        // when
        val result = nudgeService.nudgeNonVoters(gameId)

        // then
        assertThat(result.notifiedCount).isEqualTo(0)
        assertThat(result.nonVoterNames).isEmpty()

        verify(exactly = 0) { notificationService.sendNotification(any()) }
    }

    @Test
    fun `should throw GameNotFoundException when game does not exist`() {
        // given
        val gameId = 999L

        every { gameRepository.findByIdOrNull(gameId) } returns null

        // when & then
        assertThrows<GameNotFoundException> {
            nudgeService.nudgeNonVoters(gameId)
        }

        verify(exactly = 0) { attendanceVoteRepository.findNonVotersByGameId(any()) }
        verify(exactly = 0) { notificationService.sendNotification(any()) }
    }

    @Test
    fun `should send notification to each non-voter with correct userId`() {
        // given
        val gameId = 1L
        val game = createGame(gameId)
        val nonVoter1 = createNonVoter(userId = 10L, userName = "Player1")
        val nonVoter2 = createNonVoter(userId = 20L, userName = "Player2")
        val nonVoter3 = createNonVoter(userId = 30L, userName = "Player3")

        every { gameRepository.findByIdOrNull(gameId) } returns game
        every { attendanceVoteRepository.findNonVotersByGameId(gameId) } returns
            listOf(nonVoter1, nonVoter2, nonVoter3)

        val requestSlots = mutableListOf<SendNotificationRequest>()
        every { notificationService.sendNotification(capture(requestSlots)) } returns mockk()

        // when
        val result = nudgeService.nudgeNonVoters(gameId)

        // then
        assertThat(result.notifiedCount).isEqualTo(3)
        assertThat(requestSlots).hasSize(3)
        assertThat(requestSlots.map { it.userId }).containsExactlyInAnyOrder(10L, 20L, 30L)
    }

    private fun createGame(
        id: Long,
        scheduledAt: LocalDateTime = LocalDateTime.of(2026, 3, 15, 14, 0),
    ): Game {
        val game: Game = mockk(relaxed = true)
        every { game.id } returns id
        every { game.scheduledAt } returns scheduledAt
        every { game.status } returns GameStatus.SCHEDULED
        return game
    }

    private fun createNonVoter(
        userId: Long,
        userName: String,
    ): AttendanceVote {
        val user: com.nextup.core.domain.user.User = mockk(relaxed = true)
        every { user.id } returns userId
        every { user.nickname } returns userName

        val member: TeamMember = mockk(relaxed = true)
        every { member.user } returns user

        val vote: AttendanceVote = mockk(relaxed = true)
        every { vote.member } returns member
        every { vote.status } returns AttendanceStatus.UNDECIDED
        every { vote.hasResponded } returns false

        return vote
    }
}
