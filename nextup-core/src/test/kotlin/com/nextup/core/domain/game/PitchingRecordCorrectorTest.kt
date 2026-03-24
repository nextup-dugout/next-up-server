package com.nextup.core.domain.game

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PitchingRecordCorrector")
class PitchingRecordCorrectorTest {
    private lateinit var gamePlayer: GamePlayer
    private lateinit var record: PitchingRecord

    @BeforeEach
    fun setup() {
        gamePlayer = mockk<GamePlayer>(relaxed = true)
        record = PitchingRecord.create(gamePlayer)
    }

    @Nested
    @DisplayName("correctField - 기본 정정")
    inner class CorrectFieldBasicTest {
        @Test
        fun `inningsPitchedOuts 정정 시 이전 값을 반환한다`() {
            record.recordOut()
            record.recordOut()
            record.recordOut()

            val oldValue = PitchingRecordCorrector.correctField(record, "inningsPitchedOuts", "6")

            assertThat(oldValue).isEqualTo("3")
            assertThat(record.inningsPitchedOuts).isEqualTo(6)
        }

        @Test
        fun `earnedRuns 정정`() {
            record.recordHit(runsScored = 2, earnedRuns = 2)

            val oldValue = PitchingRecordCorrector.correctField(record, "earnedRuns", "1")

            assertThat(oldValue).isEqualTo("2")
            assertThat(record.earnedRuns).isEqualTo(1)
        }

        @Test
        fun `runsAllowed 정정`() {
            record.recordHit(runsScored = 3, earnedRuns = 2)

            val oldValue = PitchingRecordCorrector.correctField(record, "runsAllowed", "4")

            assertThat(oldValue).isEqualTo("3")
            assertThat(record.runsAllowed).isEqualTo(4)
        }

        @Test
        fun `hitsAllowed 정정`() {
            record.recordHit()
            record.recordHit()

            val oldValue = PitchingRecordCorrector.correctField(record, "hitsAllowed", "3")

            assertThat(oldValue).isEqualTo("2")
            assertThat(record.hitsAllowed).isEqualTo(3)
        }

        @Test
        fun `walksAllowed 정정`() {
            record.recordWalk()

            val oldValue = PitchingRecordCorrector.correctField(record, "walksAllowed", "2")

            assertThat(oldValue).isEqualTo("1")
            assertThat(record.walksAllowed).isEqualTo(2)
        }

        @Test
        fun `strikeouts 정정`() {
            record.recordOut(isStrikeout = true)

            val oldValue = PitchingRecordCorrector.correctField(record, "strikeouts", "3")

            assertThat(oldValue).isEqualTo("1")
            assertThat(record.strikeouts).isEqualTo(3)
        }

        @Test
        fun `homeRunsAllowed 정정`() {
            record.recordHit(isHomeRun = true)

            val oldValue = PitchingRecordCorrector.correctField(record, "homeRunsAllowed", "0")

            assertThat(oldValue).isEqualTo("1")
            assertThat(record.homeRunsAllowed).isEqualTo(0)
        }

        @Test
        fun `hitBatsmen 정정`() {
            record.recordHitByPitch()

            val oldValue = PitchingRecordCorrector.correctField(record, "hitBatsmen", "2")

            assertThat(oldValue).isEqualTo("1")
            assertThat(record.hitBatsmen).isEqualTo(2)
        }

        @Test
        fun `wildPitches 정정`() {
            record.recordWildPitch()

            val oldValue = PitchingRecordCorrector.correctField(record, "wildPitches", "0")

            assertThat(oldValue).isEqualTo("1")
            assertThat(record.wildPitches).isEqualTo(0)
        }

        @Test
        fun `balks 정정`() {
            record.recordBalk()

            val oldValue = PitchingRecordCorrector.correctField(record, "balks", "0")

            assertThat(oldValue).isEqualTo("1")
            assertThat(record.balks).isEqualTo(0)
        }

        @Test
        fun `battersFaced 정정`() {
            record.recordOut()
            record.recordOut()

            val oldValue = PitchingRecordCorrector.correctField(record, "battersFaced", "5")

            assertThat(oldValue).isEqualTo("2")
            assertThat(record.battersFaced).isEqualTo(5)
        }

        @Test
        fun `pitchesThrown 정정 - 기존 null이면 0 반환`() {
            val oldValue = PitchingRecordCorrector.correctField(record, "pitchesThrown", "80")

            assertThat(oldValue).isEqualTo("0")
            assertThat(record.pitchesThrown).isEqualTo(80)
        }

        @Test
        fun `strikesThrown 정정 - 기존 null이면 0 반환`() {
            record.recordPitchCount(totalPitches = 100, strikes = 60)

            val oldValue = PitchingRecordCorrector.correctField(record, "strikesThrown", "55")

            assertThat(oldValue).isEqualTo("60")
            assertThat(record.strikesThrown).isEqualTo(55)
        }

        @Test
        fun `stolenBasesAllowed 정정`() {
            record.recordStolenBaseAllowed()

            val oldValue = PitchingRecordCorrector.correctField(record, "stolenBasesAllowed", "3")

            assertThat(oldValue).isEqualTo("1")
            assertThat(record.stolenBasesAllowed).isEqualTo(3)
        }

        @Test
        fun `runnersCaughtStealing 정정`() {
            record.recordCaughtStealing()

            val oldValue = PitchingRecordCorrector.correctField(record, "runnersCaughtStealing", "2")

            assertThat(oldValue).isEqualTo("1")
            assertThat(record.runnersCaughtStealing).isEqualTo(2)
        }

        @Test
        fun `pickoffs 정정`() {
            record.recordPickoff()

            val oldValue = PitchingRecordCorrector.correctField(record, "pickoffs", "0")

            assertThat(oldValue).isEqualTo("1")
            assertThat(record.pickoffs).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("correctField - 예외 처리")
    inner class CorrectFieldExceptionTest {
        @Test
        fun `유효하지 않은 필드명이면 예외가 발생한다`() {
            assertThatThrownBy {
                PitchingRecordCorrector.correctField(record, "invalidField", "5")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("유효하지 않은 투수 기록 필드")
        }

        @Test
        fun `정수가 아닌 값이면 예외가 발생한다`() {
            assertThatThrownBy {
                PitchingRecordCorrector.correctField(record, "earnedRuns", "abc")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("정정 값은 정수여야 합니다")
        }

        @Test
        fun `음수 값이면 예외가 발생한다`() {
            assertThatThrownBy {
                PitchingRecordCorrector.correctField(record, "strikeouts", "-1")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("정정 값은 0 이상이어야 합니다")
        }

        @Test
        fun `정정 후 validate 위반하면 예외가 발생한다 - 자책점이 실점보다 큰 경우`() {
            record.recordHit(runsScored = 2, earnedRuns = 1)

            assertThatThrownBy {
                PitchingRecordCorrector.correctField(record, "earnedRuns", "5")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("자책점")
        }
    }

    @Nested
    @DisplayName("PitchingCorrectionField.fromFieldName")
    inner class FromFieldNameTest {
        @Test
        fun `모든 유효한 필드명이 올바른 sealed class 인스턴스를 반환한다`() {
            val fieldNames =
                listOf(
                    "inningsPitchedOuts",
                    "earnedRuns",
                    "runsAllowed",
                    "hitsAllowed",
                    "walksAllowed",
                    "strikeouts",
                    "homeRunsAllowed",
                    "hitBatsmen",
                    "wildPitches",
                    "balks",
                    "battersFaced",
                    "pitchesThrown",
                    "strikesThrown",
                    "stolenBasesAllowed",
                    "runnersCaughtStealing",
                    "pickoffs",
                )

            fieldNames.forEach { fieldName ->
                val field = PitchingCorrectionField.fromFieldName(fieldName)
                assertThat(field.fieldName).isEqualTo(fieldName)
            }
        }

        @Test
        fun `유효하지 않은 필드명이면 예외가 발생한다`() {
            assertThatThrownBy {
                PitchingCorrectionField.fromFieldName("nonexistentField")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("유효하지 않은 투수 기록 필드")
        }
    }

    @Nested
    @DisplayName("PitchingRecord.correctField 위임 검증")
    inner class DelegationTest {
        @Test
        fun `PitchingRecord의 correctField는 PitchingRecordCorrector에 위임한다`() {
            record.recordOut()
            record.recordOut()
            record.recordOut()

            // PitchingRecord.correctField() 호출
            val oldValue = record.correctField("inningsPitchedOuts", "9")

            assertThat(oldValue).isEqualTo("3")
            assertThat(record.inningsPitchedOuts).isEqualTo(9)
        }
    }
}
