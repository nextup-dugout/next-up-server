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
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

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
}
