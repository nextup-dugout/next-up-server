package com.nextup.core.domain.game

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("FieldingRecord.correctField")
class FieldingRecordCorrectFieldTest {
    private lateinit var gamePlayer: GamePlayer
    private lateinit var fieldingRecord: FieldingRecord

    @BeforeEach
    fun setup() {
        gamePlayer = mockk<GamePlayer>(relaxed = true)
        fieldingRecord = FieldingRecord.create(gamePlayer)
        fieldingRecord.recordPutOut()
        fieldingRecord.recordPutOut()
        fieldingRecord.recordAssist()
        fieldingRecord.recordError()
        fieldingRecord.recordDoublePlay()
        fieldingRecord.recordPassedBall()
    }

    @Nested
    @DisplayName("정상 케이스")
    inner class SuccessCases {
        @Test
        fun `putOuts 필드를 정정하면 이전 값이 반환되고 새 값이 적용된다`() {
            // given
            val currentPutOuts = fieldingRecord.putOuts

            // when
            val oldValue = fieldingRecord.correctField("putOuts", "5")

            // then
            assertThat(oldValue).isEqualTo(currentPutOuts.toString())
            assertThat(fieldingRecord.putOuts).isEqualTo(5)
        }

        @Test
        fun `assists 필드를 정정하면 이전 값이 반환되고 새 값이 적용된다`() {
            // given
            val currentAssists = fieldingRecord.assists

            // when
            val oldValue = fieldingRecord.correctField("assists", "3")

            // then
            assertThat(oldValue).isEqualTo(currentAssists.toString())
            assertThat(fieldingRecord.assists).isEqualTo(3)
        }

        @Test
        fun `errors 필드를 정정하면 이전 값이 반환되고 새 값이 적용된다`() {
            // given
            val currentErrors = fieldingRecord.errors

            // when
            val oldValue = fieldingRecord.correctField("errors", "0")

            // then
            assertThat(oldValue).isEqualTo(currentErrors.toString())
            assertThat(fieldingRecord.errors).isEqualTo(0)
        }

        @Test
        fun `doublePlays 필드를 정정하면 이전 값이 반환되고 새 값이 적용된다`() {
            // given
            val currentDoublePlays = fieldingRecord.doublePlays

            // when
            val oldValue = fieldingRecord.correctField("doublePlays", "2")

            // then
            assertThat(oldValue).isEqualTo(currentDoublePlays.toString())
            assertThat(fieldingRecord.doublePlays).isEqualTo(2)
        }

        @Test
        fun `passedBalls 필드를 정정하면 이전 값이 반환되고 새 값이 적용된다`() {
            // given
            val currentPassedBalls = fieldingRecord.passedBalls

            // when
            val oldValue = fieldingRecord.correctField("passedBalls", "0")

            // then
            assertThat(oldValue).isEqualTo(currentPassedBalls.toString())
            assertThat(fieldingRecord.passedBalls).isEqualTo(0)
        }

        @Test
        fun `모든 지원 필드를 0으로 정정할 수 있다`() {
            val supportedFields =
                listOf("putOuts", "assists", "errors", "doublePlays", "passedBalls")

            supportedFields.forEach { fieldName ->
                val freshRecord = FieldingRecord.create(gamePlayer)
                freshRecord.correctField(fieldName, "0")
                assertThat(
                    when (fieldName) {
                        "putOuts" -> freshRecord.putOuts
                        "assists" -> freshRecord.assists
                        "errors" -> freshRecord.errors
                        "doublePlays" -> freshRecord.doublePlays
                        "passedBalls" -> freshRecord.passedBalls
                        else -> throw IllegalArgumentException("unexpected field: $fieldName")
                    },
                ).isEqualTo(0)
            }
        }

        @Test
        fun `정정 후 totalChances가 새 값 기준으로 재계산된다`() {
            // when: putOuts=3, assists=2, errors=1 로 정정
            fieldingRecord.correctField("putOuts", "3")
            fieldingRecord.correctField("assists", "2")
            fieldingRecord.correctField("errors", "1")

            // then: TC = 3 + 2 + 1 = 6
            assertThat(fieldingRecord.totalChances).isEqualTo(6)
        }
    }

    @Nested
    @DisplayName("예외 케이스")
    inner class ErrorCases {
        @Test
        fun `유효하지 않은 필드명이면 IllegalArgumentException이 발생한다`() {
            assertThatThrownBy {
                fieldingRecord.correctField("invalidField", "1")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("유효하지 않은 수비 기록 필드입니다")
        }

        @Test
        fun `정수가 아닌 값이면 IllegalArgumentException이 발생한다`() {
            assertThatThrownBy {
                fieldingRecord.correctField("putOuts", "notAnInt")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("정정 값은 정수여야 합니다")
        }

        @Test
        fun `음수 값이면 IllegalArgumentException이 발생한다`() {
            assertThatThrownBy {
                fieldingRecord.correctField("putOuts", "-1")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("정정 값은 0 이상이어야 합니다")
        }

        @Test
        fun `소수점 값이면 IllegalArgumentException이 발생한다`() {
            assertThatThrownBy {
                fieldingRecord.correctField("assists", "1.5")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("정정 값은 정수여야 합니다")
        }

        @Test
        fun `빈 문자열이면 IllegalArgumentException이 발생한다`() {
            assertThatThrownBy {
                fieldingRecord.correctField("errors", "")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("정정 값은 정수여야 합니다")
        }
    }
}
