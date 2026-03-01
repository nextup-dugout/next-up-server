package com.nextup.core.domain.game

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("FieldingRecord")
class FieldingRecordTest {
    private lateinit var gamePlayer: GamePlayer
    private lateinit var fieldingRecord: FieldingRecord

    @BeforeEach
    fun setup() {
        gamePlayer = mockk<GamePlayer>(relaxed = true)
        fieldingRecord = FieldingRecord.create(gamePlayer)
    }

    @Nested
    @DisplayName("수비 기록 초기화")
    inner class InitialStateTest {
        @Test
        fun `생성 직후 모든 수비 기록은 0이다`() {
            assertThat(fieldingRecord.putOuts).isEqualTo(0)
            assertThat(fieldingRecord.assists).isEqualTo(0)
            assertThat(fieldingRecord.errors).isEqualTo(0)
            assertThat(fieldingRecord.doublePlays).isEqualTo(0)
            assertThat(fieldingRecord.passedBalls).isEqualTo(0)
        }

        @Test
        fun `수비 기회가 0이면 수비율은 null이다`() {
            assertThat(fieldingRecord.totalChances).isEqualTo(0)
            assertThat(fieldingRecord.fieldingPercentage).isNull()
        }
    }

    @Nested
    @DisplayName("자살(PO) 기록")
    inner class RecordPutOutTest {
        @Test
        fun `자살을 기록하면 putOuts가 1 증가한다`() {
            // when
            fieldingRecord.recordPutOut()

            // then
            assertThat(fieldingRecord.putOuts).isEqualTo(1)
        }

        @Test
        fun `자살을 여러 번 기록하면 누적된다`() {
            // when
            repeat(3) { fieldingRecord.recordPutOut() }

            // then
            assertThat(fieldingRecord.putOuts).isEqualTo(3)
        }
    }

    @Nested
    @DisplayName("보살(A) 기록")
    inner class RecordAssistTest {
        @Test
        fun `보살을 기록하면 assists가 1 증가한다`() {
            // when
            fieldingRecord.recordAssist()

            // then
            assertThat(fieldingRecord.assists).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("실책(E) 기록")
    inner class RecordErrorTest {
        @Test
        fun `실책을 기록하면 errors가 1 증가한다`() {
            // when
            fieldingRecord.recordError()

            // then
            assertThat(fieldingRecord.errors).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("병살 관여(DP) 기록")
    inner class RecordDoublePlayTest {
        @Test
        fun `병살 관여를 기록하면 doublePlays가 1 증가한다`() {
            // when
            fieldingRecord.recordDoublePlay()

            // then
            assertThat(fieldingRecord.doublePlays).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("포일(PB) 기록")
    inner class RecordPassedBallTest {
        @Test
        fun `포일을 기록하면 passedBalls가 1 증가한다`() {
            // when
            fieldingRecord.recordPassedBall()

            // then
            assertThat(fieldingRecord.passedBalls).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("수비 기회(TC) 계산")
    inner class TotalChancesTest {
        @Test
        fun `수비 기회는 자살 + 보살 + 실책이다`() {
            // given
            fieldingRecord.recordPutOut()
            fieldingRecord.recordPutOut()
            fieldingRecord.recordAssist()
            fieldingRecord.recordError()

            // then
            assertThat(fieldingRecord.totalChances).isEqualTo(4)
        }
    }

    @Nested
    @DisplayName("수비율(FPCT) 계산")
    inner class FieldingPercentageTest {
        @Test
        fun `수비 기회가 있을 때 수비율 = (자살 + 보살) 나누기 수비 기회`() {
            // given: PO=2, A=1, E=1 -> TC=4, FPCT=3/4=0.750
            fieldingRecord.recordPutOut()
            fieldingRecord.recordPutOut()
            fieldingRecord.recordAssist()
            fieldingRecord.recordError()

            // then
            assertThat(fieldingRecord.fieldingPercentage)
                .isEqualByComparingTo(BigDecimal("0.750"))
        }

        @Test
        fun `실책이 없으면 수비율은 1이다`() {
            // given
            fieldingRecord.recordPutOut()
            fieldingRecord.recordAssist()

            // then
            assertThat(fieldingRecord.fieldingPercentage)
                .isEqualByComparingTo(BigDecimal("1.000"))
        }

        @Test
        fun `수비 기회가 0이면 수비율은 null이다`() {
            assertThat(fieldingRecord.fieldingPercentage).isNull()
        }
    }

    @Nested
    @DisplayName("Undo 기능")
    inner class RevertTest {
        @Test
        fun `자살을 취소하면 putOuts가 1 감소한다`() {
            // given
            fieldingRecord.recordPutOut()

            // when
            fieldingRecord.revertPutOut()

            // then
            assertThat(fieldingRecord.putOuts).isEqualTo(0)
        }

        @Test
        fun `자살 기록이 없는 상태에서 취소하면 예외가 발생한다`() {
            assertThatThrownBy { fieldingRecord.revertPutOut() }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `보살을 취소하면 assists가 1 감소한다`() {
            // given
            fieldingRecord.recordAssist()

            // when
            fieldingRecord.revertAssist()

            // then
            assertThat(fieldingRecord.assists).isEqualTo(0)
        }

        @Test
        fun `실책을 취소하면 errors가 1 감소한다`() {
            // given
            fieldingRecord.recordError()

            // when
            fieldingRecord.revertError()

            // then
            assertThat(fieldingRecord.errors).isEqualTo(0)
        }

        @Test
        fun `병살 관여를 취소하면 doublePlays가 1 감소한다`() {
            // given
            fieldingRecord.recordDoublePlay()

            // when
            fieldingRecord.revertDoublePlay()

            // then
            assertThat(fieldingRecord.doublePlays).isEqualTo(0)
        }

        @Test
        fun `포일을 취소하면 passedBalls가 1 감소한다`() {
            // given
            fieldingRecord.recordPassedBall()

            // when
            fieldingRecord.revertPassedBall()

            // then
            assertThat(fieldingRecord.passedBalls).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("유효성 검증")
    inner class ValidateTest {
        @Test
        fun `정상 상태에서 validate는 예외를 발생시키지 않는다`() {
            // given
            fieldingRecord.recordPutOut()
            fieldingRecord.recordAssist()
            fieldingRecord.recordError()

            // when & then (no exception)
            fieldingRecord.validate()
        }
    }
}
