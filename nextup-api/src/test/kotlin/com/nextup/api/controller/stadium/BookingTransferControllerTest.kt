package com.nextup.api.controller.stadium

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nextup.api.dto.stadium.AcceptBookingTransferApiRequest
import com.nextup.api.dto.stadium.CreateBookingTransferApiRequest
import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.BookingTransferForbiddenException
import com.nextup.common.exception.BookingTransferNotFoundException
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.math.BigDecimal
import java.time.Instant

@DisplayName("BookingTransferController (사용자 API)")
class BookingTransferControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var bookingTransferService: BookingTransferService
    private lateinit var objectMapper: ObjectMapper

    private val authenticatedUserId = 100L

    @BeforeEach
    fun setUp() {
        bookingTransferService = mockk()
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(1L, null, emptyList())

        val controller = BookingTransferController(bookingTransferService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                    AuthenticationPrincipalResolver(authenticatedUserId),
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
    @DisplayName("POST /api/v1/booking-transfers")
    inner class RequestTransfer {
        @Test
        fun `should create transfer and return 201`() {
            // given
            val transfer = createTransfer()
            val request =
                CreateBookingTransferApiRequest(
                    bookingId = 1L,
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
                    post("/api/v1/booking-transfers")
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
                CreateBookingTransferApiRequest(
                    bookingId = 2L,
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
                    post("/api/v1/booking-transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/booking-transfers/{transferId}/accept")
    inner class AcceptTransfer {
        @Test
        fun `should accept transfer successfully`() {
            // given
            val transfer =
                createTransfer(
                    sellerTeamId = 10L,
                    status = TransferStatus.ACCEPTED,
                    buyerTeamId = 20L,
                )
            val request = AcceptBookingTransferApiRequest(buyerTeamId = 20L)
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
                    patch("/api/v1/booking-transfers/1/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.buyerTeamId").value(20))
        }

        @Test
        fun `should return 404 when transfer not found`() {
            // given
            val request = AcceptBookingTransferApiRequest(buyerTeamId = 20L)
            every {
                bookingTransferService.acceptTransfer(
                    transferId = 999L,
                    buyerTeamId = 20L,
                    userId = any(),
                )
            } throws BookingTransferNotFoundException(999L)

            // when & then
            mockMvc
                .perform(
                    patch("/api/v1/booking-transfers/999/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/booking-transfers/{transferId}/reject")
    inner class RejectTransfer {
        @Test
        fun `should reject transfer successfully`() {
            // given
            val transfer =
                createTransfer(
                    sellerTeamId = 10L,
                    status = TransferStatus.CANCELLED,
                )
            every {
                bookingTransferService.cancelTransfer(
                    transferId = 1L,
                    teamId = 10L,
                )
            } returns transfer

            // when & then
            mockMvc
                .perform(
                    patch("/api/v1/booking-transfers/1/reject")
                        .param("teamId", "10"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
        }

        @Test
        fun `should return 403 when team is not the seller`() {
            // given
            every {
                bookingTransferService.cancelTransfer(
                    transferId = 1L,
                    teamId = 99L,
                )
            } throws
                BookingTransferForbiddenException(
                    "Only the seller team can cancel the transfer. transferId=1",
                )

            // when & then
            mockMvc
                .perform(
                    patch("/api/v1/booking-transfers/1/reject")
                        .param("teamId", "99"),
                ).andExpect(status().isForbidden)
                .andExpect(jsonPath("$.success").value(false))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/booking-transfers/available")
    inner class GetAvailableTransfers {
        @Test
        fun `should return list of available transfers`() {
            // given
            val transfer1 = createTransfer(sellerTeamId = 10L)
            val transfer2 = createTransfer(sellerTeamId = 20L)
            every { bookingTransferService.getAvailableTransfers() } returns
                listOf(transfer1, transfer2)

            // when & then
            mockMvc
                .perform(get("/api/v1/booking-transfers/available"))
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
                .perform(get("/api/v1/booking-transfers/available"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(0))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/booking-transfers/my")
    inner class GetMyTransfers {
        @Test
        fun `should return transfers for the team`() {
            // given
            val sellerTransfer = createTransfer(sellerTeamId = 10L)
            val buyerTransfer =
                createTransfer(
                    sellerTeamId = 20L,
                    status = TransferStatus.ACCEPTED,
                    buyerTeamId = 10L,
                )
            every { bookingTransferService.getTransfersByTeamId(10L) } returns
                listOf(sellerTransfer, buyerTransfer)

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/booking-transfers/my")
                        .param("teamId", "10"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
        }

        @Test
        fun `should return empty list when no transfers for team`() {
            // given
            every { bookingTransferService.getTransfersByTeamId(99L) } returns emptyList()

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/booking-transfers/my")
                        .param("teamId", "99"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(0))
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
