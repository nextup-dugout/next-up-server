package com.nextup.core.domain.game

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("BattingRecord.correctField")
class BattingRecordCorrectFieldTest {
    private lateinit var gamePlayer: GamePlayer
    private lateinit var battingRecord: BattingRecord

    @BeforeEach
    fun setup() {
        gamePlayer = mockk<GamePlayer>(relaxed = true)
        battingRecord = BattingRecord.create(gamePlayer)
        // Set some initial values via applyPlateAppearanceResult
        battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE)
        battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.DOUBLE)
        battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.HOME_RUN)
        battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.STRIKEOUT)
        battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.WALK)
    }

    @Nested
    @DisplayName("정상 케이스")
    inner class SuccessCases {
        @Test
        fun `hits 필드를 정정하면 이전 값이 반환되고 새 값이 적용된다`() {
            // given
            val currentHits = battingRecord.hits
            val newValue = (currentHits - 1).toString()

            // when
            val oldValue = battingRecord.correctField("hits", newValue)

            // then
            assertThat(oldValue).isEqualTo(currentHits.toString())
            assertThat(battingRecord.hits).isEqualTo(currentHits - 1)
        }

        @Test
        fun `walks 필드를 정정하면 이전 값이 반환된다`() {
            // given
            val currentWalks = battingRecord.walks

            // when
            val oldValue = battingRecord.correctField("walks", "0")

            // then
            assertThat(oldValue).isEqualTo(currentWalks.toString())
            assertThat(battingRecord.walks).isEqualTo(0)
        }

        @Test
        fun `strikeouts 필드를 정정할 수 있다`() {
            // when
            val oldValue = battingRecord.correctField("strikeouts", "0")

            // then
            assertThat(oldValue).isEqualTo("1")
            assertThat(battingRecord.strikeouts).isEqualTo(0)
        }

        @Test
        fun `plateAppearances, atBats 등 모든 지원 필드를 정정할 수 있다`() {
            val supportedFields =
                listOf(
                    "plateAppearances",
                    "atBats",
                    "hits",
                    "doubles",
                    "triples",
                    "homeRuns",
                    "runs",
                    "runsBattedIn",
                    "walks",
                    "intentionalWalks",
                    "hitByPitch",
                    "strikeouts",
                    "sacrificeBunts",
                    "sacrificeFlies",
                    "stolenBases",
                    "caughtStealing",
                    "groundedIntoDoublePlays",
                )

            // All fields can be corrected to 0 (the validate call may fail for some combinations
            // so we just test that the field switch works without throwing for valid values)
            // We use a fresh record for each field to avoid validate failures
            supportedFields.forEach { fieldName ->
                val freshRecord = BattingRecord.create(gamePlayer)
                // Should not throw - empty record has all zeros, setting to 0 is valid
                freshRecord.correctField(fieldName, "0")
            }
        }
    }

    @Nested
    @DisplayName("예외 케이스")
    inner class ErrorCases {
        @Test
        fun `유효하지 않은 필드명이면 IllegalArgumentException이 발생한다`() {
            assertThatThrownBy {
                battingRecord.correctField("invalidField", "1")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("유효하지 않은 타격 기록 필드입니다")
        }

        @Test
        fun `정수가 아닌 값이면 IllegalArgumentException이 발생한다`() {
            assertThatThrownBy {
                battingRecord.correctField("hits", "notAnInt")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("정정 값은 정수여야 합니다")
        }

        @Test
        fun `음수 값이면 IllegalArgumentException이 발생한다`() {
            assertThatThrownBy {
                battingRecord.correctField("hits", "-1")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("정정 값은 0 이상이어야 합니다")
        }
    }
}

@DisplayName("PitchingRecord.correctField")
class PitchingRecordCorrectFieldTest {
    private lateinit var gamePlayer: GamePlayer
    private lateinit var pitchingRecord: PitchingRecord

    @BeforeEach
    fun setup() {
        gamePlayer = mockk<GamePlayer>(relaxed = true)
        pitchingRecord = PitchingRecord.create(gamePlayer, isStartingPitcher = false)
        // Set some initial values
        pitchingRecord.recordOut(isStrikeout = true)
        pitchingRecord.recordOut(isStrikeout = false)
        pitchingRecord.recordHit(isHomeRun = false, runsScored = 0, earnedRuns = 0)
        pitchingRecord.recordWalk()
    }

    @Nested
    @DisplayName("정상 케이스")
    inner class SuccessCases {
        @Test
        fun `strikeouts 필드를 정정하면 이전 값이 반환되고 새 값이 적용된다`() {
            // given
            val currentStrikeouts = pitchingRecord.strikeouts

            // when
            val oldValue = pitchingRecord.correctField("strikeouts", "0")

            // then
            assertThat(oldValue).isEqualTo(currentStrikeouts.toString())
            assertThat(pitchingRecord.strikeouts).isEqualTo(0)
        }

        @Test
        fun `walksAllowed 필드를 정정할 수 있다`() {
            // given
            val currentWalks = pitchingRecord.walksAllowed

            // when
            val oldValue = pitchingRecord.correctField("walksAllowed", "0")

            // then
            assertThat(oldValue).isEqualTo(currentWalks.toString())
            assertThat(pitchingRecord.walksAllowed).isEqualTo(0)
        }

        @Test
        fun `pitchesThrown 필드를 정정할 수 있다`() {
            // when
            pitchingRecord.correctField("pitchesThrown", "80")

            // then
            assertThat(pitchingRecord.pitchesThrown).isEqualTo(80)
        }
    }

    @Nested
    @DisplayName("예외 케이스")
    inner class ErrorCases {
        @Test
        fun `유효하지 않은 필드명이면 IllegalArgumentException이 발생한다`() {
            assertThatThrownBy {
                pitchingRecord.correctField("invalidField", "1")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("유효하지 않은 투수 기록 필드입니다")
        }

        @Test
        fun `정수가 아닌 값이면 IllegalArgumentException이 발생한다`() {
            assertThatThrownBy {
                pitchingRecord.correctField("strikeouts", "abc")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("정정 값은 정수여야 합니다")
        }

        @Test
        fun `음수 값이면 IllegalArgumentException이 발생한다`() {
            assertThatThrownBy {
                pitchingRecord.correctField("strikeouts", "-5")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("정정 값은 0 이상이어야 합니다")
        }
    }
}

@DisplayName("RecordCorrection 도메인")
class RecordCorrectionDomainTest {
    @Test
    fun `RecordCorrection을 생성할 수 있다`() {
        // when
        val correction =
            RecordCorrection.create(
                gameId = 1L,
                adminUserId = 100L,
                correctionType = CorrectionType.BATTING,
                targetRecordId = 10L,
                fieldName = "hits",
                oldValue = "2",
                newValue = "3",
                reason = "기록원 오류 정정",
            )

        // then
        assertThat(correction.gameId).isEqualTo(1L)
        assertThat(correction.adminUserId).isEqualTo(100L)
        assertThat(correction.correctionType).isEqualTo(CorrectionType.BATTING)
        assertThat(correction.targetRecordId).isEqualTo(10L)
        assertThat(correction.fieldName).isEqualTo("hits")
        assertThat(correction.oldValue).isEqualTo("2")
        assertThat(correction.newValue).isEqualTo("3")
        assertThat(correction.reason).isEqualTo("기록원 오류 정정")
    }

    @Test
    fun `gameId가 0 이하이면 예외가 발생한다`() {
        assertThatThrownBy {
            RecordCorrection.create(
                gameId = 0L,
                adminUserId = 100L,
                correctionType = CorrectionType.BATTING,
                targetRecordId = 10L,
                fieldName = "hits",
                oldValue = "2",
                newValue = "3",
                reason = "정정 사유",
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("경기 ID는 필수입니다")
    }

    @Test
    fun `reason이 빈 문자열이면 예외가 발생한다`() {
        assertThatThrownBy {
            RecordCorrection.create(
                gameId = 1L,
                adminUserId = 100L,
                correctionType = CorrectionType.BATTING,
                targetRecordId = 10L,
                fieldName = "hits",
                oldValue = "2",
                newValue = "3",
                reason = "",
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("정정 사유는 필수입니다")
    }
}
