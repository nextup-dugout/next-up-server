package com.nextup.api.controller.stadium

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.core.domain.stadium.Stadium
import com.nextup.core.domain.stadium.StadiumBooking
import com.nextup.core.domain.stadium.StadiumSlot
import com.nextup.core.service.stadium.StadiumService
import com.nextup.core.service.stadium.dto.BookSlotRequest
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("StadiumController")
class StadiumControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var stadiumService: StadiumService
    private lateinit var controller: StadiumController
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        stadiumService = mockk()
        controller = StadiumController(stadiumService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(GlobalExceptionHandler()).build()
        objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    }

    @Nested
    @DisplayName("GET /api/v1/stadiums/search")
    inner class SearchStadiums {
        @Test
        fun `should search nearby stadiums`() {
            // given
            val stadiums =
                listOf(
                    createStadium(1L, "잠실 야구장", 37.5121, 127.0717),
                    createStadium(2L, "고척 야구장", 37.4981, 126.8671),
                )
            every { stadiumService.searchStadiums(37.5, 127.0, 10.0) } returns stadiums

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/stadiums/search")
                        .param("latitude", "37.5")
                        .param("longitude", "127.0")
                        .param("radiusKm", "10.0"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("잠실 야구장"))
        }

        @Test
        fun `should use default radius when not provided`() {
            // given
            val stadiums = listOf(createStadium(1L, "잠실 야구장", 37.5121, 127.0717))
            every { stadiumService.searchStadiums(37.5, 127.0, 10.0) } returns stadiums

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/stadiums/search")
                        .param("latitude", "37.5")
                        .param("longitude", "127.0"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/stadiums/{id}")
    inner class GetStadium {
        @Test
        fun `should return stadium when found`() {
            // given
            val stadium = createStadium(1L, "잠실 야구장", 37.5121, 127.0717)
            every { stadiumService.getById(1L) } returns stadium

            // when & then
            mockMvc
                .perform(get("/api/v1/stadiums/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("잠실 야구장"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/stadiums/{id}/slots")
    inner class GetAvailableSlots {
        @Test
        fun `should return available slots for a date`() {
            // given
            val stadium = createStadium(1L, "잠실 야구장", 37.5121, 127.0717)
            val slots =
                listOf(
                    createSlot(1L, stadium, LocalDate.of(2024, 12, 25)),
                    createSlot(2L, stadium, LocalDate.of(2024, 12, 25)),
                )
            every { stadiumService.getAvailableSlots(1L, LocalDate.of(2024, 12, 25)) } returns slots

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/stadiums/1/slots")
                        .param("date", "2024-12-25"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/stadiums/book")
    inner class BookSlot {
        @Test
        fun `should book slot successfully`() {
            // given
            val request = BookSlotRequest(slotId = 1L, teamId = 100L, bookedBy = 200L)
            val stadium = createStadium(1L, "잠실 야구장", 37.5121, 127.0717)
            val slot = createSlot(1L, stadium, LocalDate.of(2024, 12, 25))
            val booking = createBooking(1L, slot, 100L, 200L)
            every { stadiumService.bookSlot(request) } returns booking

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/stadiums/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.teamId").value(100))
                .andExpect(jsonPath("$.data.bookedBy").value(200))
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
