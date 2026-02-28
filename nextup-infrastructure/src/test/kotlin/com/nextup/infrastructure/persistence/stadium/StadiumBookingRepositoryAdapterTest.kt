package com.nextup.infrastructure.persistence.stadium

import com.nextup.core.domain.stadium.BookingStatus
import com.nextup.core.domain.stadium.Stadium
import com.nextup.core.domain.stadium.StadiumBooking
import com.nextup.core.domain.stadium.StadiumSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("StadiumBookingRepositoryAdapter")
class StadiumBookingRepositoryAdapterTest {
    private lateinit var jpaRepository: StadiumBookingJpaRepository
    private lateinit var adapter: StadiumBookingRepositoryAdapter

    @BeforeEach
    fun setUp() {
        jpaRepository = mockk()
        adapter = StadiumBookingRepositoryAdapter(jpaRepository)
    }

    @Nested
    @DisplayName("save")
    inner class Save {
        @Test
        fun `should save and return booking`() {
            // given
            val booking = createBooking()
            every { jpaRepository.save(booking) } returns booking

            // when
            val result = adapter.save(booking)

            // then
            assertThat(result).isEqualTo(booking)
            verify { jpaRepository.save(booking) }
        }
    }

    @Nested
    @DisplayName("findByIdOrNull")
    inner class FindByIdOrNull {
        @Test
        fun `should return booking when found`() {
            // given
            val booking = createBooking()
            every { jpaRepository.findByIdOrNull(1L) } returns booking

            // when
            val result = adapter.findByIdOrNull(1L)

            // then
            assertThat(result).isEqualTo(booking)
        }

        @Test
        fun `should return null when not found`() {
            // given
            every { jpaRepository.findByIdOrNull(999L) } returns null

            // when
            val result = adapter.findByIdOrNull(999L)

            // then
            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("findBySlotId")
    inner class FindBySlotId {
        @Test
        fun `should return bookings for given slot`() {
            // given
            val booking = createBooking()
            every { jpaRepository.findBySlotId(1L) } returns listOf(booking)

            // when
            val result = adapter.findBySlotId(1L)

            // then
            assertThat(result).hasSize(1)
        }

        @Test
        fun `should return empty list when no bookings for slot`() {
            // given
            every { jpaRepository.findBySlotId(99L) } returns emptyList()

            // when
            val result = adapter.findBySlotId(99L)

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("findByTeamId")
    inner class FindByTeamId {
        @Test
        fun `should return bookings for given team`() {
            // given
            val booking = createBooking()
            every { jpaRepository.findByTeamId(100L) } returns listOf(booking)

            // when
            val result = adapter.findByTeamId(100L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].teamId).isEqualTo(100L)
        }

        @Test
        fun `should return all bookings for team`() {
            // given
            val bookings = listOf(createBooking(), createBooking())
            every { jpaRepository.findByTeamId(100L) } returns bookings

            // when
            val result = adapter.findByTeamId(100L)

            // then
            assertThat(result).hasSize(2)
        }

        @Test
        fun `should return empty list when no bookings for team`() {
            // given
            every { jpaRepository.findByTeamId(99L) } returns emptyList()

            // when
            val result = adapter.findByTeamId(99L)

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("findByTeamIdAndStatus")
    inner class FindByTeamIdAndStatus {
        @Test
        fun `should return confirmed bookings for given team`() {
            // given
            val booking = createBooking()
            every {
                jpaRepository.findByTeamIdAndStatus(100L, BookingStatus.CONFIRMED)
            } returns listOf(booking)

            // when
            val result = adapter.findByTeamIdAndStatus(100L, BookingStatus.CONFIRMED)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].status).isEqualTo(BookingStatus.CONFIRMED)
        }

        @Test
        fun `should return empty list when no bookings match team and status`() {
            // given
            every {
                jpaRepository.findByTeamIdAndStatus(100L, BookingStatus.CANCELLED)
            } returns emptyList()

            // when
            val result = adapter.findByTeamIdAndStatus(100L, BookingStatus.CANCELLED)

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("existsBySlotIdAndStatus")
    inner class ExistsBySlotIdAndStatus {
        @Test
        fun `should return true when booking exists for slot and status`() {
            // given
            every {
                jpaRepository.existsBySlotIdAndStatus(1L, BookingStatus.CONFIRMED)
            } returns true

            // when
            val result = adapter.existsBySlotIdAndStatus(1L, BookingStatus.CONFIRMED)

            // then
            assertThat(result).isTrue()
        }

        @Test
        fun `should return false when no booking exists for slot and status`() {
            // given
            every {
                jpaRepository.existsBySlotIdAndStatus(99L, BookingStatus.CONFIRMED)
            } returns false

            // when
            val result = adapter.existsBySlotIdAndStatus(99L, BookingStatus.CONFIRMED)

            // then
            assertThat(result).isFalse()
        }
    }

    private fun createBooking(): StadiumBooking {
        val stadium =
            Stadium.create(
                name = "잠실 야구장",
                address = "서울특별시",
                latitude = 37.5121,
                longitude = 127.0717,
            )
        val slot =
            StadiumSlot.create(
                stadium = stadium,
                date = LocalDate.of(2024, 12, 25),
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(12, 0),
            )
        return StadiumBooking.create(slot = slot, teamId = 100L, bookedBy = 200L)
    }
}
