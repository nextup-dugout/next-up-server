package com.nextup.core.domain.match

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MatchRequestTest {
    private val association = Association(name = "테스트 협회")
    private val league = League(association = association, name = "테스트 리그", foundedYear = 2020)
    private val team =
        Team(
            league = league,
            name = "테스트 팀",
            city = "서울",
            foundedYear = 2020,
        )

    @Test
    fun `should create match request with valid data`() {
        // given & when
        val matchRequest =
            MatchRequest.create(
                team = team,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = "14:00-17:00",
                preferredLocation = "서울 야구장",
                message = "연습 경기 희망합니다",
                skillLevel = SkillLevel.INTERMEDIATE,
            )

        // then
        assertThat(matchRequest.team).isEqualTo(team)
        assertThat(matchRequest.preferredDate).isEqualTo(LocalDate.now().plusDays(7))
        assertThat(matchRequest.preferredTime).isEqualTo("14:00-17:00")
        assertThat(matchRequest.preferredLocation).isEqualTo("서울 야구장")
        assertThat(matchRequest.message).isEqualTo("연습 경기 희망합니다")
        assertThat(matchRequest.skillLevel).isEqualTo(SkillLevel.INTERMEDIATE)
        assertThat(matchRequest.status).isEqualTo(MatchRequestStatus.OPEN)
        assertThat(matchRequest.version).isEqualTo(0L)
    }

    @Test
    fun `should throw exception when preferred date is in the past`() {
        // given & when & then
        assertThatThrownBy {
            MatchRequest.create(
                team = team,
                preferredDate = LocalDate.now().minusDays(1),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.ANY,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("선호 날짜는 오늘 이후여야 합니다")
    }

    @Test
    fun `should cancel open match request`() {
        // given
        val matchRequest =
            MatchRequest.create(
                team = team,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.ANY,
            )

        // when
        matchRequest.cancel()

        // then
        assertThat(matchRequest.status).isEqualTo(MatchRequestStatus.CANCELLED)
    }

    @Test
    fun `should throw exception when canceling non-open match request`() {
        // given
        val matchRequest =
            MatchRequest.create(
                team = team,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.ANY,
            )
        matchRequest.match()

        // when & then
        assertThatThrownBy { matchRequest.cancel() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("OPEN 상태의 요청만 취소할 수 있습니다")
    }

    @Test
    fun `should match open request`() {
        // given
        val matchRequest =
            MatchRequest.create(
                team = team,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.ANY,
            )

        // when
        matchRequest.match()

        // then
        assertThat(matchRequest.status).isEqualTo(MatchRequestStatus.MATCHED)
    }

    @Test
    fun `should throw exception when matching non-open request`() {
        // given
        val matchRequest =
            MatchRequest.create(
                team = team,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.ANY,
            )
        matchRequest.cancel()

        // when & then
        assertThatThrownBy { matchRequest.match() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("OPEN 상태의 요청만 매칭할 수 있습니다")
    }

    @Test
    fun `should expire open request`() {
        // given
        val matchRequest =
            MatchRequest.create(
                team = team,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.ANY,
            )

        // when
        matchRequest.expire()

        // then
        assertThat(matchRequest.status).isEqualTo(MatchRequestStatus.EXPIRED)
    }

    @Test
    fun `should throw exception when expiring non-open request`() {
        // given
        val matchRequest =
            MatchRequest.create(
                team = team,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.ANY,
            )
        matchRequest.match()

        // when & then
        assertThatThrownBy { matchRequest.expire() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("OPEN 상태의 요청만 만료시킬 수 있습니다")
    }
}
