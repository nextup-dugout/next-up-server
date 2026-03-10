package com.nextup.core.domain.game

import com.nextup.core.domain.player.Position
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PositionHistoryEntry")
class PositionHistoryEntryTest {
    @Nested
    @DisplayName("생성")
    inner class CreateTest {
        @Test
        fun `유효한 이닝과 포지션으로 생성할 수 있다`() {
            val entry = PositionHistoryEntry(1, Position.SHORTSTOP)

            assertThat(entry.inning).isEqualTo(1)
            assertThat(entry.position).isEqualTo(Position.SHORTSTOP)
        }

        @Test
        fun `이닝이 0이면 예외가 발생한다`() {
            assertThatThrownBy {
                PositionHistoryEntry(0, Position.SHORTSTOP)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("이닝은 1 이상")
        }

        @Test
        fun `이닝이 음수이면 예외가 발생한다`() {
            assertThatThrownBy {
                PositionHistoryEntry(-1, Position.SHORTSTOP)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("이닝은 1 이상")
        }
    }

    @Nested
    @DisplayName("동등성")
    inner class EqualityTest {
        @Test
        fun `같은 이닝과 포지션이면 동등하다`() {
            val entry1 = PositionHistoryEntry(3, Position.SHORTSTOP)
            val entry2 = PositionHistoryEntry(3, Position.SHORTSTOP)

            assertThat(entry1).isEqualTo(entry2)
        }

        @Test
        fun `이닝이 다르면 동등하지 않다`() {
            val entry1 = PositionHistoryEntry(3, Position.SHORTSTOP)
            val entry2 = PositionHistoryEntry(5, Position.SHORTSTOP)

            assertThat(entry1).isNotEqualTo(entry2)
        }
    }
}
