package com.nextup.core.domain.stats

import com.nextup.core.domain.player.Player
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("CareerFieldingStats.applyFieldCorrection")
class CareerFieldingStatsApplyFieldCorrectionTest {
    private lateinit var player: Player
    private lateinit var stats: CareerFieldingStats

    @BeforeEach
    fun setup() {
        player = mockk(relaxed = true)
        stats = CareerFieldingStats.create(player)
        // 초기 값을 reflection으로 설정
        setField(stats, "putOuts", 10)
        setField(stats, "assists", 8)
        setField(stats, "errors", 3)
        setField(stats, "doublePlays", 4)
        setField(stats, "passedBalls", 2)
    }

    @Nested
    @DisplayName("putOuts 필드 정정")
    inner class PutOutsCorrectionTest {
        @Test
        fun `양수 델타로 정정하면 putOuts가 증가한다`() {
            stats.applyFieldCorrection("putOuts", 5)
            assertThat(stats.putOuts).isEqualTo(15)
        }

        @Test
        fun `음수 델타로 정정하면 putOuts가 감소한다`() {
            stats.applyFieldCorrection("putOuts", -3)
            assertThat(stats.putOuts).isEqualTo(7)
        }

        @Test
        fun `delta가 0이면 putOuts가 변하지 않는다`() {
            stats.applyFieldCorrection("putOuts", 0)
            assertThat(stats.putOuts).isEqualTo(10)
        }

        @Test
        fun `음수 결과가 예상될 때 maxOf 0으로 보호된다`() {
            stats.applyFieldCorrection("putOuts", -100)
            assertThat(stats.putOuts).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("assists 필드 정정")
    inner class AssistsCorrectionTest {
        @Test
        fun `양수 델타로 assists가 증가한다`() {
            stats.applyFieldCorrection("assists", 2)
            assertThat(stats.assists).isEqualTo(10)
        }

        @Test
        fun `음수 델타로 assists가 감소한다`() {
            stats.applyFieldCorrection("assists", -5)
            assertThat(stats.assists).isEqualTo(3)
        }

        @Test
        fun `음수 결과가 예상될 때 0으로 보호된다`() {
            stats.applyFieldCorrection("assists", -100)
            assertThat(stats.assists).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("errors 필드 정정")
    inner class ErrorsCorrectionTest {
        @Test
        fun `양수 델타로 errors가 증가한다`() {
            stats.applyFieldCorrection("errors", 1)
            assertThat(stats.errors).isEqualTo(4)
        }

        @Test
        fun `음수 델타로 errors가 감소한다`() {
            stats.applyFieldCorrection("errors", -2)
            assertThat(stats.errors).isEqualTo(1)
        }

        @Test
        fun `음수 결과가 예상될 때 0으로 보호된다`() {
            stats.applyFieldCorrection("errors", -100)
            assertThat(stats.errors).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("doublePlays 필드 정정")
    inner class DoublePlaysCorrectionTest {
        @Test
        fun `양수 델타로 doublePlays가 증가한다`() {
            stats.applyFieldCorrection("doublePlays", 3)
            assertThat(stats.doublePlays).isEqualTo(7)
        }

        @Test
        fun `음수 델타로 doublePlays가 감소한다`() {
            stats.applyFieldCorrection("doublePlays", -4)
            assertThat(stats.doublePlays).isEqualTo(0)
        }

        @Test
        fun `음수 결과가 예상될 때 0으로 보호된다`() {
            stats.applyFieldCorrection("doublePlays", -100)
            assertThat(stats.doublePlays).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("passedBalls 필드 정정")
    inner class PassedBallsCorrectionTest {
        @Test
        fun `양수 델타로 passedBalls가 증가한다`() {
            stats.applyFieldCorrection("passedBalls", 1)
            assertThat(stats.passedBalls).isEqualTo(3)
        }

        @Test
        fun `음수 델타로 passedBalls가 감소한다`() {
            stats.applyFieldCorrection("passedBalls", -1)
            assertThat(stats.passedBalls).isEqualTo(1)
        }

        @Test
        fun `음수 결과가 예상될 때 0으로 보호된다`() {
            stats.applyFieldCorrection("passedBalls", -100)
            assertThat(stats.passedBalls).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("예외 케이스")
    inner class ErrorCases {
        @Test
        fun `유효하지 않은 필드명이면 IllegalArgumentException이 발생한다`() {
            assertThatThrownBy {
                stats.applyFieldCorrection("invalidField", 1)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("유효하지 않은 통산 수비 통계 필드입니다")
        }

        @Test
        fun `존재하지 않는 필드명에도 IllegalArgumentException이 발생한다`() {
            assertThatThrownBy {
                stats.applyFieldCorrection("gamesPlayed", 1)
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("validate 호출 확인")
    inner class ValidateAfterCorrectionTest {
        @Test
        fun `정정 후 validate가 호출되어 정상 상태에서는 예외가 없다`() {
            // when & then (no exception)
            stats.applyFieldCorrection("putOuts", 1)
        }
    }

    private fun setField(
        obj: Any,
        fieldName: String,
        value: Int,
    ) {
        val field = obj.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(obj, value)
    }
}
