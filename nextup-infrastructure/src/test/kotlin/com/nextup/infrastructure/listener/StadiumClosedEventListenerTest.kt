package com.nextup.infrastructure.listener

import com.nextup.core.domain.event.StadiumClosedEvent
import com.nextup.core.domain.stadium.BookingStatus
import com.nextup.core.domain.stadium.StadiumBooking
import com.nextup.core.port.repository.StadiumBookingRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("StadiumClosedEventListener 테스트")
class StadiumClosedEventListenerTest {
    private val stadiumBookingRepository: StadiumBookingRepositoryPort = mockk()

    private lateinit var listener: StadiumClosedEventListener

    @BeforeEach
    fun setUp() {
        listener =
            StadiumClosedEventListener(
                stadiumBookingRepository = stadiumBookingRepository,
            )
    }

    @Test
    fun `구장 폐업 시 CONFIRMED 예약을 일괄 취소한다`() {
        // given
        val event = StadiumClosedEvent(stadiumId = 1L, stadiumName = "잠실 야구장")
        val booking1 = mockk<StadiumBooking>(relaxed = true)
        every { booking1.id } returns 10L
        every { booking1.teamId } returns 100L
        val booking2 = mockk<StadiumBooking>(relaxed = true)
        every { booking2.id } returns 20L
        every { booking2.teamId } returns 200L

        every {
            stadiumBookingRepository.findByStadiumIdAndStatus(1L, BookingStatus.CONFIRMED)
        } returns listOf(booking1, booking2)
        every { stadiumBookingRepository.save(any()) } answers { firstArg() }

        // when
        listener.handleStadiumClosed(event)

        // then
        verify(exactly = 1) { booking1.cancel() }
        verify(exactly = 1) { booking2.cancel() }
        verify(exactly = 2) { stadiumBookingRepository.save(any()) }
    }

    @Test
    fun `구장 폐업 시 취소 대상 예약이 없으면 아무 작업도 하지 않는다`() {
        // given
        val event = StadiumClosedEvent(stadiumId = 1L, stadiumName = "잠실 야구장")
        every {
            stadiumBookingRepository.findByStadiumIdAndStatus(1L, BookingStatus.CONFIRMED)
        } returns emptyList()

        // when
        listener.handleStadiumClosed(event)

        // then
        verify(exactly = 0) { stadiumBookingRepository.save(any()) }
    }

    @Test
    fun `구장 폐업 시 CONFIRMED 상태 예약만 대상으로 한다`() {
        // given
        val event = StadiumClosedEvent(stadiumId = 1L, stadiumName = "잠실 야구장")
        val confirmedBooking = mockk<StadiumBooking>(relaxed = true)
        every { confirmedBooking.id } returns 10L
        every { confirmedBooking.teamId } returns 100L

        every {
            stadiumBookingRepository.findByStadiumIdAndStatus(1L, BookingStatus.CONFIRMED)
        } returns listOf(confirmedBooking)
        every { stadiumBookingRepository.save(any()) } answers { firstArg() }

        // when
        listener.handleStadiumClosed(event)

        // then
        verify(exactly = 1) {
            stadiumBookingRepository.findByStadiumIdAndStatus(1L, BookingStatus.CONFIRMED)
        }
        verify(exactly = 1) { confirmedBooking.cancel() }
        verify(exactly = 1) { stadiumBookingRepository.save(confirmedBooking) }
    }
}
