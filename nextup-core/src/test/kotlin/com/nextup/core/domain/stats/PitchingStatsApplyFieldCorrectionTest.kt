package com.nextup.core.domain.stats

import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("투수 통계 applyFieldCorrection 테스트")
class PitchingStatsApplyFieldCorrectionTest {
    private val testPlayer =
        Player(
            name = "홍길동",
            primaryPosition = Position.STARTING_PITCHER,
            throwingHand = ThrowingHand.RIGHT,
            battingHand = BattingHand.RIGHT,
            id = 1L,
        )

    @Nested
    @DisplayName("SeasonPitchingStats")
    inner class SeasonPitchingStatsFieldCorrection {
        @Test
        fun `모든 필드에 양수 델타가 올바르게 적용됨`() {
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            stats.applyFieldCorrection("inningsPitchedOuts", 18)
            stats.applyFieldCorrection("runsAllowed", 4)
            stats.applyFieldCorrection("earnedRuns", 3)
            stats.applyFieldCorrection("hitsAllowed", 6)
            stats.applyFieldCorrection("walksAllowed", 2)
            stats.applyFieldCorrection("strikeouts", 7)
            stats.applyFieldCorrection("homeRunsAllowed", 1)
            stats.applyFieldCorrection("hitBatsmen", 1)
            stats.applyFieldCorrection("wildPitches", 1)
            stats.applyFieldCorrection("balks", 1)
            stats.applyFieldCorrection("battersFaced", 25)
            stats.applyFieldCorrection("pitchesThrown", 90)
            stats.applyFieldCorrection("strikesThrown", 60)

            assertThat(stats.inningsPitchedOuts).isEqualTo(18)
            assertThat(stats.earnedRuns).isEqualTo(3)
            assertThat(stats.runsAllowed).isEqualTo(4)
            assertThat(stats.hitsAllowed).isEqualTo(6)
            assertThat(stats.walksAllowed).isEqualTo(2)
            assertThat(stats.strikeouts).isEqualTo(7)
            assertThat(stats.homeRunsAllowed).isEqualTo(1)
            assertThat(stats.hitBatsmen).isEqualTo(1)
            assertThat(stats.wildPitches).isEqualTo(1)
            assertThat(stats.balks).isEqualTo(1)
            assertThat(stats.battersFaced).isEqualTo(25)
            assertThat(stats.pitchesThrown).isEqualTo(90)
            assertThat(stats.strikesThrown).isEqualTo(60)
        }

        @Test
        fun `음수 델타 적용 시 0 미만으로 내려가지 않음`() {
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            // 불변식을 만족하는 순서로 설정 (RA ≥ ER, PT ≥ ST)
            stats.applyFieldCorrection("inningsPitchedOuts", 3)
            stats.applyFieldCorrection("runsAllowed", 3)
            stats.applyFieldCorrection("earnedRuns", 3)
            stats.applyFieldCorrection("hitsAllowed", 3)
            stats.applyFieldCorrection("walksAllowed", 3)
            stats.applyFieldCorrection("strikeouts", 3)
            stats.applyFieldCorrection("homeRunsAllowed", 3)
            stats.applyFieldCorrection("hitBatsmen", 3)
            stats.applyFieldCorrection("wildPitches", 3)
            stats.applyFieldCorrection("balks", 3)
            stats.applyFieldCorrection("battersFaced", 3)
            stats.applyFieldCorrection("pitchesThrown", 3)
            stats.applyFieldCorrection("strikesThrown", 3)

            // 역의존 순서로 감소 (ER → RA, ST → PT)
            stats.applyFieldCorrection("inningsPitchedOuts", -10)
            stats.applyFieldCorrection("earnedRuns", -10)
            stats.applyFieldCorrection("runsAllowed", -10)
            stats.applyFieldCorrection("hitsAllowed", -10)
            stats.applyFieldCorrection("walksAllowed", -10)
            stats.applyFieldCorrection("strikeouts", -10)
            stats.applyFieldCorrection("homeRunsAllowed", -10)
            stats.applyFieldCorrection("hitBatsmen", -10)
            stats.applyFieldCorrection("wildPitches", -10)
            stats.applyFieldCorrection("balks", -10)
            stats.applyFieldCorrection("battersFaced", -10)
            stats.applyFieldCorrection("strikesThrown", -10)
            stats.applyFieldCorrection("pitchesThrown", -10)

            assertThat(stats.inningsPitchedOuts).isZero()
            assertThat(stats.earnedRuns).isZero()
            assertThat(stats.runsAllowed).isZero()
            assertThat(stats.hitsAllowed).isZero()
            assertThat(stats.walksAllowed).isZero()
            assertThat(stats.strikeouts).isZero()
            assertThat(stats.homeRunsAllowed).isZero()
            assertThat(stats.hitBatsmen).isZero()
            assertThat(stats.wildPitches).isZero()
            assertThat(stats.balks).isZero()
            assertThat(stats.battersFaced).isZero()
            assertThat(stats.pitchesThrown).isZero()
            assertThat(stats.strikesThrown).isZero()
        }

        @Test
        fun `존재하지 않는 필드명에 대해 예외가 발생함`() {
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            assertThrows<IllegalArgumentException> {
                stats.applyFieldCorrection("unknownField", 5)
            }
        }
    }

    @Nested
    @DisplayName("CareerPitchingStats")
    inner class CareerPitchingStatsFieldCorrection {
        @Test
        fun `모든 필드에 양수 델타가 올바르게 적용됨`() {
            val stats = CareerPitchingStats.create(testPlayer)

            stats.applyFieldCorrection("inningsPitchedOuts", 18)
            stats.applyFieldCorrection("runsAllowed", 4)
            stats.applyFieldCorrection("earnedRuns", 3)
            stats.applyFieldCorrection("hitsAllowed", 6)
            stats.applyFieldCorrection("walksAllowed", 2)
            stats.applyFieldCorrection("strikeouts", 7)
            stats.applyFieldCorrection("homeRunsAllowed", 1)
            stats.applyFieldCorrection("hitBatsmen", 1)
            stats.applyFieldCorrection("wildPitches", 1)
            stats.applyFieldCorrection("balks", 1)
            stats.applyFieldCorrection("battersFaced", 25)
            stats.applyFieldCorrection("pitchesThrown", 90)
            stats.applyFieldCorrection("strikesThrown", 60)

            assertThat(stats.inningsPitchedOuts).isEqualTo(18)
            assertThat(stats.earnedRuns).isEqualTo(3)
            assertThat(stats.runsAllowed).isEqualTo(4)
            assertThat(stats.hitsAllowed).isEqualTo(6)
            assertThat(stats.walksAllowed).isEqualTo(2)
            assertThat(stats.strikeouts).isEqualTo(7)
            assertThat(stats.homeRunsAllowed).isEqualTo(1)
            assertThat(stats.hitBatsmen).isEqualTo(1)
            assertThat(stats.wildPitches).isEqualTo(1)
            assertThat(stats.balks).isEqualTo(1)
            assertThat(stats.battersFaced).isEqualTo(25)
            assertThat(stats.pitchesThrown).isEqualTo(90)
            assertThat(stats.strikesThrown).isEqualTo(60)
        }

        @Test
        fun `음수 델타 적용 시 0 미만으로 내려가지 않음`() {
            val stats = CareerPitchingStats.create(testPlayer)

            stats.applyFieldCorrection("inningsPitchedOuts", -5)
            stats.applyFieldCorrection("earnedRuns", -5)
            stats.applyFieldCorrection("runsAllowed", -5)
            stats.applyFieldCorrection("hitsAllowed", -5)
            stats.applyFieldCorrection("walksAllowed", -5)
            stats.applyFieldCorrection("strikeouts", -5)
            stats.applyFieldCorrection("homeRunsAllowed", -5)
            stats.applyFieldCorrection("hitBatsmen", -5)
            stats.applyFieldCorrection("wildPitches", -5)
            stats.applyFieldCorrection("balks", -5)
            stats.applyFieldCorrection("battersFaced", -5)
            stats.applyFieldCorrection("pitchesThrown", -5)
            stats.applyFieldCorrection("strikesThrown", -5)

            assertThat(stats.inningsPitchedOuts).isZero()
            assertThat(stats.earnedRuns).isZero()
            assertThat(stats.runsAllowed).isZero()
            assertThat(stats.hitsAllowed).isZero()
            assertThat(stats.walksAllowed).isZero()
            assertThat(stats.strikeouts).isZero()
            assertThat(stats.homeRunsAllowed).isZero()
            assertThat(stats.hitBatsmen).isZero()
            assertThat(stats.wildPitches).isZero()
            assertThat(stats.balks).isZero()
            assertThat(stats.battersFaced).isZero()
            assertThat(stats.pitchesThrown).isZero()
            assertThat(stats.strikesThrown).isZero()
        }

        @Test
        fun `존재하지 않는 필드명에 대해 예외가 발생함`() {
            val stats = CareerPitchingStats.create(testPlayer)
            assertThrows<IllegalArgumentException> {
                stats.applyFieldCorrection("unknownField", 5)
            }
        }
    }
}
