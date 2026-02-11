package com.nextup.core.domain.stadium

import com.nextup.common.exception.SlotNotAvailableException
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("StadiumSlot")
class StadiumSlotTest {
    @Nested
    @DisplayName("create")
    inner class Create {
        @Test
        fun `should create slot successfully`() {
            // given
            val stadium = mockk<Stadium>(relaxed = true)

            // when
            val slot =
                StadiumSlot.create(
                    stadium = stadium,
                    date = LocalDate.of(2024, 12, 25),
                    startTime = LocalTime.of(10, 0),
                    endTime = LocalTime.of(12, 0),
                    price = BigDecimal("50000"),
                )

            // then
            assertThat(slot.date).isEqualTo(LocalDate.of(2024, 12, 25))
            assertThat(slot.startTime).isEqualTo(LocalTime.of(10, 0))
            assertThat(slot.endTime).isEqualTo(LocalTime.of(12, 0))
            assertThat(slot.price).isEqualTo(BigDecimal("50000"))
            assertThat(slot.status).isEqualTo(SlotStatus.AVAILABLE)
        }

        @Test
        fun `should throw exception when start time is after end time`() {
            // given
            val stadium = mockk<Stadium>(relaxed = true)

            // when & then
            assertThatThrownBy {
                StadiumSlot.create(
                    stadium = stadium,
                    date = LocalDate.of(2024, 12, 25),
                    startTime = LocalTime.of(12, 0),
                    endTime = LocalTime.of(10, 0),
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Start time must be before end time")
        }

        @Test
        fun `should throw exception when price is not positive`() {
            // given
            val stadium = mockk<Stadium>(relaxed = true)

            // when & then
            assertThatThrownBy {
                StadiumSlot.create(
                    stadium = stadium,
                    date = LocalDate.of(2024, 12, 25),
                    startTime = LocalTime.of(10, 0),
                    endTime = LocalTime.of(12, 0),
                    price = BigDecimal("-1000"),
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Price must be positive")
        }
    }

    @Nested
    @DisplayName("book")
    inner class Book {
        @Test
        fun `should book slot successfully when available`() {
            // given
            val stadium = mockk<Stadium>(relaxed = true)
            val slot =
                StadiumSlot.create(
                    stadium = stadium,
                    date = LocalDate.of(2024, 12, 25),
                    startTime = LocalTime.of(10, 0),
                    endTime = LocalTime.of(12, 0),
                )

            // when
            slot.book()

            // then
            assertThat(slot.status).isEqualTo(SlotStatus.BOOKED)
        }

        @Test
        fun `should throw exception when slot is already booked`() {
            // given
            val stadium = mockk<Stadium>(relaxed = true)
            val slot =
                StadiumSlot.create(
                    stadium = stadium,
                    date = LocalDate.of(2024, 12, 25),
                    startTime = LocalTime.of(10, 0),
                    endTime = LocalTime.of(12, 0),
                )
            slot.book()

            // when & then
            assertThatThrownBy {
                slot.book()
            }.isInstanceOf(SlotNotAvailableException::class.java)
                .hasMessageContaining("not available for booking")
        }

        @Test
        fun `should throw exception when slot is in maintenance`() {
            // given
            val stadium = mockk<Stadium>(relaxed = true)
            val slot =
                StadiumSlot.create(
                    stadium = stadium,
                    date = LocalDate.of(2024, 12, 25),
                    startTime = LocalTime.of(10, 0),
                    endTime = LocalTime.of(12, 0),
                )
            slot.maintain()

            // when & then
            assertThatThrownBy {
                slot.book()
            }.isInstanceOf(SlotNotAvailableException::class.java)
        }
    }

    @Nested
    @DisplayName("cancel")
    inner class Cancel {
        @Test
        fun `should cancel booking successfully`() {
            // given
            val stadium = mockk<Stadium>(relaxed = true)
            val slot =
                StadiumSlot.create(
                    stadium = stadium,
                    date = LocalDate.of(2024, 12, 25),
                    startTime = LocalTime.of(10, 0),
                    endTime = LocalTime.of(12, 0),
                )
            slot.book()

            // when
            slot.cancel()

            // then
            assertThat(slot.status).isEqualTo(SlotStatus.AVAILABLE)
        }

        @Test
        fun `should throw exception when slot is not booked`() {
            // given
            val stadium = mockk<Stadium>(relaxed = true)
            val slot =
                StadiumSlot.create(
                    stadium = stadium,
                    date = LocalDate.of(2024, 12, 25),
                    startTime = LocalTime.of(10, 0),
                    endTime = LocalTime.of(12, 0),
                )

            // when & then
            assertThatThrownBy {
                slot.cancel()
            }.isInstanceOf(SlotNotAvailableException::class.java)
                .hasMessageContaining("not booked")
        }
    }

    @Nested
    @DisplayName("maintain")
    inner class Maintain {
        @Test
        fun `should set slot to maintenance`() {
            // given
            val stadium = mockk<Stadium>(relaxed = true)
            val slot =
                StadiumSlot.create(
                    stadium = stadium,
                    date = LocalDate.of(2024, 12, 25),
                    startTime = LocalTime.of(10, 0),
                    endTime = LocalTime.of(12, 0),
                )

            // when
            slot.maintain()

            // then
            assertThat(slot.status).isEqualTo(SlotStatus.MAINTENANCE)
        }

        @Test
        fun `should end maintenance successfully`() {
            // given
            val stadium = mockk<Stadium>(relaxed = true)
            val slot =
                StadiumSlot.create(
                    stadium = stadium,
                    date = LocalDate.of(2024, 12, 25),
                    startTime = LocalTime.of(10, 0),
                    endTime = LocalTime.of(12, 0),
                )
            slot.maintain()

            // when
            slot.endMaintenance()

            // then
            assertThat(slot.status).isEqualTo(SlotStatus.AVAILABLE)
        }

        @Test
        fun `should throw exception when ending maintenance on non-maintenance slot`() {
            // given
            val stadium = mockk<Stadium>(relaxed = true)
            val slot =
                StadiumSlot.create(
                    stadium = stadium,
                    date = LocalDate.of(2024, 12, 25),
                    startTime = LocalTime.of(10, 0),
                    endTime = LocalTime.of(12, 0),
                )

            // when & then
            assertThatThrownBy {
                slot.endMaintenance()
            }.isInstanceOf(SlotNotAvailableException::class.java)
                .hasMessageContaining("not in maintenance")
        }
    }
}
