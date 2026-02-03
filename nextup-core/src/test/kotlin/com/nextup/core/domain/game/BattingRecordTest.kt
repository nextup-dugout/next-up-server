package com.nextup.core.domain.game

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("BattingRecord")
class BattingRecordTest {

    private lateinit var gamePlayer: GamePlayer
    private lateinit var battingRecord: BattingRecord

    @BeforeEach
    fun setup() {
        gamePlayer = mockk<GamePlayer>(relaxed = true)
        battingRecord = BattingRecord.create(gamePlayer)
    }

    @Nested
    @DisplayName("타석 결과 기록")
    inner class RecordPlateAppearanceTest {

        @Test
        fun `단타를 기록하면 타석, 타수, 안타가 증가한다`() {
            // when
            battingRecord.recordPlateAppearance(PlateAppearanceResult.SINGLE)

            // then
            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(1)
            assertThat(battingRecord.hits).isEqualTo(1)
        }

        @Test
        fun `2루타를 기록하면 doubles가 증가한다`() {
            // when
            battingRecord.recordPlateAppearance(PlateAppearanceResult.DOUBLE)

            // then
            assertThat(battingRecord.hits).isEqualTo(1)
            assertThat(battingRecord.doubles).isEqualTo(1)
        }

        @Test
        fun `3루타를 기록하면 triples가 증가한다`() {
            // when
            battingRecord.recordPlateAppearance(PlateAppearanceResult.TRIPLE)

            // then
            assertThat(battingRecord.hits).isEqualTo(1)
            assertThat(battingRecord.triples).isEqualTo(1)
        }

        @Test
        fun `홈런을 기록하면 homeRuns가 증가한다`() {
            // when
            battingRecord.recordPlateAppearance(PlateAppearanceResult.HOME_RUN)

            // then
            assertThat(battingRecord.hits).isEqualTo(1)
            assertThat(battingRecord.homeRuns).isEqualTo(1)
        }

        @Test
        fun `삼진을 기록하면 타수가 증가하고 strikeouts가 증가한다`() {
            // when
            battingRecord.recordPlateAppearance(PlateAppearanceResult.STRIKEOUT)

            // then
            assertThat(battingRecord.atBats).isEqualTo(1)
            assertThat(battingRecord.hits).isEqualTo(0)
            assertThat(battingRecord.strikeouts).isEqualTo(1)
        }

        @Test
        fun `볼넷을 기록하면 타수는 증가하지 않고 walks가 증가한다`() {
            // when
            battingRecord.recordPlateAppearance(PlateAppearanceResult.WALK)

            // then
            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.walks).isEqualTo(1)
        }

        @Test
        fun `고의사구를 기록하면 intentionalWalks가 증가한다`() {
            // when
            battingRecord.recordPlateAppearance(PlateAppearanceResult.INTENTIONAL_WALK)

            // then
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.intentionalWalks).isEqualTo(1)
        }

        @Test
        fun `사구를 기록하면 hitByPitch가 증가한다`() {
            // when
            battingRecord.recordPlateAppearance(PlateAppearanceResult.HIT_BY_PITCH)

            // then
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.hitByPitch).isEqualTo(1)
        }

        @Test
        fun `희생번트를 기록하면 타수는 증가하지 않고 sacrificeBunts가 증가한다`() {
            // when
            battingRecord.recordPlateAppearance(PlateAppearanceResult.SACRIFICE_BUNT)

            // then
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.sacrificeBunts).isEqualTo(1)
        }

        @Test
        fun `희생플라이를 기록하면 타수는 증가하지 않고 sacrificeFlies가 증가한다`() {
            // when
            battingRecord.recordPlateAppearance(PlateAppearanceResult.SACRIFICE_FLY)

            // then
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.sacrificeFlies).isEqualTo(1)
        }

        @Test
        fun `병살타를 기록하면 groundedIntoDoublePlays가 증가한다`() {
            // when
            battingRecord.recordPlateAppearance(PlateAppearanceResult.DOUBLE_PLAY)

            // then
            assertThat(battingRecord.groundedIntoDoublePlays).isEqualTo(1)
        }

        @Test
        fun `타점을 함께 기록할 수 있다`() {
            // when
            battingRecord.recordPlateAppearance(PlateAppearanceResult.SINGLE, runsBattedIn = 2)

            // then
            assertThat(battingRecord.runsBattedIn).isEqualTo(2)
        }

        @Test
        fun `득점을 함께 기록할 수 있다`() {
            // when
            battingRecord.recordPlateAppearance(PlateAppearanceResult.HOME_RUN, runsScored = true)

            // then
            assertThat(battingRecord.runs).isEqualTo(1)
        }

        @Test
        fun `음수 타점은 허용되지 않는다`() {
            // when & then
            assertThatThrownBy {
                battingRecord.recordPlateAppearance(PlateAppearanceResult.SINGLE, runsBattedIn = -1)
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("도루 기록")
    inner class StolenBaseTest {

        @Test
        fun `도루 성공을 기록할 수 있다`() {
            // when
            battingRecord.recordStolenBase()

            // then
            assertThat(battingRecord.stolenBases).isEqualTo(1)
        }

        @Test
        fun `도루 실패를 기록할 수 있다`() {
            // when
            battingRecord.recordCaughtStealing()

            // then
            assertThat(battingRecord.caughtStealing).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("득점 기록")
    inner class RunTest {

        @Test
        fun `득점을 별도로 기록할 수 있다`() {
            // when
            battingRecord.recordRun()

            // then
            assertThat(battingRecord.runs).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("계산된 속성")
    inner class CalculatedPropertiesTest {

        @Test
        fun `singles는 안타에서 장타를 뺀 값이다`() {
            // given
            battingRecord.recordPlateAppearance(PlateAppearanceResult.SINGLE)
            battingRecord.recordPlateAppearance(PlateAppearanceResult.SINGLE)
            battingRecord.recordPlateAppearance(PlateAppearanceResult.DOUBLE)
            battingRecord.recordPlateAppearance(PlateAppearanceResult.HOME_RUN)

            // then
            assertThat(battingRecord.singles).isEqualTo(2)
        }

        @Test
        fun `totalBases를 올바르게 계산한다`() {
            // given
            battingRecord.recordPlateAppearance(PlateAppearanceResult.SINGLE) // 1
            battingRecord.recordPlateAppearance(PlateAppearanceResult.DOUBLE) // 2
            battingRecord.recordPlateAppearance(PlateAppearanceResult.TRIPLE) // 3
            battingRecord.recordPlateAppearance(PlateAppearanceResult.HOME_RUN) // 4

            // then
            assertThat(battingRecord.totalBases).isEqualTo(10)
        }

        @Test
        fun `extraBaseHits를 올바르게 계산한다`() {
            // given
            battingRecord.recordPlateAppearance(PlateAppearanceResult.SINGLE)
            battingRecord.recordPlateAppearance(PlateAppearanceResult.DOUBLE)
            battingRecord.recordPlateAppearance(PlateAppearanceResult.TRIPLE)

            // then
            assertThat(battingRecord.extraBaseHits).isEqualTo(2)
        }

        @Test
        fun `sacrifices를 올바르게 계산한다`() {
            // given
            battingRecord.recordPlateAppearance(PlateAppearanceResult.SACRIFICE_BUNT)
            battingRecord.recordPlateAppearance(PlateAppearanceResult.SACRIFICE_FLY)

            // then
            assertThat(battingRecord.sacrifices).isEqualTo(2)
        }

        @Test
        fun `totalWalks를 올바르게 계산한다`() {
            // given
            battingRecord.recordPlateAppearance(PlateAppearanceResult.WALK)
            battingRecord.recordPlateAppearance(PlateAppearanceResult.WALK)
            battingRecord.recordPlateAppearance(PlateAppearanceResult.INTENTIONAL_WALK)

            // then
            assertThat(battingRecord.totalWalks).isEqualTo(3)
        }
    }

    @Nested
    @DisplayName("타율 계산")
    inner class BattingAverageTest {

        @Test
        fun `타수가 0이면 타율은 0이다`() {
            // when
            battingRecord.recordPlateAppearance(PlateAppearanceResult.WALK)

            // then
            assertThat(battingRecord.battingAverage).isEqualTo(BigDecimal("0.000"))
        }

        @Test
        fun `3타수 1안타면 타율은 0_333이다`() {
            // given
            battingRecord.recordPlateAppearance(PlateAppearanceResult.SINGLE)
            battingRecord.recordPlateAppearance(PlateAppearanceResult.STRIKEOUT)
            battingRecord.recordPlateAppearance(PlateAppearanceResult.GROUND_OUT)

            // then
            assertThat(battingRecord.battingAverage).isEqualTo(BigDecimal("0.333"))
        }

        @Test
        fun `4타수 2안타면 타율은 0_500이다`() {
            // given
            battingRecord.recordPlateAppearance(PlateAppearanceResult.SINGLE)
            battingRecord.recordPlateAppearance(PlateAppearanceResult.DOUBLE)
            battingRecord.recordPlateAppearance(PlateAppearanceResult.STRIKEOUT)
            battingRecord.recordPlateAppearance(PlateAppearanceResult.GROUND_OUT)

            // then
            assertThat(battingRecord.battingAverage).isEqualTo(BigDecimal("0.500"))
        }
    }

    @Nested
    @DisplayName("출루율 계산")
    inner class OnBasePercentageTest {

        @Test
        fun `분모가 0이면 출루율은 0이다`() {
            // then
            assertThat(battingRecord.onBasePercentage).isEqualTo(BigDecimal("0.000"))
        }

        @Test
        fun `출루율은 안타, 볼넷, 사구를 타수, 볼넷, 사구, 희생플라이로 나눈 값이다`() {
            // given: 4타수 1안타 1볼넷 1사구
            battingRecord.recordPlateAppearance(PlateAppearanceResult.SINGLE) // H=1, AB=1
            battingRecord.recordPlateAppearance(PlateAppearanceResult.WALK) // BB=1
            battingRecord.recordPlateAppearance(PlateAppearanceResult.HIT_BY_PITCH) // HBP=1
            battingRecord.recordPlateAppearance(PlateAppearanceResult.STRIKEOUT) // AB=2
            battingRecord.recordPlateAppearance(PlateAppearanceResult.GROUND_OUT) // AB=3

            // when: OBP = (1 + 1 + 1) / (3 + 1 + 1 + 0) = 3/5 = 0.600
            // then
            assertThat(battingRecord.onBasePercentage).isEqualTo(BigDecimal("0.600"))
        }
    }

    @Nested
    @DisplayName("장타율 계산")
    inner class SluggingPercentageTest {

        @Test
        fun `타수가 0이면 장타율은 0이다`() {
            // then
            assertThat(battingRecord.sluggingPercentage).isEqualTo(BigDecimal("0.000"))
        }

        @Test
        fun `총루타를 타수로 나눈 값이다`() {
            // given: 4타수 - 단타(1), 2루타(2), 삼진(0), 땅볼(0) = 3루타
            battingRecord.recordPlateAppearance(PlateAppearanceResult.SINGLE) // 1
            battingRecord.recordPlateAppearance(PlateAppearanceResult.DOUBLE) // 2
            battingRecord.recordPlateAppearance(PlateAppearanceResult.STRIKEOUT) // 0
            battingRecord.recordPlateAppearance(PlateAppearanceResult.GROUND_OUT) // 0

            // when: SLG = 3/4 = 0.750
            // then
            assertThat(battingRecord.sluggingPercentage).isEqualTo(BigDecimal("0.750"))
        }
    }

    @Nested
    @DisplayName("OPS 계산")
    inner class OpsTest {

        @Test
        fun `OPS는 출루율 + 장타율이다`() {
            // given: 간단한 예시 - 4타수 2안타(단타 2개)
            battingRecord.recordPlateAppearance(PlateAppearanceResult.SINGLE)
            battingRecord.recordPlateAppearance(PlateAppearanceResult.SINGLE)
            battingRecord.recordPlateAppearance(PlateAppearanceResult.STRIKEOUT)
            battingRecord.recordPlateAppearance(PlateAppearanceResult.GROUND_OUT)

            // AVG = 0.500, OBP = 0.500, SLG = 0.500
            // OPS = 1.000
            // then
            assertThat(battingRecord.ops).isEqualTo(BigDecimal("1.000"))
        }
    }

    @Nested
    @DisplayName("도루 성공률 계산")
    inner class StolenBasePercentageTest {

        @Test
        fun `도루 시도가 없으면 0이다`() {
            // then
            assertThat(battingRecord.stolenBasePercentage).isEqualTo(BigDecimal("0.000"))
        }

        @Test
        fun `도루를 총 도루시도로 나눈 값이다`() {
            // given
            battingRecord.recordStolenBase()
            battingRecord.recordStolenBase()
            battingRecord.recordCaughtStealing()

            // when: 2 / 3 = 0.667
            // then
            assertThat(battingRecord.stolenBasePercentage).isEqualTo(BigDecimal("0.667"))
        }
    }

    @Nested
    @DisplayName("유효성 검증")
    inner class ValidationTest {

        @Test
        fun `정상적인 기록은 검증에 통과한다`() {
            // given
            battingRecord.recordPlateAppearance(PlateAppearanceResult.SINGLE)
            battingRecord.recordPlateAppearance(PlateAppearanceResult.DOUBLE)
            battingRecord.recordPlateAppearance(PlateAppearanceResult.STRIKEOUT)

            // when & then
            battingRecord.validate() // 예외 없이 통과
        }
    }

    @Nested
    @DisplayName("applyPlateAppearanceResult - BoxScore 자동 계산")
    inner class ApplyPlateAppearanceResultTest {

        @Test
        fun `단타를 적용하면 타석, 타수, 안타가 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)

            // then
            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(1)
            assertThat(battingRecord.hits).isEqualTo(1)
        }

        @Test
        fun `2루타를 적용하면 doubles가 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.DOUBLE)

            // then
            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(1)
            assertThat(battingRecord.hits).isEqualTo(1)
            assertThat(battingRecord.doubles).isEqualTo(1)
        }

        @Test
        fun `3루타를 적용하면 triples가 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.TRIPLE)

            // then
            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(1)
            assertThat(battingRecord.hits).isEqualTo(1)
            assertThat(battingRecord.triples).isEqualTo(1)
        }

        @Test
        fun `홈런을 적용하면 homeRuns와 runs가 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.HOME_RUN)

            // then
            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(1)
            assertThat(battingRecord.hits).isEqualTo(1)
            assertThat(battingRecord.homeRuns).isEqualTo(1)
            assertThat(battingRecord.runs).isEqualTo(1) // 홈런은 타자 자신도 득점
        }

        @Test
        fun `삼진을 적용하면 타수와 strikeouts가 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.STRIKEOUT)

            // then
            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(1)
            assertThat(battingRecord.strikeouts).isEqualTo(1)
            assertThat(battingRecord.hits).isEqualTo(0)
        }

        @Test
        fun `볼넷을 적용하면 타수는 증가하지 않고 walks가 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.WALK)

            // then
            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.walks).isEqualTo(1)
        }

        @Test
        fun `고의사구를 적용하면 intentionalWalks가 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.INTENTIONAL_WALK)

            // then
            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.intentionalWalks).isEqualTo(1)
        }

        @Test
        fun `사구를 적용하면 hitByPitch가 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.HIT_BY_PITCH)

            // then
            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.hitByPitch).isEqualTo(1)
        }

        @Test
        fun `희생플라이를 적용하면 타수는 증가하지 않고 sacrificeFlies가 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SACRIFICE_FLY)

            // then
            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.sacrificeFlies).isEqualTo(1)
        }

        @Test
        fun `희생번트를 적용하면 타수는 증가하지 않고 sacrificeBunts가 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SACRIFICE_BUNT)

            // then
            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.sacrificeBunts).isEqualTo(1)
        }

        @Test
        fun `땅볼아웃을 적용하면 타수만 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.GROUND_OUT)

            // then
            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(1)
            assertThat(battingRecord.hits).isEqualTo(0)
        }

        @Test
        fun `병살타를 적용하면 타수와 groundedIntoDoublePlays가 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.DOUBLE_PLAY)

            // then
            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(1)
            assertThat(battingRecord.groundedIntoDoublePlays).isEqualTo(1)
        }

        @Test
        fun `방해는 타수에 포함되지 않는다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.INTERFERENCE)

            // then
            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(0)
        }

        @Test
        fun `타점을 함께 적용할 수 있다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE, rbis = 2)

            // then
            assertThat(battingRecord.runsBattedIn).isEqualTo(2)
        }

        @Test
        fun `음수 타점은 허용되지 않는다`() {
            // when & then
            assertThatThrownBy {
                battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE, rbis = -1)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("타점은 0 이상이어야 합니다")
        }

        @Test
        fun `연속된 타석 결과를 올바르게 누적한다`() {
            // given
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE, rbis = 0)
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.DOUBLE, rbis = 1)
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.HOME_RUN, rbis = 2)
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.STRIKEOUT, rbis = 0)

            // then
            assertThat(battingRecord.plateAppearances).isEqualTo(4)
            assertThat(battingRecord.atBats).isEqualTo(4)
            assertThat(battingRecord.hits).isEqualTo(3)
            assertThat(battingRecord.doubles).isEqualTo(1)
            assertThat(battingRecord.homeRuns).isEqualTo(1)
            assertThat(battingRecord.runs).isEqualTo(1) // 홈런 득점
            assertThat(battingRecord.runsBattedIn).isEqualTo(3)
            assertThat(battingRecord.strikeouts).isEqualTo(1)
        }
    }
}
