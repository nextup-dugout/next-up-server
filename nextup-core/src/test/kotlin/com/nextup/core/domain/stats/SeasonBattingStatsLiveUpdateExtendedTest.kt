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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@DisplayName("SeasonBattingStats 실시간 통계 갱신 확장 테스트")
class SeasonBattingStatsLiveUpdateExtendedTest {
    private val testPlayer =
        Player(
            name = "이순신",
            primaryPosition = Position.SHORTSTOP,
            throwingHand = ThrowingHand.RIGHT,
            battingHand = BattingHand.LEFT,
            id = 2L,
        )

    private lateinit var stats: SeasonBattingStats

    @BeforeEach
    fun setUp() {
        stats = SeasonBattingStats.create(testPlayer, 2024)
    }

    @Nested
    @DisplayName("applyLiveUpdate - 아웃 결과 (타수 O, 안타 X)")
    inner class ApplyOutResults {
        @Test
        fun `플라이 아웃 기록 시 타석, 타수 증가 (안타 없음)`() {
            // when
            stats.applyLiveUpdate(PlateAppearanceResult.FLY_OUT)

            // then
            assertThat(stats.plateAppearances).isEqualTo(1)
            assertThat(stats.atBats).isEqualTo(1)
            assertThat(stats.hits).isZero
            assertThat(stats.strikeouts).isZero
        }

        @Test
        fun `라인드라이브 아웃 기록 시 타석, 타수 증가`() {
            // when
            stats.applyLiveUpdate(PlateAppearanceResult.LINE_OUT)

            // then
            assertThat(stats.plateAppearances).isEqualTo(1)
            assertThat(stats.atBats).isEqualTo(1)
            assertThat(stats.hits).isZero
        }

        @Test
        fun `병살타 기록 시 타석, 타수 증가`() {
            // when
            stats.applyLiveUpdate(PlateAppearanceResult.DOUBLE_PLAY)

            // then
            assertThat(stats.plateAppearances).isEqualTo(1)
            assertThat(stats.atBats).isEqualTo(1)
            assertThat(stats.hits).isZero
        }

        @Test
        fun `삼중살 기록 시 타석, 타수 증가`() {
            // when
            stats.applyLiveUpdate(PlateAppearanceResult.TRIPLE_PLAY)

            // then
            assertThat(stats.plateAppearances).isEqualTo(1)
            assertThat(stats.atBats).isEqualTo(1)
            assertThat(stats.hits).isZero
        }

        @Test
        fun `야수 선택 기록 시 타석, 타수 증가`() {
            // when
            stats.applyLiveUpdate(PlateAppearanceResult.FIELDERS_CHOICE)

            // then
            assertThat(stats.plateAppearances).isEqualTo(1)
            assertThat(stats.atBats).isEqualTo(1)
            assertThat(stats.hits).isZero
        }

        @Test
        fun `실책 기록 시 타석, 타수 증가 (안타 아님)`() {
            // when
            stats.applyLiveUpdate(PlateAppearanceResult.ERROR)

            // then
            assertThat(stats.plateAppearances).isEqualTo(1)
            assertThat(stats.atBats).isEqualTo(1)
            assertThat(stats.hits).isZero
        }
    }

    @Nested
    @DisplayName("applyLiveUpdate - 비타수 결과 (타수 X)")
    inner class ApplyNonAtBatResults {
        @Test
        fun `방해 기록 시 타석만 증가, 타수 미포함`() {
            // when
            stats.applyLiveUpdate(PlateAppearanceResult.INTERFERENCE)

            // then
            assertThat(stats.plateAppearances).isEqualTo(1)
            assertThat(stats.atBats).isZero
            assertThat(stats.hits).isZero
        }
    }

    @Nested
    @DisplayName("revertLiveUpdate - 추가 역산 케이스")
    inner class RevertAdditionalCases {
        @Test
        fun `3루타 역산 시 타석, 타수, 안타, 3루타 감소`() {
            // given
            stats.applyLiveUpdate(PlateAppearanceResult.TRIPLE)

            // when
            stats.revertLiveUpdate(PlateAppearanceResult.TRIPLE)

            // then
            assertThat(stats.plateAppearances).isZero
            assertThat(stats.atBats).isZero
            assertThat(stats.hits).isZero
            assertThat(stats.triples).isZero
        }

        @Test
        fun `고의4구 역산 시 타석, 고의4구 감소`() {
            // given
            stats.applyLiveUpdate(PlateAppearanceResult.INTENTIONAL_WALK)

            // when
            stats.revertLiveUpdate(PlateAppearanceResult.INTENTIONAL_WALK)

            // then
            assertThat(stats.plateAppearances).isZero
            assertThat(stats.atBats).isZero
            assertThat(stats.intentionalWalks).isZero
        }

        @Test
        fun `사구 역산 시 타석, 사구 감소`() {
            // given
            stats.applyLiveUpdate(PlateAppearanceResult.HIT_BY_PITCH)

            // when
            stats.revertLiveUpdate(PlateAppearanceResult.HIT_BY_PITCH)

            // then
            assertThat(stats.plateAppearances).isZero
            assertThat(stats.atBats).isZero
            assertThat(stats.hitByPitch).isZero
        }

        @Test
        fun `플라이 아웃 역산 시 타석, 타수 감소`() {
            // given
            stats.applyLiveUpdate(PlateAppearanceResult.FLY_OUT)

            // when
            stats.revertLiveUpdate(PlateAppearanceResult.FLY_OUT)

            // then
            assertThat(stats.plateAppearances).isZero
            assertThat(stats.atBats).isZero
        }

        @Test
        fun `병살타 역산 시 타석, 타수 감소`() {
            // given
            stats.applyLiveUpdate(PlateAppearanceResult.DOUBLE_PLAY)

            // when
            stats.revertLiveUpdate(PlateAppearanceResult.DOUBLE_PLAY)

            // then
            assertThat(stats.plateAppearances).isZero
            assertThat(stats.atBats).isZero
        }

        @Test
        fun `방해 역산 시 타석 감소, 타수 미포함`() {
            // given
            stats.applyLiveUpdate(PlateAppearanceResult.INTERFERENCE)

            // when
            stats.revertLiveUpdate(PlateAppearanceResult.INTERFERENCE)

            // then
            assertThat(stats.plateAppearances).isZero
            assertThat(stats.atBats).isZero
        }

        @Test
        fun `고의4구가 0일 때 역산해도 음수가 되지 않음`() {
            // when - 0 상태에서 역산
            stats.revertLiveUpdate(PlateAppearanceResult.INTENTIONAL_WALK)

            // then
            assertThat(stats.intentionalWalks).isZero
            assertThat(stats.plateAppearances).isZero
        }

        @Test
        fun `사구가 0일 때 역산해도 음수가 되지 않음`() {
            // when
            stats.revertLiveUpdate(PlateAppearanceResult.HIT_BY_PITCH)

            // then
            assertThat(stats.hitByPitch).isZero
            assertThat(stats.plateAppearances).isZero
        }

        @Test
        fun `3루타가 0일 때 역산해도 음수가 되지 않음`() {
            // when
            stats.revertLiveUpdate(PlateAppearanceResult.TRIPLE)

            // then
            assertThat(stats.triples).isZero
            assertThat(stats.plateAppearances).isZero
        }
    }

    @Nested
    @DisplayName("applyLiveUpdate - 계산 속성 검증")
    inner class CalculatedPropertiesAfterLiveUpdate {
        @Test
        fun `출루율이 볼넷 포함하여 올바르게 계산됨`() {
            // given: 3타수 1안타 1볼넷 1사구
            stats.applyLiveUpdate(PlateAppearanceResult.SINGLE)
            stats.applyLiveUpdate(PlateAppearanceResult.GROUND_OUT)
            stats.applyLiveUpdate(PlateAppearanceResult.GROUND_OUT)
            stats.applyLiveUpdate(PlateAppearanceResult.WALK)
            stats.applyLiveUpdate(PlateAppearanceResult.HIT_BY_PITCH)

            // then: OBP = (1+1+1)/(3+1+1+0) = 3/5 = 0.600
            assertThat(stats.onBasePercentage).isEqualByComparingTo("0.600")
        }

        @Test
        fun `고의4구가 총 볼넷에 포함됨`() {
            // given
            stats.applyLiveUpdate(PlateAppearanceResult.WALK)
            stats.applyLiveUpdate(PlateAppearanceResult.INTENTIONAL_WALK)

            // then
            assertThat(stats.totalWalks).isEqualTo(2)
            assertThat(stats.walks).isEqualTo(1)
            assertThat(stats.intentionalWalks).isEqualTo(1)
        }

        @Test
        fun `희생번트와 희생플라이가 총 희생타에 포함됨`() {
            // given
            stats.applyLiveUpdate(PlateAppearanceResult.SACRIFICE_BUNT)
            stats.applyLiveUpdate(PlateAppearanceResult.SACRIFICE_FLY)

            // then
            assertThat(stats.sacrifices).isEqualTo(2)
        }

        @Test
        fun `장타율이 올바르게 계산됨 (2루타 포함)`() {
            // given: 5타수, 1단타, 1-2루타 = totalBases = 1+2 = 3
            stats.applyLiveUpdate(PlateAppearanceResult.SINGLE)
            stats.applyLiveUpdate(PlateAppearanceResult.DOUBLE)
            stats.applyLiveUpdate(PlateAppearanceResult.STRIKEOUT)
            stats.applyLiveUpdate(PlateAppearanceResult.GROUND_OUT)
            stats.applyLiveUpdate(PlateAppearanceResult.FLY_OUT)

            // then: SLG = 3/5 = 0.600
            assertThat(stats.sluggingPercentage).isEqualByComparingTo("0.600")
        }

        @Test
        fun `도루 성공률이 0일 때 0을 반환함`() {
            // given: 도루 시도 없음
            // then
            assertThat(stats.stolenBasePercentage).isEqualByComparingTo("0.000")
        }

        @Test
        fun `단타 수가 올바르게 계산됨`() {
            // given: 3안타 중 2루타 1개
            stats.applyLiveUpdate(PlateAppearanceResult.SINGLE)
            stats.applyLiveUpdate(PlateAppearanceResult.SINGLE)
            stats.applyLiveUpdate(PlateAppearanceResult.DOUBLE)

            // then: singles = 3 - 0 - 0 - 1 = 2 (hits=3, doubles=1)
            assertThat(stats.singles).isEqualTo(2)
        }

        @Test
        fun `장타 수 (extraBaseHits) 가 올바르게 계산됨`() {
            // given
            stats.applyLiveUpdate(PlateAppearanceResult.DOUBLE)
            stats.applyLiveUpdate(PlateAppearanceResult.TRIPLE)
            stats.applyLiveUpdate(PlateAppearanceResult.HOME_RUN)

            // then
            assertThat(stats.extraBaseHits).isEqualTo(3)
        }

        @Test
        fun `OPS가 출루율과 장타율의 합으로 계산됨`() {
            // given: 4타수 2안타 (단타, 2루타)
            stats.applyLiveUpdate(PlateAppearanceResult.SINGLE)
            stats.applyLiveUpdate(PlateAppearanceResult.DOUBLE)
            stats.applyLiveUpdate(PlateAppearanceResult.STRIKEOUT)
            stats.applyLiveUpdate(PlateAppearanceResult.GROUND_OUT)

            // then
            val obp = stats.onBasePercentage
            val slg = stats.sluggingPercentage
            assertThat(stats.ops).isEqualByComparingTo(obp.add(slg))
        }
    }

    @Nested
    @DisplayName("apply 후 revert 종합 시나리오")
    inner class ApplyRevertScenarios {
        @Test
        fun `여러 결과 적용 후 순차 역산 시 올바르게 복원됨`() {
            // given
            stats.applyLiveUpdate(PlateAppearanceResult.SINGLE)
            stats.applyLiveUpdate(PlateAppearanceResult.WALK)
            stats.applyLiveUpdate(PlateAppearanceResult.DOUBLE)

            // when - 2루타 역산
            stats.revertLiveUpdate(PlateAppearanceResult.DOUBLE)

            // then
            assertThat(stats.plateAppearances).isEqualTo(2)
            assertThat(stats.atBats).isEqualTo(1)
            assertThat(stats.hits).isEqualTo(1)
            assertThat(stats.doubles).isZero
        }

        @ParameterizedTest
        @EnumSource(
            value = PlateAppearanceResult::class,
            names = [
                "SINGLE", "DOUBLE", "TRIPLE", "HOME_RUN",
                "STRIKEOUT", "GROUND_OUT", "FLY_OUT", "LINE_OUT",
                "WALK", "INTENTIONAL_WALK", "HIT_BY_PITCH",
                "SACRIFICE_BUNT", "SACRIFICE_FLY",
            ],
        )
        fun `모든 주요 결과에 대해 apply 후 revert 시 초기 상태로 복원됨`(result: PlateAppearanceResult) {
            // given
            val initialPA = stats.plateAppearances

            // when
            stats.applyLiveUpdate(result)
            stats.revertLiveUpdate(result)

            // then
            assertThat(stats.plateAppearances).isEqualTo(initialPA)
        }
    }
}
