package com.nextup.core.domain.stats

import com.nextup.common.exception.StatsValidationException
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.PitchingDecision
import com.nextup.core.domain.game.PitchingRecord
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

@DisplayName("CareerPitchingStats 테스트")
class CareerPitchingStatsTest {
    private val testPlayer =
        Player(
            name = "박찬호",
            primaryPosition = Position.STARTING_PITCHER,
            throwingHand = ThrowingHand.RIGHT,
            battingHand = BattingHand.RIGHT,
            id = 1L,
        )

    @Nested
    @DisplayName("통산 투수 통계 생성")
    inner class Create {
        @Test
        fun `should create career pitching stats successfully`() {
            // when
            val stats = CareerPitchingStats.create(testPlayer)

            // then
            assertThat(stats.player).isEqualTo(testPlayer)
            assertThat(stats.seasonsPlayed).isZero
            assertThat(stats.gamesPlayed).isZero
            assertThat(stats.wins).isZero
            assertThat(stats.inningsPitchedOuts).isZero
        }
    }

    @Nested
    @DisplayName("경기 기록 누적")
    inner class AddGameRecord {
        @Test
        fun `should accumulate single game record for starting pitcher correctly`() {
            // given
            val stats = CareerPitchingStats.create(testPlayer)
            val gamePlayer = mockk<GamePlayer>()
            val record =
                PitchingRecord.create(gamePlayer, isStartingPitcher = true).apply {
                    setStats(
                        inningsPitchedOuts = 18,
                        earnedRuns = 2,
                        runsAllowed = 2,
                        hitsAllowed = 5,
                        walksAllowed = 2,
                        strikeouts = 7,
                        battersFaced = 25,
                        decision = PitchingDecision.WIN,
                    )
                }

            // when
            stats.addGameRecord(record)

            // then
            assertThat(stats.gamesPlayed).isEqualTo(1)
            assertThat(stats.gamesStarted).isEqualTo(1)
            assertThat(stats.inningsPitchedOuts).isEqualTo(18)
            assertThat(stats.earnedRuns).isEqualTo(2)
            assertThat(stats.runsAllowed).isEqualTo(2)
            assertThat(stats.hitsAllowed).isEqualTo(5)
            assertThat(stats.walksAllowed).isEqualTo(2)
            assertThat(stats.strikeouts).isEqualTo(7)
            assertThat(stats.battersFaced).isEqualTo(25)
            assertThat(stats.wins).isEqualTo(1)
            assertThat(stats.losses).isZero
        }

        @Test
        fun `should accumulate relief pitcher record correctly`() {
            // given
            val stats = CareerPitchingStats.create(testPlayer)
            val gamePlayer = mockk<GamePlayer>()
            val record =
                PitchingRecord.create(gamePlayer, isStartingPitcher = false).apply {
                    setStats(
                        inningsPitchedOuts = 6,
                        earnedRuns = 0,
                        runsAllowed = 0,
                        hitsAllowed = 1,
                        walksAllowed = 0,
                        strikeouts = 3,
                        battersFaced = 7,
                        decision = PitchingDecision.SAVE,
                    )
                }

            // when
            stats.addGameRecord(record)

            // then
            assertThat(stats.gamesPlayed).isEqualTo(1)
            assertThat(stats.gamesStarted).isZero
            assertThat(stats.saves).isEqualTo(1)
            assertThat(stats.wins).isZero
        }

        @Test
        fun `should accumulate multiple game records correctly`() {
            // given
            val stats = CareerPitchingStats.create(testPlayer)
            val gamePlayer = mockk<GamePlayer>()

            // Game 1: 선발 6이닝 승리
            val record1 =
                PitchingRecord.create(gamePlayer, isStartingPitcher = true).apply {
                    setStats(
                        inningsPitchedOuts = 18,
                        earnedRuns = 2,
                        runsAllowed = 2,
                        hitsAllowed = 5,
                        walksAllowed = 2,
                        strikeouts = 6,
                        battersFaced = 24,
                        decision = PitchingDecision.WIN,
                    )
                }

            // Game 2: 선발 7이닝 패배
            val record2 =
                PitchingRecord.create(gamePlayer, isStartingPitcher = true).apply {
                    setStats(
                        inningsPitchedOuts = 21,
                        earnedRuns = 4,
                        runsAllowed = 4,
                        hitsAllowed = 8,
                        walksAllowed = 1,
                        strikeouts = 5,
                        battersFaced = 28,
                        decision = PitchingDecision.LOSS,
                    )
                }

            // Game 3: 중계 2이닝 홀드
            val record3 =
                PitchingRecord.create(gamePlayer, isStartingPitcher = false).apply {
                    setStats(
                        inningsPitchedOuts = 6,
                        earnedRuns = 0,
                        runsAllowed = 0,
                        hitsAllowed = 1,
                        walksAllowed = 1,
                        strikeouts = 2,
                        battersFaced = 7,
                        decision = PitchingDecision.HOLD,
                    )
                }

            // Game 4: 중계 1이닝 블론세이브
            val record4 =
                PitchingRecord.create(gamePlayer, isStartingPitcher = false).apply {
                    setStats(
                        inningsPitchedOuts = 3,
                        earnedRuns = 2,
                        runsAllowed = 2,
                        hitsAllowed = 3,
                        walksAllowed = 1,
                        strikeouts = 1,
                        battersFaced = 6,
                        decision = PitchingDecision.BLOWN_SAVE,
                    )
                }

            // when
            stats.addGameRecord(record1)
            stats.addGameRecord(record2)
            stats.addGameRecord(record3)
            stats.addGameRecord(record4)

            // then
            assertThat(stats.gamesPlayed).isEqualTo(4)
            assertThat(stats.gamesStarted).isEqualTo(2)
            assertThat(stats.inningsPitchedOuts).isEqualTo(48) // 18 + 21 + 6 + 3
            assertThat(stats.wins).isEqualTo(1)
            assertThat(stats.losses).isEqualTo(1)
            assertThat(stats.holds).isEqualTo(1)
            assertThat(stats.blownSaves).isEqualTo(1)
            assertThat(stats.earnedRuns).isEqualTo(8)
            assertThat(stats.hitsAllowed).isEqualTo(17)
            assertThat(stats.strikeouts).isEqualTo(14)
        }

        @Test
        fun `should accumulate pitches thrown when provided`() {
            // given
            val stats = CareerPitchingStats.create(testPlayer)
            val gamePlayer = mockk<GamePlayer>()
            val record1 =
                PitchingRecord.create(gamePlayer).apply {
                    setStats(
                        inningsPitchedOuts = 18,
                        pitchesThrown = 95,
                        strikesThrown = 63,
                    )
                }
            val record2 =
                PitchingRecord.create(gamePlayer).apply {
                    setStats(
                        inningsPitchedOuts = 18,
                        pitchesThrown = 90,
                        strikesThrown = 60,
                    )
                }

            // when
            stats.addGameRecord(record1)
            stats.addGameRecord(record2)

            // then
            assertThat(stats.pitchesThrown).isEqualTo(185)
            assertThat(stats.strikesThrown).isEqualTo(123)
        }

        @Test
        fun `should handle null pitches thrown correctly`() {
            // given
            val stats = CareerPitchingStats.create(testPlayer)
            val gamePlayer = mockk<GamePlayer>()
            val record =
                PitchingRecord.create(gamePlayer).apply {
                    setStats(inningsPitchedOuts = 18)
                }

            // when
            stats.addGameRecord(record)

            // then
            assertThat(stats.pitchesThrown).isNull()
            assertThat(stats.strikesThrown).isNull()
        }

        @Test
        fun `should handle decision NONE correctly`() {
            // given
            val stats = CareerPitchingStats.create(testPlayer)
            val gamePlayer = mockk<GamePlayer>()
            val record =
                PitchingRecord.create(gamePlayer).apply {
                    setStats(
                        inningsPitchedOuts = 6,
                        decision = PitchingDecision.NONE,
                    )
                }

            // when
            stats.addGameRecord(record)

            // then
            assertThat(stats.wins).isZero
            assertThat(stats.losses).isZero
            assertThat(stats.saves).isZero
            assertThat(stats.holds).isZero
            assertThat(stats.blownSaves).isZero
        }
    }

    @Nested
    @DisplayName("시즌 추가")
    inner class AddSeason {
        @Test
        fun `should increment seasons played`() {
            // given
            val stats = CareerPitchingStats.create(testPlayer)

            // when
            stats.addSeason()
            stats.addSeason()
            stats.addSeason()

            // then
            assertThat(stats.seasonsPlayed).isEqualTo(3)
        }
    }

    @Nested
    @DisplayName("계산 속성 검증")
    inner class CalculatedProperties {
        @Test
        fun `should calculate ERA correctly`() {
            // given: 18 outs (6이닝), 3자책 => ERA = (3/6)*9 = 4.50
            val stats = CareerPitchingStats.create(testPlayer)
            setStatsDirectly(stats, inningsPitchedOuts = 18, earnedRuns = 3)

            // when
            val era = stats.earnedRunAverage

            // then
            assertThat(era).isEqualByComparingTo(BigDecimal("4.50"))
        }

        @Test
        fun `should return zero ERA when no innings pitched and no earned runs`() {
            // given
            val stats = CareerPitchingStats.create(testPlayer)

            // when
            val era = stats.earnedRunAverage

            // then
            assertThat(era).isNotNull().isEqualByComparingTo(BigDecimal("0.00"))
        }

        @Test
        fun `should return null ERA when no innings pitched but earned runs exist`() {
            // given: 0이닝이지만 자책점이 있는 경우 (무한대)
            val stats = CareerPitchingStats.create(testPlayer)
            setStatsDirectly(stats, inningsPitchedOuts = 0, earnedRuns = 3)

            // when
            val era = stats.earnedRunAverage

            // then
            assertThat(era).isNull()
        }

        @Test
        fun `should calculate WHIP correctly`() {
            // given: 18 outs (6이닝), 5피안타, 2볼넷 => WHIP = (5+2)/6 = 1.17
            val stats = CareerPitchingStats.create(testPlayer)
            setStatsDirectly(stats, inningsPitchedOuts = 18, hitsAllowed = 5, walksAllowed = 2)

            // when
            val whip = stats.whip

            // then
            assertThat(whip).isEqualByComparingTo(BigDecimal("1.17"))
        }

        @Test
        fun `should return zero WHIP when no innings pitched`() {
            // given
            val stats = CareerPitchingStats.create(testPlayer)

            // when
            val whip = stats.whip

            // then
            assertThat(whip).isEqualByComparingTo(BigDecimal("0.00"))
        }

        @Test
        fun `should calculate K per 9 correctly`() {
            // given: 18 outs (6이닝), 9삼진 => K/9 = (9/6)*9 = 13.50
            val stats = CareerPitchingStats.create(testPlayer)
            setStatsDirectly(stats, inningsPitchedOuts = 18, strikeouts = 9)

            // when
            val k9 = stats.strikeoutsPer9

            // then
            assertThat(k9).isEqualByComparingTo(BigDecimal("13.50"))
        }

        @Test
        fun `should return zero K per 9 when no innings pitched`() {
            // given
            val stats = CareerPitchingStats.create(testPlayer)

            // when
            val k9 = stats.strikeoutsPer9

            // then
            assertThat(k9).isEqualByComparingTo(BigDecimal("0.00"))
        }

        @Test
        fun `should calculate BB per 9 correctly`() {
            // given: 27 outs (9이닝), 3볼넷 => BB/9 = (3/9)*9 = 3.00
            val stats = CareerPitchingStats.create(testPlayer)
            setStatsDirectly(stats, inningsPitchedOuts = 27, walksAllowed = 3)

            // when
            val bb9 = stats.walksPer9

            // then
            assertThat(bb9).isEqualByComparingTo(BigDecimal("3.00"))
        }

        @Test
        fun `should return zero BB per 9 when no innings pitched`() {
            // given
            val stats = CareerPitchingStats.create(testPlayer)

            // when
            val bb9 = stats.walksPer9

            // then
            assertThat(bb9).isEqualByComparingTo(BigDecimal("0.00"))
        }

        @Test
        fun `should calculate K-BB ratio correctly`() {
            // given: 9삼진, 3볼넷 => K/BB = 3.00
            val stats = CareerPitchingStats.create(testPlayer)
            setStatsDirectly(stats, strikeouts = 9, walksAllowed = 3)

            // when
            val kbb = stats.strikeoutToWalkRatio

            // then
            assertThat(kbb).isEqualByComparingTo(BigDecimal("3.00"))
        }

        @Test
        fun `should return strikeouts when walks is zero for K-BB ratio`() {
            // given: 10삼진, 0볼넷
            val stats = CareerPitchingStats.create(testPlayer)
            setStatsDirectly(stats, strikeouts = 10, walksAllowed = 0)

            // when
            val kbb = stats.strikeoutToWalkRatio

            // then
            assertThat(kbb).isEqualByComparingTo(BigDecimal("10.00"))
        }

        @Test
        fun `should return zero when both strikeouts and walks are zero for K-BB ratio`() {
            // given
            val stats = CareerPitchingStats.create(testPlayer)

            // when
            val kbb = stats.strikeoutToWalkRatio

            // then
            assertThat(kbb).isEqualByComparingTo(BigDecimal("0.00"))
        }

        @Test
        fun `should calculate strike percentage correctly`() {
            // given: 100투구, 65스트라이크 => 65%
            val stats = CareerPitchingStats.create(testPlayer)
            setStatsDirectly(stats, pitchesThrown = 100, strikesThrown = 65)

            // when
            val strikePct = stats.strikePercentage

            // then
            assertThat(strikePct).isNotNull
            assertThat(strikePct).isEqualByComparingTo(BigDecimal("0.650"))
        }

        @Test
        fun `should return null strike percentage when pitches not recorded`() {
            // given
            val stats = CareerPitchingStats.create(testPlayer)

            // when
            val strikePct = stats.strikePercentage

            // then
            assertThat(strikePct).isNull()
        }

        @Test
        fun `should return null strike percentage when pitches is zero`() {
            // given
            val stats = CareerPitchingStats.create(testPlayer)
            setStatsDirectly(stats, pitchesThrown = 0, strikesThrown = 0)

            // when
            val strikePct = stats.strikePercentage

            // then
            assertThat(strikePct).isNull()
        }

        @Test
        fun `should calculate winning percentage correctly`() {
            // given: 8승 2패 => .800
            val stats = CareerPitchingStats.create(testPlayer)
            setStatsDirectly(stats, wins = 8, losses = 2)

            // when
            val winPct = stats.winningPercentage

            // then
            assertThat(winPct).isEqualByComparingTo(BigDecimal("0.800"))
        }

        @Test
        fun `should return zero winning percentage when no decisions`() {
            // given
            val stats = CareerPitchingStats.create(testPlayer)

            // when
            val winPct = stats.winningPercentage

            // then
            assertThat(winPct).isEqualByComparingTo(BigDecimal("0.000"))
        }

        @Test
        fun `should calculate innings pitched display correctly`() {
            // given: 20 outs = 6.2 이닝
            val stats = CareerPitchingStats.create(testPlayer)
            setStatsDirectly(stats, inningsPitchedOuts = 20)

            // when
            val display = stats.inningsPitchedDisplay

            // then
            assertThat(display).isEqualTo("6.2")
        }

        @Test
        fun `should calculate complete innings correctly`() {
            // given: 20 outs = 6 complete innings
            val stats = CareerPitchingStats.create(testPlayer)
            setStatsDirectly(stats, inningsPitchedOuts = 20)

            // when
            val completeInnings = stats.completeInnings

            // then
            assertThat(completeInnings).isEqualTo(6)
        }

        @Test
        fun `should calculate remaining outs correctly`() {
            // given: 20 outs = 2 remaining outs
            val stats = CareerPitchingStats.create(testPlayer)
            setStatsDirectly(stats, inningsPitchedOuts = 20)

            // when
            val remainingOuts = stats.remainingOuts

            // then
            assertThat(remainingOuts).isEqualTo(2)
        }

        @Test
        fun `should calculate innings pitched correctly`() {
            // given: 18 outs = 6.00 innings
            val stats = CareerPitchingStats.create(testPlayer)
            setStatsDirectly(stats, inningsPitchedOuts = 18)

            // when
            val inningsPitched = stats.inningsPitched

            // then
            assertThat(inningsPitched).isEqualByComparingTo(BigDecimal("6.00"))
        }

        @Test
        fun `should calculate unearned runs correctly`() {
            // given: 5실점, 3자책 => 2비자책
            val stats = CareerPitchingStats.create(testPlayer)
            setStatsDirectly(stats, runsAllowed = 5, earnedRuns = 3)

            // when
            val unearnedRuns = stats.unearnedRuns

            // then
            assertThat(unearnedRuns).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("유효성 검증")
    inner class Validation {
        @Test
        fun `should validate successfully with valid stats`() {
            // given
            val stats = CareerPitchingStats.create(testPlayer)
            setStatsDirectly(
                stats,
                seasonsPlayed = 3,
                gamesPlayed = 50,
                gamesStarted = 30,
                inningsPitchedOuts = 600,
                earnedRuns = 50,
                runsAllowed = 60,
            )

            // when & then
            stats.validate() // Should not throw
        }

        @Test
        fun `should throw exception when seasons played is negative`() {
            // given
            val stats = CareerPitchingStats.create(testPlayer)
            setStatsDirectly(stats, seasonsPlayed = -1)

            // when & then
            assertThrows<StatsValidationException> {
                stats.validate()
            }
        }

        @Test
        fun `should throw exception when games played is negative`() {
            // given
            val stats = CareerPitchingStats.create(testPlayer)
            setStatsDirectly(stats, gamesPlayed = -1)

            // when & then
            assertThrows<StatsValidationException> {
                stats.validate()
            }
        }

        @Test
        fun `should throw exception when games started exceeds games played`() {
            // given
            val stats = CareerPitchingStats.create(testPlayer)
            setStatsDirectly(stats, gamesPlayed = 5, gamesStarted = 6)

            // when & then
            assertThrows<StatsValidationException> {
                stats.validate()
            }
        }

        @Test
        fun `should throw exception when earned runs exceed runs allowed`() {
            // given
            val stats = CareerPitchingStats.create(testPlayer)
            setStatsDirectly(stats, earnedRuns = 10, runsAllowed = 5)

            // when & then
            assertThrows<StatsValidationException> {
                stats.validate()
            }
        }

        @Test
        fun `should throw exception when strikes exceed pitches thrown`() {
            // given
            val stats = CareerPitchingStats.create(testPlayer)
            setStatsDirectly(stats, pitchesThrown = 100, strikesThrown = 101)

            // when & then
            assertThrows<StatsValidationException> {
                stats.validate()
            }
        }

        @Test
        fun `should not throw when pitches are null`() {
            // given
            val stats = CareerPitchingStats.create(testPlayer)

            // when & then
            stats.validate() // Should not throw
        }
    }

    // Helper methods

    private fun PitchingRecord.setStats(
        inningsPitchedOuts: Int = 0,
        earnedRuns: Int = 0,
        runsAllowed: Int = 0,
        hitsAllowed: Int = 0,
        walksAllowed: Int = 0,
        strikeouts: Int = 0,
        battersFaced: Int = 0,
        decision: PitchingDecision = PitchingDecision.NONE,
        pitchesThrown: Int? = null,
        strikesThrown: Int? = null,
    ) {
        setField("inningsPitchedOuts", inningsPitchedOuts)
        setField("earnedRuns", earnedRuns)
        setField("runsAllowed", runsAllowed)
        setField("hitsAllowed", hitsAllowed)
        setField("walksAllowed", walksAllowed)
        setField("strikeouts", strikeouts)
        setField("battersFaced", battersFaced)
        setField("decision", decision)
        setField("pitchesThrown", pitchesThrown)
        setField("strikesThrown", strikesThrown)
    }

    private fun PitchingRecord.setField(
        fieldName: String,
        value: Any?,
    ) {
        val field = PitchingRecord::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(this, value)
    }

    private fun setStatsDirectly(
        stats: CareerPitchingStats,
        seasonsPlayed: Int = 0,
        gamesPlayed: Int = 0,
        gamesStarted: Int = 0,
        inningsPitchedOuts: Int = 0,
        wins: Int = 0,
        losses: Int = 0,
        earnedRuns: Int = 0,
        runsAllowed: Int = 0,
        hitsAllowed: Int = 0,
        walksAllowed: Int = 0,
        strikeouts: Int = 0,
        pitchesThrown: Int? = null,
        strikesThrown: Int? = null,
    ) {
        val clazz = CareerPitchingStats::class.java
        setField(clazz, stats, "seasonsPlayed", seasonsPlayed)
        setField(clazz, stats, "gamesPlayed", gamesPlayed)
        setField(clazz, stats, "gamesStarted", gamesStarted)
        setField(clazz, stats, "inningsPitchedOuts", inningsPitchedOuts)
        setField(clazz, stats, "wins", wins)
        setField(clazz, stats, "losses", losses)
        setField(clazz, stats, "earnedRuns", earnedRuns)
        setField(clazz, stats, "runsAllowed", runsAllowed)
        setField(clazz, stats, "hitsAllowed", hitsAllowed)
        setField(clazz, stats, "walksAllowed", walksAllowed)
        setField(clazz, stats, "strikeouts", strikeouts)
        setField(clazz, stats, "pitchesThrown", pitchesThrown)
        setField(clazz, stats, "strikesThrown", strikesThrown)
    }

    private fun setField(
        clazz: Class<*>,
        obj: Any,
        fieldName: String,
        value: Any?,
    ) {
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(obj, value)
    }
}
