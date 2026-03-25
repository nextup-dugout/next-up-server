package com.nextup.api.controller.stadium

import com.nextup.api.dto.stadium.BookingDetailResponse
import com.nextup.api.dto.stadium.BookingResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.domain.stadium.BookingStatus
import com.nextup.core.service.stadium.StadiumService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 구장 예약 관리 API Controller (일반 사용자용)
 */
@RestController
@RequestMapping("/api/v1/bookings")
class BookingController(
    private val stadiumService: StadiumService,
) {
    /**
     * 예약 상세 정보를 조회합니다.
     */
    @GetMapping("/{id}")
    fun getBookingDetail(
        @PathVariable id: Long,
    ): ApiResponse<BookingDetailResponse> {
        val booking = stadiumService.getBookingById(id)
        return ApiResponse.success(BookingDetailResponse.from(booking))
    }

    /**
     * 팀의 예약 목록을 조회합니다.
     */
    @GetMapping("/team/{teamId}")
    fun getTeamBookings(
        @PathVariable teamId: Long,
        @RequestParam(required = false) status: BookingStatus?,
    ): ApiResponse<List<BookingResponse>> {
        val bookings = stadiumService.getTeamBookings(teamId, status)
        return ApiResponse.success(bookings.map { BookingResponse.from(it) })
    }

    /**
     * 예약을 취소합니다.
     * 예약 생성자만 취소할 수 있습니다 (소유권 검증).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun cancelBooking(
        @PathVariable id: Long,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<BookingResponse> {
        val booking = stadiumService.cancelBooking(id, userId)
        return ApiResponse.success(BookingResponse.from(booking))
    }

    /**
     * 예약을 완료 처리합니다.
     * 예약 생성자만 완료 처리할 수 있습니다 (소유권 검증).
     */
    @PutMapping("/{id}/complete")
    @PreAuthorize("isAuthenticated()")
    fun completeBooking(
        @PathVariable id: Long,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<BookingResponse> {
        val booking = stadiumService.completeBooking(id, userId)
        return ApiResponse.success(BookingResponse.from(booking))
    }
}
