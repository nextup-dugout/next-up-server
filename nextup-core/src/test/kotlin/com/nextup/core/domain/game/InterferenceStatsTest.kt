package com.nextup.core.domain.game

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("L-2: 방해 유형 세분화 통계")
class InterferenceStatsTest {
    private lateinit var gamePlayer: GamePlayer
    private lateinit var battingRecord: BattingRecord

    @BeforeEach
    fun setup() {
        gamePlayer = mockk<GamePlayer>(relaxed = true)
        battingRecord = BattingRecord.create(gamePlayer)
    }

    @Nested
    @DisplayName("BATTER_INTERFERENCE 기록")
    inner class BatterInterferenceTest {
        @Test
        fun `타격방해 결과를 기록하면 batterInterferences가 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.BATTER_INTERFERENCE)

            // then
            assertThat(battingRecord.batterInterferences).isEqualTo(1)
            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(0) // 비타수
        }

        @Test
        fun `타격방해를 여러 번 기록하면 누적된다`() {
            // when
            repeat(3) {
                battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.BATTER_INTERFERENCE)
            }

            // then
            assertThat(battingRecord.batterInterferences).isEqualTo(3)
            assertThat(battingRecord.plateAppearances).isEqualTo(3)
        }

        @Test
        fun `타격방해 롤백 시 batterInterferences가 감소한다`() {
            // given
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.BATTER_INTERFERENCE)

            // when
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.BATTER_INTERFERENCE)

            // then
            assertThat(battingRecord.batterInterferences).isEqualTo(0)
            assertThat(battingRecord.plateAppearances).isEqualTo(0)
        }

        @Test
        fun `타격방해 기록이 없는 상태에서 롤백하면 예외가 발생한다`() {
            // given
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE) // PA=1이어야 롤백 가능

            // when & then
            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.BATTER_INTERFERENCE)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 타격방해 기록이 없습니다")
        }
    }

    @Nested
    @DisplayName("RUNNER_INTERFERENCE 기록")
    inner class RunnerInterferenceTest {
        @Test
        fun `주루방해 결과를 기록하면 runnerInterferences가 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.RUNNER_INTERFERENCE)

            // then
            assertThat(battingRecord.runnerInterferences).isEqualTo(1)
            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(0) // 비타수
        }

        @Test
        fun `주루방해 롤백 시 runnerInterferences가 감소한다`() {
            // given
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.RUNNER_INTERFERENCE)

            // when
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.RUNNER_INTERFERENCE)

            // then
            assertThat(battingRecord.runnerInterferences).isEqualTo(0)
        }

        @Test
        fun `주루방해 기록이 없는 상태에서 롤백하면 예외가 발생한다`() {
            // given
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)

            // when & then
            assertThatThrownBy {
                battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.RUNNER_INTERFERENCE)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 주루방해 기록이 없습니다")
        }
    }

    @Nested
    @DisplayName("기존 INTERFERENCE 하위호환")
    inner class LegacyInterferenceTest {
        @Test
        fun `기존 INTERFERENCE는 별도 카운트 없이 타석만 증가한다`() {
            // when
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.INTERFERENCE)

            // then
            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.batterInterferences).isEqualTo(0)
            assertThat(battingRecord.runnerInterferences).isEqualTo(0)
        }

        @Test
        fun `기존 INTERFERENCE 롤백은 타석만 감소한다`() {
            // given
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.INTERFERENCE)

            // when
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.INTERFERENCE)

            // then
            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(battingRecord.batterInterferences).isEqualTo(0)
            assertThat(battingRecord.runnerInterferences).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("validate 검증")
    inner class ValidateTest {
        @Test
        fun `방해 기록이 포함된 상태에서 validate는 성공한다`() {
            // given
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.BATTER_INTERFERENCE)
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.RUNNER_INTERFERENCE)

            // when & then (no exception)
            battingRecord.validate()
        }
    }

    @Nested
    @DisplayName("correctField 기록 정정")
    inner class CorrectFieldTest {
        @Test
        fun `batterInterferences 필드를 정정할 수 있다`() {
            val oldValue = battingRecord.correctField("batterInterferences", "5")
            assertThat(oldValue).isEqualTo("0")
            assertThat(battingRecord.batterInterferences).isEqualTo(5)
        }

        @Test
        fun `runnerInterferences 필드를 정정할 수 있다`() {
            val oldValue = battingRecord.correctField("runnerInterferences", "3")
            assertThat(oldValue).isEqualTo("0")
            assertThat(battingRecord.runnerInterferences).isEqualTo(3)
        }
    }
}
