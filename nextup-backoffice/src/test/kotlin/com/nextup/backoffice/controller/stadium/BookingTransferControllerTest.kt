package com.nextup.backoffice.controller.stadium

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@DisplayName("BookingTransferController (관리자)")
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
    @DisplayName("POST /api/backoffice/bookings/{bookingId}/transfer")
    inner class CreateTransfer {
        @Test
        fun `should create transfer and return 201`() {
            // given
            val transfer = createTransfer()
            val request =
                CreateBookingTransferRequest(
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
                    post("/api/backoffice/bookings/1/transfer")
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
    @DisplayName("GET /api/backoffice/transfers/sent")
    inner class GetSentTransfers {
        @Test
        fun `should return list of sent transfers`() {
            // given
            val transfer1 = createTransfer(fromTeamId = 10L)
            val transfer2 = createTransfer(fromTeamId = 10L, toTeamId = 30L)
            every { bookingTransferService.getSentTransfers(10L) } returns listOf(transfer1, transfer2)

            // when & then
            mockMvc
                .perform(
                    get("/api/backoffice/transfers/sent")
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
                    get("/api/backoffice/transfers/sent")
                        .param("teamId", "99"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(0))
        }
    }

    @Nested
    @DisplayName("PATCH /api/backoffice/transfers/{transferId}/accept")
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
                    patch("/api/backoffice/transfers/1/accept"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
        }
    }

    @Nested
    @DisplayName("PATCH /api/backoffice/transfers/{transferId}/reject")
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
                    patch("/api/backoffice/transfers/1/reject"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
        }
    }

    @Nested
    @DisplayName("GET /api/backoffice/transfers/received")
    inner class GetReceivedTransfers {
        @Test
        fun `should return list of received transfers`() {
            // given
            val transfer = createTransfer(fromTeamId = 30L, toTeamId = 10L)
            every { bookingTransferService.getReceivedTransfers(10L) } returns listOf(transfer)

            // when & then
            mockMvc
                .perform(
                    get("/api/backoffice/transfers/received")
                        .param("teamId", "10"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(1))
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
