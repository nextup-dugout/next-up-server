package com.nextup.backoffice.controller.stadium

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nextup.backoffice.dto.stadium.AcceptBookingTransferRequest
import com.nextup.backoffice.dto.stadium.CancelBookingTransferRequest
import com.nextup.backoffice.dto.stadium.CreateBookingTransferRequest
import com.nextup.core.domain.stadium.BookingTransfer
import com.nextup.core.domain.stadium.TransferStatus
import com.nextup.core.service.stadium.BookingTransferService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.math.BigDecimal
import java.time.Instant

@DisplayName("BookingTransferController")
class BookingTransferControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var bookingTransferService: BookingTransferService
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        bookingTransferService = mockk()
        val controller = BookingTransferController(bookingTransferService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(
                    AuthenticationPrincipalResolver(100L),
                )
                .build()
        objectMapper =
            jacksonObjectMapper().registerModule(JavaTimeModule())
    }

    private fun createTransfer(
        sellerTeamId: Long = 10L,
        status: TransferStatus = TransferStatus.OPEN,
        buyerTeamId: Long? = null,
    ): BookingTransfer {
        val transfer =
            BookingTransfer.create(
                bookingId = 1L,
                sellerTeamId = sellerTeamId,
                transferPrice = BigDecimal("50000"),
                message = "양도합니다",
                expiresAt = Instant.now().plusSeconds(3600),
            )
        if (status == TransferStatus.ACCEPTED && buyerTeamId != null) {
            transfer.accept(buyerTeamId)
        } else if (status == TransferStatus.CANCELLED) {
            transfer.cancel()
        } else if (status == TransferStatus.EXPIRED) {
            transfer.expire()
        }
        return transfer
    }

    @Nested
    @DisplayName("POST /api/backoffice/bookings/{bookingId}/transfer")
    inner class CreateTransfer {
        @Test
        fun `should create transfer and return 201`() {
            // given
            val transfer = createTransfer()
            val request =
                CreateBookingTransferRequest(
                    sellerTeamId = 10L,
                    transferPrice = BigDecimal("50000"),
                    message = "양도합니다",
                    expiresAt = null,
                )
            every {
                bookingTransferService.createTransfer(
                    bookingId = 1L,
                    teamId = 10L,
                    price = BigDecimal("50000"),
                    message = "양도합니다",
                    expiresAt = any(),
                    userId = any(),
                )
            } returns transfer

            // when & then
            mockMvc
                .perform(
                    post("/api/backoffice/bookings/1/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sellerTeamId").value(10))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
        }

        @Test
        fun `should create transfer with custom expiresAt`() {
            // given
            val expiresAt = Instant.now().plusSeconds(7200)
            val transfer = createTransfer()
            val request =
                CreateBookingTransferRequest(
                    sellerTeamId = 10L,
                    transferPrice = null,
                    message = null,
                    expiresAt = expiresAt,
                )
            every {
                bookingTransferService.createTransfer(
                    bookingId = 2L,
                    teamId = 10L,
                    price = null,
                    message = null,
                    expiresAt = expiresAt,
                    userId = any(),
                )
            } returns transfer

            // when & then
            mockMvc
                .perform(
                    post("/api/backoffice/bookings/2/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
        }
    }

    @Nested
    @DisplayName("GET /api/backoffice/transfers/available")
    inner class GetAvailableTransfers {
        @Test
        fun `should return list of available transfers`() {
            // given
            val transfer1 = createTransfer(sellerTeamId = 10L)
            val transfer2 = createTransfer(sellerTeamId = 20L)
            every { bookingTransferService.getAvailableTransfers() } returns listOf(transfer1, transfer2)

            // when & then
            mockMvc
                .perform(get("/api/backoffice/transfers/available"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
        }

        @Test
        fun `should return empty list when no available transfers`() {
            // given
            every { bookingTransferService.getAvailableTransfers() } returns emptyList()

            // when & then
            mockMvc
                .perform(get("/api/backoffice/transfers/available"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(0))
        }
    }

    @Nested
    @DisplayName("POST /api/backoffice/transfers/{transferId}/accept")
    inner class AcceptTransfer {
        @Test
        fun `should accept transfer successfully`() {
            // given
            val transfer = createTransfer(sellerTeamId = 10L, status = TransferStatus.ACCEPTED, buyerTeamId = 20L)
            val request = AcceptBookingTransferRequest(buyerTeamId = 20L)
            every {
                bookingTransferService.acceptTransfer(
                    transferId = 1L,
                    buyerTeamId = 20L,
                    userId = any(),
                )
            } returns transfer

            // when & then
            mockMvc
                .perform(
                    post("/api/backoffice/transfers/1/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.buyerTeamId").value(20))
        }
    }

    @Nested
    @DisplayName("DELETE /api/backoffice/transfers/{transferId}")
    inner class CancelTransfer {
        @Test
        fun `should cancel transfer successfully`() {
            // given
            val transfer = createTransfer(sellerTeamId = 10L, status = TransferStatus.CANCELLED)
            val request = CancelBookingTransferRequest(teamId = 10L)
            every {
                bookingTransferService.cancelTransfer(
                    transferId = 1L,
                    teamId = 10L,
                )
            } returns transfer

            // when & then
            mockMvc
                .perform(
                    delete("/api/backoffice/transfers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
        }
    }
}

/**
 * 테스트용 @AuthenticationPrincipal 리졸버
 */
private class AuthenticationPrincipalResolver(
    private val userId: Long,
) : org.springframework.web.method.support.HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: org.springframework.core.MethodParameter): Boolean =
        parameter.hasParameterAnnotation(
            org.springframework.security.core.annotation.AuthenticationPrincipal::class.java,
        )

    override fun resolveArgument(
        parameter: org.springframework.core.MethodParameter,
        mavContainer: org.springframework.web.method.support.ModelAndViewContainer?,
        webRequest: org.springframework.web.context.request.NativeWebRequest,
        binderFactory: org.springframework.web.bind.support.WebDataBinderFactory?,
    ): Any = userId
}
