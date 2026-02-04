package com.nextup.scorer.websocket.mapper

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.game.*
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class GameStateMapperTest {

    private val mapper = GameStateMapper()

    @Test
    fun `should map game state to message`() {
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

        val gameState =
            GameState(
                outs = 2,
                balls = 2,
                strikes = 1,
                runnerOnFirstId = 1L,
                runnerOnSecondId = null,
                runnerOnThirdId = 3L,
                currentBatterId = 4L,
                currentPitcherId = 5L
            )

        val team = Team(league = league, name = "테스트팀", city = "서울", foundedYear = 2020)
        val gameTeam = GameTeam(game, team, HomeAway.HOME)

        val batter = Player(name = "타자", primaryPosition = Position.CENTER_FIELD)
        val pitcher = Player(name = "투수", primaryPosition = Position.STARTING_PITCHER)
        val runner1 = Player(name = "1루주자", primaryPosition = Position.SHORTSTOP)
        val runner3 = Player(name = "3루주자", primaryPosition = Position.SECOND_BASE)

        val currentBatter = GamePlayer(gameTeam, batter, Position.CENTER_FIELD, 4, 10)
        val currentPitcher = GamePlayer(gameTeam, pitcher, Position.STARTING_PITCHER, null, 1)
        val runnerOnFirst = GamePlayer(gameTeam, runner1, Position.SHORTSTOP, 2, 5)
        val runnerOnThird = GamePlayer(gameTeam, runner3, Position.SECOND_BASE, 3, 7)

        // when
        val result =
            mapper.toGameStateMessage(
                game,
                gameState,
                currentBatter,
                currentPitcher,
                runnerOnFirst,
                null,
                runnerOnThird
            )

        // then
        assertThat(result.gameId).isEqualTo(game.id)
        assertThat(result.inning).isEqualTo(game.currentInning)
        assertThat(result.isTopInning).isEqualTo(game.isTopInning)
        assertThat(result.outs).isEqualTo(2)
        assertThat(result.balls).isEqualTo(2)
        assertThat(result.strikes).isEqualTo(1)

        assertThat(result.currentBatter?.name).isEqualTo("타자")
        assertThat(result.currentBatter?.backNumber).isEqualTo(10)

        assertThat(result.currentPitcher?.name).isEqualTo("투수")
        assertThat(result.currentPitcher?.backNumber).isEqualTo(1)

        assertThat(result.runners.first?.name).isEqualTo("1루주자")
        assertThat(result.runners.second).isNull()
        assertThat(result.runners.third?.name).isEqualTo("3루주자")
    }
}
