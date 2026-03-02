package com.nextup.infrastructure.persistence.attendance

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.attendance.ActivityScore
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.team.TeamMemberStatus
import com.nextup.core.domain.user.User
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull

@DisplayName("ActivityScoreRepositoryAdapter 테스트")
class ActivityScoreRepositoryAdapterTest {
    private lateinit var jpaRepository: ActivityScoreJpaRepository
    private lateinit var adapter: ActivityScoreRepositoryAdapter

    private lateinit var team: Team
    private lateinit var member: TeamMember
    private lateinit var activityScore: ActivityScore

    @BeforeEach
    fun setUp() {
        jpaRepository = mockk()
        adapter = ActivityScoreRepositoryAdapter(jpaRepository)

        val association = Association(name = "테스트협회", region = "서울")
        val league = League(association = association, name = "테스트 리그", foundedYear = 2024)
        team = Team(league = league, name = "테스트 팀", city = "서울", foundedYear = 2024)
        val user =
            User.createLocalUser(
                email = "test@test.com",
                encodedPassword = "encoded123",
                nickname = "테스트",
            )
        val player = Player(name = "홍길동", primaryPosition = Position.STARTING_PITCHER)
        member = TeamMember.create(team = team, user = user, player = player, uniformNumber = 10)
        activityScore = ActivityScore.create(team = team, member = member)
    }

    @Nested
    @DisplayName("save")
    inner class Save {
        @Test
        @DisplayName("활동 점수를 저장한다")
        fun `should save activity score`() {
            // given
            val activityScore = mockk<ActivityScore>()
            every { jpaRepository.save(activityScore) } returns activityScore

            // when
            val result = adapter.save(activityScore)

            // then
            assertThat(result).isEqualTo(activityScore)
            verify { jpaRepository.save(activityScore) }
        }
    }

    @Nested
    @DisplayName("findById")
    inner class FindById {
        @Test
        @DisplayName("ID로 활동 점수를 조회한다")
        fun `should return activity score when found`() {
            // given
            val activityScore = mockk<ActivityScore>()
            every { jpaRepository.findByIdOrNull(1L) } returns activityScore

            // when
            val result = adapter.findById(1L)

            // then
            assertThat(result).isEqualTo(activityScore)
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회 시 null을 반환한다")
        fun `should return null when not found`() {
            // given
            every { jpaRepository.findByIdOrNull(999L) } returns null

            // when
            val result = adapter.findById(999L)

            // then
            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("findByTeamAndMember")
    inner class FindByTeamAndMember {
        @Test
        @DisplayName("팀과 멤버로 활동 점수를 조회한다")
        fun `should return activity score for team and member`() {
            // given
            val team = mockk<Team>()
            val member = mockk<TeamMember>()
            val activityScore = mockk<ActivityScore>()
            every { team.id } returns 1L
            every { member.id } returns 100L
            every { jpaRepository.findByTeamIdAndMemberId(1L, 100L) } returns activityScore

            // when
            val result = adapter.findByTeamAndMember(team, member)

            // then
            assertThat(result).isEqualTo(activityScore)
        }

        @Test
        @DisplayName("존재하지 않으면 null을 반환한다")
        fun `should return null when not found`() {
            // given
            val team = mockk<Team>()
            val member = mockk<TeamMember>()
            every { team.id } returns 1L
            every { member.id } returns 999L
            every { jpaRepository.findByTeamIdAndMemberId(1L, 999L) } returns null

            // when
            val result = adapter.findByTeamAndMember(team, member)

            // then
            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("findByTeamIdAndMemberId")
    inner class FindByTeamIdAndMemberId {
        @Test
        @DisplayName("팀 ID와 멤버 ID로 활동 점수를 조회한다")
        fun `should return activity score for teamId and memberId`() {
            // given
            val activityScore = mockk<ActivityScore>()
            every { jpaRepository.findByTeamIdAndMemberId(1L, 100L) } returns activityScore

            // when
            val result = adapter.findByTeamIdAndMemberId(1L, 100L)

            // then
            assertThat(result).isEqualTo(activityScore)
        }
    }

    @Nested
    @DisplayName("findByTeam")
    inner class FindByTeam {
        @Test
        @DisplayName("팀의 모든 활동 점수를 조회한다")
        fun `should return all activity scores for team`() {
            // given
            val team = mockk<Team>()
            val scores = listOf(mockk<ActivityScore>(), mockk<ActivityScore>())
            every { team.id } returns 1L
            every { jpaRepository.findByTeamId(1L) } returns scores

            // when
            val result = adapter.findByTeam(team)

            // then
            assertThat(result).hasSize(2)
        }
    }

    @Nested
    @DisplayName("findByTeamId")
    inner class FindByTeamId {
        @Test
        @DisplayName("팀 ID로 모든 활동 점수를 조회한다")
        fun `should return all activity scores for teamId`() {
            // given
            val scores = listOf(mockk<ActivityScore>())
            every { jpaRepository.findByTeamId(1L) } returns scores

            // when
            val result = adapter.findByTeamId(1L)

            // then
            assertThat(result).hasSize(1)
        }

        @Test
        @DisplayName("활동 점수가 없으면 빈 리스트를 반환한다")
        fun `should return empty list when no scores`() {
            // given
            every { jpaRepository.findByTeamId(999L) } returns emptyList()

            // when
            val result = adapter.findByTeamId(999L)

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("findByTeamIdAndMemberStatus")
    inner class FindByTeamIdAndMemberStatus {
        @Test
        fun `팀 ID와 멤버 상태로 활동 점수를 조회할 수 있다`() {
            // given
            val teamId = 1L
            every {
                jpaRepository.findByTeamIdAndMemberStatus(teamId, TeamMemberStatus.ACTIVE)
            } returns listOf(activityScore)

            // when
            val result = adapter.findByTeamIdAndMemberStatus(teamId, TeamMemberStatus.ACTIVE)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0]).isEqualTo(activityScore)
            verify { jpaRepository.findByTeamIdAndMemberStatus(teamId, TeamMemberStatus.ACTIVE) }
        }

        @Test
        fun `해당 상태의 멤버가 없으면 빈 목록을 반환한다`() {
            // given
            val teamId = 1L
            every {
                jpaRepository.findByTeamIdAndMemberStatus(teamId, TeamMemberStatus.LEFT)
            } returns emptyList()

            // when
            val result = adapter.findByTeamIdAndMemberStatus(teamId, TeamMemberStatus.LEFT)

            // then
            assertThat(result).isEmpty()
            verify { jpaRepository.findByTeamIdAndMemberStatus(teamId, TeamMemberStatus.LEFT) }
        }
    }

    @Nested
    @DisplayName("delete")
    inner class Delete {
        @Test
        @DisplayName("활동 점수를 삭제한다")
        fun `should delete activity score`() {
            // given
            val activityScore = mockk<ActivityScore>()
            justRun { jpaRepository.delete(activityScore) }

            // when
            adapter.delete(activityScore)

            // then
            verify { jpaRepository.delete(activityScore) }
        }
    }

    @Nested
    @DisplayName("existsByTeamAndMember")
    inner class ExistsByTeamAndMember {
        @Test
        @DisplayName("팀과 멤버 조합이 존재하면 true를 반환한다")
        fun `should return true when exists`() {
            // given
            val team = mockk<Team>()
            val member = mockk<TeamMember>()
            every { team.id } returns 1L
            every { member.id } returns 100L
            every { jpaRepository.existsByTeamIdAndMemberId(1L, 100L) } returns true

            // when
            val result = adapter.existsByTeamAndMember(team, member)

            // then
            assertThat(result).isTrue()
        }

        @Test
        @DisplayName("팀과 멤버 조합이 존재하지 않으면 false를 반환한다")
        fun `should return false when not exists`() {
            // given
            val team = mockk<Team>()
            val member = mockk<TeamMember>()
            every { team.id } returns 1L
            every { member.id } returns 999L
            every { jpaRepository.existsByTeamIdAndMemberId(1L, 999L) } returns false

            // when
            val result = adapter.existsByTeamAndMember(team, member)

            // then
            assertThat(result).isFalse()
        }
    }

    @Nested
    @DisplayName("deleteByMemberId")
    inner class DeleteByMemberId {
        @Test
        @DisplayName("멤버 ID로 활동 점수를 삭제한다")
        fun `should delete activity scores by memberId`() {
            // given
            justRun { jpaRepository.deleteByMemberId(100L) }

            // when
            adapter.deleteByMemberId(100L)

            // then
            verify { jpaRepository.deleteByMemberId(100L) }
        }
    }

    @Nested
    @DisplayName("deleteByTeamId")
    inner class DeleteByTeamId {
        @Test
        @DisplayName("팀 ID로 모든 활동 점수를 삭제한다")
        fun `should delete all activity scores by teamId`() {
            // given
            justRun { jpaRepository.deleteByTeamId(1L) }

            // when
            adapter.deleteByTeamId(1L)

            // then
            verify { jpaRepository.deleteByTeamId(1L) }
        }
    }
}
