package com.nextup.core.domain.game

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@DisplayName("PlateAppearanceResult")
class PlateAppearanceResultTest {

    @Nested
    @DisplayName("isAtBat 속성")
    inner class IsAtBatTest {

        @ParameterizedTest
        @EnumSource(
            value = PlateAppearanceResult::class,
            names = ["SINGLE", "DOUBLE", "TRIPLE", "HOME_RUN", "STRIKEOUT", "GROUND_OUT", "FLY_OUT", "LINE_OUT", "FIELDERS_CHOICE", "ERROR", "DOUBLE_PLAY", "TRIPLE_PLAY"]
        )
        fun `타수에 포함되는 결과를 반환한다`(result: PlateAppearanceResult) {
            assertThat(result.isAtBat).isTrue()
        }

        @ParameterizedTest
        @EnumSource(
            value = PlateAppearanceResult::class,
            names = ["WALK", "INTENTIONAL_WALK", "HIT_BY_PITCH", "SACRIFICE_BUNT", "SACRIFICE_FLY", "INTERFERENCE"]
        )
        fun `타수에 포함되지 않는 결과를 반환한다`(result: PlateAppearanceResult) {
            assertThat(result.isAtBat).isFalse()
        }
    }

    @Nested
    @DisplayName("isHit 속성")
    inner class IsHitTest {

        @ParameterizedTest
        @EnumSource(
            value = PlateAppearanceResult::class,
            names = ["SINGLE", "DOUBLE", "TRIPLE", "HOME_RUN"]
        )
        fun `안타 결과를 반환한다`(result: PlateAppearanceResult) {
            assertThat(result.isHit).isTrue()
        }

        @ParameterizedTest
        @EnumSource(
            value = PlateAppearanceResult::class,
            names = ["STRIKEOUT", "GROUND_OUT", "FLY_OUT", "WALK", "SACRIFICE_BUNT"]
        )
        fun `안타가 아닌 결과를 반환한다`(result: PlateAppearanceResult) {
            assertThat(result.isHit).isFalse()
        }
    }

    @Nested
    @DisplayName("totalBases 속성")
    inner class TotalBasesTest {

        @Test
        fun `단타는 1루타를 반환한다`() {
            assertThat(PlateAppearanceResult.SINGLE.totalBases).isEqualTo(1)
        }

        @Test
        fun `2루타는 2루타를 반환한다`() {
            assertThat(PlateAppearanceResult.DOUBLE.totalBases).isEqualTo(2)
        }

        @Test
        fun `3루타는 3루타를 반환한다`() {
            assertThat(PlateAppearanceResult.TRIPLE.totalBases).isEqualTo(3)
        }

        @Test
        fun `홈런은 4루타를 반환한다`() {
            assertThat(PlateAppearanceResult.HOME_RUN.totalBases).isEqualTo(4)
        }

        @Test
        fun `안타가 아닌 결과는 0을 반환한다`() {
            assertThat(PlateAppearanceResult.STRIKEOUT.totalBases).isEqualTo(0)
            assertThat(PlateAppearanceResult.WALK.totalBases).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("장타 속성")
    inner class ExtraBaseHitTest {

        @Test
        fun `2루타는 장타이다`() {
            assertThat(PlateAppearanceResult.DOUBLE.isExtraBaseHit).isTrue()
        }

        @Test
        fun `3루타는 장타이다`() {
            assertThat(PlateAppearanceResult.TRIPLE.isExtraBaseHit).isTrue()
        }

        @Test
        fun `홈런은 장타이다`() {
            assertThat(PlateAppearanceResult.HOME_RUN.isExtraBaseHit).isTrue()
        }

        @Test
        fun `단타는 장타가 아니다`() {
            assertThat(PlateAppearanceResult.SINGLE.isExtraBaseHit).isFalse()
        }
    }

    @Nested
    @DisplayName("출루 속성")
    inner class IsOnBaseTest {

        @ParameterizedTest
        @EnumSource(
            value = PlateAppearanceResult::class,
            names = ["SINGLE", "DOUBLE", "TRIPLE", "HOME_RUN", "WALK", "INTENTIONAL_WALK", "HIT_BY_PITCH", "FIELDERS_CHOICE", "ERROR", "INTERFERENCE"]
        )
        fun `출루에 성공한 결과를 반환한다`(result: PlateAppearanceResult) {
            assertThat(result.isOnBase).isTrue()
        }

        @ParameterizedTest
        @EnumSource(
            value = PlateAppearanceResult::class,
            names = ["STRIKEOUT", "GROUND_OUT", "FLY_OUT", "LINE_OUT", "SACRIFICE_BUNT", "SACRIFICE_FLY", "DOUBLE_PLAY", "TRIPLE_PLAY"]
        )
        fun `출루에 실패한 결과를 반환한다`(result: PlateAppearanceResult) {
            assertThat(result.isOnBase).isFalse()
        }
    }

    @Nested
    @DisplayName("볼넷 관련 속성")
    inner class WalkTest {

        @Test
        fun `볼넷은 isWalk가 true이다`() {
            assertThat(PlateAppearanceResult.WALK.isWalk).isTrue()
        }

        @Test
        fun `고의사구도 isWalk가 true이다`() {
            assertThat(PlateAppearanceResult.INTENTIONAL_WALK.isWalk).isTrue()
        }

        @Test
        fun `다른 결과는 isWalk가 false이다`() {
            assertThat(PlateAppearanceResult.HIT_BY_PITCH.isWalk).isFalse()
        }
    }

    @Nested
    @DisplayName("희생타 속성")
    inner class SacrificeTest {

        @Test
        fun `희생번트는 희생타이다`() {
            assertThat(PlateAppearanceResult.SACRIFICE_BUNT.isSacrifice).isTrue()
        }

        @Test
        fun `희생플라이는 희생타이다`() {
            assertThat(PlateAppearanceResult.SACRIFICE_FLY.isSacrifice).isTrue()
        }

        @Test
        fun `일반 플라이아웃은 희생타가 아니다`() {
            assertThat(PlateAppearanceResult.FLY_OUT.isSacrifice).isFalse()
        }
    }
}
