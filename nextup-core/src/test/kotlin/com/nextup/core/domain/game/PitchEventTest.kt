package com.nextup.core.domain.game

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

class PitchEventTest {
    @Test
    fun `투구 이벤트를 생성할 수 있다`() {
        // given
        val game = createGame()
        val pitcher = createPitcher(game)
        val batter = createBatter(game)

        // when
        val pitchEvent =
            PitchEvent.create(
                game = game,
                pitcher = pitcher,
                batter = batter,
                pitchNumber = 1,
                result = PitchResult.STRIKE,
                ballCount = 0,
                strikeCount = 1,
                description = "스트라이크",
            )

        // then
        assertThat(pitchEvent.game).isEqualTo(game)
        assertThat(pitchEvent.pitcher).isEqualTo(pitcher)
        assertThat(pitchEvent.batter).isEqualTo(batter)
        assertThat(pitchEvent.pitchNumber).isEqualTo(1)
        assertThat(pitchEvent.result).isEqualTo(PitchResult.STRIKE)
        assertThat(pitchEvent.ballCount).isEqualTo(0)
        assertThat(pitchEvent.strikeCount).isEqualTo(1)
        assertThat(pitchEvent.countDisplay).isEqualTo("0B-1S")
    }

    @Test
    fun `투수가 아닌 선수는 투구할 수 없다`() {
        // given
        val game = createGame()
        val pitcher = createBatter(game) // 타자를 투수로 사용
        val batter = createBatter(game)

        // when & then
        assertThrows<IllegalArgumentException> {
            PitchEvent.create(
                game = game,
                pitcher = pitcher,
                batter = batter,
                pitchNumber = 1,
                result = PitchResult.STRIKE,
                ballCount = 0,
                strikeCount = 1,
            )
        }
    }

    @Test
    fun `투수와 타자는 서로 다른 팀이어야 한다`() {
        // given
        val game = createGame()
        val homeTeam = createHomeTeam(game)
        val pitcher = createGamePlayer(homeTeam, Position.STARTING_PITCHER)
        val batter = createGamePlayer(homeTeam, Position.FIRST_BASE) // 같은 팀

        // when & then
        assertThrows<IllegalArgumentException> {
            PitchEvent.create(
                game = game,
                pitcher = pitcher,
                batter = batter,
                pitchNumber = 1,
                result = PitchResult.STRIKE,
                ballCount = 0,
                strikeCount = 1,
            )
        }
    }

    @Test
    fun `투구 번호는 1 이상이어야 한다`() {
        // given
        val game = createGame()
        val pitcher = createPitcher(game)
        val batter = createBatter(game)

        // when & then
        assertThrows<IllegalArgumentException> {
            PitchEvent.create(
                game = game,
                pitcher = pitcher,
                batter = batter,
                pitchNumber = 0,
                result = PitchResult.STRIKE,
                ballCount = 0,
                strikeCount = 1,
            )
        }
    }

    @Test
    fun `볼카운트는 0-4 사이여야 한다`() {
        // given
        val game = createGame()
        val pitcher = createPitcher(game)
        val batter = createBatter(game)

        // when & then
        assertThrows<IllegalArgumentException> {
            PitchEvent.create(
                game = game,
                pitcher = pitcher,
                batter = batter,
                pitchNumber = 1,
                result = PitchResult.BALL,
                ballCount = 5,
                strikeCount = 0,
            )
        }
    }

    @Test
    fun `스트라이크 카운트는 0-3 사이여야 한다`() {
        // given
        val game = createGame()
        val pitcher = createPitcher(game)
        val batter = createBatter(game)

        // when & then
        assertThrows<IllegalArgumentException> {
            PitchEvent.create(
                game = game,
                pitcher = pitcher,
                batter = batter,
                pitchNumber = 1,
                result = PitchResult.STRIKE,
                ballCount = 0,
                strikeCount = 4,
            )
        }
    }

    @Test
    fun `풀카운트를 확인할 수 있다`() {
        // given
        val game = createGame()
        val pitcher = createPitcher(game)
        val batter = createBatter(game)

        // when
        val pitchEvent =
            PitchEvent.create(
                game = game,
                pitcher = pitcher,
                batter = batter,
                pitchNumber = 1,
                result = PitchResult.BALL,
                ballCount = 3,
                strikeCount = 2,
            )

        // then
        assertThat(pitchEvent.isFullCount).isTrue()
    }

    @Test
    fun `타자에게 유리한 카운트를 확인할 수 있다`() {
        // given
        val game = createGame()
        val pitcher = createPitcher(game)
        val batter = createBatter(game)

        // when
        val pitchEvent =
            PitchEvent.create(
                game = game,
                pitcher = pitcher,
                batter = batter,
                pitchNumber = 1,
                result = PitchResult.BALL,
                ballCount = 3,
                strikeCount = 1,
            )

        // then
        assertThat(pitchEvent.isHitterCount).isTrue()
        assertThat(pitchEvent.isPitcherCount).isFalse()
    }

    @Test
    fun `투수에게 유리한 카운트를 확인할 수 있다`() {
        // given
        val game = createGame()
        val pitcher = createPitcher(game)
        val batter = createBatter(game)

        // when
        val pitchEvent =
            PitchEvent.create(
                game = game,
                pitcher = pitcher,
                batter = batter,
                pitchNumber = 1,
                result = PitchResult.STRIKE,
                ballCount = 1,
                strikeCount = 2,
            )

        // then
        assertThat(pitchEvent.isPitcherCount).isTrue()
        assertThat(pitchEvent.isHitterCount).isFalse()
    }

    // Helper methods
    private fun createGame(): Game {
        val association = Association(name = "테스트 협회", id = 1L)
        val league = League(association = association, name = "테스트 리그", foundedYear = 2020, id = 1L)
        val competition =
            Competition(
                league = league,
                name = "테스트 대회",
                year = 2024,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2024, 3, 1),
                id = 1L,
            )

        return Game(
            competition = competition,
            scheduledAt = LocalDateTime.of(2024, 6, 1, 14, 0),
            status = GameStatus.IN_PROGRESS,
            currentInning = 1,
            id = 1L,
        )
    }

    private fun createHomeTeam(game: Game): GameTeam {
        val association = Association(name = "테스트 협회", id = 1L)
        val league = League(association = association, name = "테스트 리그", foundedYear = 2020, id = 1L)
        val team = Team(league = league, name = "홈팀", city = "서울", foundedYear = 2020, id = 1L)
        return GameTeam(
            game = game,
            team = team,
            homeAway = HomeAway.HOME,
            id = 1L,
        )
    }

    private fun createAwayTeam(game: Game): GameTeam {
        val association = Association(name = "테스트 협회", id = 2L)
        val league = League(association = association, name = "테스트 리그", foundedYear = 2020, id = 2L)
        val team = Team(league = league, name = "원정팀", city = "부산", foundedYear = 2020, id = 2L)
        return GameTeam(
            game = game,
            team = team,
            homeAway = HomeAway.AWAY,
            id = 2L,
        )
    }

    private fun createPitcher(game: Game): GamePlayer {
        val awayTeam = createAwayTeam(game)
        return createGamePlayer(awayTeam, Position.STARTING_PITCHER)
    }

    private fun createBatter(game: Game): GamePlayer {
        val homeTeam = createHomeTeam(game)
        return createGamePlayer(homeTeam, Position.FIRST_BASE)
    }

    private fun createGamePlayer(
        gameTeam: GameTeam,
        position: Position,
    ): GamePlayer {
        val player =
            Player(
                name = "선수",
                birthDate = LocalDate.of(1995, 1, 1),
                primaryPosition = position,
            )
        return GamePlayer(
            gameTeam = gameTeam,
            player = player,
            position = position,
            battingOrder = 1,
        )
    }
}
