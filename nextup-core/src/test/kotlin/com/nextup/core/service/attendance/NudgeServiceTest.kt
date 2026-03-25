package com.nextup.core.service.attendance

import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.domain.attendance.AttendancePoll
import com.nextup.core.domain.attendance.AttendanceVote
import com.nextup.core.domain.attendance.VoteType
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.notification.NotificationType
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.user.User
import com.nextup.core.port.attendance.AttendancePollRepositoryPort
import com.nextup.core.port.attendance.AttendanceVoteRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
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
    private val gameTeamRepository: GameTeamRepositoryPort = mockk()
    private val attendancePollRepository: AttendancePollRepositoryPort = mockk()
    private val attendanceVoteRepository: AttendanceVoteRepositoryPort = mockk()
    private val teamMemberRepository: TeamMemberRepositoryPort = mockk()
    private val notificationService: NotificationService = mockk()

    private val nudgeService =
        NudgeService(
            gameRepository = gameRepository,
            gameTeamRepository = gameTeamRepository,
            attendancePollRepository = attendancePollRepository,
            attendanceVoteRepository = attendanceVoteRepository,
            teamMemberRepository = teamMemberRepository,
            notificationService = notificationService,
        )

    @Test
    fun `should send nudge notifications to non-voters`() {
        // given
        val gameId = 1L
        val teamId = 10L
        val pollId = 100L
        val game = createGame(gameId)
        val gameTeam = createGameTeam(teamId)
        val poll = createPoll(pollId)
        val vote1 = createUndecidedVote(playerId = 1L)
        val vote2 = createUndecidedVote(playerId = 2L)
        val member1 = createTeamMember(teamId, userId = 10L, playerId = 1L, userName = "Player1")
        val member2 = createTeamMember(teamId, userId = 20L, playerId = 2L, userName = "Player2")

        every { gameRepository.findByIdOrNull(gameId) } returns game
        every { gameTeamRepository.findAllByGameId(gameId) } returns listOf(gameTeam)
        every { attendancePollRepository.findByGameIdAndTeamId(gameId, teamId) } returns poll
        every { attendanceVoteRepository.findByPollId(pollId) } returns listOf(vote1, vote2)
        every { teamMemberRepository.findByPlayerIdActive(1L) } returns listOf(member1)
        every { teamMemberRepository.findByPlayerIdActive(2L) } returns listOf(member2)
        every { notificationService.sendNotification(any()) } returns mockk()

        // when
        val result = nudgeService.nudgeNonVoters(gameId)

        // then
        assertThat(result.notifiedCount).isEqualTo(2)
        assertThat(result.nonVoterNames).containsExactlyInAnyOrder("Player1", "Player2")
        verify(exactly = 2) { notificationService.sendNotification(any()) }
    }

    @Test
    fun `should return empty result when no non-voters exist`() {
        // given
        val gameId = 1L
        val teamId = 10L
        val pollId = 100L
        val game = createGame(gameId)
        val gameTeam = createGameTeam(teamId)
        val poll = createPoll(pollId)
        val attendingVote = createAttendingVote(playerId = 1L)

        every { gameRepository.findByIdOrNull(gameId) } returns game
        every { gameTeamRepository.findAllByGameId(gameId) } returns listOf(gameTeam)
        every { attendancePollRepository.findByGameIdAndTeamId(gameId, teamId) } returns poll
        every { attendanceVoteRepository.findByPollId(pollId) } returns listOf(attendingVote)

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

        verify(exactly = 0) { notificationService.sendNotification(any()) }
    }

    @Test
    fun `should send custom message when provided`() {
        // given
        val gameId = 1L
        val teamId = 10L
        val pollId = 100L
        val customMessage = "Please vote for tomorrow's game!"
        val game = createGame(gameId)
        val gameTeam = createGameTeam(teamId)
        val poll = createPoll(pollId)
        val vote = createUndecidedVote(playerId = 1L)
        val member = createTeamMember(teamId, userId = 10L, playerId = 1L, userName = "Player1")

        every { gameRepository.findByIdOrNull(gameId) } returns game
        every { gameTeamRepository.findAllByGameId(gameId) } returns listOf(gameTeam)
        every { attendancePollRepository.findByGameIdAndTeamId(gameId, teamId) } returns poll
        every { attendanceVoteRepository.findByPollId(pollId) } returns listOf(vote)
        every { teamMemberRepository.findByPlayerIdActive(1L) } returns listOf(member)

        val requestSlot = slot<SendNotificationRequest>()
        every { notificationService.sendNotification(capture(requestSlot)) } returns mockk()

        // when
        nudgeService.nudgeNonVoters(gameId, customMessage)

        // then
        val capturedRequest = requestSlot.captured
        assertThat(capturedRequest.body).isEqualTo(customMessage)
        assertThat(capturedRequest.title).isEqualTo("출석 투표 요청")
        assertThat(capturedRequest.type).isEqualTo(NotificationType.ATTENDANCE_NUDGE)
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

    private fun createGameTeam(teamId: Long): GameTeam {
        val team: Team = mockk(relaxed = true)
        every { team.id } returns teamId

        val gameTeam: GameTeam = mockk(relaxed = true)
        every { gameTeam.team } returns team
        return gameTeam
    }

    private fun createPoll(pollId: Long): AttendancePoll {
        val poll: AttendancePoll = mockk(relaxed = true)
        every { poll.id } returns pollId
        return poll
    }

    private fun createUndecidedVote(playerId: Long): AttendanceVote {
        val player: Player = mockk(relaxed = true)
        every { player.id } returns playerId

        val vote: AttendanceVote = mockk(relaxed = true)
        every { vote.player } returns player
        every { vote.voteType } returns VoteType.UNDECIDED
        return vote
    }

    private fun createAttendingVote(playerId: Long): AttendanceVote {
        val player: Player = mockk(relaxed = true)
        every { player.id } returns playerId

        val vote: AttendanceVote = mockk(relaxed = true)
        every { vote.player } returns player
        every { vote.voteType } returns VoteType.ATTEND
        return vote
    }

    private fun createTeamMember(
        teamId: Long,
        userId: Long,
        playerId: Long,
        userName: String,
    ): TeamMember {
        val user: User = mockk(relaxed = true)
        every { user.id } returns userId
        every { user.nickname } returns userName

        val team: Team = mockk(relaxed = true)
        every { team.id } returns teamId

        val player: Player = mockk(relaxed = true)
        every { player.id } returns playerId

        val member: TeamMember = mockk(relaxed = true)
        every { member.user } returns user
        every { member.team } returns team
        every { member.player } returns player
        return member
    }
}
