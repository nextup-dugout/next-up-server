package com.nextup.api.mapper.attendance

import com.nextup.core.domain.attendance.AbsenceReason
import com.nextup.core.domain.game.AttendanceStatus
import com.nextup.core.domain.game.AttendanceVote
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.user.User
import com.nextup.core.service.game.dto.AttendanceSummaryDto
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AttendanceMapperTest {
    private val game = mockk<Game> { every { id } returns 1L }
    private val position = mockk<Position> { every { abbreviation } returns "P" }
    private val player =
        mockk<Player> {
            every { id } returns 100L
            every { name } returns "김투수"
            every { primaryPosition } returns position
        }
    private val user =
        mockk<User> {
            every { id } returns 10L
            every { nickname } returns "pitcher01"
        }
    private val member =
        mockk<TeamMember> {
            every { id } returns 50L
            every { this@mockk.user } returns this@AttendanceMapperTest.user
            every { this@mockk.player } returns this@AttendanceMapperTest.player
            every { uniformNumber } returns 18
        }

    @Test
    fun `should map AttendanceVote to AttendanceVoteResponse`() {
        // given
        val respondedAt = LocalDateTime.of(2026, 2, 1, 10, 0)
        val vote =
            mockk<AttendanceVote> {
                every { id } returns 1L
                every { this@mockk.game } returns this@AttendanceMapperTest.game
                every { this@mockk.member } returns this@AttendanceMapperTest.member
                every { status } returns AttendanceStatus.ATTENDING
                every { absenceReason } returns null
                every { reasonDetail } returns null
                every { this@mockk.respondedAt } returns respondedAt
            }

        // when
        val response = vote.toResponse()

        // then
        assertThat(response.voteId).isEqualTo(1L)
        assertThat(response.gameId).isEqualTo(1L)
        assertThat(response.memberId).isEqualTo(50L)
        assertThat(response.status).isEqualTo(AttendanceStatus.ATTENDING)
        assertThat(response.absenceReason).isNull()
        assertThat(response.reasonDetail).isNull()
        assertThat(response.respondedAt).isEqualTo(respondedAt)
    }

    @Test
    fun `should map AttendanceVote with absence reason to response`() {
        // given
        val respondedAt = LocalDateTime.of(2026, 2, 1, 10, 0)
        val vote =
            mockk<AttendanceVote> {
                every { id } returns 2L
                every { this@mockk.game } returns this@AttendanceMapperTest.game
                every { this@mockk.member } returns this@AttendanceMapperTest.member
                every { status } returns AttendanceStatus.ABSENT
                every { absenceReason } returns AbsenceReason.INJURY
                every { reasonDetail } returns null
                every { this@mockk.respondedAt } returns respondedAt
            }

        // when
        val response = vote.toResponse()

        // then
        assertThat(response.absenceReason).isEqualTo(AbsenceReason.INJURY)
        assertThat(response.reasonDetail).isNull()
    }

    @Test
    fun `should map AttendanceVote to MemberVoteResponse`() {
        // given
        val respondedAt = LocalDateTime.of(2026, 2, 1, 10, 0)
        val vote =
            mockk<AttendanceVote> {
                every { id } returns 2L
                every { this@mockk.member } returns this@AttendanceMapperTest.member
                every { status } returns AttendanceStatus.ABSENT
                every { absenceReason } returns AbsenceReason.OTHER
                every { reasonDetail } returns "부상"
                every { this@mockk.respondedAt } returns respondedAt
            }

        // when
        val response = vote.toMemberVoteResponse()

        // then
        assertThat(response.voteId).isEqualTo(2L)
        assertThat(response.member.memberId).isEqualTo(50L)
        assertThat(response.member.nickname).isEqualTo("pitcher01")
        assertThat(response.member.uniformNumber).isEqualTo(18)
        assertThat(response.member.position).isEqualTo("P")
        assertThat(response.status).isEqualTo(AttendanceStatus.ABSENT)
        assertThat(response.absenceReason).isEqualTo(AbsenceReason.OTHER)
        assertThat(response.reasonDetail).isEqualTo("부상")
    }

    @Test
    fun `should map AttendanceSummaryDto to AttendanceSummaryResponse`() {
        // given
        val summary =
            AttendanceSummaryDto(
                gameId = 1L,
                totalMembers = 20,
                attending = 15,
                absent = 3,
                undecided = 2,
            )

        // when
        val response = summary.toResponse()

        // then
        assertThat(response.gameId).isEqualTo(1L)
        assertThat(response.totalMembers).isEqualTo(20)
        assertThat(response.attending).isEqualTo(15)
        assertThat(response.absent).isEqualTo(3)
        assertThat(response.undecided).isEqualTo(2)
        assertThat(response.responseRate).isEqualTo(0.9)
    }

    @Test
    fun `should map List of AttendanceVote to List of AttendanceVoteResponse`() {
        // given
        val vote1 =
            mockk<AttendanceVote> {
                every { id } returns 1L
                every { this@mockk.game } returns this@AttendanceMapperTest.game
                every { this@mockk.member } returns this@AttendanceMapperTest.member
                every { status } returns AttendanceStatus.ATTENDING
                every { absenceReason } returns null
                every { reasonDetail } returns null
                every { respondedAt } returns LocalDateTime.now()
            }
        val vote2 =
            mockk<AttendanceVote> {
                every { id } returns 2L
                every { this@mockk.game } returns this@AttendanceMapperTest.game
                every { this@mockk.member } returns this@AttendanceMapperTest.member
                every { status } returns AttendanceStatus.ABSENT
                every { absenceReason } returns AbsenceReason.WORK
                every { reasonDetail } returns null
                every { respondedAt } returns LocalDateTime.now()
            }

        // when
        val responses = listOf(vote1, vote2).toResponse()

        // then
        assertThat(responses).hasSize(2)
        assertThat(responses[0].status).isEqualTo(AttendanceStatus.ATTENDING)
        assertThat(responses[1].status).isEqualTo(AttendanceStatus.ABSENT)
    }

    @Test
    fun `should map List of AttendanceVote to List of MemberVoteResponse`() {
        // given
        val vote =
            mockk<AttendanceVote> {
                every { id } returns 1L
                every { this@mockk.member } returns this@AttendanceMapperTest.member
                every { status } returns AttendanceStatus.UNDECIDED
                every { absenceReason } returns null
                every { reasonDetail } returns null
                every { respondedAt } returns null
            }

        // when
        val responses = listOf(vote).toMemberVoteResponse()

        // then
        assertThat(responses).hasSize(1)
        assertThat(responses[0].status).isEqualTo(AttendanceStatus.UNDECIDED)
    }
}
