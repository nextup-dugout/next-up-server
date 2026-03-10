package com.nextup.core.domain.game

import com.nextup.core.domain.player.Position
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PositionHistoryConverter")
class PositionHistoryConverterTest {
    private val converter = PositionHistoryConverter()

    @Nested
    @DisplayName("convertToDatabaseColumn")
    inner class ConvertToDatabaseColumnTest {
        @Test
        fun `null이면 null을 반환한다`() {
            assertThat(converter.convertToDatabaseColumn(null)).isNull()
        }

        @Test
        fun `빈 리스트이면 null을 반환한다`() {
            assertThat(converter.convertToDatabaseColumn(emptyList())).isNull()
        }

        @Test
        fun `단일 항목을 CSV로 변환한다`() {
            val entries = listOf(PositionHistoryEntry(3, Position.SHORTSTOP))

            assertThat(converter.convertToDatabaseColumn(entries)).isEqualTo("3:SHORTSTOP")
        }

        @Test
        fun `여러 항목을 CSV로 변환한다`() {
            val entries = listOf(
                PositionHistoryEntry(3, Position.SHORTSTOP),
                PositionHistoryEntry(6, Position.SECOND_BASE),
            )

            assertThat(converter.convertToDatabaseColumn(entries))
                .isEqualTo("3:SHORTSTOP,6:SECOND_BASE")
        }
    }

    @Nested
    @DisplayName("convertToEntityAttribute")
    inner class ConvertToEntityAttributeTest {
        @Test
        fun `null이면 빈 리스트를 반환한다`() {
            assertThat(converter.convertToEntityAttribute(null)).isEmpty()
        }

        @Test
        fun `빈 문자열이면 빈 리스트를 반환한다`() {
            assertThat(converter.convertToEntityAttribute("")).isEmpty()
        }

        @Test
        fun `공백 문자열이면 빈 리스트를 반환한다`() {
            assertThat(converter.convertToEntityAttribute("  ")).isEmpty()
        }

        @Test
        fun `단일 CSV 항목을 파싱한다`() {
            val result = converter.convertToEntityAttribute("3:SHORTSTOP")

            assertThat(result).containsExactly(
                PositionHistoryEntry(3, Position.SHORTSTOP),
            )
        }

        @Test
        fun `여러 CSV 항목을 파싱한다`() {
            val result = converter.convertToEntityAttribute("3:SHORTSTOP,6:SECOND_BASE")

            assertThat(result).containsExactly(
                PositionHistoryEntry(3, Position.SHORTSTOP),
                PositionHistoryEntry(6, Position.SECOND_BASE),
            )
        }

        @Test
        fun `잘못된 형식의 항목은 건너뛴다`() {
            val result = converter.convertToEntityAttribute("3:SHORTSTOP,invalid,6:SECOND_BASE")

            assertThat(result).containsExactly(
                PositionHistoryEntry(3, Position.SHORTSTOP),
                PositionHistoryEntry(6, Position.SECOND_BASE),
            )
        }

        @Test
        fun `이닝이 숫자가 아닌 항목은 건너뛴다`() {
            val result = converter.convertToEntityAttribute("abc:SHORTSTOP,3:SECOND_BASE")

            assertThat(result).containsExactly(
                PositionHistoryEntry(3, Position.SECOND_BASE),
            )
        }

        @Test
        fun `존재하지 않는 포지션 항목은 건너뛴다`() {
            val result = converter.convertToEntityAttribute("3:INVALID_POS,6:SHORTSTOP")

            assertThat(result).containsExactly(
                PositionHistoryEntry(6, Position.SHORTSTOP),
            )
        }

        @Test
        fun `공백이 포함된 CSV도 정상 파싱한다`() {
            val result = converter.convertToEntityAttribute(" 3 : SHORTSTOP , 6 : SECOND_BASE ")

            assertThat(result).containsExactly(
                PositionHistoryEntry(3, Position.SHORTSTOP),
                PositionHistoryEntry(6, Position.SECOND_BASE),
            )
        }
    }
}
