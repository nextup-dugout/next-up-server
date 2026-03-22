package com.nextup.api.controller.stadium

import com.nextup.api.dto.stadium.AcceptBookingTransferApiRequest
import com.nextup.api.dto.stadium.BookingTransferResponse
import com.nextup.api.dto.stadium.CreateBookingTransferApiRequest
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.stadium.BookingTransferService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 예약 양도 사용자 API Controller
 *
 * 일반 사용자가 예약 양도를 요청/수락/거절/조회할 수 있는 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/booking-transfers")
class BookingTransferController(
    private val bookingTransferService: BookingTransferService,
) {
    /**
     * 예약 양도를 등록합니다.
     *
     * POST /api/v1/booking-transfers
     */
    @PostMapping
    @PreAuthorize("@teamSecurity.isOwnerOrManager(#request.sellerTeamId, authentication.principal)")
    fun requestTransfer(
        @AuthenticationPrincipal userId: Long,
        @RequestBody @Valid request: CreateBookingTransferApiRequest,
    ): ResponseEntity<ApiResponse<BookingTransferResponse>> {
        val transfer =
            bookingTransferService.createTransfer(
                bookingId = request.bookingId,
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
            .body(ApiResponse.success(BookingTransferResponse.from(transfer)))
    }

    /**
     * 예약 양도를 수락합니다.
     *
     * PATCH /api/v1/booking-transfers/{transferId}/accept
     */
    @PatchMapping("/{transferId}/accept")
    @PreAuthorize("@teamSecurity.isOwnerOrManager(#request.buyerTeamId, authentication.principal)")
    fun acceptTransfer(
        @PathVariable transferId: Long,
        @AuthenticationPrincipal userId: Long,
        @RequestBody @Valid request: AcceptBookingTransferApiRequest,
    ): ApiResponse<BookingTransferResponse> {
        val transfer =
            bookingTransferService.acceptTransfer(
                transferId = transferId,
                buyerTeamId = request.buyerTeamId,
                userId = userId,
            )
        return ApiResponse.success(BookingTransferResponse.from(transfer))
    }

    /**
     * 예약 양도를 취소(거절)합니다.
     *
     * PATCH /api/v1/booking-transfers/{transferId}/reject
     */
    @PatchMapping("/{transferId}/reject")
    @PreAuthorize("@teamSecurity.isMember(#teamId, authentication.principal)")
    fun rejectTransfer(
        @PathVariable transferId: Long,
        @RequestParam teamId: Long,
    ): ApiResponse<BookingTransferResponse> {
        val transfer =
            bookingTransferService.cancelTransfer(
                transferId = transferId,
                teamId = teamId,
            )
        return ApiResponse.success(BookingTransferResponse.from(transfer))
    }

    /**
     * 양도 가능한 예약 목록을 조회합니다.
     *
     * GET /api/v1/booking-transfers/available
     */
    @GetMapping("/available")
    fun getAvailableTransfers(): ApiResponse<List<BookingTransferResponse>> {
        val transfers = bookingTransferService.getAvailableTransfers()
        return ApiResponse.success(transfers.map { BookingTransferResponse.from(it) })
    }

    /**
     * 내 팀의 양도 목록을 조회합니다 (판매/구매 포함).
     *
     * GET /api/v1/booking-transfers/my?teamId={teamId}
     */
    @GetMapping("/my")
    @PreAuthorize("@teamSecurity.isMember(#teamId, authentication.principal)")
    fun getMyTransfers(
        @RequestParam teamId: Long,
    ): ApiResponse<List<BookingTransferResponse>> {
        val transfers = bookingTransferService.getTransfersByTeamId(teamId)
        return ApiResponse.success(transfers.map { BookingTransferResponse.from(it) })
    }
}
