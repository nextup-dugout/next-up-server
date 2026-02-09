package com.nextup.infrastructure.service.attendance

import com.nextup.common.exception.TeamMemberNotFoundException
import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.attendance.ActivityScore
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.user.User
import com.nextup.core.port.attendance.ActivityScoreRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

@DisplayName("ActivityServiceImpl")
class ActivityServiceImplTest {
    private lateinit var activityScoreRepository: ActivityScoreRepositoryPort
    private lateinit var teamRepository: TeamRepositoryPort
    private lateinit var teamMemberRepository: TeamMemberRepositoryPort
    private lateinit var activityService: ActivityServiceImpl

    private lateinit var team: Team
    private lateinit var member: TeamMember
    private lateinit var activityScore: ActivityScore

    @BeforeEach
    fun setUp() {
        activityScoreRepository = mockk()
        teamRepository = mockk()
        teamMemberRepository = mockk()
        activityService =
            ActivityServiceImpl(
                activityScoreRepository,
                teamRepository,
                teamMemberRepository,
            )

        val association = Association(name = "테스트협회", region = "서울")
        val league = League(association = association, name = "테스트 리그", foundedYear = 2024)
        team = Team(league = league, name = "테스트 팀", city = "서울", foundedYear = 2024)
        val user = User.createLocalUser(email = "test@test.com", encodedPassword = "encoded123", nickname = "테스트")
        val player = Player(name = "홍길동", primaryPosition = Position.STARTING_PITCHER)
        member = TeamMember.create(team = team, user = user, player = player, uniformNumber = 10)
        activityScore = ActivityScore.create(team = team, member = member)
    }

    @Nested
    @DisplayName("getActivityScore")
    inner class GetActivityScore {
        @Test
        fun `활동 점수를 조회할 수 있다`() {
            // given
            every { teamRepository.findByIdOrNull(1L) } returns team
            every { teamMemberRepository.findByIdOrNull(1L) } returns member
            every { activityScoreRepository.findByTeamIdAndMemberId(1L, 1L) } returns activityScore

            // when
            val result = activityService.getActivityScore(1L, 1L)

            // then
            assertThat(result).isNotNull
            assertThat(result.team).isEqualTo(team)
            assertThat(result.member).isEqualTo(member)
        }

        @Test
        fun `활동 점수가 없으면 새로 생성한다`() {
            // given
            every { teamRepository.findByIdOrNull(1L) } returns team
            every { teamMemberRepository.findByIdOrNull(1L) } returns member
            every { activityScoreRepository.findByTeamIdAndMemberId(1L, 1L) } returns null
            every { activityScoreRepository.save(any()) } returnsArgument 0

            // when
            val result = activityService.getActivityScore(1L, 1L)

            // then
            assertThat(result).isNotNull
            verify { activityScoreRepository.save(any()) }
        }

        @Test
        fun `팀이 존재하지 않으면 TeamNotFoundException 발생`() {
            // given
            every { teamRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThrows<TeamNotFoundException> {
                activityService.getActivityScore(999L, 1L)
            }
        }

        @Test
        fun `멤버가 존재하지 않으면 TeamMemberNotFoundException 발생`() {
            // given
            every { teamRepository.findByIdOrNull(1L) } returns team
            every { teamMemberRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThrows<TeamMemberNotFoundException> {
                activityService.getActivityScore(1L, 999L)
            }
        }
    }

    @Nested
    @DisplayName("updateGameParticipationRate")
    inner class UpdateGameParticipationRate {
        @Test
        fun `경기 참여율을 업데이트할 수 있다`() {
            // given
            every { teamRepository.findByIdOrNull(1L) } returns team
            every { teamMemberRepository.findByIdOrNull(1L) } returns member
            every { activityScoreRepository.findByTeamIdAndMemberId(1L, 1L) } returns activityScore
            every { activityScoreRepository.save(any()) } returnsArgument 0

            // when
            val result = activityService.updateGameParticipationRate(1L, 1L, BigDecimal("85.50"))

            // then
            assertThat(result.gameParticipationRate).isEqualByComparingTo(BigDecimal("85.50"))
            verify { activityScoreRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("updatePracticeAttendanceRate")
    inner class UpdatePracticeAttendanceRate {
        @Test
        fun `연습 참석률을 업데이트할 수 있다`() {
            // given
            every { teamRepository.findByIdOrNull(1L) } returns team
            every { teamMemberRepository.findByIdOrNull(1L) } returns member
            every { activityScoreRepository.findByTeamIdAndMemberId(1L, 1L) } returns activityScore
            every { activityScoreRepository.save(any()) } returnsArgument 0

            // when
            val result = activityService.updatePracticeAttendanceRate(1L, 1L, BigDecimal("90.00"))

            // then
            assertThat(result.practiceAttendanceRate).isEqualByComparingTo(BigDecimal("90.00"))
            verify { activityScoreRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("updateContributionScore")
    inner class UpdateContributionScore {
        @Test
        fun `기여도 점수를 업데이트할 수 있다`() {
            // given
            every { teamRepository.findByIdOrNull(1L) } returns team
            every { teamMemberRepository.findByIdOrNull(1L) } returns member
            every { activityScoreRepository.findByTeamIdAndMemberId(1L, 1L) } returns activityScore
            every { activityScoreRepository.save(any()) } returnsArgument 0

            // when
            val result = activityService.updateContributionScore(1L, 1L, BigDecimal("75.00"))

            // then
            assertThat(result.contributionScore).isEqualByComparingTo(BigDecimal("75.00"))
            verify { activityScoreRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("listActivityScores")
    inner class ListActivityScores {
        @Test
        fun `팀의 모든 활동 점수를 조회할 수 있다`() {
            // given
            every { teamRepository.existsById(1L) } returns true
            every { activityScoreRepository.findByTeamId(1L) } returns listOf(activityScore)

            // when
            val result = activityService.listActivityScores(1L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].team).isEqualTo(team)
        }

        @Test
        fun `팀이 존재하지 않으면 TeamNotFoundException 발생`() {
            // given
            every { teamRepository.existsById(999L) } returns false

            // when & then
            assertThrows<TeamNotFoundException> {
                activityService.listActivityScores(999L)
            }
        }
    }
}
