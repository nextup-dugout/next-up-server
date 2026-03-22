package com.nextup.core.domain.stats

import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SeasonBattingStats 실시간 통계 갱신 테스트")
class SeasonBattingStatsLiveUpdateTest {
    private val testPlayer =
        Player(
            name = "홍길동",
            primaryPosition = Position.CATCHER,
            throwingHand = ThrowingHand.RIGHT,
            battingHand = BattingHand.RIGHT,
            id = 1L,
        )

    private lateinit var stats: SeasonBattingStats

    @BeforeEach
    fun setUp() {
        stats = SeasonBattingStats.create(testPlayer, 2024)
    }

    @Nested
    @DisplayName("applyLiveUpdate - 타석 결과 즉시 반영")
    inner class ApplyLiveUpdate {
        @Test
        fun `단타 기록 시 타석, 타수, 안타 증가`() {
            stats.applyLiveUpdate(PlateAppearanceResult.SINGLE)

            assertThat(stats.plateAppearances).isEqualTo(1)
            assertThat(stats.atBats).isEqualTo(1)
            assertThat(stats.hits).isEqualTo(1)
            assertThat(stats.doubles).isZero
            assertThat(stats.triples).isZero
            assertThat(stats.homeRuns).isZero
        }

        @Test
        fun `2루타 기록 시 타석, 타수, 안타, 2루타 증가`() {
            stats.applyLiveUpdate(PlateAppearanceResult.DOUBLE)

            assertThat(stats.plateAppearances).isEqualTo(1)
            assertThat(stats.atBats).isEqualTo(1)
            assertThat(stats.hits).isEqualTo(1)
            assertThat(stats.doubles).isEqualTo(1)
        }

        @Test
        fun `3루타 기록 시 타석, 타수, 안타, 3루타 증가`() {
            stats.applyLiveUpdate(PlateAppearanceResult.TRIPLE)

            assertThat(stats.plateAppearances).isEqualTo(1)
            assertThat(stats.atBats).isEqualTo(1)
            assertThat(stats.hits).isEqualTo(1)
            assertThat(stats.triples).isEqualTo(1)
        }

        @Test
        fun `홈런 기록 시 타석, 타수, 안타, 홈런 증가`() {
            stats.applyLiveUpdate(PlateAppearanceResult.HOME_RUN)

            assertThat(stats.plateAppearances).isEqualTo(1)
            assertThat(stats.atBats).isEqualTo(1)
            assertThat(stats.hits).isEqualTo(1)
            assertThat(stats.homeRuns).isEqualTo(1)
        }

        @Test
        fun `삼진 기록 시 타석, 타수, 삼진 증가 (안타 아님)`() {
            stats.applyLiveUpdate(PlateAppearanceResult.STRIKEOUT)

            assertThat(stats.plateAppearances).isEqualTo(1)
            assertThat(stats.atBats).isEqualTo(1)
            assertThat(stats.hits).isZero
            assertThat(stats.strikeouts).isEqualTo(1)
        }

        @Test
        fun `땅볼 아웃 기록 시 타석, 타수 증가 (안타, 삼진 아님)`() {
            stats.applyLiveUpdate(PlateAppearanceResult.GROUND_OUT)

            assertThat(stats.plateAppearances).isEqualTo(1)
            assertThat(stats.atBats).isEqualTo(1)
            assertThat(stats.hits).isZero
            assertThat(stats.strikeouts).isZero
        }

        @Test
        fun `볼넷 기록 시 타석만 증가, 타수 제외, 볼넷 증가`() {
            stats.applyLiveUpdate(PlateAppearanceResult.WALK)

            assertThat(stats.plateAppearances).isEqualTo(1)
            assertThat(stats.atBats).isZero
            assertThat(stats.hits).isZero
            assertThat(stats.walks).isEqualTo(1)
        }

        @Test
        fun `고의4구 기록 시 타석만 증가, 타수 제외, 고의4구 증가`() {
            stats.applyLiveUpdate(PlateAppearanceResult.INTENTIONAL_WALK)

            assertThat(stats.plateAppearances).isEqualTo(1)
            assertThat(stats.atBats).isZero
            assertThat(stats.intentionalWalks).isEqualTo(1)
        }

        @Test
        fun `사구 기록 시 타석만 증가, 타수 제외, 사구 증가`() {
            stats.applyLiveUpdate(PlateAppearanceResult.HIT_BY_PITCH)

            assertThat(stats.plateAppearances).isEqualTo(1)
            assertThat(stats.atBats).isZero
            assertThat(stats.hitByPitch).isEqualTo(1)
        }

        @Test
        fun `희생번트 기록 시 타석만 증가, 타수 제외, 희생번트 증가`() {
            stats.applyLiveUpdate(PlateAppearanceResult.SACRIFICE_BUNT)

            assertThat(stats.plateAppearances).isEqualTo(1)
            assertThat(stats.atBats).isZero
            assertThat(stats.sacrificeBunts).isEqualTo(1)
        }

        @Test
        fun `희생플라이 기록 시 타석만 증가, 타수 제외, 희생플라이 증가`() {
            stats.applyLiveUpdate(PlateAppearanceResult.SACRIFICE_FLY)

            assertThat(stats.plateAppearances).isEqualTo(1)
            assertThat(stats.atBats).isZero
            assertThat(stats.sacrificeFlies).isEqualTo(1)
        }

        @Test
        fun `낫아웃 삼진 기록 시 타석, 타수, 삼진 증가 (안타 아님)`() {
            stats.applyLiveUpdate(PlateAppearanceResult.STRIKEOUT_DROPPED_THIRD)

            assertThat(stats.plateAppearances).isEqualTo(1)
            assertThat(stats.atBats).isEqualTo(1)
            assertThat(stats.hits).isZero
            assertThat(stats.strikeouts).isEqualTo(1)
        }

        @Test
        fun `여러 타석 결과 누적 시 통계가 올바르게 집계됨`() {
            // 4타수 2안타 (단타, 홈런), 1볼넷, 1삼진
            stats.applyLiveUpdate(PlateAppearanceResult.SINGLE)
            stats.applyLiveUpdate(PlateAppearanceResult.HOME_RUN)
            stats.applyLiveUpdate(PlateAppearanceResult.WALK)
            stats.applyLiveUpdate(PlateAppearanceResult.STRIKEOUT)

            assertThat(stats.plateAppearances).isEqualTo(4)
            assertThat(stats.atBats).isEqualTo(3)
            assertThat(stats.hits).isEqualTo(2)
            assertThat(stats.homeRuns).isEqualTo(1)
            assertThat(stats.walks).isEqualTo(1)
            assertThat(stats.strikeouts).isEqualTo(1)
        }

        @Test
        fun `타율이 올바르게 계산됨 (3타수 1안타 = 333)`() {
            stats.applyLiveUpdate(PlateAppearanceResult.SINGLE)
            stats.applyLiveUpdate(PlateAppearanceResult.GROUND_OUT)
            stats.applyLiveUpdate(PlateAppearanceResult.STRIKEOUT)

            assertThat(stats.battingAverage).isEqualByComparingTo("0.333")
        }
    }

    @Nested
    @DisplayName("revertLiveUpdate - 타석 결과 역산")
    inner class RevertLiveUpdate {
        @Test
        fun `단타 역산 시 타석, 타수, 안타 감소`() {
            stats.applyLiveUpdate(PlateAppearanceResult.SINGLE)
            stats.revertLiveUpdate(PlateAppearanceResult.SINGLE)

            assertThat(stats.plateAppearances).isZero
            assertThat(stats.atBats).isZero
            assertThat(stats.hits).isZero
        }

        @Test
        fun `홈런 역산 시 타석, 타수, 안타, 홈런 감소`() {
            stats.applyLiveUpdate(PlateAppearanceResult.HOME_RUN)
            stats.revertLiveUpdate(PlateAppearanceResult.HOME_RUN)

            assertThat(stats.plateAppearances).isZero
            assertThat(stats.atBats).isZero
            assertThat(stats.hits).isZero
            assertThat(stats.homeRuns).isZero
        }

        @Test
        fun `2루타 역산 시 타석, 타수, 안타, 2루타 감소`() {
            stats.applyLiveUpdate(PlateAppearanceResult.DOUBLE)
            stats.revertLiveUpdate(PlateAppearanceResult.DOUBLE)

            assertThat(stats.plateAppearances).isZero
            assertThat(stats.atBats).isZero
            assertThat(stats.hits).isZero
            assertThat(stats.doubles).isZero
        }

        @Test
        fun `볼넷 역산 시 타석, 볼넷 감소`() {
            stats.applyLiveUpdate(PlateAppearanceResult.WALK)
            stats.revertLiveUpdate(PlateAppearanceResult.WALK)

            assertThat(stats.plateAppearances).isZero
            assertThat(stats.atBats).isZero
            assertThat(stats.walks).isZero
        }

        @Test
        fun `삼진 역산 시 타석, 타수, 삼진 감소`() {
            stats.applyLiveUpdate(PlateAppearanceResult.STRIKEOUT)
            stats.revertLiveUpdate(PlateAppearanceResult.STRIKEOUT)

            assertThat(stats.plateAppearances).isZero
            assertThat(stats.atBats).isZero
            assertThat(stats.strikeouts).isZero
        }

        @Test
        fun `낫아웃 삼진 역산 시 타석, 타수, 삼진 감소`() {
            stats.applyLiveUpdate(PlateAppearanceResult.STRIKEOUT_DROPPED_THIRD)
            stats.revertLiveUpdate(PlateAppearanceResult.STRIKEOUT_DROPPED_THIRD)

            assertThat(stats.plateAppearances).isZero
            assertThat(stats.atBats).isZero
            assertThat(stats.strikeouts).isZero
        }

        @Test
        fun `apply 후 revert 시 초기 상태로 복원`() {
            stats.applyLiveUpdate(PlateAppearanceResult.SINGLE)
            stats.applyLiveUpdate(PlateAppearanceResult.DOUBLE)
            stats.applyLiveUpdate(PlateAppearanceResult.WALK)
            stats.applyLiveUpdate(PlateAppearanceResult.STRIKEOUT)

            // 마지막 삼진 Undo
            stats.revertLiveUpdate(PlateAppearanceResult.STRIKEOUT)

            assertThat(stats.plateAppearances).isEqualTo(3)
            assertThat(stats.atBats).isEqualTo(2)
            assertThat(stats.hits).isEqualTo(2)
            assertThat(stats.walks).isEqualTo(1)
            assertThat(stats.strikeouts).isZero
        }

        @Test
        fun `통계가 0일 때 역산해도 음수가 되지 않음`() {
            // 이미 0인 상태에서 revert 호출
            stats.revertLiveUpdate(PlateAppearanceResult.SINGLE)

            assertThat(stats.plateAppearances).isZero
            assertThat(stats.atBats).isZero
            assertThat(stats.hits).isZero
        }

        @Test
        fun `희생번트 역산 시 타석, 희생번트 감소`() {
            stats.applyLiveUpdate(PlateAppearanceResult.SACRIFICE_BUNT)
            stats.revertLiveUpdate(PlateAppearanceResult.SACRIFICE_BUNT)

            assertThat(stats.plateAppearances).isZero
            assertThat(stats.atBats).isZero
            assertThat(stats.sacrificeBunts).isZero
        }

        @Test
        fun `희생플라이 역산 시 타석, 희생플라이 감소`() {
            stats.applyLiveUpdate(PlateAppearanceResult.SACRIFICE_FLY)
            stats.revertLiveUpdate(PlateAppearanceResult.SACRIFICE_FLY)

            assertThat(stats.plateAppearances).isZero
            assertThat(stats.atBats).isZero
            assertThat(stats.sacrificeFlies).isZero
        }
    }
}
