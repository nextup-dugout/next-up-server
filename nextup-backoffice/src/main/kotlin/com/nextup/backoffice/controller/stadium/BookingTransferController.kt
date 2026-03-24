package com.nextup.backoffice.controller.stadium

import com.nextup.backoffice.dto.stadium.BookingTransferResponse
import com.nextup.backoffice.dto.stadium.CreateBookingTransferRequest
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
 * 구장 예약 양도 관리자 Controller
 *
 * 관리자가 예약 양도를 등록/수락/거절/조회할 수 있는 API를 제공합니다.
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
            bookingTransferService.requestTransfer(
                bookingId = bookingId,
                fromTeamId = request.fromTeamId,
                toTeamId = request.toTeamId,
                message = request.message,
                userId = userId,
            )
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(BookingTransferResponse.from(transfer)))
    }

    /**
     * 예약 양도를 수락합니다.
     *
     * PATCH /api/backoffice/transfers/{transferId}/accept
     */
    @PatchMapping("/transfers/{transferId}/accept")
    fun acceptTransfer(
        @PathVariable transferId: Long,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<BookingTransferResponse> {
        val transfer =
            bookingTransferService.acceptTransfer(
                transferId = transferId,
                userId = userId,
            )
        return ApiResponse.success(BookingTransferResponse.from(transfer))
    }

    /**
     * 예약 양도를 거절합니다.
     *
     * PATCH /api/backoffice/transfers/{transferId}/reject
     */
    @PatchMapping("/transfers/{transferId}/reject")
    fun rejectTransfer(
        @PathVariable transferId: Long,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<BookingTransferResponse> {
        val transfer =
            bookingTransferService.rejectTransfer(
                transferId = transferId,
                userId = userId,
            )
        return ApiResponse.success(BookingTransferResponse.from(transfer))
    }

    /**
     * 특정 팀의 보낸 양도 목록을 조회합니다.
     *
     * GET /api/backoffice/transfers/sent?teamId={teamId}
     */
    @GetMapping("/transfers/sent")
    fun getSentTransfers(
        @RequestParam teamId: Long,
    ): ApiResponse<List<BookingTransferResponse>> {
        val transfers = bookingTransferService.getSentTransfers(teamId)
        return ApiResponse.success(transfers.map { BookingTransferResponse.from(it) })
    }

    /**
     * 특정 팀의 받은 양도 목록을 조회합니다.
     *
     * GET /api/backoffice/transfers/received?teamId={teamId}
     */
    @GetMapping("/transfers/received")
    fun getReceivedTransfers(
        @RequestParam teamId: Long,
    ): ApiResponse<List<BookingTransferResponse>> {
        val transfers = bookingTransferService.getReceivedTransfers(teamId)
        return ApiResponse.success(transfers.map { BookingTransferResponse.from(it) })
    }
}
