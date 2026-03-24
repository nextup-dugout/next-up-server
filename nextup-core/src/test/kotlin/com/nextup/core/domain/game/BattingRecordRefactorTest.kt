package com.nextup.core.domain.game

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("BattingRecord 리팩토링 테스트 (TRIPLE_PLAY 삭제, INTERFERENCE 통합)")
class BattingRecordRefactorTest {
    private lateinit var battingRecord: BattingRecord

    @BeforeEach
    fun setUp() {
        val gamePlayer = mockk<GamePlayer>(relaxed = true)
        every { gamePlayer.id } returns 1L
        battingRecord = BattingRecord.create(gamePlayer)
    }

    @Nested
    @DisplayName("INTERFERENCE 통합 처리")
    inner class InterferenceUnified {
        @Test
        @DisplayName("INTERFERENCE 적용 시 타수에 포함되지 않는다")
        fun interferenceNotAtBat() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.INTERFERENCE)

            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(0)
        }

        @Test
        @DisplayName("INTERFERENCE 롤백이 정상 동작한다")
        fun interferenceRevert() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.INTERFERENCE)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.INTERFERENCE)

            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(battingRecord.atBats).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("DOUBLE_PLAY 처리")
    inner class DoublePlayTest {
        @Test
        @DisplayName("DOUBLE_PLAY 적용 시 타수와 병살타가 증가한다")
        fun doublePlay() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.DOUBLE_PLAY)

            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(1)
            assertThat(battingRecord.groundedIntoDoublePlays).isEqualTo(1)
        }

        @Test
        @DisplayName("DOUBLE_PLAY 롤백이 정상 동작한다")
        fun doublePlayRevert() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.DOUBLE_PLAY)
            battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.DOUBLE_PLAY)

            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.groundedIntoDoublePlays).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("기본 안타/아웃 처리")
    inner class BasicResults {
        @Test
        @DisplayName("SINGLE 적용 시 타수, 안타가 증가한다")
        fun single() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)

            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(1)
            assertThat(battingRecord.hits).isEqualTo(1)
        }

        @Test
        @DisplayName("HOME_RUN 적용 시 득점도 증가한다")
        fun homeRun() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.HOME_RUN, rbis = 2)

            assertThat(battingRecord.homeRuns).isEqualTo(1)
            assertThat(battingRecord.runs).isEqualTo(1)
            assertThat(battingRecord.runsBattedIn).isEqualTo(2)
        }

        @Test
        @DisplayName("WALK은 비타수이다")
        fun walk() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.WALK)

            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.walks).isEqualTo(1)
        }

        @Test
        @DisplayName("INFIELD_FLY는 타수에 포함된다")
        fun infieldFly() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.INFIELD_FLY)

            assertThat(battingRecord.plateAppearances).isEqualTo(1)
            assertThat(battingRecord.atBats).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("correctField - 삭제된 필드 거부")
    inner class CorrectField {
        @Test
        @DisplayName("triplePlays 필드 정정 시 예외 발생")
        fun rejectTriplePlays() {
            org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
                battingRecord.correctField("triplePlays", "0")
            }
        }

        @Test
        @DisplayName("batterInterferences 필드 정정 시 예외 발생")
        fun rejectBatterInterferences() {
            org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
                battingRecord.correctField("batterInterferences", "0")
            }
        }

        @Test
        @DisplayName("runnerInterferences 필드 정정 시 예외 발생")
        fun rejectRunnerInterferences() {
            org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
                battingRecord.correctField("runnerInterferences", "0")
            }
        }

        @Test
        @DisplayName("유효한 필드 정정은 성공한다")
        fun validFieldCorrection() {
            battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)
            val oldValue = battingRecord.correctField("runs", "3")
            assertThat(oldValue).isEqualTo("0")
            assertThat(battingRecord.runs).isEqualTo(3)
        }
    }
}
