package com.nextup.core.domain.game

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.competition.GameRules
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("연장전 타이브레이크 및 이닝 제한 규칙")
class TiebreakerTest {
    private lateinit var league: League
    private lateinit var team1: Team
    private lateinit var team2: Team

    @BeforeEach
    fun setUp() {
        val association = Association(name = "서울시야구협회", region = "서울")
        league = League(association = association, name = "1부 리그", foundedYear = 2020)
        team1 = Team(league = league, name = "홈팀", city = "서울", foundedYear = 2020, id = 1L)
        team2 = Team(league = league, name = "원정팀", city = "서울", foundedYear = 2020, id = 2L)
    }

    private fun createCompetition(rules: GameRules): Competition =
        Competition(
            league = league,
            name = "2025 춘계대회",
            year = 2025,
            season = 1,
            type = CompetitionType.LEAGUE,
            startDate = LocalDate.of(2025, 3, 1),
            status = CompetitionStatus.IN_PROGRESS,
            gameRules = rules,
        )

    private fun createGame(
        competition: Competition,
        currentInning: Int,
        isTopInning: Boolean,
        totalInnings: Int = 9,
    ): Game =
        Game.createForTest(
            competition = competition,
            homeTeam = team1,
            awayTeam = team2,
            scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
            status = GameStatus.IN_PROGRESS,
            currentInning = currentInning,
            isTopInning = isTopInning,
            totalInnings = totalInnings,
        )

    @Nested
    @DisplayName("타이브레이크 미적용 (일반 이닝 전환)")
    inner class NoTiebreaker {

        @Test
        fun `타이브레이커가 비활성화된 경우 연장전에서도 NORMAL을 반환한다`() {
            // given: 타이브레이커 비활성화
            val competition =
                createCompetition(
                    GameRules(tiebreakerEnabled = false, maxExtraInnings = null),
                )
            // 9말 종료 후 다음 이닝(10회초)으로 전환
            val game = createGame(competition, currentInning = 9, isTopInning = false)

            // when
            val result = game.nextHalfInning()

            // then
            assertThat(result).isEqualTo(TiebreakerResult.NORMAL)
            assertThat(game.currentInning).isEqualTo(10)
            assertThat(game.isTopInning).isTrue()
            assertThat(game.gameState.runnerOnFirstId).isNull()
            assertThat(game.gameState.runnerOnSecondId).isNull()
        }

        @Test
        fun `정규 이닝 중 초에서 말로 전환 시 NORMAL을 반환한다`() {
            // given
            val competition = createCompetition(GameRules())
            val game = createGame(competition, currentInning = 5, isTopInning = true)

            // when
            val result = game.nextHalfInning()

            // then
            assertThat(result).isEqualTo(TiebreakerResult.NORMAL)
            assertThat(game.currentInning).isEqualTo(5)
            assertThat(game.isTopInning).isFalse()
        }

        @Test
        fun `타이브레이커가 활성화되어도 정규 이닝에서는 NORMAL을 반환한다`() {
            // given: 타이브레이커 활성화, 하지만 정규 이닝 중
            val competition = createCompetition(GameRules(tiebreakerEnabled = true))
            val game = createGame(competition, currentInning = 8, isTopInning = false)

            // when: 8말 → 9회초 (정규 이닝)
            val result = game.nextHalfInning()

            // then
            assertThat(result).isEqualTo(TiebreakerResult.NORMAL)
            assertThat(game.currentInning).isEqualTo(9)
            assertThat(game.gameState.runnerOnFirstId).isNull()
            assertThat(game.gameState.runnerOnSecondId).isNull()
        }
    }

    @Nested
    @DisplayName("타이브레이크 적용 (연장전 무사 1,2루)")
    inner class WithTiebreaker {

        @Test
        fun `타이브레이커 활성화 시 연장 이닝 초 시작에서 TIEBREAKER_APPLIED를 반환한다`() {
            // given: 9말 → 10회초 (연장전 시작)
            val competition = createCompetition(GameRules(tiebreakerEnabled = true))
            val game = createGame(competition, currentInning = 9, isTopInning = false)

            // when
            val result =
                game.nextHalfInning(
                    tiebreakerFirstRunnerId = 101L,
                    tiebreakerSecondRunnerId = 102L,
                )

            // then
            assertThat(result).isEqualTo(TiebreakerResult.TIEBREAKER_APPLIED)
            assertThat(game.currentInning).isEqualTo(10)
            assertThat(game.isTopInning).isTrue()
            assertThat(game.gameState.runnerOnFirstId).isEqualTo(101L)
            assertThat(game.gameState.runnerOnSecondId).isEqualTo(102L)
            assertThat(game.gameState.runnerOnThirdId).isNull()
            assertThat(game.gameState.outs).isEqualTo(0)
        }

        @Test
        fun `타이브레이커 주자 ID가 null이어도 기본 무사 주자 배치 상태로 적용된다`() {
            // given
            val competition = createCompetition(GameRules(tiebreakerEnabled = true))
            val game = createGame(competition, currentInning = 9, isTopInning = false)

            // when: 주자 ID 없이 타이브레이커 적용
            val result = game.nextHalfInning()

            // then
            assertThat(result).isEqualTo(TiebreakerResult.TIEBREAKER_APPLIED)
            assertThat(game.gameState.runnerOnFirstId).isNull()
            assertThat(game.gameState.runnerOnSecondId).isNull()
            assertThat(game.gameState.outs).isEqualTo(0)
        }

        @Test
        fun `타이브레이커 연장 이닝 말 전환은 NORMAL을 반환한다`() {
            // given: 이미 10회초인 상태에서 말로 전환
            val competition = createCompetition(GameRules(tiebreakerEnabled = true))
            val game = createGame(competition, currentInning = 10, isTopInning = true)

            // when: 10회초 → 10회말 (말은 타이브레이커 미적용)
            val result =
                game.nextHalfInning(
                    tiebreakerFirstRunnerId = 101L,
                    tiebreakerSecondRunnerId = 102L,
                )

            // then
            assertThat(result).isEqualTo(TiebreakerResult.NORMAL)
            assertThat(game.currentInning).isEqualTo(10)
            assertThat(game.isTopInning).isFalse()
            // 말에는 타이브레이커 주자 배치 안 함
            assertThat(game.gameState.runnerOnFirstId).isNull()
            assertThat(game.gameState.runnerOnSecondId).isNull()
        }

        @Test
        fun `연속 연장 이닝에서 매 초마다 타이브레이커가 적용된다`() {
            // given
            val competition = createCompetition(GameRules(tiebreakerEnabled = true))
            val game = createGame(competition, currentInning = 9, isTopInning = false)

            // when: 9말 → 10회초 (타이브레이커)
            val result1 =
                game.nextHalfInning(
                    tiebreakerFirstRunnerId = 101L,
                    tiebreakerSecondRunnerId = 102L,
                )
            assertThat(result1).isEqualTo(TiebreakerResult.TIEBREAKER_APPLIED)

            // when: 10회초 → 10회말 (일반)
            val result2 = game.nextHalfInning()
            assertThat(result2).isEqualTo(TiebreakerResult.NORMAL)

            // when: 10회말 → 11회초 (타이브레이커 재적용)
            val result3 =
                game.nextHalfInning(
                    tiebreakerFirstRunnerId = 201L,
                    tiebreakerSecondRunnerId = 202L,
                )
            assertThat(result3).isEqualTo(TiebreakerResult.TIEBREAKER_APPLIED)
            assertThat(game.currentInning).isEqualTo(11)
            assertThat(game.gameState.runnerOnFirstId).isEqualTo(201L)
            assertThat(game.gameState.runnerOnSecondId).isEqualTo(202L)
        }
    }

    @Nested
    @DisplayName("최대 연장 이닝 제한 (무승부 처리)")
    inner class MaxExtraInnings {

        @Test
        fun `최대 연장 이닝 도달 후 말 종료 시 DRAW_BY_INNINGS_LIMIT을 반환한다`() {
            // given: 최대 연장 이닝 = 2 (9+2=11이닝까지 허용, 11말 종료 시 무승부)
            val competition =
                createCompetition(
                    GameRules(tiebreakerEnabled = true, maxExtraInnings = 2),
                )
            // 11회말 (9 + 2 = 11이닝, 이 말이 끝나면 무승부)
            val game = createGame(competition, currentInning = 11, isTopInning = false)

            // when
            val result = game.nextHalfInning(gameTeams = game.gameTeams)

            // then
            assertThat(result).isEqualTo(TiebreakerResult.DRAW_BY_INNINGS_LIMIT)
            assertThat(game.status).isEqualTo(GameStatus.FINISHED)
            assertThat(game.endedAt).isNotNull()
        }

        @Test
        fun `최대 연장 이닝 미도달 시 DRAW_BY_INNINGS_LIMIT이 반환되지 않는다`() {
            // given: 최대 연장 이닝 = 2, 현재 10회말 (11이닝 미도달)
            val competition =
                createCompetition(
                    GameRules(tiebreakerEnabled = true, maxExtraInnings = 2),
                )
            val game = createGame(competition, currentInning = 10, isTopInning = false)

            // when: 10회말 → 11회초 (아직 제한 아님)
            val result =
                game.nextHalfInning(
                    tiebreakerFirstRunnerId = 101L,
                    tiebreakerSecondRunnerId = 102L,
                )

            // then
            assertThat(result).isEqualTo(TiebreakerResult.TIEBREAKER_APPLIED)
            assertThat(game.status).isEqualTo(GameStatus.IN_PROGRESS)
        }

        @Test
        fun `maxExtraInnings가 null이면 연장 이닝 제한이 없다`() {
            // given: 최대 연장 이닝 미설정
            val competition =
                createCompetition(
                    GameRules(tiebreakerEnabled = false, maxExtraInnings = null),
                )
            // 매우 높은 이닝도 무승부 처리 안 함
            val game = createGame(competition, currentInning = 20, isTopInning = false)

            // when
            val result = game.nextHalfInning()

            // then
            assertThat(result).isEqualTo(TiebreakerResult.NORMAL)
            assertThat(game.status).isEqualTo(GameStatus.IN_PROGRESS)
            assertThat(game.currentInning).isEqualTo(21)
        }

        @Test
        fun `무승부 처리 시 gameTeams 미전달 시에도 경기는 FINISHED 상태가 된다`() {
            // given
            val competition =
                createCompetition(
                    GameRules(tiebreakerEnabled = true, maxExtraInnings = 1),
                )
            // 10회말 (9+1=10이닝 제한)
            val game = createGame(competition, currentInning = 10, isTopInning = false)

            // when: gameTeams 빈 리스트 전달
            val result = game.nextHalfInning(gameTeams = emptyList())

            // then
            assertThat(result).isEqualTo(TiebreakerResult.DRAW_BY_INNINGS_LIMIT)
            assertThat(game.status).isEqualTo(GameStatus.FINISHED)
        }
    }

    @Nested
    @DisplayName("GameState 타이브레이크 주자 배치")
    inner class GameStateSetupTiebreaker {

        @Test
        fun `setupTiebreaker 호출 시 1루와 2루에 주자가 배치된다`() {
            // given
            val state = GameState()

            // when
            state.setupTiebreaker(firstRunnerId = 10L, secondRunnerId = 20L)

            // then
            assertThat(state.runnerOnFirstId).isEqualTo(10L)
            assertThat(state.runnerOnSecondId).isEqualTo(20L)
            assertThat(state.runnerOnThirdId).isNull()
        }

        @Test
        fun `setupTiebreaker 호출 시 기존 3루 주자는 제거된다`() {
            // given: 이전에 3루 주자 있었던 경우
            val state = GameState()
            state.setRunner(Base.THIRD, 99L)
            assertThat(state.runnerOnThirdId).isEqualTo(99L)

            // when
            state.setupTiebreaker(firstRunnerId = 10L, secondRunnerId = 20L)

            // then
            assertThat(state.runnerOnThirdId).isNull()
        }

        @Test
        fun `setupTiebreaker 후 아웃 카운트는 0이다`() {
            // given
            val state = GameState()

            // when
            state.setupTiebreaker(firstRunnerId = 10L, secondRunnerId = 20L)

            // then
            assertThat(state.outs).isEqualTo(0)
        }
    }
}
