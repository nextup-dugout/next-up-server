package com.nextup.core.domain.match

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MatchResponseTest {
    private val association = Association(name = "테스트 협회")
    private val league = League(association = association, name = "테스트 리그", foundedYear = 2020)
    private val requestTeam =
        Team(
            league = league,
            name = "요청 팀",
            city = "서울",
            foundedYear = 2020,
        )
    private val respondTeam =
        Team(
            league = league,
            name = "응답 팀",
            city = "부산",
            foundedYear = 2021,
        )

    @Test
    fun `should create match response for open request`() {
        // given
        val matchRequest =
            MatchRequest.create(
                team = requestTeam,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.ANY,
            )

        // when
        val matchResponse =
            MatchResponse.create(
                matchRequest = matchRequest,
                respondTeam = respondTeam,
                message = "함께 경기하고 싶습니다",
            )

        // then
        assertThat(matchResponse.matchRequest).isEqualTo(matchRequest)
        assertThat(matchResponse.respondTeam).isEqualTo(respondTeam)
        assertThat(matchResponse.message).isEqualTo("함께 경기하고 싶습니다")
        assertThat(matchResponse.status).isEqualTo(MatchResponseStatus.PENDING)
    }

    @Test
    fun `should throw exception when creating response for non-open request`() {
        // given
        val matchRequest =
            MatchRequest.create(
                team = requestTeam,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.ANY,
            )
        matchRequest.cancel()

        // when & then
        assertThatThrownBy {
            MatchResponse.create(
                matchRequest = matchRequest,
                respondTeam = respondTeam,
                message = null,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("OPEN 상태의 요청에만 응답할 수 있습니다")
    }

    @Test
    fun `should accept pending response`() {
        // given
        val matchRequest =
            MatchRequest.create(
                team = requestTeam,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.ANY,
            )
        val matchResponse =
            MatchResponse.create(
                matchRequest = matchRequest,
                respondTeam = respondTeam,
                message = null,
            )

        // when
        matchResponse.accept()

        // then
        assertThat(matchResponse.status).isEqualTo(MatchResponseStatus.ACCEPTED)
    }

    @Test
    fun `should throw exception when accepting non-pending response`() {
        // given
        val matchRequest =
            MatchRequest.create(
                team = requestTeam,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.ANY,
            )
        val matchResponse =
            MatchResponse.create(
                matchRequest = matchRequest,
                respondTeam = respondTeam,
                message = null,
            )
        matchResponse.reject()

        // when & then
        assertThatThrownBy { matchResponse.accept() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("PENDING 상태의 응답만 수락할 수 있습니다")
    }

    @Test
    fun `should reject pending response`() {
        // given
        val matchRequest =
            MatchRequest.create(
                team = requestTeam,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.ANY,
            )
        val matchResponse =
            MatchResponse.create(
                matchRequest = matchRequest,
                respondTeam = respondTeam,
                message = null,
            )

        // when
        matchResponse.reject()

        // then
        assertThat(matchResponse.status).isEqualTo(MatchResponseStatus.REJECTED)
    }

    @Test
    fun `should throw exception when rejecting non-pending response`() {
        // given
        val matchRequest =
            MatchRequest.create(
                team = requestTeam,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.ANY,
            )
        val matchResponse =
            MatchResponse.create(
                matchRequest = matchRequest,
                respondTeam = respondTeam,
                message = null,
            )
        matchResponse.accept()

        // when & then
        assertThatThrownBy { matchResponse.reject() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("PENDING 상태의 응답만 거절할 수 있습니다")
    }
}
