package com.nextup.backoffice.controller.stadium

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nextup.backoffice.dto.stadium.CreateSlotRequest
import com.nextup.backoffice.dto.stadium.CreateStadiumRequest
import com.nextup.backoffice.dto.stadium.UpdateStadiumRequest
import com.nextup.core.domain.stadium.Stadium
import com.nextup.core.domain.stadium.StadiumSlot
import com.nextup.core.service.stadium.StadiumAdminService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("StadiumAdminController")
class StadiumAdminControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var stadiumAdminService: StadiumAdminService
    private lateinit var controller: StadiumAdminController
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        stadiumAdminService = mockk()
        controller = StadiumAdminController(stadiumAdminService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
        objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    }

    @Nested
    @DisplayName("POST /api/backoffice/stadiums")
    inner class CreateStadium {
        @Test
        fun `should create stadium successfully`() {
            // given
            val request =
                CreateStadiumRequest(
                    name = "잠실 야구장",
                    address = "서울특별시 송파구 올림픽로 25",
                    latitude = 37.5121,
                    longitude = 127.0717,
                    capacity = 25000,
                    facilities = "주차장, 샤워실",
                    contactInfo = "02-1234-5678",
                )
            val stadium = createStadium(1L, "잠실 야구장", 37.5121, 127.0717)
            every { stadiumAdminService.createStadium(any()) } returns stadium

            // when & then
            mockMvc
                .perform(
                    post("/api/backoffice/stadiums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("잠실 야구장"))
                .andExpect(jsonPath("$.data.latitude").value(37.5121))
        }
    }

    @Nested
    @DisplayName("PUT /api/backoffice/stadiums/{id}")
    inner class UpdateStadium {
        @Test
        fun `should update stadium successfully`() {
            // given
            val request =
                UpdateStadiumRequest(
                    address = "서울특별시 송파구 올림픽로 25",
                    capacity = 26000,
                )
            val stadium =
                createStadium(1L, "잠실 야구장", 37.5121, 127.0717).apply {
                    update(address = "서울특별시 송파구 올림픽로 25", capacity = 26000)
                }
            every {
                stadiumAdminService.updateStadium(1L, any(), any(), any(), any(), any(), any(), any())
            } returns stadium

            // when & then
            mockMvc
                .perform(
                    put("/api/backoffice/stadiums/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
        }
    }

    @Nested
    @DisplayName("POST /api/backoffice/stadiums/slots")
    inner class CreateSlots {
        @Test
        fun `should create slots successfully`() {
            // given
            val requests =
                listOf(
                    CreateSlotRequest(
                        stadiumId = 1L,
                        date = LocalDate.of(2024, 12, 25),
                        startTime = LocalTime.of(10, 0),
                        endTime = LocalTime.of(12, 0),
                        price = BigDecimal("50000"),
                    ),
                    CreateSlotRequest(
                        stadiumId = 1L,
                        date = LocalDate.of(2024, 12, 25),
                        startTime = LocalTime.of(14, 0),
                        endTime = LocalTime.of(16, 0),
                        price = BigDecimal("50000"),
                    ),
                )
            val stadium = createStadium(1L, "잠실 야구장", 37.5121, 127.0717)
            val slots =
                listOf(
                    createSlot(1L, stadium, LocalDate.of(2024, 12, 25)),
                    createSlot(2L, stadium, LocalDate.of(2024, 12, 25)),
                )
            every { stadiumAdminService.createSlots(any()) } returns slots

            // when & then
            mockMvc
                .perform(
                    post("/api/backoffice/stadiums/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
        }
    }

    @Nested
    @DisplayName("PUT /api/backoffice/stadiums/{id}/deactivate")
    inner class DeactivateStadium {
        @Test
        fun `should deactivate stadium successfully`() {
            // given
            val stadium = createStadium(1L, "잠실 야구장", 37.5121, 127.0717).apply { deactivate() }
            every { stadiumAdminService.deactivateStadium(1L) } returns stadium

            // when & then
            mockMvc
                .perform(put("/api/backoffice/stadiums/1/deactivate"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(false))
        }
    }

    @Nested
    @DisplayName("PUT /api/backoffice/stadiums/{id}/activate")
    inner class ActivateStadium {
        @Test
        fun `should activate stadium successfully`() {
            // given
            val stadium = createStadium(1L, "잠실 야구장", 37.5121, 127.0717)
            every { stadiumAdminService.activateStadium(1L) } returns stadium

            // when & then
            mockMvc
                .perform(put("/api/backoffice/stadiums/1/activate"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(true))
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
}
