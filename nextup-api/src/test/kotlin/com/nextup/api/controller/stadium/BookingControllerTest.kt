package com.nextup.api.controller.stadium

import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.core.domain.stadium.BookingStatus
import com.nextup.core.domain.stadium.Stadium
import com.nextup.core.domain.stadium.StadiumBooking
import com.nextup.core.domain.stadium.StadiumSlot
import com.nextup.core.service.stadium.StadiumService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("BookingController")
class BookingControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var stadiumService: StadiumService
    private lateinit var controller: BookingController

    @BeforeEach
    fun setUp() {
        stadiumService = mockk()
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(1L, null, emptyList())

        controller = BookingController(stadiumService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())
                .build()
    }

    @Nested
    @DisplayName("GET /api/v1/bookings/{id}")
    inner class GetBookingDetail {
        @Test
        fun `should return booking detail successfully`() {
            // given
            val stadium = createStadium(1L, "잠실 야구장")
            val slot = createSlot(1L, stadium)
            val booking = createBooking(1L, slot, 100L, 200L)
            every { stadiumService.getBookingById(1L) } returns booking

            // when & then
            mockMvc
                .perform(get("/api/v1/bookings/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.teamId").value(100))
                .andExpect(jsonPath("$.data.bookedBy").value(200))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.stadium.id").value(1))
                .andExpect(jsonPath("$.data.stadium.name").value("잠실 야구장"))
                .andExpect(jsonPath("$.data.slot.id").value(1))
        }

        @Test
        fun `should return 404 when booking not found`() {
            // given
            every { stadiumService.getBookingById(999L) } throws
                com.nextup.common.exception.BookingNotFoundException(999L)

            // when & then
            mockMvc
                .perform(get("/api/v1/bookings/999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/bookings/team/{teamId}")
    inner class GetTeamBookings {
        @Test
        fun `should return all team bookings`() {
            // given
            val stadium = createStadium(1L, "잠실 야구장")
            val slot = createSlot(1L, stadium)
            val bookings =
                listOf(
                    createBooking(1L, slot, 100L, 200L),
                    createBooking(2L, slot, 100L, 200L),
                )
            every { stadiumService.getTeamBookings(100L, null) } returns bookings

            // when & then
            mockMvc
                .perform(get("/api/v1/bookings/team/100"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
        }

        @Test
        fun `should filter by status when provided`() {
            // given
            val stadium = createStadium(1L, "잠실 야구장")
            val slot = createSlot(1L, stadium)
            val bookings = listOf(createBooking(1L, slot, 100L, 200L))
            every { stadiumService.getTeamBookings(100L, BookingStatus.CONFIRMED) } returns bookings

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/bookings/team/100")
                        .param("status", "CONFIRMED"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/bookings/{id}")
    inner class CancelBooking {
        @Test
        fun `should cancel booking successfully`() {
            // given
            val stadium = createStadium(1L, "잠실 야구장")
            val slot = createSlot(1L, stadium)
            slot.book()
            val booking = createBooking(1L, slot, 100L, 200L)
            booking.cancel()
            every { stadiumService.cancelBooking(1L) } returns booking

            // when & then
            mockMvc
                .perform(delete("/api/v1/bookings/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/bookings/{id}/complete")
    inner class CompleteBooking {
        @Test
        fun `should complete booking successfully`() {
            // given
            val stadium = createStadium(1L, "잠실 야구장")
            val slot = createSlot(1L, stadium)
            val booking = createBooking(1L, slot, 100L, 200L)
            booking.complete()
            every { stadiumService.completeBooking(1L) } returns booking

            // when & then
            mockMvc
                .perform(put("/api/v1/bookings/1/complete"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
        }
    }

    private fun createStadium(
        id: Long,
        name: String,
    ): Stadium {
        val stadium =
            Stadium.create(
                name = name,
                address = "서울특별시",
                latitude = 37.5121,
                longitude = 127.0717,
            )
        val idField = Stadium::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(stadium, id)
        return stadium
    }

    private fun createSlot(
        id: Long,
        stadium: Stadium,
    ): StadiumSlot {
        val slot =
            StadiumSlot.create(
                stadium = stadium,
                date = LocalDate.of(2024, 12, 25),
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
