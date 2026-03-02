package com.nextup.core.domain.game

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.league.League
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("GameEvent")
class GameEventTest {
    private lateinit var game: Game
    private lateinit var batter: GamePlayer
    private lateinit var pitcher: GamePlayer

    @BeforeEach
    fun setUp() {
        game = createGame()
        batter = mockk(relaxed = true)
        pitcher = mockk(relaxed = true)
    }

    @Nested
    @DisplayName("createPlateAppearance")
    inner class CreatePlateAppearance {
        @Test
        fun `should create plate appearance event with single`() {
            // given
            val result = PlateAppearanceResult.SINGLE

            // when
            val event =
                GameEvent.createPlateAppearance(
                    game = game,
                    batter = batter,
                    pitcher = pitcher,
                    result = result,
                    description = "우전 안타",
                    outCountBefore = 0,
                    outCountAfter = 0,
                    runnersBeforeJson = null,
                    runnersAfterJson = """{"first": 1}""",
                    runsScored = 0,
                    rbis = 0,
                )

            // then
            assertThat(event.eventType).isEqualTo(GameEventType.PLATE_APPEARANCE)
            assertThat(event.plateAppearanceResult).isEqualTo(PlateAppearanceResult.SINGLE)
            assertThat(event.description).isEqualTo("우전 안타")
            assertThat(event.inning).isEqualTo(game.currentInning)
            assertThat(event.isTopInning).isEqualTo(game.isTopInning)
            assertThat(event.outCountBefore).isEqualTo(0)
            assertThat(event.outCountAfter).isEqualTo(0)
            assertThat(event.runsScored).isEqualTo(0)
            assertThat(event.rbis).isEqualTo(0)
        }

        @Test
        fun `should create plate appearance event with home run and runs scored`() {
            // given
            val result = PlateAppearanceResult.HOME_RUN

            // when
            val event =
                GameEvent.createPlateAppearance(
                    game = game,
                    batter = batter,
                    pitcher = pitcher,
                    result = result,
                    description = "3점 홈런",
                    outCountBefore = 1,
                    outCountAfter = 1,
                    runnersBeforeJson = """{"first": 1, "second": 2}""",
                    runnersAfterJson = null,
                    runsScored = 3,
                    rbis = 3,
                )

            // then
            assertThat(event.eventType).isEqualTo(GameEventType.PLATE_APPEARANCE)
            assertThat(event.plateAppearanceResult).isEqualTo(PlateAppearanceResult.HOME_RUN)
            assertThat(event.runsScored).isEqualTo(3)
            assertThat(event.rbis).isEqualTo(3)
        }

        @Test
        fun `should create plate appearance event with strikeout and out increase`() {
            // given
            val result = PlateAppearanceResult.STRIKEOUT

            // when
            val event =
                GameEvent.createPlateAppearance(
                    game = game,
                    batter = batter,
                    pitcher = pitcher,
                    result = result,
                    description = "헛스윙 삼진",
                    outCountBefore = 1,
                    outCountAfter = 2,
                    runnersBeforeJson = null,
                    runnersAfterJson = null,
                    runsScored = 0,
                    rbis = 0,
                )

            // then
            assertThat(event.outCountBefore).isEqualTo(1)
            assertThat(event.outCountAfter).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("createInningChange")
    inner class CreateInningChange {
        @Test
        fun `should create inning change event`() {
            // when
            val event =
                GameEvent.createInningChange(
                    game = game,
                    description = "1회초 종료",
                )

            // then
            assertThat(event.eventType).isEqualTo(GameEventType.INNING_CHANGE)
            assertThat(event.description).isEqualTo("1회초 종료")
            assertThat(event.outCountBefore).isEqualTo(3)
            assertThat(event.outCountAfter).isEqualTo(0)
            assertThat(event.batter).isNull()
            assertThat(event.pitcher).isNull()
        }
    }

    @Nested
    @DisplayName("createSubstitution")
    inner class CreateSubstitution {
        @Test
        fun `선수 교체 이벤트를 생성한다`() {
            // given
            val incomingPlayer = mockk<GamePlayer>(relaxed = true)
            val outgoingPlayer = mockk<GamePlayer>(relaxed = true)

            // when
            val event =
                GameEvent.createSubstitution(
                    game = game,
                    incomingPlayer = incomingPlayer,
                    outgoingPlayer = outgoingPlayer,
                    description = "5회초: 홍길동 → 김철수 (좌익수)",
                )

            // then
            assertThat(event.eventType).isEqualTo(GameEventType.SUBSTITUTION)
            assertThat(event.description).isEqualTo("5회초: 홍길동 → 김철수 (좌익수)")
            assertThat(event.inning).isEqualTo(game.currentInning)
            assertThat(event.isTopInning).isEqualTo(game.isTopInning)
            assertThat(event.outCountBefore).isEqualTo(game.gameState.outs)
            assertThat(event.outCountAfter).isEqualTo(game.gameState.outs)
            assertThat(event.batter).isEqualTo(incomingPlayer)
            assertThat(event.pitcher).isEqualTo(outgoingPlayer)
            assertThat(event.plateAppearanceResult).isNull()
            assertThat(event.runsScored).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("createGameStatus")
    inner class CreateGameStatus {
        @Test
        fun `should create game status event for game start`() {
            // when
            val event =
                GameEvent.createGameStatus(
                    game = game,
                    description = "경기 시작",
                )

            // then
            assertThat(event.eventType).isEqualTo(GameEventType.GAME_STATUS)
            assertThat(event.description).isEqualTo("경기 시작")
            assertThat(event.inning).isEqualTo(game.currentInning)
        }

        @Test
        fun `should create game status event for game end`() {
            // given
            game.currentInning = 9
            game.isTopInning = false

            // when
            val event =
                GameEvent.createGameStatus(
                    game = game,
                    description = "경기 종료",
                )

            // then
            assertThat(event.eventType).isEqualTo(GameEventType.GAME_STATUS)
            assertThat(event.description).isEqualTo("경기 종료")
            assertThat(event.inning).isEqualTo(9)
            assertThat(event.isTopInning).isFalse()
        }
    }

    private fun createGame(): Game {
        val association =
            Association(
                name = "서울시야구협회",
                abbreviation = null,
                region = "서울",
                description = null,
                logoUrl = null,
                websiteUrl = null,
            ).apply {
                val idField = Association::class.java.getDeclaredField("id")
                idField.isAccessible = true
                idField.set(this, 1L)
            }

        val league =
            League(
                association = association,
                name = "1부 리그",
                abbreviation = null,
                foundedYear = 2020,
                divisionLevel = 1,
                description = null,
                logoUrl = null,
            ).apply {
                val idField = League::class.java.getDeclaredField("id")
                idField.isAccessible = true
                idField.set(this, 1L)
            }

        val competition =
            Competition(
                league = league,
                name = "2025 춘계대회",
                year = 2025,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2025, 3, 1),
                endDate = LocalDate.of(2025, 6, 30),
                status = CompetitionStatus.IN_PROGRESS,
                description = null,
                maxTeams = null,
            ).apply {
                val idField = Competition::class.java.getDeclaredField("id")
                idField.isAccessible = true
                idField.set(this, 1L)
            }

        return Game(
            competition = competition,
            scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
            location = "잠실구장",
            fieldName = "1구장",
            gameNumber = 1,
            status = GameStatus.IN_PROGRESS,
            currentInning = 1,
            isTopInning = true,
            totalInnings = 9,
            gameState = GameState(),
        ).apply {
            val idField = Game::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, 1L)
        }
    }
}
