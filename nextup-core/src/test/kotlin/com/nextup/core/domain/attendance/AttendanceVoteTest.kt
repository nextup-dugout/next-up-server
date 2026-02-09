package com.nextup.core.domain.attendance

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class AttendanceVoteTest {
    private val association =
        Association(
            name = "테스트협회",
            region = "서울",
        )

    private val league =
        League(
            association = association,
            name = "테스트 리그",
            foundedYear = 2024,
        )

    private val team =
        Team(
            league = league,
            name = "테스트 팀",
            city = "서울",
            foundedYear = 2024,
        )

    private val poll =
        AttendancePoll.create(
            team = team,
            title = "테스트 투표",
            eventDate = LocalDateTime.now().plusDays(7),
            deadline = LocalDateTime.now().plusDays(5),
        )

    private val player =
        Player(
            name = "홍길동",
            primaryPosition = Position.STARTING_PITCHER,
        )

    @Test
    fun `출석 투표 응답을 생성할 수 있다`() {
        // when
        val vote =
            AttendanceVote.create(
                poll = poll,
                player = player,
                voteType = VoteType.ATTEND,
            )

        // then
        assertThat(vote.poll).isEqualTo(poll)
        assertThat(vote.player).isEqualTo(player)
        assertThat(vote.voteType).isEqualTo(VoteType.ATTEND)
    }

    @Test
    fun `투표를 변경할 수 있다`() {
        // given
        val vote =
            AttendanceVote.create(
                poll = poll,
                player = player,
                voteType = VoteType.UNDECIDED,
            )

        // when
        vote.changeVote(VoteType.ATTEND)

        // then
        assertThat(vote.voteType).isEqualTo(VoteType.ATTEND)
    }

    @Test
    fun `마감된 투표는 변경할 수 없다`() {
        // given - 투표를 먼저 생성 후 마감
        val vote =
            AttendanceVote.create(
                poll = poll,
                player = player,
                voteType = VoteType.UNDECIDED,
            )
        poll.close()

        // when & then
        assertThrows<IllegalStateException> {
            vote.changeVote(VoteType.ATTEND)
        }
    }

    @Test
    fun `참석 의사 확인이 가능하다`() {
        // given
        val attendVote =
            AttendanceVote.create(
                poll = poll,
                player = player,
                voteType = VoteType.ATTEND,
            )

        val absentVote =
            AttendanceVote.create(
                poll = poll,
                player = player,
                voteType = VoteType.ABSENT,
            )

        // when & then
        assertThat(attendVote.isAttending()).isTrue()
        assertThat(attendVote.isAbsent()).isFalse()

        assertThat(absentVote.isAttending()).isFalse()
        assertThat(absentVote.isAbsent()).isTrue()
    }

    @Test
    fun `마감된 투표에는 새로운 응답을 생성할 수 없다`() {
        // given
        poll.close()

        // when & then
        assertThrows<IllegalStateException> {
            AttendanceVote.create(
                poll = poll,
                player = player,
                voteType = VoteType.ATTEND,
            )
        }
    }
}
