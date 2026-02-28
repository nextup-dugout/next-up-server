package com.nextup.core.service.stadium

import com.nextup.common.exception.BookingNotFoundException
import com.nextup.common.exception.InvalidInputException
import com.nextup.common.exception.SlotNotFoundException
import com.nextup.common.exception.StadiumNotFoundException
import com.nextup.core.domain.stadium.BookingStatus
import com.nextup.core.domain.stadium.SlotStatus
import com.nextup.core.domain.stadium.Stadium
import com.nextup.core.domain.stadium.StadiumBooking
import com.nextup.core.domain.stadium.StadiumSlot
import com.nextup.core.port.repository.StadiumBookingRepositoryPort
import com.nextup.core.port.repository.StadiumRepositoryPort
import com.nextup.core.port.repository.StadiumSlotRepositoryPort
import com.nextup.core.service.stadium.dto.BookSlotRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("StadiumService")
class StadiumServiceTest {
    private lateinit var stadiumRepository: StadiumRepositoryPort
    private lateinit var slotRepository: StadiumSlotRepositoryPort
    private lateinit var bookingRepository: StadiumBookingRepositoryPort
    private lateinit var stadiumService: StadiumService

    @BeforeEach
    fun setUp() {
        stadiumRepository = mockk()
        slotRepository = mockk()
        bookingRepository = mockk()
        stadiumService = StadiumService(stadiumRepository, slotRepository, bookingRepository)
    }

    @Nested
    @DisplayName("findNearbyStadiums")
    inner class FindNearbyStadiums {
        @Test
        fun `should return paged stadiums ordered by distance`() {
            // given
            val stadiums =
                listOf(
                    createStadium(1L, "잠실 야구장", 37.5121, 127.0717),
                    createStadium(2L, "고척 야구장", 37.4981, 126.8671),
                )
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(stadiums, pageable, 2)
            every { stadiumRepository.findNearbyStadiums(37.5, 127.0, 10.0, pageable) } returns page

            // when
            val result = stadiumService.findNearbyStadiums(37.5, 127.0, 10.0, pageable)

            // then
            assertThat(result.content).hasSize(2)
            assertThat(result.totalElements).isEqualTo(2)
            assertThat(result.content[0].name).isEqualTo("잠실 야구장")
        }

        @Test
        fun `should throw InvalidInputException when latitude is out of range`() {
            // given
            val pageable = PageRequest.of(0, 20)

            // when & then
            assertThatThrownBy {
                stadiumService.findNearbyStadiums(91.0, 127.0, 10.0, pageable)
            }.isInstanceOf(InvalidInputException::class.java)
                .hasMessageContaining("위도는 -90 ~ 90 범위여야 합니다")
        }

        @Test
        fun `should throw InvalidInputException when longitude is out of range`() {
            // given
            val pageable = PageRequest.of(0, 20)

            // when & then
            assertThatThrownBy {
                stadiumService.findNearbyStadiums(37.5, 181.0, 10.0, pageable)
            }.isInstanceOf(InvalidInputException::class.java)
                .hasMessageContaining("경도는 -180 ~ 180 범위여야 합니다")
        }

        @Test
        fun `should throw InvalidInputException when radiusKm is zero`() {
            // given
            val pageable = PageRequest.of(0, 20)

            // when & then
            assertThatThrownBy {
                stadiumService.findNearbyStadiums(37.5, 127.0, 0.0, pageable)
            }.isInstanceOf(InvalidInputException::class.java)
                .hasMessageContaining("검색 반경은 0보다 커야 합니다")
        }

        @Test
        fun `should throw InvalidInputException when radiusKm is negative`() {
            // given
            val pageable = PageRequest.of(0, 20)

            // when & then
            assertThatThrownBy {
                stadiumService.findNearbyStadiums(37.5, 127.0, -5.0, pageable)
            }.isInstanceOf(InvalidInputException::class.java)
                .hasMessageContaining("검색 반경은 0보다 커야 합니다")
        }

        @Test
        fun `should accept boundary latitude values`() {
            // given
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(emptyList<Stadium>(), pageable, 0)
            every { stadiumRepository.findNearbyStadiums(90.0, 180.0, 10.0, pageable) } returns page

            // when
            val result = stadiumService.findNearbyStadiums(90.0, 180.0, 10.0, pageable)

            // then
            assertThat(result.content).isEmpty()
        }
    }

    @Nested
    @DisplayName("searchStadiums")
    inner class SearchStadiums {
        @Test
        fun `should search nearby stadiums`() {
            // given
            val stadiums =
                listOf(
                    createStadium(1L, "잠실 야구장", 37.5121, 127.0717),
                    createStadium(2L, "고척 야구장", 37.4981, 126.8671),
                )
            every { stadiumRepository.findNearby(37.5, 127.0, 10.0) } returns stadiums

            // when
            val result = stadiumService.searchStadiums(37.5, 127.0, 10.0)

            // then
            assertThat(result).hasSize(2)
            assertThat(result[0].name).isEqualTo("잠실 야구장")
        }
    }

    @Nested
    @DisplayName("getById")
    inner class GetById {
        @Test
        fun `should return stadium when found`() {
            // given
            val stadium = createStadium(1L, "잠실 야구장", 37.5121, 127.0717)
            every { stadiumRepository.findByIdOrNull(1L) } returns stadium

            // when
            val result = stadiumService.getById(1L)

            // then
            assertThat(result.id).isEqualTo(1L)
            assertThat(result.name).isEqualTo("잠실 야구장")
        }

        @Test
        fun `should throw exception when not found`() {
            // given
            every { stadiumRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                stadiumService.getById(999L)
            }.isInstanceOf(StadiumNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("getAvailableSlots")
    inner class GetAvailableSlots {
        @Test
        fun `should return only available slots`() {
            // given
            val stadium = createStadium(1L, "잠실 야구장", 37.5121, 127.0717)
            val availableSlot = createSlot(1L, stadium, LocalDate.of(2024, 12, 25))
            val bookedSlot = createSlot(2L, stadium, LocalDate.of(2024, 12, 25)).apply { book() }
            every { stadiumRepository.findByIdOrNull(1L) } returns stadium
            every { slotRepository.findByStadiumIdAndDate(1L, LocalDate.of(2024, 12, 25)) } returns
                listOf(availableSlot, bookedSlot)

            // when
            val result = stadiumService.getAvailableSlots(1L, LocalDate.of(2024, 12, 25))

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].status).isEqualTo(SlotStatus.AVAILABLE)
        }

        @Test
        fun `should throw exception when stadium not found`() {
            // given
            every { stadiumRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                stadiumService.getAvailableSlots(999L, LocalDate.of(2024, 12, 25))
            }.isInstanceOf(StadiumNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("bookSlot")
    inner class BookSlot {
        @Test
        fun `should book slot successfully`() {
            // given
            val stadium = createStadium(1L, "잠실 야구장", 37.5121, 127.0717)
            val slot = createSlot(1L, stadium, LocalDate.of(2024, 12, 25))
            val request = BookSlotRequest(slotId = 1L, teamId = 100L, bookedBy = 200L)
            every { slotRepository.findByIdOrNull(1L) } returns slot
            every { bookingRepository.save(any()) } answers { firstArg() }

            // when
            val result = stadiumService.bookSlot(request)

            // then
            assertThat(slot.status).isEqualTo(SlotStatus.BOOKED)
            assertThat(result.teamId).isEqualTo(100L)
            verify { bookingRepository.save(any()) }
        }

        @Test
        fun `should throw exception when slot not found`() {
            // given
            val request = BookSlotRequest(slotId = 999L, teamId = 100L, bookedBy = 200L)
            every { slotRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                stadiumService.bookSlot(request)
            }.isInstanceOf(SlotNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("cancelBooking")
    inner class CancelBooking {
        @Test
        fun `should cancel booking and restore slot status`() {
            // given
            val stadium = createStadium(1L, "잠실 야구장", 37.5121, 127.0717)
            val slot = createSlot(1L, stadium, LocalDate.of(2024, 12, 25)).apply { book() }
            val booking = createBooking(1L, slot, 100L, 200L)
            every { bookingRepository.findByIdOrNull(1L) } returns booking

            // when
            val result = stadiumService.cancelBooking(1L)

            // then
            assertThat(result.status).isEqualTo(BookingStatus.CANCELLED)
            assertThat(slot.status).isEqualTo(SlotStatus.AVAILABLE)
        }

        @Test
        fun `should throw exception when booking not found`() {
            // given
            every { bookingRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                stadiumService.cancelBooking(999L)
            }.isInstanceOf(BookingNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("completeBooking")
    inner class CompleteBooking {
        @Test
        fun `should complete booking successfully`() {
            // given
            val stadium = createStadium(1L, "잠실 야구장", 37.5121, 127.0717)
            val slot = createSlot(1L, stadium, LocalDate.of(2024, 12, 25))
            val booking = createBooking(1L, slot, 100L, 200L)
            every { bookingRepository.findByIdOrNull(1L) } returns booking

            // when
            val result = stadiumService.completeBooking(1L)

            // then
            assertThat(result.status).isEqualTo(BookingStatus.COMPLETED)
        }
    }

    @Nested
    @DisplayName("getTeamBookings")
    inner class GetTeamBookings {
        @Test
        fun `should return all team bookings when status is null`() {
            // given
            val stadium = createStadium(1L, "잠실 야구장", 37.5121, 127.0717)
            val slot = createSlot(1L, stadium, LocalDate.of(2024, 12, 25))
            val bookings =
                listOf(
                    createBooking(1L, slot, 100L, 200L),
                    createBooking(2L, slot, 100L, 200L),
                )
            every { bookingRepository.findByTeamId(100L) } returns bookings

            // when
            val result = stadiumService.getTeamBookings(100L, null)

            // then
            assertThat(result).hasSize(2)
        }

        @Test
        fun `should filter by status when provided`() {
            // given
            val stadium = createStadium(1L, "잠실 야구장", 37.5121, 127.0717)
            val slot = createSlot(1L, stadium, LocalDate.of(2024, 12, 25))
            val bookings = listOf(createBooking(1L, slot, 100L, 200L))
            every { bookingRepository.findByTeamIdAndStatus(100L, BookingStatus.CONFIRMED) } returns bookings

            // when
            val result = stadiumService.getTeamBookings(100L, BookingStatus.CONFIRMED)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].status).isEqualTo(BookingStatus.CONFIRMED)
        }
    }

    private fun createStadium(
        id: Long,
        name: String,
        latitude: Double,
        longitude: Double,
    ): Stadium {
        val stadium =
            Stadium.create(
                name = name,
                address = "서울특별시",
                latitude = latitude,
                longitude = longitude,
            )
        val idField = Stadium::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(stadium, id)
        return stadium
    }

    private fun createSlot(
        id: Long,
        stadium: Stadium,
        date: LocalDate,
    ): StadiumSlot {
        val slot =
            StadiumSlot.create(
                stadium = stadium,
                date = date,
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(12, 0),
            )
        val idField = StadiumSlot::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(slot, id)
        return slot
    }

    private fun createBooking(
        id: Long,
        slot: StadiumSlot,
        teamId: Long,
        bookedBy: Long,
    ): StadiumBooking {
        val booking = StadiumBooking.create(slot = slot, teamId = teamId, bookedBy = bookedBy)
        val idField = StadiumBooking::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(booking, id)
        return booking
    }
}
