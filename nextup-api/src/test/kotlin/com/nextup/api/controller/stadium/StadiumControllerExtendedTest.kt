package com.nextup.api.controller.stadium

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.SlotNotFoundException
import com.nextup.common.exception.StadiumNotFoundException
import com.nextup.core.service.stadium.StadiumService
import com.nextup.core.service.stadium.dto.BookSlotRequest
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate

@DisplayName("StadiumController - Extended")
class StadiumControllerExtendedTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var stadiumService: StadiumService
    private lateinit var controller: StadiumController
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        stadiumService = mockk()
        controller = StadiumController(stadiumService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .setCustomArgumentResolvers(PageableHandlerMethodArgumentResolver())
                .build()
        objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    }

    @Nested
    @DisplayName("GET /api/v1/stadiums/{id}")
    inner class GetStadium {
        @Test
        fun `should return 404 when stadium not found`() {
            // given
            every { stadiumService.getById(999L) } throws StadiumNotFoundException(999L)

            // when & then
            mockMvc
                .perform(get("/api/v1/stadiums/999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("STADIUM_NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/stadiums/{id}/slots")
    inner class GetAvailableSlots {
        @Test
        fun `should return 404 when stadium not found for slots`() {
            // given
            every {
                stadiumService.getAvailableSlots(999L, LocalDate.of(2024, 12, 25))
            } throws StadiumNotFoundException(999L)

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/stadiums/999/slots")
                        .param("date", "2024-12-25"),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("STADIUM_NOT_FOUND"))
        }

        @Test
        fun `should return empty list when no slots available`() {
            // given
            every {
                stadiumService.getAvailableSlots(1L, LocalDate.of(2024, 12, 25))
            } returns emptyList()

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/stadiums/1/slots")
                        .param("date", "2024-12-25"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(0))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/stadiums/book")
    inner class BookSlot {
        @Test
        fun `should return 404 when slot not found`() {
            // given
            val request = BookSlotRequest(slotId = 999L, teamId = 100L, bookedBy = 200L)
            every { stadiumService.bookSlot(request) } throws SlotNotFoundException(999L)

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/stadiums/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SLOT_NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/stadiums/search - error cases")
    inner class SearchStadiumsErrors {
        @Test
        fun `should return empty list when no stadiums in range`() {
            // given
            every {
                stadiumService.searchStadiums(37.5, 127.0, 0.5)
            } returns emptyList()

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/stadiums/search")
                        .param("latitude", "37.5")
                        .param("longitude", "127.0")
                        .param("radiusKm", "0.5"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(0))
        }
    }
}
