package com.nextup.core.domain.stats

import com.nextup.common.exception.StatsValidationException
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

@DisplayName("SeasonPitchingStats 테스트")
class SeasonPitchingStatsTest {
    private val testPlayer =
        Player(
            name = "박투수",
            primaryPosition = Position.STARTING_PITCHER,
            throwingHand = ThrowingHand.RIGHT,
            battingHand = BattingHand.RIGHT,
            id = 1L,
        )

    @Nested
    @DisplayName("시즌 투수 통계 생성")
    inner class Create {
        @Test
        fun `should create season pitching stats successfully`() {
            // when
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // then
            assertThat(stats.player).isEqualTo(testPlayer)
            assertThat(stats.year).isEqualTo(2024)
            assertThat(stats.teamId).isNull()
            assertThat(stats.competitionType).isEqualTo(CompetitionType.LEAGUE)
            assertThat(stats.gamesPlayed).isZero
            assertThat(stats.gamesStarted).isZero
            assertThat(stats.inningsPitchedOuts).isZero
            assertThat(stats.wins).isZero
            assertThat(stats.losses).isZero
            assertThat(stats.saves).isZero
            assertThat(stats.holds).isZero
            assertThat(stats.blownSaves).isZero
            assertThat(stats.earnedRuns).isZero
            assertThat(stats.runsAllowed).isZero
            assertThat(stats.hitsAllowed).isZero
            assertThat(stats.walksAllowed).isZero
            assertThat(stats.strikeouts).isZero
            assertThat(stats.homeRunsAllowed).isZero
            assertThat(stats.hitBatsmen).isZero
            assertThat(stats.wildPitches).isZero
            assertThat(stats.balks).isZero
            assertThat(stats.battersFaced).isZero
            assertThat(stats.pitchesThrown).isNull()
            assertThat(stats.strikesThrown).isNull()
            assertThat(stats.version).isEqualTo(0L)
        }

        @Test
        fun `should throw exception when year is invalid`() {
            // when & then
            assertThrows<StatsValidationException> {
                SeasonPitchingStats.create(testPlayer, 0)
            }
        }

        @Test
        fun `should create season pitching stats with teamId`() {
            // when
            val stats = SeasonPitchingStats.create(testPlayer, 2024, teamId = 5L)

            // then
            assertThat(stats.player).isEqualTo(testPlayer)
            assertThat(stats.year).isEqualTo(2024)
            assertThat(stats.teamId).isEqualTo(5L)
            assertThat(stats.gamesPlayed).isZero
            assertThat(stats.version).isEqualTo(0L)
        }

        @Test
        fun `should create season pitching stats with competition type`() {
            // when
            val stats =
                SeasonPitchingStats.create(
                    testPlayer,
                    2024,
                    competitionType = CompetitionType.FRIENDLY,
                )

            // then
            assertThat(stats.competitionType).isEqualTo(CompetitionType.FRIENDLY)
            assertThat(stats.version).isEqualTo(0L)
        }
    }

    @Nested
    @DisplayName("계산 속성 검증")
    inner class CalculatedProperties {
        @Test
        fun `should calculate ERA correctly`() {
            // given: 9이닝(27아웃) 3자책점 = ERA 3.00
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, inningsPitchedOuts = 27, earnedRuns = 3, runsAllowed = 3)

            // when
            val era = stats.earnedRunAverage

            // then
            assertThat(era).isEqualByComparingTo(BigDecimal("3.00"))
        }

        @Test
        fun `should return zero ERA when no innings and no earned runs`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // when
            val era = stats.earnedRunAverage

            // then
            assertThat(era).isEqualByComparingTo(BigDecimal("0.00"))
        }

        @Test
        fun `should return null ERA when no innings but has earned runs`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, earnedRuns = 3, runsAllowed = 3)

            // when
            val era = stats.earnedRunAverage

            // then
            assertThat(era).isNull()
        }

        @Test
        fun `should calculate WHIP correctly`() {
            // given: 9이닝(27아웃) 9피안타 3볼넷 = WHIP (9+3)/9 = 1.33
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, inningsPitchedOuts = 27, hitsAllowed = 9, walksAllowed = 3)

            // when
            val whip = stats.whip

            // then
            assertThat(whip).isEqualByComparingTo(BigDecimal("1.33"))
        }

        @Test
        fun `should return zero WHIP when no innings`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // when
            val whip = stats.whip

            // then
            assertThat(whip).isEqualByComparingTo(BigDecimal("0.00"))
        }

        @Test
        fun `should calculate innings pitched display correctly`() {
            // given: 16아웃 = 5.1
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, inningsPitchedOuts = 16)

            // when & then
            assertThat(stats.completeInnings).isEqualTo(5)
            assertThat(stats.remainingOuts).isEqualTo(1)
            assertThat(stats.inningsPitchedDisplay).isEqualTo("5.1")
        }

        @Test
        fun `should calculate winning percentage correctly`() {
            // given: 10승 5패 = .667
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, wins = 10, losses = 5)

            // when
            val winPct = stats.winningPercentage

            // then
            assertThat(winPct).isEqualByComparingTo(BigDecimal("0.667"))
        }

        @Test
        fun `should return zero winning percentage when no decisions`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // when
            val winPct = stats.winningPercentage

            // then
            assertThat(winPct).isEqualByComparingTo(BigDecimal("0.000"))
        }

        @Test
        fun `should calculate strikeout to walk ratio correctly`() {
            // given: 9삼진 3볼넷 = K/BB 3.00
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, strikeouts = 9, walksAllowed = 3)

            // when
            val ratio = stats.strikeoutToWalkRatio

            // then
            assertThat(ratio).isEqualByComparingTo(BigDecimal("3.00"))
        }

        @Test
        fun `should calculate strikeout to walk ratio with zero walks`() {
            // given: 삼진 5, 볼넷 0
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, strikeouts = 5, walksAllowed = 0)

            // when
            val ratio = stats.strikeoutToWalkRatio

            // then
            assertThat(ratio).isEqualByComparingTo(BigDecimal("5.00"))
        }

        @Test
        fun `should return zero K-BB ratio when both are zero`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // when
            val ratio = stats.strikeoutToWalkRatio

            // then
            assertThat(ratio).isEqualByComparingTo(BigDecimal("0.00"))
        }

        @Test
        fun `should calculate strike percentage correctly`() {
            // given: 100투구 65스트라이크
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setNullableStats(stats, pitchesThrown = 100, strikesThrown = 65)

            // when
            val pct = stats.strikePercentage

            // then
            assertThat(pct).isEqualByComparingTo(BigDecimal("0.650"))
        }

        @Test
        fun `should return null strike percentage when pitches not tracked`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // when
            val pct = stats.strikePercentage

            // then
            assertThat(pct).isNull()
        }

        @Test
        fun `should calculate K per 9 correctly`() {
            // given: 9이닝(27아웃) 9삼진 = K/9 9.00
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, inningsPitchedOuts = 27, strikeouts = 9)

            // when
            val kPer9 = stats.strikeoutsPer9

            // then
            assertThat(kPer9).isEqualByComparingTo(BigDecimal("9.00"))
        }

        @Test
        fun `should return zero K per 9 when no innings`() {
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            assertThat(stats.strikeoutsPer9).isEqualByComparingTo(BigDecimal("0.00"))
        }

        @Test
        fun `should calculate BB per 9 correctly`() {
            // given: 9이닝(27아웃) 3볼넷 = BB/9 3.00
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, inningsPitchedOuts = 27, walksAllowed = 3)

            // when
            val bbPer9 = stats.walksPer9

            // then
            assertThat(bbPer9).isEqualByComparingTo(BigDecimal("3.00"))
        }

        @Test
        fun `should return zero BB per 9 when no innings`() {
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            assertThat(stats.walksPer9).isEqualByComparingTo(BigDecimal("0.00"))
        }

        @Test
        fun `should calculate unearned runs correctly`() {
            // given: 실점 5, 자책점 3 = 비자책 2
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, runsAllowed = 5, earnedRuns = 3)

            // when & then
            assertThat(stats.unearnedRuns).isEqualTo(2)
        }

        @Test
        fun `should calculate innings pitched as decimal`() {
            // given: 16아웃 = 5.33...
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, inningsPitchedOuts = 16)

            // when
            val ip = stats.inningsPitched

            // then
            assertThat(ip).isEqualByComparingTo(BigDecimal("5.33"))
        }
    }

    @Nested
    @DisplayName("유효성 검증")
    inner class Validation {
        @Test
        fun `should validate successfully with valid stats`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, gamesPlayed = 10, gamesStarted = 5, earnedRuns = 10, runsAllowed = 15)

            // when & then
            stats.validate() // Should not throw
        }

        @Test
        fun `should throw exception when games played is negative`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, gamesPlayed = -1)

            // when & then
            assertThrows<StatsValidationException> {
                stats.validate()
            }
        }

        @Test
        fun `should throw exception when games started exceeds games played`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, gamesPlayed = 5, gamesStarted = 6)

            // when & then
            assertThrows<StatsValidationException> {
                stats.validate()
            }
        }

        @Test
        fun `should throw exception when earned runs exceed runs allowed`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, earnedRuns = 10, runsAllowed = 5)

            // when & then
            assertThrows<StatsValidationException> {
                stats.validate()
            }
        }

        @Test
        fun `should throw exception when strikes exceed pitches`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setNullableStats(stats, pitchesThrown = 50, strikesThrown = 60)

            // when & then
            assertThrows<StatsValidationException> {
                stats.validate()
            }
        }
    }

    // Helper methods

    private fun setStatsDirectly(
        stats: SeasonPitchingStats,
        gamesPlayed: Int = 0,
        gamesStarted: Int = 0,
        inningsPitchedOuts: Int = 0,
        wins: Int = 0,
        losses: Int = 0,
        saves: Int = 0,
        holds: Int = 0,
        blownSaves: Int = 0,
        earnedRuns: Int = 0,
        runsAllowed: Int = 0,
        hitsAllowed: Int = 0,
        walksAllowed: Int = 0,
        strikeouts: Int = 0,
        homeRunsAllowed: Int = 0,
        hitBatsmen: Int = 0,
        wildPitches: Int = 0,
        balks: Int = 0,
        battersFaced: Int = 0,
    ) {
        val clazz = SeasonPitchingStats::class.java
        setField(clazz, stats, "gamesPlayed", gamesPlayed)
        setField(clazz, stats, "gamesStarted", gamesStarted)
        setField(clazz, stats, "inningsPitchedOuts", inningsPitchedOuts)
        setField(clazz, stats, "wins", wins)
        setField(clazz, stats, "losses", losses)
        setField(clazz, stats, "saves", saves)
        setField(clazz, stats, "holds", holds)
        setField(clazz, stats, "blownSaves", blownSaves)
        setField(clazz, stats, "earnedRuns", earnedRuns)
        setField(clazz, stats, "runsAllowed", runsAllowed)
        setField(clazz, stats, "hitsAllowed", hitsAllowed)
        setField(clazz, stats, "walksAllowed", walksAllowed)
        setField(clazz, stats, "strikeouts", strikeouts)
        setField(clazz, stats, "homeRunsAllowed", homeRunsAllowed)
        setField(clazz, stats, "hitBatsmen", hitBatsmen)
        setField(clazz, stats, "wildPitches", wildPitches)
        setField(clazz, stats, "balks", balks)
        setField(clazz, stats, "battersFaced", battersFaced)
    }

    private fun setNullableStats(
        stats: SeasonPitchingStats,
        pitchesThrown: Int? = null,
        strikesThrown: Int? = null,
    ) {
        val clazz = SeasonPitchingStats::class.java
        val ptField = clazz.getDeclaredField("pitchesThrown")
        ptField.isAccessible = true
        ptField.set(stats, pitchesThrown)
        val stField = clazz.getDeclaredField("strikesThrown")
        stField.isAccessible = true
        stField.set(stats, strikesThrown)
    }

    private fun setField(
        clazz: Class<*>,
        obj: Any,
        fieldName: String,
        value: Int,
    ) {
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(obj, value)
    }
}
