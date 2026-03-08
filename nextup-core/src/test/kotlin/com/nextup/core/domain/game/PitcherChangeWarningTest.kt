package com.nextup.core.domain.game

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.competition.GameRules
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("투수 교체 제한 규칙 (한 타자 최소 대면)")
class PitcherChangeWarningTest {
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
                name = "2025 춘계대회",
                year = 2025,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2025, 3, 1),
                status = CompetitionStatus.IN_PROGRESS,
                gameRules = GameRules(),
            )
        homeTeam = Team(league = league, name = "홈팀", city = "서울", foundedYear = 2020, id = 1L)
        awayTeam = Team(league = league, name = "원정팀", city = "부산", foundedYear = 2020, id = 2L)
    }

    private fun createInProgressGame(outs: Int = 0): Game {
        val game =
            Game.createForTest(
                competition = competition,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
                status = GameStatus.IN_PROGRESS,
                currentInning = 3,
                isTopInning = true,
            )
        repeat(outs) { game.recordOut() }
        return game
    }

    @Nested
    @DisplayName("이닝 중 투수 교체")
    inner class MidInningChange {

        @Test
        fun `이닝 중 0명 대면한 투수 교체 시 경고를 반환한다`() {
            // given
            val game = createInProgressGame(outs = 0)

            // when
            val warning = game.checkPitcherChangeAllowed(currentPitcherBattersFaced = 0)

            // then
            assertThat(warning).isNotNull
            assertThat(warning!!.currentBattersFaced).isEqualTo(0)
            assertThat(warning.minBattersFaced).isEqualTo(Game.DEFAULT_MIN_BATTERS_FACED)
            assertThat(warning.message).contains("0명")
            assertThat(warning.message).contains("최소 1명")
        }

        @Test
        fun `이닝 중 1명 이상 대면한 투수 교체 시 경고 없음`() {
            // given
            val game = createInProgressGame(outs = 0)

            // when
            val warning = game.checkPitcherChangeAllowed(currentPitcherBattersFaced = 1)

            // then
            assertThat(warning).isNull()
        }

        @Test
        fun `최소 대면 타자 수를 2명으로 설정 시 1명 대면 투수 교체는 경고`() {
            // given
            val game = createInProgressGame(outs = 0)

            // when
            val warning =
                game.checkPitcherChangeAllowed(
                    currentPitcherBattersFaced = 1,
                    minBattersFaced = 2,
                )

            // then
            assertThat(warning).isNotNull
            assertThat(warning!!.currentBattersFaced).isEqualTo(1)
            assertThat(warning.minBattersFaced).isEqualTo(2)
        }

        @Test
        fun `최소 대면 타자 수를 2명으로 설정 시 2명 이상 대면하면 경고 없음`() {
            // given
            val game = createInProgressGame(outs = 0)

            // when
            val warning =
                game.checkPitcherChangeAllowed(
                    currentPitcherBattersFaced = 2,
                    minBattersFaced = 2,
                )

            // then
            assertThat(warning).isNull()
        }
    }

    @Nested
    @DisplayName("이닝 완료 후 투수 교체")
    inner class EndOfInningChange {

        @Test
        fun `3아웃 후 투수 교체는 대면 타자 수와 관계없이 항상 허용된다`() {
            // given - 3아웃 (이닝 종료)
            val game = createInProgressGame(outs = 3)

            // when: 0명 대면했어도 경고 없음
            val warning = game.checkPitcherChangeAllowed(currentPitcherBattersFaced = 0)

            // then
            assertThat(warning).isNull()
        }
    }

    @Nested
    @DisplayName("잘못된 입력 처리")
    inner class InvalidInput {

        @Test
        fun `최소 대면 타자 수가 0 이하이면 예외가 발생한다`() {
            // given
            val game = createInProgressGame()

            // when & then
            assertThatThrownBy {
                game.checkPitcherChangeAllowed(currentPitcherBattersFaced = 0, minBattersFaced = 0)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("최소 대면 타자 수는 1 이상이어야 합니다")
        }

        @Test
        fun `진행 중이 아닌 경기에서 투수 교체 확인 시 예외가 발생한다`() {
            // given
            val game =
                Game.createForTest(
                    competition = competition,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
                    status = GameStatus.SCHEDULED,
                )

            // when & then
            assertThatThrownBy {
                game.checkPitcherChangeAllowed(currentPitcherBattersFaced = 0)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("진행 중인 경기에서만")
        }
    }

    @Nested
    @DisplayName("PitcherChangeWarning 데이터 클래스")
    inner class WarningDataClass {

        @Test
        fun `경고 메시지에 현재 대면 타자 수와 최소 요건을 포함한다`() {
            // given
            val warning =
                PitcherChangeWarning(
                    currentBattersFaced = 0,
                    minBattersFaced = 1,
                )

            // then
            assertThat(warning.message).contains("0명")
            assertThat(warning.message).contains("1명")
        }

        @Test
        fun `경고는 데이터 클래스로 동등 비교가 가능하다`() {
            // given
            val warning1 = PitcherChangeWarning(currentBattersFaced = 0, minBattersFaced = 1)
            val warning2 = PitcherChangeWarning(currentBattersFaced = 0, minBattersFaced = 1)

            // then
            assertThat(warning1).isEqualTo(warning2)
        }
    }
}
