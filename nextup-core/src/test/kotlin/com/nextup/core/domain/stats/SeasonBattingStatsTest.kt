package com.nextup.core.domain.stats

import com.nextup.common.exception.StatsValidationException
import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

@DisplayName("SeasonBattingStats 테스트")
class SeasonBattingStatsTest {
    private val testPlayer =
        Player(
            name = "홍길동",
            primaryPosition = Position.CATCHER,
            throwingHand = ThrowingHand.RIGHT,
            battingHand = BattingHand.RIGHT,
            id = 1L,
        )

    @Nested
    @DisplayName("시즌 타격 통계 생성")
    inner class Create {
        @Test
        fun `should create season batting stats successfully`() {
            // when
            val stats = SeasonBattingStats.create(testPlayer, 2024)

            // then
            assertThat(stats.player).isEqualTo(testPlayer)
            assertThat(stats.year).isEqualTo(2024)
            assertThat(stats.gamesPlayed).isZero
            assertThat(stats.hits).isZero
            assertThat(stats.atBats).isZero
        }

        @Test
        fun `should throw exception when year is invalid`() {
            // when & then
            assertThrows<StatsValidationException> {
                SeasonBattingStats.create(testPlayer, 0)
            }
        }
    }

    @Nested
    @DisplayName("경기 기록 누적")
    inner class AddGameRecord {
        @Test
        fun `should accumulate single game record correctly`() {
            // given
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            val gamePlayer = mockk<GamePlayer>()
            val record =
                BattingRecord.create(gamePlayer).apply {
                    // 4타수 2안타 (2루타 1개) 1볼넷 1삼진
                    val field = BattingRecord::class.java.getDeclaredField("plateAppearances")
                    field.isAccessible = true
                    field.set(this, 5)

                    val atBatsField = BattingRecord::class.java.getDeclaredField("atBats")
                    atBatsField.isAccessible = true
                    atBatsField.set(this, 4)

                    val hitsField = BattingRecord::class.java.getDeclaredField("hits")
                    hitsField.isAccessible = true
                    hitsField.set(this, 2)

                    val doublesField = BattingRecord::class.java.getDeclaredField("doubles")
                    doublesField.isAccessible = true
                    doublesField.set(this, 1)

                    val walksField = BattingRecord::class.java.getDeclaredField("walks")
                    walksField.isAccessible = true
                    walksField.set(this, 1)

                    val strikeoutsField = BattingRecord::class.java.getDeclaredField("strikeouts")
                    strikeoutsField.isAccessible = true
                    strikeoutsField.set(this, 1)

                    val runsField = BattingRecord::class.java.getDeclaredField("runs")
                    runsField.isAccessible = true
                    runsField.set(this, 1)

                    val rbiField = BattingRecord::class.java.getDeclaredField("runsBattedIn")
                    rbiField.isAccessible = true
                    rbiField.set(this, 1)
                }

            // when
            stats.addGameRecord(record)

            // then
            assertThat(stats.gamesPlayed).isEqualTo(1)
            assertThat(stats.plateAppearances).isEqualTo(5)
            assertThat(stats.atBats).isEqualTo(4)
            assertThat(stats.hits).isEqualTo(2)
            assertThat(stats.doubles).isEqualTo(1)
            assertThat(stats.walks).isEqualTo(1)
            assertThat(stats.strikeouts).isEqualTo(1)
            assertThat(stats.runs).isEqualTo(1)
            assertThat(stats.runsBattedIn).isEqualTo(1)
        }

        @Test
        fun `should accumulate multiple game records correctly`() {
            // given
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            val gamePlayer = mockk<GamePlayer>()

            // Game 1: 4타수 2안타 1홈런
            val record1 =
                BattingRecord.create(gamePlayer).apply {
                    setStats(pa = 4, ab = 4, h = 2, hr = 1, runs = 2, rbi = 2)
                }

            // Game 2: 3타수 1안타 1볼넷
            val record2 =
                BattingRecord.create(gamePlayer).apply {
                    setStats(pa = 4, ab = 3, h = 1, bb = 1, runs = 0, rbi = 0)
                }

            // Game 3: 5타수 3안타 (2루타 1개, 3루타 1개)
            val record3 =
                BattingRecord.create(gamePlayer).apply {
                    setStats(pa = 5, ab = 5, h = 3, doubles = 1, triples = 1, runs = 1, rbi = 2)
                }

            // when
            stats.addGameRecord(record1)
            stats.addGameRecord(record2)
            stats.addGameRecord(record3)

            // then
            assertThat(stats.gamesPlayed).isEqualTo(3)
            assertThat(stats.plateAppearances).isEqualTo(13)
            assertThat(stats.atBats).isEqualTo(12)
            assertThat(stats.hits).isEqualTo(6)
            assertThat(stats.homeRuns).isEqualTo(1)
            assertThat(stats.doubles).isEqualTo(1)
            assertThat(stats.triples).isEqualTo(1)
            assertThat(stats.walks).isEqualTo(1)
            assertThat(stats.runs).isEqualTo(3)
            assertThat(stats.runsBattedIn).isEqualTo(4)
        }
    }

    @Nested
    @DisplayName("계산 속성 검증")
    inner class CalculatedProperties {
        @Test
        fun `should calculate batting average correctly`() {
            // given: 10타수 3안타 = .300
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, atBats = 10, hits = 3)

            // when
            val avg = stats.battingAverage

            // then
            assertThat(avg).isEqualByComparingTo(BigDecimal("0.300"))
        }

        @Test
        fun `should return zero batting average when at-bats is zero`() {
            // given
            val stats = SeasonBattingStats.create(testPlayer, 2024)

            // when
            val avg = stats.battingAverage

            // then
            assertThat(avg).isEqualByComparingTo(BigDecimal("0.000"))
        }

        @Test
        fun `should calculate on-base percentage correctly`() {
            // given: 10타수 3안타, 2볼넷, 1사구 = (3+2+1)/(10+2+1+0) = .462
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, atBats = 10, hits = 3, walks = 2, hbp = 1)

            // when
            val obp = stats.onBasePercentage

            // then
            assertThat(obp).isEqualByComparingTo(BigDecimal("0.462"))
        }

        @Test
        fun `should calculate slugging percentage correctly`() {
            // given: 10타수, 1단타, 1-2루타, 1-3루타, 1홈런
            // totalBases = 1 + 2 + 3 + 4 = 10, SLG = 10/10 = 1.000
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, atBats = 10, hits = 4, doubles = 1, triples = 1, homeRuns = 1)

            // when
            val slg = stats.sluggingPercentage

            // then
            assertThat(slg).isEqualByComparingTo(BigDecimal("1.000"))
        }

        @Test
        fun `should calculate OPS correctly`() {
            // given: OBP .400 + SLG .600 = OPS 1.000
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            // 10타수 4안타 (2루타 2개) = AVG .400, SLG .800
            // 10타수 4안타 + 볼넷 0 = OBP .400
            setStatsDirectly(stats, atBats = 10, hits = 4, doubles = 2)

            // when
            val ops = stats.ops

            // then
            // OBP = 4/10 = 0.400
            // SLG = (2 + 4)/10 = 6/10 = 0.600
            // OPS = 1.000
            assertThat(ops).isEqualByComparingTo(BigDecimal("1.000"))
        }

        @Test
        fun `should calculate stolen base percentage correctly`() {
            // given: 8도루 2실패 = 80%
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, stolenBases = 8, caughtStealing = 2)

            // when
            val sbPct = stats.stolenBasePercentage

            // then
            assertThat(sbPct).isEqualByComparingTo(BigDecimal("0.800"))
        }

        @Test
        fun `should calculate singles correctly`() {
            // given: 10안타 - 2-2루타 - 1-3루타 - 1홈런 = 6단타
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, hits = 10, doubles = 2, triples = 1, homeRuns = 1)

            // when
            val singles = stats.singles

            // then
            assertThat(singles).isEqualTo(6)
        }

        @Test
        fun `should calculate total bases correctly`() {
            // given: 6단타 + 2-2루타 + 1-3루타 + 1홈런
            // TB = 6*1 + 2*2 + 1*3 + 1*4 = 6 + 4 + 3 + 4 = 17
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, hits = 10, doubles = 2, triples = 1, homeRuns = 1)

            // when
            val totalBases = stats.totalBases

            // then
            assertThat(totalBases).isEqualTo(17)
        }
    }

    @Nested
    @DisplayName("유효성 검증")
    inner class Validation {
        @Test
        fun `should validate successfully with valid stats`() {
            // given
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, gamesPlayed = 10, pa = 50, atBats = 40, hits = 12, doubles = 2)

            // when & then
            stats.validate() // Should not throw
        }

        @Test
        fun `should throw exception when hits exceed at-bats`() {
            // given
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, atBats = 10, hits = 11)

            // when & then
            assertThrows<StatsValidationException> {
                stats.validate()
            }
        }

        @Test
        fun `should throw exception when extra-base hits exceed total hits`() {
            // given
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, hits = 5, doubles = 2, triples = 2, homeRuns = 2)

            // when & then
            assertThrows<StatsValidationException> {
                stats.validate()
            }
        }

        @Test
        fun `should throw exception when at-bats exceed plate appearances`() {
            // given
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, pa = 10, atBats = 11)

            // when & then
            assertThrows<StatsValidationException> {
                stats.validate()
            }
        }

        @Test
        fun `should throw exception when games played is negative`() {
            // given
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, gamesPlayed = -1)

            // when & then
            assertThrows<StatsValidationException> {
                stats.validate()
            }
        }
    }

    // Helper methods

    private fun BattingRecord.setStats(
        pa: Int = 0,
        ab: Int = 0,
        h: Int = 0,
        doubles: Int = 0,
        triples: Int = 0,
        hr: Int = 0,
        bb: Int = 0,
        hbp: Int = 0,
        runs: Int = 0,
        rbi: Int = 0,
    ) {
        setField("plateAppearances", pa)
        setField("atBats", ab)
        setField("hits", h)
        setField("doubles", doubles)
        setField("triples", triples)
        setField("homeRuns", hr)
        setField("walks", bb)
        setField("hitByPitch", hbp)
        setField("runs", runs)
        setField("runsBattedIn", rbi)
    }

    private fun BattingRecord.setField(
        fieldName: String,
        value: Int,
    ) {
        val field = BattingRecord::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(this, value)
    }

    private fun setStatsDirectly(
        stats: SeasonBattingStats,
        gamesPlayed: Int = 0,
        pa: Int = 0,
        atBats: Int = 0,
        hits: Int = 0,
        doubles: Int = 0,
        triples: Int = 0,
        homeRuns: Int = 0,
        walks: Int = 0,
        hbp: Int = 0,
        stolenBases: Int = 0,
        caughtStealing: Int = 0,
    ) {
        val clazz = SeasonBattingStats::class.java
        setField(clazz, stats, "gamesPlayed", gamesPlayed)
        setField(clazz, stats, "plateAppearances", pa)
        setField(clazz, stats, "atBats", atBats)
        setField(clazz, stats, "hits", hits)
        setField(clazz, stats, "doubles", doubles)
        setField(clazz, stats, "triples", triples)
        setField(clazz, stats, "homeRuns", homeRuns)
        setField(clazz, stats, "walks", walks)
        setField(clazz, stats, "hitByPitch", hbp)
        setField(clazz, stats, "stolenBases", stolenBases)
        setField(clazz, stats, "caughtStealing", caughtStealing)
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
