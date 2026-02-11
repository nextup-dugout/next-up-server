package com.nextup.core.domain.game

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.team.TeamMemberRole
import com.nextup.core.domain.user.User
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

@DisplayName("AttendanceVote 엔티티 테스트")
class AttendanceVoteTest {
    private lateinit var game: Game
    private lateinit var member: TeamMember
    private lateinit var team: Team

    @BeforeEach
    fun setUp() {
        val association = Association(name = "서울시야구협회", region = "서울")
        val league = League(association = association, name = "1부 리그", foundedYear = 2020)
        team = Team(league = league, name = "타이거즈", city = "서울", foundedYear = 2015)

        val competition = mockk<com.nextup.core.domain.competition.Competition>()
        game = Game(competition = competition, scheduledAt = LocalDateTime.now().plusDays(7))

        val user = User.createLocalUser("member@example.com", "password", "회원")
        val player = Player(name = "회원", primaryPosition = Position.SHORTSTOP)
        member = TeamMember.create(team, user, player, 7, TeamMemberRole.MEMBER)
    }

    @Nested
    @DisplayName("출석 투표 생성")
    inner class Create {
        @Test
        fun `should create vote with UNDECIDED status`() {
            // when
            val vote = AttendanceVote.createForGame(game, member)

            // then
            assertThat(vote.game).isEqualTo(game)
            assertThat(vote.member).isEqualTo(member)
            assertThat(vote.status).isEqualTo(AttendanceStatus.UNDECIDED)
            assertThat(vote.reason).isNull()
            assertThat(vote.respondedAt).isNull()
            assertThat(vote.hasResponded).isFalse()
            assertThat(vote.isAttending).isFalse()
        }
    }

    @Nested
    @DisplayName("투표하기")
    inner class Vote {
        @Test
        fun `should vote attending with reason`() {
            // given
            val vote = AttendanceVote.createForGame(game, member)

            // when
            vote.vote(AttendanceStatus.ATTENDING, "참석합니다")

            // then
            assertThat(vote.status).isEqualTo(AttendanceStatus.ATTENDING)
            assertThat(vote.reason).isEqualTo("참석합니다")
            assertThat(vote.respondedAt).isNotNull()
            assertThat(vote.hasResponded).isTrue()
            assertThat(vote.isAttending).isTrue()
        }

        @Test
        fun `should vote absent without reason`() {
            // given
            val vote = AttendanceVote.createForGame(game, member)

            // when
            vote.vote(AttendanceStatus.ABSENT)

            // then
            assertThat(vote.status).isEqualTo(AttendanceStatus.ABSENT)
            assertThat(vote.reason).isNull()
            assertThat(vote.hasResponded).isTrue()
        }

        @Test
        fun `should throw when member cannot vote`() {
            // given
            val kickedUser = User.createLocalUser("kicked@example.com", "password", "강퇴회원")
            val kickedPlayer = Player(name = "강퇴회원", primaryPosition = Position.STARTING_PITCHER)
            val owner = TeamMember.create(team, kickedUser, kickedPlayer, 1, TeamMemberRole.OWNER)
            val kickedMember = TeamMember.create(team, kickedUser, kickedPlayer, 20, TeamMemberRole.MEMBER)
            setTeamMemberId(owner, 1L)
            setTeamMemberId(kickedMember, 2L)
            kickedMember.kick("규칙 위반", owner)

            val vote = AttendanceVote.createForGame(game, kickedMember)

            // when & then
            assertThrows<IllegalStateException> {
                vote.vote(AttendanceStatus.ATTENDING)
            }
        }
    }

    @Nested
    @DisplayName("투표 변경")
    inner class ChangeVote {
        @Test
        fun `should change vote from attending to absent`() {
            // given
            val vote = AttendanceVote.createForGame(game, member)
            vote.vote(AttendanceStatus.ATTENDING)
            val firstRespondedAt = vote.respondedAt

            // when
            vote.changeVote(AttendanceStatus.ABSENT, "일정이 변경되었습니다")

            // then
            assertThat(vote.status).isEqualTo(AttendanceStatus.ABSENT)
            assertThat(vote.reason).isEqualTo("일정이 변경되었습니다")
            assertThat(vote.respondedAt).isNotEqualTo(firstRespondedAt)
        }

        @Test
        fun `should throw when changing vote without initial vote`() {
            // given
            val vote = AttendanceVote.createForGame(game, member)

            // when & then
            assertThrows<IllegalStateException> {
                vote.changeVote(AttendanceStatus.ATTENDING)
            }
        }
    }

    @Nested
    @DisplayName("투표 상태 추적")
    inner class StatusTracking {
        @Test
        fun `should track responded timestamp`() {
            // given
            val vote = AttendanceVote.createForGame(game, member)

            // when
            vote.vote(AttendanceStatus.ATTENDING)

            // then
            assertThat(vote.respondedAt).isNotNull()
            assertThat(vote.hasResponded).isTrue()
        }

        @Test
        fun `should update responded timestamp when changing vote`() {
            // given
            val vote = AttendanceVote.createForGame(game, member)
            vote.vote(AttendanceStatus.ATTENDING)
            val firstTimestamp = vote.respondedAt

            // Small delay to ensure different timestamp
            Thread.sleep(10)

            // when
            vote.changeVote(AttendanceStatus.ABSENT)

            // then
            assertThat(vote.respondedAt).isNotNull()
            assertThat(vote.respondedAt).isAfter(firstTimestamp)
        }
    }

    private fun setTeamMemberId(
        teamMember: TeamMember,
        id: Long,
    ) {
        val idField = TeamMember::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(teamMember, id)
    }
}
