package com.nextup.backoffice.controller.stadium

import com.nextup.backoffice.dto.stadium.AcceptBookingTransferRequest
import com.nextup.backoffice.dto.stadium.BookingTransferResponse
import com.nextup.backoffice.dto.stadium.CancelBookingTransferRequest
import com.nextup.backoffice.dto.stadium.CreateBookingTransferRequest
import com.nextup.backoffice.dto.stadium.toResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.stadium.BookingTransferService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 구장 예약 양도 Controller
 *
 * 긴급 양도 등록, 양수 수락, 양도 취소, 양도 가능 목록 조회 기능을 제공합니다.
 */
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/backoffice")
class BookingTransferController(
    private val bookingTransferService: BookingTransferService,
) {
    /**
     * 예약 양도를 등록합니다.
     *
     * POST /api/backoffice/bookings/{bookingId}/transfer
     */
    @PostMapping("/bookings/{bookingId}/transfer")
    fun createTransfer(
        @PathVariable bookingId: Long,
        @RequestBody @Valid request: CreateBookingTransferRequest,
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<ApiResponse<BookingTransferResponse>> {
        val transfer =
            bookingTransferService.createTransfer(
                bookingId = bookingId,
                teamId = request.sellerTeamId,
                price = request.transferPrice,
                message = request.message,
                expiresAt =
                    request.expiresAt
                        ?: java.time.Instant.now()
                            .plusSeconds(BookingTransferService.DEFAULT_EXPIRY_SECONDS),
                userId = userId,
            )
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(transfer.toResponse()))
    }

    /**
     * 양도 가능한 예약 목록을 조회합니다.
     *
     * GET /api/backoffice/transfers/available
     */
    @GetMapping("/transfers/available")
    fun getAvailableTransfers(): ApiResponse<List<BookingTransferResponse>> {
        val transfers = bookingTransferService.getAvailableTransfers()
        return ApiResponse.success(transfers.map { it.toResponse() })
    }

    /**
     * 예약 양도를 수락합니다.
     *
     * POST /api/backoffice/transfers/{transferId}/accept
     */
    @PostMapping("/transfers/{transferId}/accept")
    fun acceptTransfer(
        @PathVariable transferId: Long,
        @RequestBody @Valid request: AcceptBookingTransferRequest,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<BookingTransferResponse> {
        val transfer =
            bookingTransferService.acceptTransfer(
                transferId = transferId,
                buyerTeamId = request.buyerTeamId,
                userId = userId,
            )
        return ApiResponse.success(transfer.toResponse())
    }

    /**
     * 예약 양도를 취소합니다.
     *
     * DELETE /api/backoffice/transfers/{transferId}
     */
    @DeleteMapping("/transfers/{transferId}")
    fun cancelTransfer(
        @PathVariable transferId: Long,
        @RequestBody @Valid request: CancelBookingTransferRequest,
    ): ApiResponse<BookingTransferResponse> {
        val transfer =
            bookingTransferService.cancelTransfer(
                transferId = transferId,
                teamId = request.teamId,
            )
        return ApiResponse.success(transfer.toResponse())
    }
}
