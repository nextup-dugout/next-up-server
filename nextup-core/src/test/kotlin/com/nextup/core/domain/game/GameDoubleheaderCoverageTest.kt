package com.nextup.core.domain.game

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("Game 더블헤더 커버리지 보완 테스트")
class GameDoubleheaderCoverageTest {
    private lateinit var competition: Competition
    private lateinit var league: League
    private lateinit var homeTeam: Team
    private lateinit var awayTeam: Team

    @BeforeEach
    fun setUp() {
        val association = Association(name = "서울시야구협회", region = "서울")
        league = League(association = association, name = "1부 리그", foundedYear = 2020)
        competition =
            Competition(
                league = league,
                name = "2025 춨계대회",
                year = 2025,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2025, 3, 1),
                status = CompetitionStatus.IN_PROGRESS,
            )
        homeTeam = Team(league = league, name = "홈팀", city = "서울", foundedYear = 2020, id = 1L)
        awayTeam = Team(league = league, name = "원정팀", city = "부산", foundedYear = 2020, id = 2L)
    }

    @Test
    fun `더블헤더 경기에 gameNumber가 3이면 예외가 발생한다`() {
        assertThatThrownBy {
            Game.create(
                competition = competition,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
                gameNumber = 3,
                isDoubleheader = true,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("gameNumber가 1 또는 2여야 합니다")
    }

    @Test
    fun `더블헤더이지만 gameNumber가 null이면 doubleheaderDisplay는 null이다`() {
        // given - createForTest로 isDoubleheader=true, gameNumber=null 조합 생성
        val game =
            Game.createForTest(
                competition = competition,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
                isDoubleheader = true,
                gameNumber = null,
            )

        // then
        assertThat(game.isDoubleheader).isTrue()
        assertThat(game.doubleheaderDisplay).isNull()
    }

    @Test
    fun `더블헤더가 아닌 경기는 isDoubleheader가 false이다`() {
        // given
        val game =
            Game.create(
                competition = competition,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
            )

        // then
        assertThat(game.isDoubleheader).isFalse()
        assertThat(game.doubleheaderDisplay).isNull()
    }
}
