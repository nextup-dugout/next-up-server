package com.nextup.scorer.websocket.mapper

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ScoreboardMapperTest {

    private val mapper = ScoreboardMapper()

    @Test
    fun `should map game and teams to scoreboard message`() {
        // given
        val association = Association(name = "테스트협회", region = "서울")
        val league = League(association = association, name = "테스트리그", foundedYear = 2020)
        val competition =
            Competition(
                league = league,
                name = "2024 시즌",
                year = 2024,
                startDate = LocalDate.now()
            )
        val game =
            Game(
                competition = competition,
                scheduledAt = LocalDateTime.now(),
                location = "테스트구장"
            )

        val homeTeamEntity = Team(league = league, name = "홈팀", city = "서울", foundedYear = 2020)
        val awayTeamEntity = Team(league = league, name = "원정팀", city = "부산", foundedYear = 2020)

        val homeTeam =
            GameTeam(game, homeTeamEntity, HomeAway.HOME).apply {
                updateScore(5, 8, 1)
                recordInningScore(1, 0)
                recordInningScore(2, 1)
                recordInningScore(3, 2)
                recordInningScore(4, 0)
                recordInningScore(5, 2)
            }

        val awayTeam =
            GameTeam(game, awayTeamEntity, HomeAway.AWAY).apply {
                updateScore(3, 6, 0)
                recordInningScore(1, 1)
                recordInningScore(2, 0)
                recordInningScore(3, 0)
                recordInningScore(4, 2)
                recordInningScore(5, 0)
            }

        // when
        val result = mapper.toScoreboardMessage(game, homeTeam, awayTeam)

        // then
        assertThat(result.gameId).isEqualTo(game.id)
        assertThat(result.currentInning).isEqualTo(game.currentInning)
        assertThat(result.isTopInning).isEqualTo(game.isTopInning)

        assertThat(result.homeTeam.teamName).isEqualTo("홈팀")
        assertThat(result.homeTeam.runs).isEqualTo(5)
        assertThat(result.homeTeam.hits).isEqualTo(8)
        assertThat(result.homeTeam.errors).isEqualTo(1)

        assertThat(result.awayTeam.teamName).isEqualTo("원정팀")
        assertThat(result.awayTeam.runs).isEqualTo(3)
        assertThat(result.awayTeam.hits).isEqualTo(6)
        assertThat(result.awayTeam.errors).isEqualTo(0)

        assertThat(result.inningScores.homeScores).isEqualTo(listOf(0, 1, 2, 0, 2, 0, 0, 0, 0))
        assertThat(result.inningScores.awayScores).isEqualTo(listOf(1, 0, 0, 2, 0, 0, 0, 0, 0))
    }
}
