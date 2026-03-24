package com.nextup.core.domain.stats

import com.nextup.common.exception.FrozenStatsException
import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.PitchingDecision
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * 6개 Stats Entity의 reset() 메서드 단위 테스트.
 *
 * reset()은 모든 통계 필드를 초기값(0 또는 null)으로 되돌려서
 * 전체 재집계를 안전하게 수행할 수 있게 합니다.
 */
@DisplayName("Stats Entity reset() 테스트")
class StatsResetTest {
    private val testPlayer =
        Player(
            name = "홍길동",
            primaryPosition = Position.CATCHER,
            throwingHand = ThrowingHand.RIGHT,
            battingHand = BattingHand.RIGHT,
            id = 1L,
        )

    // ========== SeasonBattingStats ==========

    @Nested
    @DisplayName("SeasonBattingStats.reset()")
    inner class SeasonBattingStatsReset {
        @Test
        @DisplayName("누적된 모든 타격 통계 필드를 0으로 초기화한다")
        fun `should reset all batting fields to zero`() {
            // given
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            val gamePlayer = mockk<GamePlayer>()
            val record =
                BattingRecord.create(gamePlayer).apply {
                    setField("plateAppearances", 5)
                    setField("atBats", 4)
                    setField("hits", 2)
                    setField("doubles", 1)
                    setField("triples", 0)
                    setField("homeRuns", 1)
                    setField("runs", 2)
                    setField("runsBattedIn", 3)
                    setField("walks", 1)
                    setField("intentionalWalks", 0)
                    setField("hitByPitch", 0)
                    setField("strikeouts", 1)
                    setField("sacrificeBunts", 0)
                    setField("sacrificeFlies", 0)
                    setField("stolenBases", 1)
                    setField("caughtStealing", 0)
                    setField("groundedIntoDoublePlays", 0)
                    setField("batterInterferences", 0)
                    setField("runnerInterferences", 0)
                }
            stats.addGameRecord(record)

            // when
            stats.reset()

            // then
            assertThat(stats.gamesPlayed).isZero
            assertThat(stats.plateAppearances).isZero
            assertThat(stats.atBats).isZero
            assertThat(stats.hits).isZero
            assertThat(stats.doubles).isZero
            assertThat(stats.triples).isZero
            assertThat(stats.homeRuns).isZero
            assertThat(stats.runs).isZero
            assertThat(stats.runsBattedIn).isZero
            assertThat(stats.walks).isZero
            assertThat(stats.intentionalWalks).isZero
            assertThat(stats.hitByPitch).isZero
            assertThat(stats.strikeouts).isZero
            assertThat(stats.sacrificeBunts).isZero
            assertThat(stats.sacrificeFlies).isZero
            assertThat(stats.stolenBases).isZero
            assertThat(stats.caughtStealing).isZero
            assertThat(stats.groundedIntoDoublePlays).isZero
            assertThat(stats.batterInterferences).isZero
            assertThat(stats.runnerInterferences).isZero
        }

        @Test
        @DisplayName("reset 후 다시 기록을 누적할 수 있다")
        fun `should allow adding records after reset`() {
            // given
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            val gamePlayer = mockk<GamePlayer>()
            val record =
                BattingRecord.create(gamePlayer).apply {
                    setField("plateAppearances", 4)
                    setField("atBats", 3)
                    setField("hits", 1)
                }
            stats.addGameRecord(record)
            stats.reset()

            // when
            val newRecord =
                BattingRecord.create(gamePlayer).apply {
                    setField("plateAppearances", 3)
                    setField("atBats", 2)
                    setField("hits", 2)
                }
            stats.addGameRecord(newRecord)

            // then
            assertThat(stats.gamesPlayed).isEqualTo(1)
            assertThat(stats.plateAppearances).isEqualTo(3)
            assertThat(stats.atBats).isEqualTo(2)
            assertThat(stats.hits).isEqualTo(2)
        }

        @Test
        @DisplayName("확정된 통계에서 reset()을 호출하면 예외가 발생한다")
        fun `should throw exception when reset on finalized stats`() {
            // given
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            stats.finalize()

            // when & then
            assertThrows<FrozenStatsException> {
                stats.reset()
            }
        }
    }

    // ========== SeasonPitchingStats ==========

    @Nested
    @DisplayName("SeasonPitchingStats.reset()")
    inner class SeasonPitchingStatsReset {
        @Test
        @DisplayName("누적된 모든 투수 통계 필드를 초기화한다")
        fun `should reset all pitching fields to zero or null`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            val gamePlayer = mockk<GamePlayer>()
            val record = mockk<PitchingRecord>()
            every { record.isStartingPitcher } returns true
            every { record.inningsPitchedOuts } returns 18
            every { record.earnedRuns } returns 2
            every { record.runsAllowed } returns 3
            every { record.hitsAllowed } returns 5
            every { record.walksAllowed } returns 2
            every { record.strikeouts } returns 7
            every { record.homeRunsAllowed } returns 1
            every { record.hitBatsmen } returns 1
            every { record.wildPitches } returns 1
            every { record.balks } returns 0
            every { record.battersFaced } returns 25
            every { record.pitchesThrown } returns 95
            every { record.strikesThrown } returns 60
            every { record.decision } returns PitchingDecision.WIN
            stats.addGameRecord(record)

            // when
            stats.reset()

            // then
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
        }

        @Test
        @DisplayName("확정된 통계에서 reset()을 호출하면 예외가 발생한다")
        fun `should throw exception when reset on finalized stats`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            stats.finalize()

            // when & then
            assertThrows<FrozenStatsException> {
                stats.reset()
            }
        }
    }

    // ========== SeasonFieldingStats ==========

    @Nested
    @DisplayName("SeasonFieldingStats.reset()")
    inner class SeasonFieldingStatsReset {
        @Test
        @DisplayName("누적된 모든 수비 통계 필드를 0으로 초기화한다")
        fun `should reset all fielding fields to zero`() {
            // given
            val stats = SeasonFieldingStats.create(testPlayer, 2024)
            val record = mockk<FieldingRecord>()
            every { record.putOuts } returns 5
            every { record.assists } returns 3
            every { record.errors } returns 1
            every { record.doublePlays } returns 1
            every { record.passedBalls } returns 0
            every { record.triplePlays } returns 0
            every { record.caughtStealing } returns 2
            every { record.stolenBasesAllowed } returns 1
            stats.addGameRecord(record)

            // when
            stats.reset()

            // then
            assertThat(stats.gamesPlayed).isZero
            assertThat(stats.putOuts).isZero
            assertThat(stats.assists).isZero
            assertThat(stats.errors).isZero
            assertThat(stats.doublePlays).isZero
            assertThat(stats.passedBalls).isZero
            assertThat(stats.triplePlays).isZero
            assertThat(stats.caughtStealing).isZero
            assertThat(stats.stolenBasesAllowed).isZero
        }

        @Test
        @DisplayName("확정된 통계에서 reset()을 호출하면 예외가 발생한다")
        fun `should throw exception when reset on finalized stats`() {
            // given
            val stats = SeasonFieldingStats.create(testPlayer, 2024)
            stats.finalize()

            // when & then
            assertThrows<FrozenStatsException> {
                stats.reset()
            }
        }
    }

    // ========== CareerBattingStats ==========

    @Nested
    @DisplayName("CareerBattingStats.reset()")
    inner class CareerBattingStatsReset {
        @Test
        @DisplayName("누적된 모든 통산 타격 통계 필드를 0으로 초기화한다")
        fun `should reset all career batting fields to zero`() {
            // given
            val stats = CareerBattingStats.create(testPlayer)
            val gamePlayer = mockk<GamePlayer>()
            val record =
                BattingRecord.create(gamePlayer).apply {
                    setField("plateAppearances", 5)
                    setField("atBats", 4)
                    setField("hits", 2)
                    setField("doubles", 1)
                    setField("homeRuns", 1)
                    setField("runs", 2)
                    setField("runsBattedIn", 3)
                    setField("walks", 1)
                    setField("strikeouts", 1)
                    setField("stolenBases", 1)
                }
            stats.addGameRecord(record)
            stats.addSeason()

            // when
            stats.reset()

            // then
            assertThat(stats.seasonsPlayed).isZero
            assertThat(stats.gamesPlayed).isZero
            assertThat(stats.plateAppearances).isZero
            assertThat(stats.atBats).isZero
            assertThat(stats.hits).isZero
            assertThat(stats.doubles).isZero
            assertThat(stats.triples).isZero
            assertThat(stats.homeRuns).isZero
            assertThat(stats.runs).isZero
            assertThat(stats.runsBattedIn).isZero
            assertThat(stats.walks).isZero
            assertThat(stats.intentionalWalks).isZero
            assertThat(stats.hitByPitch).isZero
            assertThat(stats.strikeouts).isZero
            assertThat(stats.sacrificeBunts).isZero
            assertThat(stats.sacrificeFlies).isZero
            assertThat(stats.stolenBases).isZero
            assertThat(stats.caughtStealing).isZero
            assertThat(stats.groundedIntoDoublePlays).isZero
            assertThat(stats.batterInterferences).isZero
            assertThat(stats.runnerInterferences).isZero
        }

        @Test
        @DisplayName("reset 후 다시 기록을 누적할 수 있다")
        fun `should allow adding records after reset`() {
            // given
            val stats = CareerBattingStats.create(testPlayer)
            val gamePlayer = mockk<GamePlayer>()
            val record =
                BattingRecord.create(gamePlayer).apply {
                    setField("plateAppearances", 4)
                    setField("atBats", 3)
                    setField("hits", 1)
                }
            stats.addGameRecord(record)
            stats.addSeason()
            stats.reset()

            // when
            val newRecord =
                BattingRecord.create(gamePlayer).apply {
                    setField("plateAppearances", 5)
                    setField("atBats", 4)
                    setField("hits", 3)
                }
            stats.addGameRecord(newRecord)
            stats.addSeason()

            // then
            assertThat(stats.seasonsPlayed).isEqualTo(1)
            assertThat(stats.gamesPlayed).isEqualTo(1)
            assertThat(stats.hits).isEqualTo(3)
        }
    }

    // ========== CareerPitchingStats ==========

    @Nested
    @DisplayName("CareerPitchingStats.reset()")
    inner class CareerPitchingStatsReset {
        @Test
        @DisplayName("누적된 모든 통산 투수 통계 필드를 초기화한다")
        fun `should reset all career pitching fields to zero or null`() {
            // given
            val stats = CareerPitchingStats.create(testPlayer)
            val record = mockk<PitchingRecord>()
            every { record.isStartingPitcher } returns true
            every { record.inningsPitchedOuts } returns 21
            every { record.earnedRuns } returns 3
            every { record.runsAllowed } returns 4
            every { record.hitsAllowed } returns 6
            every { record.walksAllowed } returns 2
            every { record.strikeouts } returns 8
            every { record.homeRunsAllowed } returns 1
            every { record.hitBatsmen } returns 0
            every { record.wildPitches } returns 1
            every { record.balks } returns 0
            every { record.battersFaced } returns 28
            every { record.pitchesThrown } returns 100
            every { record.strikesThrown } returns 65
            every { record.decision } returns PitchingDecision.WIN
            stats.addGameRecord(record)
            stats.addSeason()

            // when
            stats.reset()

            // then
            assertThat(stats.seasonsPlayed).isZero
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
        }
    }

    // ========== CareerFieldingStats ==========

    @Nested
    @DisplayName("CareerFieldingStats.reset()")
    inner class CareerFieldingStatsReset {
        @Test
        @DisplayName("누적된 모든 통산 수비 통계 필드를 0으로 초기화한다")
        fun `should reset all career fielding fields to zero`() {
            // given
            val stats = CareerFieldingStats.create(testPlayer)
            val record = mockk<FieldingRecord>()
            every { record.putOuts } returns 5
            every { record.assists } returns 3
            every { record.errors } returns 1
            every { record.doublePlays } returns 1
            every { record.passedBalls } returns 2
            every { record.triplePlays } returns 0
            every { record.caughtStealing } returns 3
            every { record.stolenBasesAllowed } returns 1
            stats.addGameRecord(record)
            stats.addSeason()

            // when
            stats.reset()

            // then
            assertThat(stats.seasonsPlayed).isZero
            assertThat(stats.gamesPlayed).isZero
            assertThat(stats.putOuts).isZero
            assertThat(stats.assists).isZero
            assertThat(stats.errors).isZero
            assertThat(stats.doublePlays).isZero
            assertThat(stats.passedBalls).isZero
            assertThat(stats.triplePlays).isZero
            assertThat(stats.caughtStealing).isZero
            assertThat(stats.stolenBasesAllowed).isZero
        }
    }

    // ========== Helper ==========

    private fun BattingRecord.setField(
        fieldName: String,
        value: Int,
    ) {
        val field = BattingRecord::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(this, value)
    }
}
