package com.nextup.core.domain.attendance

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.user.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class ActivityScoreTest {
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

    private val user =
        User.createLocalUser(
            email = "test@test.com",
            encodedPassword = "encoded123",
            nickname = "테스트",
        )

    private val player =
        Player(
            name = "홍길동",
            primaryPosition = Position.STARTING_PITCHER,
        )

    private val member =
        TeamMember.create(
            team = team,
            user = user,
            player = player,
            uniformNumber = 10,
        )

    @Test
    fun `활동 점수를 생성할 수 있다`() {
        // when
        val activityScore =
            ActivityScore.create(
                team = team,
                member = member,
            )

        // then
        assertThat(activityScore.team).isEqualTo(team)
        assertThat(activityScore.member).isEqualTo(member)
        assertThat(activityScore.gameParticipationRate).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(activityScore.practiceAttendanceRate).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(activityScore.contributionScore).isEqualByComparingTo(BigDecimal.ZERO)
    }

    @Test
    fun `경기 참여율을 업데이트할 수 있다`() {
        // given
        val activityScore =
            ActivityScore.create(
                team = team,
                member = member,
            )

        // when
        activityScore.updateGameParticipationRate(BigDecimal("85.50"))

        // then
        assertThat(activityScore.gameParticipationRate).isEqualByComparingTo(BigDecimal("85.50"))
    }

    @Test
    fun `경기 참여율이 0 미만이면 예외가 발생한다`() {
        // given
        val activityScore =
            ActivityScore.create(
                team = team,
                member = member,
            )

        // when & then
        assertThrows<IllegalArgumentException> {
            activityScore.updateGameParticipationRate(BigDecimal("-1"))
        }
    }

    @Test
    fun `경기 참여율이 100 초과이면 예외가 발생한다`() {
        // given
        val activityScore =
            ActivityScore.create(
                team = team,
                member = member,
            )

        // when & then
        assertThrows<IllegalArgumentException> {
            activityScore.updateGameParticipationRate(BigDecimal("101"))
        }
    }

    @Test
    fun `연습 참석률을 업데이트할 수 있다`() {
        // given
        val activityScore =
            ActivityScore.create(
                team = team,
                member = member,
            )

        // when
        activityScore.updatePracticeAttendanceRate(BigDecimal("90.25"))

        // then
        assertThat(activityScore.practiceAttendanceRate).isEqualByComparingTo(BigDecimal("90.25"))
    }

    @Test
    fun `기여도 점수를 업데이트할 수 있다`() {
        // given
        val activityScore =
            ActivityScore.create(
                team = team,
                member = member,
            )

        // when
        activityScore.updateContributionScore(BigDecimal("75.00"))

        // then
        assertThat(activityScore.contributionScore).isEqualByComparingTo(BigDecimal("75.00"))
    }

    @Test
    fun `전체 활동 점수를 계산할 수 있다`() {
        // given
        val activityScore =
            ActivityScore.create(
                team = team,
                member = member,
            )
        activityScore.updateGameParticipationRate(BigDecimal("80.00"))
        activityScore.updatePracticeAttendanceRate(BigDecimal("90.00"))
        activityScore.updateContributionScore(BigDecimal("70.00"))

        // when
        val totalScore = activityScore.calculateTotalScore()

        // then
        // 80 * 0.4 + 90 * 0.4 + 70 * 0.2 = 32 + 36 + 14 = 82
        assertThat(totalScore).isEqualByComparingTo(BigDecimal("82.00"))
    }

    @Test
    fun `모든 점수가 0이면 전체 점수도 0이다`() {
        // given
        val activityScore =
            ActivityScore.create(
                team = team,
                member = member,
            )

        // when
        val totalScore = activityScore.calculateTotalScore()

        // then
        assertThat(totalScore).isEqualByComparingTo(BigDecimal("0.00"))
    }

    @Test
    fun `모든 점수가 100이면 전체 점수도 100이다`() {
        // given
        val activityScore =
            ActivityScore.create(
                team = team,
                member = member,
            )
        activityScore.updateGameParticipationRate(BigDecimal("100.00"))
        activityScore.updatePracticeAttendanceRate(BigDecimal("100.00"))
        activityScore.updateContributionScore(BigDecimal("100.00"))

        // when
        val totalScore = activityScore.calculateTotalScore()

        // then
        assertThat(totalScore).isEqualByComparingTo(BigDecimal("100.00"))
    }
}
