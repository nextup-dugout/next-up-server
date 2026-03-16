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
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)

            // then
            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(1)
            assertThat(battingRecord.hits).isEqualTo(1)
        }

        @Test
        fun `2루타를 기록하면 doubles가 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.DOUBLE)

            // then
            assertThat(battingRecord.hits).isEqualTo(1)
            assertThat(battingRecord.doubles).isEqualTo(1)
        }

        @Test
        fun `3루타를 기록하면 triples가 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.TRIPLE)

            // then
            assertThat(battingRecord.hits).isEqualTo(1)
            assertThat(battingRecord.triples).isEqualTo(1)
        }

        @Test
        fun `홈런을 기록하면 homeRuns가 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.HOME_RUN)

            // then
            assertThat(battingRecord.hits).isEqualTo(1)
            assertThat(battingRecord.homeRuns).isEqualTo(1)
        }

        @Test
        fun `삼진을 기록하면 타수가 증가하고 strikeouts가 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.STRIKEOUT)

            // then
            assertThat(battingRecord.atBats).isEqualTo(1)
            assertThat(battingRecord.hits).isEqualTo(0)
            assertThat(battingRecord.strikeouts).isEqualTo(1)
        }

        @Test
        fun `볼넷을 기록하면 타수는 증가하지 않고 walks가 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.WALK)

            // then
            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.walks).isEqualTo(1)
        }

        @Test
        fun `고의사구를 기록하면 intentionalWalks가 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.INTENTIONAL_WALK)

            // then
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.intentionalWalks).isEqualTo(1)
        }

        @Test
        fun `사구를 기록하면 hitByPitch가 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.HIT_BY_PITCH)

            // then
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.hitByPitch).isEqualTo(1)
        }

        @Test
        fun `희생번트를 기록하면 타수는 증가하지 않고 sacrificeBunts가 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SACRIFICE_BUNT)

            // then
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.sacrificeBunts).isEqualTo(1)
        }

        @Test
        fun `희생플라이를 기록하면 타수는 증가하지 않고 sacrificeFlies가 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SACRIFICE_FLY)

            // then
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.sacrificeFlies).isEqualTo(1)
        }

        @Test
        fun `병살타를 기록하면 groundedIntoDoublePlays가 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.DOUBLE_PLAY)

            // then
            assertThat(battingRecord.groundedIntoDoublePlays).isEqualTo(1)
        }

        @Test
        fun `타점을 함께 기록할 수 있다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE, rbis = 2)

            // then
            assertThat(battingRecord.runsBattedIn).isEqualTo(2)
        }

        @Test
        fun `홈런을 기록하면 타자 자신의 득점도 자동으로 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.HOME_RUN)

            // then
            assertThat(battingRecord.runs).isEqualTo(1)
        }

        @Test
        fun `음수 타점은 허용되지 않는다`() {
            // when & then
            assertThatThrownBy {
                battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE, rbis = -1)
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
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.DOUBLE)
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.HOME_RUN)

            // then
            assertThat(battingRecord.singles).isEqualTo(2)
        }

        @Test
        fun `totalBases를 올바르게 계산한다`() {
            // given
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE) // 1
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.DOUBLE) // 2
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.TRIPLE) // 3
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.HOME_RUN) // 4

            // then
            assertThat(battingRecord.totalBases).isEqualTo(10)
        }

        @Test
        fun `extraBaseHits를 올바르게 계산한다`() {
            // given
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.DOUBLE)
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.TRIPLE)

            // then
            assertThat(battingRecord.extraBaseHits).isEqualTo(2)
        }

        @Test
        fun `sacrifices를 올바르게 계산한다`() {
            // given
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SACRIFICE_BUNT)
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SACRIFICE_FLY)

            // then
            assertThat(battingRecord.sacrifices).isEqualTo(2)
        }

        @Test
        fun `totalWalks를 올바르게 계산한다`() {
            // given
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.WALK)
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.WALK)
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.INTENTIONAL_WALK)

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
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.WALK)

            // then
            assertThat(battingRecord.battingAverage).isEqualTo(BigDecimal("0.000"))
        }

        @Test
        fun `3타수 1안타면 타율은 0_333이다`() {
            // given
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.STRIKEOUT)
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.GROUND_OUT)

            // then
            assertThat(battingRecord.battingAverage).isEqualTo(BigDecimal("0.333"))
        }

        @Test
        fun `4타수 2안타면 타율은 0_500이다`() {
            // given
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.DOUBLE)
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.STRIKEOUT)
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.GROUND_OUT)

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
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE) // H=1, AB=1
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.WALK) // BB=1
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.HIT_BY_PITCH) // HBP=1
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.STRIKEOUT) // AB=2
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.GROUND_OUT) // AB=3

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
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE) // 1
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.DOUBLE) // 2
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.STRIKEOUT) // 0
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.GROUND_OUT) // 0

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
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.STRIKEOUT)
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.GROUND_OUT)

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
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.DOUBLE)
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.STRIKEOUT)

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

        @Test
        fun `플라이아웃을 적용하면 타수만 증가한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.FLY_OUT)

            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(1)
            assertThat(battingRecord.hits).isEqualTo(0)
        }

        @Test
        fun `라인아웃을 적용하면 타수만 증가한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.LINE_OUT)

            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(1)
            assertThat(battingRecord.hits).isEqualTo(0)
        }

        @Test
        fun `야수선택을 적용하면 타수만 증가한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.FIELDERS_CHOICE)

            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(1)
            assertThat(battingRecord.hits).isEqualTo(0)
        }

        @Test
        fun `실책을 적용하면 타수만 증가한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.ERROR)

            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(1)
            assertThat(battingRecord.hits).isEqualTo(0)
        }

        @Test
        fun `삼중살을 적용하면 타수와 triplePlays가 증가한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.TRIPLE_PLAY)

            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(1)
            assertThat(battingRecord.triplePlays).isEqualTo(1)
            assertThat(battingRecord.groundedIntoDoublePlays).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("revertPlateAppearanceResult - 타석 결과 롤백")
    inner class RevertPlateAppearanceResultTest {
        @Test
        fun `단타를 롤백하면 타석, 타수, 안타가 감소한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE, rbis = 1)

            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.SINGLE, rbis = 1)

            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.hits).isEqualTo(0)
            assertThat(battingRecord.runsBattedIn).isEqualTo(0)
        }

        @Test
        fun `2루타를 롤백하면 doubles가 감소한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.DOUBLE)

            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.DOUBLE)

            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.hits).isEqualTo(0)
            assertThat(battingRecord.doubles).isEqualTo(0)
        }

        @Test
        fun `3루타를 롤백하면 triples가 감소한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.TRIPLE)

            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.TRIPLE)

            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(battingRecord.hits).isEqualTo(0)
            assertThat(battingRecord.triples).isEqualTo(0)
        }

        @Test
        fun `홈런을 롤백하면 homeRuns와 runs가 감소한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.HOME_RUN, rbis = 3)

            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.HOME_RUN, rbis = 3)

            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(battingRecord.hits).isEqualTo(0)
            assertThat(battingRecord.homeRuns).isEqualTo(0)
            assertThat(battingRecord.runs).isEqualTo(0)
            assertThat(battingRecord.runsBattedIn).isEqualTo(0)
        }

        @Test
        fun `삼진을 롤백하면 타수와 strikeouts가 감소한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.STRIKEOUT)

            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.STRIKEOUT)

            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.strikeouts).isEqualTo(0)
        }

        @Test
        fun `땅볼아웃을 롤백하면 타수가 감소한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.GROUND_OUT)

            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.GROUND_OUT)

            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(battingRecord.atBats).isEqualTo(0)
        }

        @Test
        fun `플라이아웃을 롤백하면 타수가 감소한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.FLY_OUT)

            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.FLY_OUT)

            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(battingRecord.atBats).isEqualTo(0)
        }

        @Test
        fun `라인아웃을 롤백하면 타수가 감소한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.LINE_OUT)

            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.LINE_OUT)

            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(battingRecord.atBats).isEqualTo(0)
        }

        @Test
        fun `야수선택을 롤백하면 타수가 감소한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.FIELDERS_CHOICE)

            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.FIELDERS_CHOICE)

            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(battingRecord.atBats).isEqualTo(0)
        }

        @Test
        fun `실책을 롤백하면 타수가 감소한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.ERROR)

            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.ERROR)

            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(battingRecord.atBats).isEqualTo(0)
        }

        @Test
        fun `볼넷을 롤백하면 walks가 감소한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.WALK)

            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.WALK)

            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(battingRecord.walks).isEqualTo(0)
        }

        @Test
        fun `고의사구를 롤백하면 intentionalWalks가 감소한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.INTENTIONAL_WALK)

            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.INTENTIONAL_WALK)

            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(battingRecord.intentionalWalks).isEqualTo(0)
        }

        @Test
        fun `사구를 롤백하면 hitByPitch가 감소한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.HIT_BY_PITCH)

            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.HIT_BY_PITCH)

            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(battingRecord.hitByPitch).isEqualTo(0)
        }

        @Test
        fun `희생플라이를 롤백하면 sacrificeFlies가 감소한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SACRIFICE_FLY)

            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.SACRIFICE_FLY)

            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(battingRecord.sacrificeFlies).isEqualTo(0)
        }

        @Test
        fun `희생번트를 롤백하면 sacrificeBunts가 감소한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SACRIFICE_BUNT)

            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.SACRIFICE_BUNT)

            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(battingRecord.sacrificeBunts).isEqualTo(0)
        }

        @Test
        fun `병살타를 롤백하면 타수와 groundedIntoDoublePlays가 감소한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.DOUBLE_PLAY)

            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.DOUBLE_PLAY)

            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.groundedIntoDoublePlays).isEqualTo(0)
        }

        @Test
        fun `삼중살을 롤백하면 타수와 triplePlays가 감소한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.TRIPLE_PLAY)

            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.TRIPLE_PLAY)

            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.triplePlays).isEqualTo(0)
            assertThat(battingRecord.groundedIntoDoublePlays).isEqualTo(0)
        }

        @Test
        fun `방해를 롤백하면 타석만 감소한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.INTERFERENCE)

            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.INTERFERENCE)

            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(battingRecord.atBats).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("득점 취소")
    inner class RevertRunTest {
        @Test
        fun `득점을 취소할 수 있다`() {
            battingRecord.recordRun()
            assertThat(battingRecord.runs).isEqualTo(1)

            battingRecord.revertRun()
            assertThat(battingRecord.runs).isEqualTo(0)
        }

        @Test
        fun `취소할 득점이 없으면 예외가 발생한다`() {
            assertThatThrownBy {
                battingRecord.revertRun()
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("취소할 득점이 없습니다")
        }
    }

    @Nested
    @DisplayName("유효성 검증 실패 케이스")
    inner class ValidationFailureTest {
        @Test
        fun `안타가 타수보다 크면 검증에 실패한다`() {
            // 볼넷 2개로 타석만 올리고 (타수 0) 안타를 기록할 수 없으므로
            // recordPlateAppearance로 안타를 기록한 후 revert로 타수만 감소시키는 방식
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)
            // hits=2, atBats=2 → revert로 atBats만 줄이기 위해 직접 조작
            // validate는 내부 상태 검증이므로 강제로 상태를 만들어 검증
            // 정상적인 API로는 불가능하므로 리플렉션 사용
            val atBatsField = BattingRecord::class.java.getDeclaredField("atBats")
            atBatsField.isAccessible = true
            atBatsField.setInt(battingRecord, 1) // hits=2 > atBats=1

            assertThatThrownBy {
                battingRecord.validate()
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("안타 수")
        }

        @Test
        fun `장타 합이 안타보다 크면 검증에 실패한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.DOUBLE)
            // hits=1, doubles=1 → doubles를 강제로 올림
            val doublesField = BattingRecord::class.java.getDeclaredField("doubles")
            doublesField.isAccessible = true
            doublesField.setInt(battingRecord, 2) // doubles=2 > hits=1

            assertThatThrownBy {
                battingRecord.validate()
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("총 안타 수")
        }

        @Test
        fun `타수가 타석보다 크면 검증에 실패한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.WALK)
            // plateAppearances=1, atBats=0 → atBats를 강제로 올림
            val atBatsField = BattingRecord::class.java.getDeclaredField("atBats")
            atBatsField.isAccessible = true
            atBatsField.setInt(battingRecord, 2) // atBats=2 > plateAppearances=1

            assertThatThrownBy {
                battingRecord.validate()
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("타수")
        }
    }

    @Nested
    @DisplayName("applyPlateAppearanceResult - 삼중살 기록")
    inner class RecordTriplePlayTest {
        @Test
        fun `삼중살을 기록하면 triplePlays가 증가한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.TRIPLE_PLAY)

            assertThat(battingRecord.triplePlays).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(1)
            assertThat(battingRecord.groundedIntoDoublePlays).isEqualTo(0)
        }

        @Test
        fun `삼중살 롤백 시 triplePlays가 감소한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.TRIPLE_PLAY)

            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.TRIPLE_PLAY)

            assertThat(battingRecord.triplePlays).isEqualTo(0)
            assertThat(battingRecord.atBats).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("revertPlateAppearanceResult - 음수 값 방지 가드")
    inner class RevertNegativeGuardTest {
        @Test
        fun `타석 기록이 없을 때 롤백하면 예외가 발생한다`() {
            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.SINGLE)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 타석 기록이 없습니다")
        }

        @Test
        fun `단타 기록이 없을 때 단타를 롤백하면 예외가 발생한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.WALK)

            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.SINGLE)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 타수 기록이 없습니다")
        }

        @Test
        fun `2루타 기록이 없을 때 2루타를 롤백하면 예외가 발생한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)

            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.DOUBLE)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 2루타 기록이 없습니다")
        }

        @Test
        fun `3루타 기록이 없을 때 3루타를 롤백하면 예외가 발생한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)

            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.TRIPLE)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 3루타 기록이 없습니다")
        }

        @Test
        fun `홈런 기록이 없을 때 홈런을 롤백하면 예외가 발생한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)

            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.HOME_RUN)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 홈런 기록이 없습니다")
        }

        @Test
        fun `삼진 기록이 없을 때 삼진을 롤백하면 예외가 발생한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.GROUND_OUT)

            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.STRIKEOUT)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 삼진 기록이 없습니다")
        }

        @Test
        fun `볼넷 기록이 없을 때 볼넷을 롤백하면 예외가 발생한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.WALK)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.WALK)

            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.WALK)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 타석 기록이 없습니다")
        }

        @Test
        fun `롤백할 타점이 현재 타점보다 크면 예외가 발생한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE, rbis = 1)

            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.SINGLE, rbis = 2)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 타점")
        }

        @Test
        fun `병살타 기록이 없을 때 병살타를 롤백하면 예외가 발생한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.GROUND_OUT)

            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.DOUBLE_PLAY)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 병살타 기록이 없습니다")
        }

        @Test
        fun `삼중살 기록이 없을 때 삼중살을 롤백하면 예외가 발생한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.GROUND_OUT)

            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.TRIPLE_PLAY)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 삼중살 기록이 없습니다")
        }
    }

    @Nested
    @DisplayName("revertPlateAppearanceResult - 중간 가드 커버리지")
    inner class RevertIntermediateGuardTest {
        private fun setField(
            fieldName: String,
            value: Int,
        ) {
            val field = BattingRecord::class.java.getDeclaredField(fieldName)
            field.isAccessible = true
            field.setInt(battingRecord, value)
        }

        @Test
        fun `롤백 시 음수 타점은 허용되지 않는다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE, rbis = 1)

            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.SINGLE, rbis = -1)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("타점은 0 이상이어야 합니다")
        }

        @Test
        fun `단타 롤백 시 안타가 없으면 예외가 발생한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.GROUND_OUT)

            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.SINGLE)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 안타 기록이 없습니다")
        }

        @Test
        fun `2루타 롤백 시 타수가 없으면 예외가 발생한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.WALK)

            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.DOUBLE)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 타수 기록이 없습니다")
        }

        @Test
        fun `2루타 롤백 시 안타가 없으면 예외가 발생한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.GROUND_OUT)

            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.DOUBLE)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 안타 기록이 없습니다")
        }

        @Test
        fun `3루타 롤백 시 타수가 없으면 예외가 발생한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.WALK)

            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.TRIPLE)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 타수 기록이 없습니다")
        }

        @Test
        fun `3루타 롤백 시 안타가 없으면 예외가 발생한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.GROUND_OUT)

            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.TRIPLE)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 안타 기록이 없습니다")
        }

        @Test
        fun `홈런 롤백 시 타수가 없으면 예외가 발생한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.WALK)

            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.HOME_RUN)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 타수 기록이 없습니다")
        }

        @Test
        fun `홈런 롤백 시 안타가 없으면 예외가 발생한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.GROUND_OUT)

            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.HOME_RUN)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 안타 기록이 없습니다")
        }

        @Test
        fun `홈런 롤백 시 득점이 없으면 예외가 발생한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)
            setField("homeRuns", 1)

            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.HOME_RUN)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 득점 기록이 없습니다")
        }

        @Test
        fun `삼진 롤백 시 타수가 없으면 예외가 발생한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.WALK)

            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.STRIKEOUT)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 타수 기록이 없습니다")
        }

        @Test
        fun `땅볼아웃 롤백 시 타수가 없으면 예외가 발생한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.WALK)

            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.GROUND_OUT)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 타수 기록이 없습니다")
        }

        @Test
        fun `볼넷 롤백 시 볼넷 기록이 없으면 예외가 발생한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)

            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.WALK)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 볼넷 기록이 없습니다")
        }

        @Test
        fun `고의사구 롤백 시 고의사구 기록이 없으면 예외가 발생한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)

            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.INTENTIONAL_WALK)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 고의사구 기록이 없습니다")
        }

        @Test
        fun `사구 롤백 시 사구 기록이 없으면 예외가 발생한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)

            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.HIT_BY_PITCH)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 사구 기록이 없습니다")
        }

        @Test
        fun `희생플라이 롤백 시 희생플라이 기록이 없으면 예외가 발생한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)

            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.SACRIFICE_FLY)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 희생플라이 기록이 없습니다")
        }

        @Test
        fun `희생번트 롤백 시 희생번트 기록이 없으면 예외가 발생한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)

            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.SACRIFICE_BUNT)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 희생번트 기록이 없습니다")
        }

        @Test
        fun `병살타 롤백 시 타수가 없으면 예외가 발생한다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.WALK)

            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.DOUBLE_PLAY)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 타수 기록이 없습니다")
        }
    }

    @Nested
    @DisplayName("validate - 음수 필드 검증")
    inner class ValidateNonNegativeFieldTest {
        private fun setField(
            fieldName: String,
            value: Int,
        ) {
            val field = BattingRecord::class.java.getDeclaredField(fieldName)
            field.isAccessible = true
            field.setInt(battingRecord, value)
        }

        @Test
        fun `타석이 음수이면 검증에 실패한다`() {
            setField("plateAppearances", -1)
            assertThatThrownBy { battingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("타석")
        }

        @Test
        fun `타수가 음수이면 검증에 실패한다`() {
            setField("atBats", -1)
            assertThatThrownBy { battingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("타수")
        }

        @Test
        fun `안타가 음수이면 검증에 실패한다`() {
            setField("hits", -1)
            assertThatThrownBy { battingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("안타")
        }

        @Test
        fun `2루타가 음수이면 검증에 실패한다`() {
            setField("doubles", -1)
            assertThatThrownBy { battingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("2루타")
        }

        @Test
        fun `3루타가 음수이면 검증에 실패한다`() {
            setField("triples", -1)
            assertThatThrownBy { battingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("3루타")
        }

        @Test
        fun `홈런이 음수이면 검증에 실패한다`() {
            setField("homeRuns", -1)
            assertThatThrownBy { battingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("홈런")
        }

        @Test
        fun `득점이 음수이면 검증에 실패한다`() {
            setField("runs", -1)
            assertThatThrownBy { battingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("득점")
        }

        @Test
        fun `타점이 음수이면 검증에 실패한다`() {
            setField("runsBattedIn", -1)
            assertThatThrownBy { battingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("타점")
        }

        @Test
        fun `볼넷이 음수이면 검증에 실패한다`() {
            setField("walks", -1)
            assertThatThrownBy { battingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("볼넷")
        }

        @Test
        fun `고의사구가 음수이면 검증에 실패한다`() {
            setField("intentionalWalks", -1)
            assertThatThrownBy { battingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("고의사구")
        }

        @Test
        fun `사구가 음수이면 검증에 실패한다`() {
            setField("hitByPitch", -1)
            assertThatThrownBy { battingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("사구")
        }

        @Test
        fun `삼진이 음수이면 검증에 실패한다`() {
            setField("strikeouts", -1)
            assertThatThrownBy { battingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("삼진")
        }

        @Test
        fun `희생번트가 음수이면 검증에 실패한다`() {
            setField("sacrificeBunts", -1)
            assertThatThrownBy { battingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("희생번트")
        }

        @Test
        fun `희생플라이가 음수이면 검증에 실패한다`() {
            setField("sacrificeFlies", -1)
            assertThatThrownBy { battingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("희생플라이")
        }

        @Test
        fun `도루가 음수이면 검증에 실패한다`() {
            setField("stolenBases", -1)
            assertThatThrownBy { battingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("도루")
        }

        @Test
        fun `도루실패가 음수이면 검증에 실패한다`() {
            setField("caughtStealing", -1)
            assertThatThrownBy { battingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("도루실패")
        }

        @Test
        fun `병살타가 음수이면 검증에 실패한다`() {
            setField("groundedIntoDoublePlays", -1)
            assertThatThrownBy { battingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("병살타")
        }

        @Test
        fun `삼중살이 음수이면 검증에 실패한다`() {
            setField("triplePlays", -1)
            assertThatThrownBy { battingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("삼중살")
        }
    }

    @Nested
    @DisplayName("apply → revert 단일 왕복 검증")
    inner class ApplyRevertRoundTripTest {

        private fun assertAllFieldsZero(record: BattingRecord) {
            assertThat(record.plateAppearances).isEqualTo(0)
            assertThat(record.atBats).isEqualTo(0)
            assertThat(record.hits).isEqualTo(0)
            assertThat(record.doubles).isEqualTo(0)
            assertThat(record.triples).isEqualTo(0)
            assertThat(record.homeRuns).isEqualTo(0)
            assertThat(record.runs).isEqualTo(0)
            assertThat(record.runsBattedIn).isEqualTo(0)
            assertThat(record.walks).isEqualTo(0)
            assertThat(record.intentionalWalks).isEqualTo(0)
            assertThat(record.hitByPitch).isEqualTo(0)
            assertThat(record.strikeouts).isEqualTo(0)
            assertThat(record.sacrificeBunts).isEqualTo(0)
            assertThat(record.sacrificeFlies).isEqualTo(0)
            assertThat(record.groundedIntoDoublePlays).isEqualTo(0)
            assertThat(record.triplePlays).isEqualTo(0)
        }

        @Test
        fun `단타 apply 후 revert하면 초기 상태로 복원된다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.SINGLE)
            assertAllFieldsZero(battingRecord)
        }

        @Test
        fun `2루타 apply 후 revert하면 초기 상태로 복원된다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.DOUBLE)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.DOUBLE)
            assertAllFieldsZero(battingRecord)
        }

        @Test
        fun `3루타 apply 후 revert하면 초기 상태로 복원된다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.TRIPLE)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.TRIPLE)
            assertAllFieldsZero(battingRecord)
        }

        @Test
        fun `홈런 apply 후 revert하면 초기 상태로 복원된다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.HOME_RUN)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.HOME_RUN)
            assertAllFieldsZero(battingRecord)
        }

        @Test
        fun `삼진 apply 후 revert하면 초기 상태로 복원된다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.STRIKEOUT)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.STRIKEOUT)
            assertAllFieldsZero(battingRecord)
        }

        @Test
        fun `땅볼아웃 apply 후 revert하면 초기 상태로 복원된다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.GROUND_OUT)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.GROUND_OUT)
            assertAllFieldsZero(battingRecord)
        }

        @Test
        fun `플라이아웃 apply 후 revert하면 초기 상태로 복원된다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.FLY_OUT)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.FLY_OUT)
            assertAllFieldsZero(battingRecord)
        }

        @Test
        fun `라인아웃 apply 후 revert하면 초기 상태로 복원된다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.LINE_OUT)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.LINE_OUT)
            assertAllFieldsZero(battingRecord)
        }

        @Test
        fun `야수선택 apply 후 revert하면 초기 상태로 복원된다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.FIELDERS_CHOICE)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.FIELDERS_CHOICE)
            assertAllFieldsZero(battingRecord)
        }

        @Test
        fun `실책 apply 후 revert하면 초기 상태로 복원된다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.ERROR)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.ERROR)
            assertAllFieldsZero(battingRecord)
        }

        @Test
        fun `병살타 apply 후 revert하면 초기 상태로 복원된다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.DOUBLE_PLAY)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.DOUBLE_PLAY)
            assertAllFieldsZero(battingRecord)
        }

        @Test
        fun `삼중살 apply 후 revert하면 초기 상태로 복원된다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.TRIPLE_PLAY)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.TRIPLE_PLAY)
            assertAllFieldsZero(battingRecord)
        }

        @Test
        fun `볼넷 apply 후 revert하면 초기 상태로 복원된다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.WALK)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.WALK)
            assertAllFieldsZero(battingRecord)
        }

        @Test
        fun `고의4구 apply 후 revert하면 초기 상태로 복원된다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.INTENTIONAL_WALK)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.INTENTIONAL_WALK)
            assertAllFieldsZero(battingRecord)
        }

        @Test
        fun `사구 apply 후 revert하면 초기 상태로 복원된다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.HIT_BY_PITCH)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.HIT_BY_PITCH)
            assertAllFieldsZero(battingRecord)
        }

        @Test
        fun `희생번트 apply 후 revert하면 초기 상태로 복원된다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SACRIFICE_BUNT)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.SACRIFICE_BUNT)
            assertAllFieldsZero(battingRecord)
        }

        @Test
        fun `희생플라이 apply 후 revert하면 초기 상태로 복원된다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SACRIFICE_FLY)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.SACRIFICE_FLY)
            assertAllFieldsZero(battingRecord)
        }

        @Test
        fun `방해 apply 후 revert하면 초기 상태로 복원된다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.INTERFERENCE)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.INTERFERENCE)
            assertAllFieldsZero(battingRecord)
        }

        @Test
        fun `타점 포함 단타 apply 후 revert하면 초기 상태로 복원된다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE, rbis = 2)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.SINGLE, rbis = 2)
            assertAllFieldsZero(battingRecord)
        }

        @Test
        fun `타점 포함 홈런 apply 후 revert하면 초기 상태로 복원된다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.HOME_RUN, rbis = 4)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.HOME_RUN, rbis = 4)
            assertAllFieldsZero(battingRecord)
        }

        @Test
        fun `타점 포함 희생플라이 apply 후 revert하면 초기 상태로 복원된다`() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SACRIFICE_FLY, rbis = 1)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.SACRIFICE_FLY, rbis = 1)
            assertAllFieldsZero(battingRecord)
        }
    }

    @Nested
    @DisplayName("apply → revert 복합 왕복 검증")
    inner class MultipleApplyRevertRoundTripTest {

        private fun assertAllFieldsZero(record: BattingRecord) {
            assertThat(record.plateAppearances).isEqualTo(0)
            assertThat(record.atBats).isEqualTo(0)
            assertThat(record.hits).isEqualTo(0)
            assertThat(record.doubles).isEqualTo(0)
            assertThat(record.triples).isEqualTo(0)
            assertThat(record.homeRuns).isEqualTo(0)
            assertThat(record.runs).isEqualTo(0)
            assertThat(record.runsBattedIn).isEqualTo(0)
            assertThat(record.walks).isEqualTo(0)
            assertThat(record.intentionalWalks).isEqualTo(0)
            assertThat(record.hitByPitch).isEqualTo(0)
            assertThat(record.strikeouts).isEqualTo(0)
            assertThat(record.sacrificeBunts).isEqualTo(0)
            assertThat(record.sacrificeFlies).isEqualTo(0)
            assertThat(record.groundedIntoDoublePlays).isEqualTo(0)
            assertThat(record.triplePlays).isEqualTo(0)
        }

        @Test
        fun `단타, 삼진, 볼넷 순서로 apply 후 역순 revert하면 초기 상태로 복원된다`() {
            // apply
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.STRIKEOUT)
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.WALK)

            // revert (역순)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.WALK)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.STRIKEOUT)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.SINGLE)

            assertAllFieldsZero(battingRecord)
        }

        @Test
        fun `홈런(2타점), 2루타(1타점) apply 후 역순 revert하면 초기 상태로 복원된다`() {
            // apply
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.HOME_RUN, rbis = 2)
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.DOUBLE, rbis = 1)

            // revert (역순)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.DOUBLE, rbis = 1)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.HOME_RUN, rbis = 2)

            assertAllFieldsZero(battingRecord)
        }

        @Test
        fun `18개 전체 타석 결과 apply 후 역순 전체 revert하면 초기 상태로 복원된다`() {
            val resultsWithRbis =
                listOf(
                    PlateAppearanceResult.SINGLE to 1,
                    PlateAppearanceResult.DOUBLE to 0,
                    PlateAppearanceResult.TRIPLE to 2,
                    PlateAppearanceResult.HOME_RUN to 3,
                    PlateAppearanceResult.STRIKEOUT to 0,
                    PlateAppearanceResult.GROUND_OUT to 0,
                    PlateAppearanceResult.FLY_OUT to 0,
                    PlateAppearanceResult.LINE_OUT to 0,
                    PlateAppearanceResult.FIELDERS_CHOICE to 1,
                    PlateAppearanceResult.ERROR to 0,
                    PlateAppearanceResult.DOUBLE_PLAY to 0,
                    PlateAppearanceResult.TRIPLE_PLAY to 0,
                    PlateAppearanceResult.WALK to 0,
                    PlateAppearanceResult.INTENTIONAL_WALK to 0,
                    PlateAppearanceResult.HIT_BY_PITCH to 0,
                    PlateAppearanceResult.SACRIFICE_BUNT to 0,
                    PlateAppearanceResult.SACRIFICE_FLY to 1,
                    PlateAppearanceResult.INTERFERENCE to 0,
                )

            // apply (순서대로)
            for ((result, rbis) in resultsWithRbis) {
                battingRecord.applyPlateAppearanceResult(result, rbis)
            }

            // revert (역순)
            for ((result, rbis) in resultsWithRbis.reversed()) {
                battingRecord.revertPlateAppearanceResult(result, rbis)
            }

            assertAllFieldsZero(battingRecord)
        }

        @Test
        fun `동일 결과 여러 번 apply 후 동일 횟수 revert하면 초기 상태로 복원된다`() {
            // 단타 3번
            repeat(3) { battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE, rbis = 1) }

            // revert 3번
            repeat(3) { battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.SINGLE, rbis = 1) }

            assertAllFieldsZero(battingRecord)
        }

        @Test
        fun `실전 시나리오 - 한 이닝 타석 결과 apply 후 전체 revert하면 초기 상태로 복원된다`() {
            // 1번타자: 볼넷
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.WALK)
            // 같은 타자 2번째 타석: 2루타 1타점
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.DOUBLE, rbis = 1)
            // 같은 타자 3번째 타석: 희생플라이 1타점
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SACRIFICE_FLY, rbis = 1)

            // 전체 revert (역순)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.SACRIFICE_FLY, rbis = 1)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.DOUBLE, rbis = 1)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.WALK)

            assertAllFieldsZero(battingRecord)
        }
    }

    @Nested
    @DisplayName("낫아웃 삼진 (STRIKEOUT_DROPPED_THIRD) 처리")
    inner class DroppedThirdStrikeTest {
        @Test
        fun `낫아웃 삼진을 기록하면 타석, 타수, 삼진이 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.STRIKEOUT_DROPPED_THIRD)

            // then
            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(1)
            assertThat(battingRecord.strikeouts).isEqualTo(1)
            assertThat(battingRecord.hits).isEqualTo(0)
        }

        @Test
        fun `낫아웃 삼진 롤백이 정상적으로 동작한다`() {
            // given
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.STRIKEOUT_DROPPED_THIRD)

            // when
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.STRIKEOUT_DROPPED_THIRD)

            // then
            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.strikeouts).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("방해 결과 타입 (BATTER_INTERFERENCE, RUNNER_INTERFERENCE) 처리")
    inner class InterferenceResultTest {
        @Test
        fun `타격방해를 기록하면 타석만 증가하고 타수는 증가하지 않는다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.BATTER_INTERFERENCE)

            // then
            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(0)
        }

        @Test
        fun `주루방해를 기록하면 타석만 증가하고 타수는 증가하지 않는다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.RUNNER_INTERFERENCE)

            // then
            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(0)
        }

        @Test
        fun `타격방해 롤백이 정상적으로 동작한다`() {
            // given
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.BATTER_INTERFERENCE)

            // when
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.BATTER_INTERFERENCE)

            // then
            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(battingRecord.atBats).isEqualTo(0)
        }

        @Test
        fun `주루방해 롤백이 정상적으로 동작한다`() {
            // given
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.RUNNER_INTERFERENCE)

            // when
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.RUNNER_INTERFERENCE)

            // then
            assertThat(battingRecord.plateAppearances).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("PlateAppearanceResult - 새 결과 타입 속성 확인")
    inner class NewResultTypePropertyTest {
        @Test
        fun `STRIKEOUT_DROPPED_THIRD는 삼진이다`() {
            assertThat(PlateAppearanceResult.STRIKEOUT_DROPPED_THIRD.isStrikeout).isTrue()
        }

        @Test
        fun `STRIKEOUT_DROPPED_THIRD는 낫아웃 삼진이다`() {
            assertThat(PlateAppearanceResult.STRIKEOUT_DROPPED_THIRD.isDroppedThirdStrike).isTrue()
        }

        @Test
        fun `STRIKEOUT_DROPPED_THIRD는 출루에 성공한다`() {
            assertThat(PlateAppearanceResult.STRIKEOUT_DROPPED_THIRD.isOnBase).isTrue()
        }

        @Test
        fun `BATTER_INTERFERENCE는 출루에 성공한다`() {
            assertThat(PlateAppearanceResult.BATTER_INTERFERENCE.isOnBase).isTrue()
        }

        @Test
        fun `RUNNER_INTERFERENCE는 출루에 성공하지 못한다`() {
            assertThat(PlateAppearanceResult.RUNNER_INTERFERENCE.isOnBase).isFalse()
        }

        @Test
        fun `BATTER_INTERFERENCE는 타수에 포함되지 않는다`() {
            assertThat(PlateAppearanceResult.BATTER_INTERFERENCE.isAtBat).isFalse()
        }

        @Test
        fun `RUNNER_INTERFERENCE는 타수에 포함되지 않는다`() {
            assertThat(PlateAppearanceResult.RUNNER_INTERFERENCE.isAtBat).isFalse()
        }
    }
}
