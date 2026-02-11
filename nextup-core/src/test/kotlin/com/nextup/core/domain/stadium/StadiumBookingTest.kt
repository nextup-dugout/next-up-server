package com.nextup.core.domain.stadium

import com.nextup.common.exception.InvalidStateException
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("StadiumBooking")
class StadiumBookingTest {
    @Nested
    @DisplayName("create")
    inner class Create {
        @Test
        fun `should create booking successfully`() {
            // given
            val slot = mockk<StadiumSlot>(relaxed = true)
            every { slot.id } returns 1L

            // when
            val booking =
                StadiumBooking.create(
                    slot = slot,
                    teamId = 100L,
                    bookedBy = 200L,
                )

            // then
            assertThat(booking.teamId).isEqualTo(100L)
            assertThat(booking.bookedBy).isEqualTo(200L)
            assertThat(booking.status).isEqualTo(BookingStatus.CONFIRMED)
        }

        @Test
        fun `should throw exception when team ID is not positive`() {
            // given
            val slot = mockk<StadiumSlot>(relaxed = true)

            // when & then
            assertThatThrownBy {
                StadiumBooking.create(
                    slot = slot,
                    teamId = 0L,
                    bookedBy = 200L,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Team ID must be positive")
        }

        @Test
        fun `should throw exception when booked by ID is not positive`() {
            // given
            val slot = mockk<StadiumSlot>(relaxed = true)

            // when & then
            assertThatThrownBy {
                StadiumBooking.create(
                    slot = slot,
                    teamId = 100L,
                    bookedBy = -1L,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Booked by user ID must be positive")
        }
    }

    @Nested
    @DisplayName("cancel")
    inner class Cancel {
        @Test
        fun `should cancel confirmed booking`() {
            // given
            val slot = mockk<StadiumSlot>(relaxed = true)
            val booking =
                StadiumBooking.create(
                    slot = slot,
                    teamId = 100L,
                    bookedBy = 200L,
                )

            // when
            booking.cancel()

            // then
            assertThat(booking.status).isEqualTo(BookingStatus.CANCELLED)
        }

        @Test
        fun `should throw exception when cancelling already cancelled booking`() {
            // given
            val slot = mockk<StadiumSlot>(relaxed = true)
            val booking =
                StadiumBooking.create(
                    slot = slot,
                    teamId = 100L,
                    bookedBy = 200L,
                )
            booking.cancel()

            // when & then
            assertThatThrownBy {
                booking.cancel()
            }.isInstanceOf(InvalidStateException::class.java)
                .hasMessageContaining("cannot be cancelled")
        }

        @Test
        fun `should throw exception when cancelling completed booking`() {
            // given
            val slot = mockk<StadiumSlot>(relaxed = true)
            val booking =
                StadiumBooking.create(
                    slot = slot,
                    teamId = 100L,
                    bookedBy = 200L,
                )
            booking.complete()

            // when & then
            assertThatThrownBy {
                booking.cancel()
            }.isInstanceOf(InvalidStateException::class.java)
        }
    }

    @Nested
    @DisplayName("complete")
    inner class Complete {
        @Test
        fun `should complete confirmed booking`() {
            // given
            val slot = mockk<StadiumSlot>(relaxed = true)
            val booking =
                StadiumBooking.create(
                    slot = slot,
                    teamId = 100L,
                    bookedBy = 200L,
                )

            // when
            booking.complete()

            // then
            assertThat(booking.status).isEqualTo(BookingStatus.COMPLETED)
        }

        @Test
        fun `should throw exception when completing cancelled booking`() {
            // given
            val slot = mockk<StadiumSlot>(relaxed = true)
            val booking =
                StadiumBooking.create(
                    slot = slot,
                    teamId = 100L,
                    bookedBy = 200L,
                )
            booking.cancel()

            // when & then
            assertThatThrownBy {
                booking.complete()
            }.isInstanceOf(InvalidStateException::class.java)
                .hasMessageContaining("cannot be completed")
        }
    }
}
