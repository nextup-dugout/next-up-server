package com.nextup.api.controller.stadium

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
        fromTeamId: Long = 10L,
        toTeamId: Long = 20L,
        status: TransferStatus = TransferStatus.PENDING,
    ): BookingTransfer {
        val transfer =
            BookingTransfer.create(
                bookingId = 1L,
                fromTeamId = fromTeamId,
                toTeamId = toTeamId,
                message = "양도합니다",
            )
        if (status == TransferStatus.ACCEPTED) {
            transfer.accept()
        } else if (status == TransferStatus.REJECTED) {
            transfer.reject()
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
                    fromTeamId = 10L,
                    toTeamId = 20L,
                    message = "양도합니다",
                )
            every {
                bookingTransferService.requestTransfer(
                    bookingId = 1L,
                    fromTeamId = 10L,
                    toTeamId = 20L,
                    message = "양도합니다",
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
                .andExpect(jsonPath("$.data.fromTeamId").value(10))
                .andExpect(jsonPath("$.data.toTeamId").value(20))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/booking-transfers/{transferId}/accept")
    inner class AcceptTransfer {
        @Test
        fun `should accept transfer successfully`() {
            // given
            val transfer = createTransfer(status = TransferStatus.ACCEPTED)
            every {
                bookingTransferService.acceptTransfer(
                    transferId = 1L,
                    userId = any(),
                )
            } returns transfer

            // when & then
            mockMvc
                .perform(
                    patch("/api/v1/booking-transfers/1/accept"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
        }

        @Test
        fun `should return 404 when transfer not found`() {
            // given
            every {
                bookingTransferService.acceptTransfer(
                    transferId = 999L,
                    userId = any(),
                )
            } throws BookingTransferNotFoundException(999L)

            // when & then
            mockMvc
                .perform(
                    patch("/api/v1/booking-transfers/999/accept"),
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
            val transfer = createTransfer(status = TransferStatus.REJECTED)
            every {
                bookingTransferService.rejectTransfer(
                    transferId = 1L,
                    userId = any(),
                )
            } returns transfer

            // when & then
            mockMvc
                .perform(
                    patch("/api/v1/booking-transfers/1/reject"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
        }

        @Test
        fun `should return 403 when user is not a member of target team`() {
            // given
            every {
                bookingTransferService.rejectTransfer(
                    transferId = 1L,
                    userId = any(),
                )
            } throws
                BookingTransferForbiddenException(
                    "User 100 is not a member of team 20",
                )

            // when & then
            mockMvc
                .perform(
                    patch("/api/v1/booking-transfers/1/reject"),
                ).andExpect(status().isForbidden)
                .andExpect(jsonPath("$.success").value(false))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/booking-transfers/sent")
    inner class GetSentTransfers {
        @Test
        fun `should return list of sent transfers`() {
            // given
            val transfer1 = createTransfer(fromTeamId = 10L)
            val transfer2 = createTransfer(fromTeamId = 10L, toTeamId = 30L)
            every { bookingTransferService.getSentTransfers(10L) } returns
                listOf(transfer1, transfer2)

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/booking-transfers/sent")
                        .param("teamId", "10"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
        }

        @Test
        fun `should return empty list when no sent transfers`() {
            // given
            every { bookingTransferService.getSentTransfers(99L) } returns emptyList()

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/booking-transfers/sent")
                        .param("teamId", "99"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(0))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/booking-transfers/received")
    inner class GetReceivedTransfers {
        @Test
        fun `should return list of received transfers`() {
            // given
            val transfer = createTransfer(fromTeamId = 30L, toTeamId = 10L)
            every { bookingTransferService.getReceivedTransfers(10L) } returns listOf(transfer)

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/booking-transfers/received")
                        .param("teamId", "10"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(1))
        }

        @Test
        fun `should return empty list when no received transfers`() {
            // given
            every { bookingTransferService.getReceivedTransfers(99L) } returns emptyList()

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/booking-transfers/received")
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
