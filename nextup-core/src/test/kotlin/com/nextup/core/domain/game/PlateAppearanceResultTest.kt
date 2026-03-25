package com.nextup.core.domain.game

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@DisplayName("PlateAppearanceResult enum 테스트")
class PlateAppearanceResultTest {
    @ParameterizedTest
    @EnumSource(
        PlateAppearanceResult::class,
        names = [
            "SINGLE", "DOUBLE", "TRIPLE", "HOME_RUN", "STRIKEOUT", "GROUND_OUT",
            "FLY_OUT", "LINE_OUT", "FIELDERS_CHOICE", "ERROR", "DOUBLE_PLAY",
        ],
    )
    @DisplayName("타수에 포함되는 결과는 isAtBat이 true이다")
    fun atBatResults(result: PlateAppearanceResult) {
        assertThat(result.isAtBat).isTrue()
    }

    @ParameterizedTest
    @EnumSource(
        PlateAppearanceResult::class,
        names = [
            "WALK", "INTENTIONAL_WALK", "HIT_BY_PITCH",
            "SACRIFICE_BUNT", "SACRIFICE_FLY", "INTERFERENCE",
        ],
    )
    @DisplayName("비타수 결과는 isAtBat이 false이다")
    fun nonAtBatResults(result: PlateAppearanceResult) {
        assertThat(result.isAtBat).isFalse()
    }

    @ParameterizedTest
    @EnumSource(
        PlateAppearanceResult::class,
        names = ["SINGLE", "DOUBLE", "TRIPLE", "HOME_RUN"],
    )
    @DisplayName("안타 결과는 isHit이 true이다")
    fun hitResults(result: PlateAppearanceResult) {
        assertThat(result.isHit).isTrue()
    }

    @Nested
    @DisplayName("TRIPLE_PLAY과 BATTER/RUNNER_INTERFERENCE는 삭제됨")
    inner class RemovedValues {
        @Test
        @DisplayName("PlateAppearanceResult에 TRIPLE_PLAY이 없다")
        fun noTriplePlay() {
            val names = PlateAppearanceResult.entries.map { it.name }
            assertThat(names).doesNotContain("TRIPLE_PLAY")
        }

        @Test
        @DisplayName("PlateAppearanceResult에 BATTER_INTERFERENCE가 없다")
        fun noBatterInterference() {
            val names = PlateAppearanceResult.entries.map { it.name }
            assertThat(names).doesNotContain("BATTER_INTERFERENCE")
        }

        @Test
        @DisplayName("PlateAppearanceResult에 RUNNER_INTERFERENCE가 없다")
        fun noRunnerInterference() {
            val names = PlateAppearanceResult.entries.map { it.name }
            assertThat(names).doesNotContain("RUNNER_INTERFERENCE")
        }

        @Test
        @DisplayName("INTERFERENCE는 유지된다")
        fun interferenceKept() {
            val names = PlateAppearanceResult.entries.map { it.name }
            assertThat(names).contains("INTERFERENCE")
        }
    }

    @Nested
    @DisplayName("INFIELD_FLY와 낫아웃 유지 확인")
    inner class RetainedValues {
        @Test
        @DisplayName("INFIELD_FLY가 유지된다")
        fun infieldFlyKept() {
            assertThat(PlateAppearanceResult.INFIELD_FLY.isAtBat).isTrue()
            assertThat(PlateAppearanceResult.INFIELD_FLY.isHit).isFalse()
        }

        @Test
        @DisplayName("STRIKEOUT_DROPPED_THIRD가 유지된다")
        fun droppedThirdKept() {
            assertThat(PlateAppearanceResult.STRIKEOUT_DROPPED_THIRD.isAtBat).isTrue()
            assertThat(PlateAppearanceResult.STRIKEOUT_DROPPED_THIRD.isDroppedThirdStrike).isTrue()
            assertThat(PlateAppearanceResult.STRIKEOUT_DROPPED_THIRD.isStrikeout).isTrue()
        }
    }

    @Nested
    @DisplayName("출루 판정")
    inner class OnBase {
        @Test
        @DisplayName("INTERFERENCE는 출루에 성공한다")
        fun interferenceIsOnBase() {
            assertThat(PlateAppearanceResult.INTERFERENCE.isOnBase).isTrue()
        }

        @ParameterizedTest
        @EnumSource(
            PlateAppearanceResult::class,
            names = [
                "STRIKEOUT", "GROUND_OUT", "FLY_OUT", "INFIELD_FLY",
                "LINE_OUT", "DOUBLE_PLAY",
                "SACRIFICE_BUNT", "SACRIFICE_FLY",
            ],
        )
        @DisplayName("아웃 결과는 출루하지 않는다")
        fun outResultsNotOnBase(result: PlateAppearanceResult) {
            assertThat(result.isOnBase).isFalse()
        }
    }

    @Nested
    @DisplayName("루타 수")
    inner class TotalBases {
        @Test
        fun `SINGLE은 1루타`() {
            assertThat(PlateAppearanceResult.SINGLE.totalBases).isEqualTo(1)
        }

        @Test
        fun `DOUBLE은 2루타`() {
            assertThat(PlateAppearanceResult.DOUBLE.totalBases).isEqualTo(2)
        }

        @Test
        fun `TRIPLE은 3루타`() {
            assertThat(PlateAppearanceResult.TRIPLE.totalBases).isEqualTo(3)
        }

        @Test
        fun `HOME_RUN은 4루타`() {
            assertThat(PlateAppearanceResult.HOME_RUN.totalBases).isEqualTo(4)
        }

        @Test
        fun `WALK은 0루타`() {
            assertThat(PlateAppearanceResult.WALK.totalBases).isEqualTo(0)
        }
    }
}
