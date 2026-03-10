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

@DisplayName("타격 통계 applyFieldCorrection 테스트")
class BattingStatsApplyFieldCorrectionTest {
    private val testPlayer =
        Player(
            name = "홍길동",
            primaryPosition = Position.CATCHER,
            throwingHand = ThrowingHand.RIGHT,
            battingHand = BattingHand.RIGHT,
            id = 1L,
        )

    @Nested
    @DisplayName("SeasonBattingStats")
    inner class SeasonBattingStatsFieldCorrection {
        @Test
        fun `모든 필드에 양수 델타가 올바르게 적용됨`() {
            val stats = SeasonBattingStats.create(testPlayer, 2024)

            stats.applyFieldCorrection("plateAppearances", 10)
            stats.applyFieldCorrection("atBats", 8)
            stats.applyFieldCorrection("hits", 3)
            stats.applyFieldCorrection("doubles", 1)
            stats.applyFieldCorrection("triples", 1)
            stats.applyFieldCorrection("homeRuns", 1)
            stats.applyFieldCorrection("runs", 2)
            stats.applyFieldCorrection("runsBattedIn", 3)
            stats.applyFieldCorrection("walks", 1)
            stats.applyFieldCorrection("intentionalWalks", 1)
            stats.applyFieldCorrection("hitByPitch", 1)
            stats.applyFieldCorrection("strikeouts", 2)
            stats.applyFieldCorrection("sacrificeBunts", 1)
            stats.applyFieldCorrection("sacrificeFlies", 1)
            stats.applyFieldCorrection("stolenBases", 2)
            stats.applyFieldCorrection("caughtStealing", 1)
            stats.applyFieldCorrection("groundedIntoDoublePlays", 1)

            assertThat(stats.plateAppearances).isEqualTo(10)
            assertThat(stats.atBats).isEqualTo(8)
            assertThat(stats.hits).isEqualTo(3)
            assertThat(stats.doubles).isEqualTo(1)
            assertThat(stats.triples).isEqualTo(1)
            assertThat(stats.homeRuns).isEqualTo(1)
            assertThat(stats.runs).isEqualTo(2)
            assertThat(stats.runsBattedIn).isEqualTo(3)
            assertThat(stats.walks).isEqualTo(1)
            assertThat(stats.intentionalWalks).isEqualTo(1)
            assertThat(stats.hitByPitch).isEqualTo(1)
            assertThat(stats.strikeouts).isEqualTo(2)
            assertThat(stats.sacrificeBunts).isEqualTo(1)
            assertThat(stats.sacrificeFlies).isEqualTo(1)
            assertThat(stats.stolenBases).isEqualTo(2)
            assertThat(stats.caughtStealing).isEqualTo(1)
            assertThat(stats.groundedIntoDoublePlays).isEqualTo(1)
        }

        @Test
        fun `음수 델타 적용 시 0 미만으로 내려가지 않음`() {
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            // 불변식을 만족하는 초기값 설정 (PA ≥ AB ≥ H ≥ 2B+3B+HR)
            stats.applyFieldCorrection("plateAppearances", 20)
            stats.applyFieldCorrection("atBats", 15)
            stats.applyFieldCorrection("hits", 10)
            stats.applyFieldCorrection("doubles", 2)
            stats.applyFieldCorrection("triples", 2)
            stats.applyFieldCorrection("homeRuns", 2)
            stats.applyFieldCorrection("runs", 2)
            stats.applyFieldCorrection("runsBattedIn", 2)
            stats.applyFieldCorrection("walks", 2)
            stats.applyFieldCorrection("intentionalWalks", 2)
            stats.applyFieldCorrection("hitByPitch", 2)
            stats.applyFieldCorrection("strikeouts", 2)
            stats.applyFieldCorrection("sacrificeBunts", 2)
            stats.applyFieldCorrection("sacrificeFlies", 2)
            stats.applyFieldCorrection("stolenBases", 2)
            stats.applyFieldCorrection("caughtStealing", 2)
            stats.applyFieldCorrection("groundedIntoDoublePlays", 2)

            // 역의존 순서로 큰 음수 적용 (2B+3B+HR → H → AB → PA)
            stats.applyFieldCorrection("doubles", -100)
            stats.applyFieldCorrection("triples", -100)
            stats.applyFieldCorrection("homeRuns", -100)
            stats.applyFieldCorrection("hits", -100)
            stats.applyFieldCorrection("atBats", -100)
            stats.applyFieldCorrection("plateAppearances", -100)
            stats.applyFieldCorrection("runs", -100)
            stats.applyFieldCorrection("runsBattedIn", -100)
            stats.applyFieldCorrection("walks", -100)
            stats.applyFieldCorrection("intentionalWalks", -100)
            stats.applyFieldCorrection("hitByPitch", -100)
            stats.applyFieldCorrection("strikeouts", -100)
            stats.applyFieldCorrection("sacrificeBunts", -100)
            stats.applyFieldCorrection("sacrificeFlies", -100)
            stats.applyFieldCorrection("stolenBases", -100)
            stats.applyFieldCorrection("caughtStealing", -100)
            stats.applyFieldCorrection("groundedIntoDoublePlays", -100)

            assertThat(stats.plateAppearances).isZero()
            assertThat(stats.atBats).isZero()
            assertThat(stats.hits).isZero()
            assertThat(stats.doubles).isZero()
            assertThat(stats.triples).isZero()
            assertThat(stats.homeRuns).isZero()
            assertThat(stats.runs).isZero()
            assertThat(stats.runsBattedIn).isZero()
            assertThat(stats.walks).isZero()
            assertThat(stats.intentionalWalks).isZero()
            assertThat(stats.hitByPitch).isZero()
            assertThat(stats.strikeouts).isZero()
            assertThat(stats.sacrificeBunts).isZero()
            assertThat(stats.sacrificeFlies).isZero()
            assertThat(stats.stolenBases).isZero()
            assertThat(stats.caughtStealing).isZero()
            assertThat(stats.groundedIntoDoublePlays).isZero()
        }

        @Test
        fun `존재하지 않는 필드명에 대해 예외가 발생함`() {
            val stats = SeasonBattingStats.create(testPlayer, 2024)
            assertThrows<IllegalArgumentException> {
                stats.applyFieldCorrection("unknownField", 5)
            }
        }
    }

    @Nested
    @DisplayName("CareerBattingStats")
    inner class CareerBattingStatsFieldCorrection {
        @Test
        fun `모든 필드에 양수 델타가 올바르게 적용됨`() {
            val stats = CareerBattingStats.create(testPlayer)

            stats.applyFieldCorrection("plateAppearances", 10)
            stats.applyFieldCorrection("atBats", 8)
            stats.applyFieldCorrection("hits", 3)
            stats.applyFieldCorrection("doubles", 1)
            stats.applyFieldCorrection("triples", 1)
            stats.applyFieldCorrection("homeRuns", 1)
            stats.applyFieldCorrection("runs", 2)
            stats.applyFieldCorrection("runsBattedIn", 3)
            stats.applyFieldCorrection("walks", 1)
            stats.applyFieldCorrection("intentionalWalks", 1)
            stats.applyFieldCorrection("hitByPitch", 1)
            stats.applyFieldCorrection("strikeouts", 2)
            stats.applyFieldCorrection("sacrificeBunts", 1)
            stats.applyFieldCorrection("sacrificeFlies", 1)
            stats.applyFieldCorrection("stolenBases", 2)
            stats.applyFieldCorrection("caughtStealing", 1)
            stats.applyFieldCorrection("groundedIntoDoublePlays", 1)

            assertThat(stats.plateAppearances).isEqualTo(10)
            assertThat(stats.atBats).isEqualTo(8)
            assertThat(stats.hits).isEqualTo(3)
            assertThat(stats.doubles).isEqualTo(1)
            assertThat(stats.triples).isEqualTo(1)
            assertThat(stats.homeRuns).isEqualTo(1)
            assertThat(stats.runs).isEqualTo(2)
            assertThat(stats.runsBattedIn).isEqualTo(3)
            assertThat(stats.walks).isEqualTo(1)
            assertThat(stats.intentionalWalks).isEqualTo(1)
            assertThat(stats.hitByPitch).isEqualTo(1)
            assertThat(stats.strikeouts).isEqualTo(2)
            assertThat(stats.sacrificeBunts).isEqualTo(1)
            assertThat(stats.sacrificeFlies).isEqualTo(1)
            assertThat(stats.stolenBases).isEqualTo(2)
            assertThat(stats.caughtStealing).isEqualTo(1)
            assertThat(stats.groundedIntoDoublePlays).isEqualTo(1)
        }

        @Test
        fun `음수 델타 적용 시 0 미만으로 내려가지 않음`() {
            val stats = CareerBattingStats.create(testPlayer)

            stats.applyFieldCorrection("plateAppearances", -5)
            stats.applyFieldCorrection("atBats", -5)
            stats.applyFieldCorrection("hits", -5)
            stats.applyFieldCorrection("doubles", -5)
            stats.applyFieldCorrection("triples", -5)
            stats.applyFieldCorrection("homeRuns", -5)
            stats.applyFieldCorrection("runs", -5)
            stats.applyFieldCorrection("runsBattedIn", -5)
            stats.applyFieldCorrection("walks", -5)
            stats.applyFieldCorrection("intentionalWalks", -5)
            stats.applyFieldCorrection("hitByPitch", -5)
            stats.applyFieldCorrection("strikeouts", -5)
            stats.applyFieldCorrection("sacrificeBunts", -5)
            stats.applyFieldCorrection("sacrificeFlies", -5)
            stats.applyFieldCorrection("stolenBases", -5)
            stats.applyFieldCorrection("caughtStealing", -5)
            stats.applyFieldCorrection("groundedIntoDoublePlays", -5)

            assertThat(stats.plateAppearances).isZero()
            assertThat(stats.atBats).isZero()
            assertThat(stats.hits).isZero()
            assertThat(stats.doubles).isZero()
            assertThat(stats.triples).isZero()
            assertThat(stats.homeRuns).isZero()
            assertThat(stats.runs).isZero()
            assertThat(stats.runsBattedIn).isZero()
            assertThat(stats.walks).isZero()
            assertThat(stats.intentionalWalks).isZero()
            assertThat(stats.hitByPitch).isZero()
            assertThat(stats.strikeouts).isZero()
            assertThat(stats.sacrificeBunts).isZero()
            assertThat(stats.sacrificeFlies).isZero()
            assertThat(stats.stolenBases).isZero()
            assertThat(stats.caughtStealing).isZero()
            assertThat(stats.groundedIntoDoublePlays).isZero()
        }

        @Test
        fun `존재하지 않는 필드명에 대해 예외가 발생함`() {
            val stats = CareerBattingStats.create(testPlayer)
            assertThrows<IllegalArgumentException> {
                stats.applyFieldCorrection("unknownField", 5)
            }
        }
    }
}
