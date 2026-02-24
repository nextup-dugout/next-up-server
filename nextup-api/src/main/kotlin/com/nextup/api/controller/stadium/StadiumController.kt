package com.nextup.api.controller.stadium

import com.nextup.api.dto.stadium.BookingResponse
import com.nextup.api.dto.stadium.StadiumResponse
import com.nextup.api.dto.stadium.StadiumSlotResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.stadium.StadiumService
import com.nextup.core.service.stadium.dto.BookSlotRequest
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

/**
 * 구장 조회 및 예약 API Controller (일반 사용자용)
 */
@RestController
@RequestMapping("/api/v1/stadiums")
class StadiumController(
    private val stadiumService: StadiumService,
) {
    /**
     * 위치 기반으로 근처 구장을 검색합니다.
     */
    @GetMapping("/search")
    fun searchStadiums(
        @RequestParam latitude: Double,
        @RequestParam longitude: Double,
        @RequestParam(defaultValue = "10.0") radiusKm: Double,
    ): ApiResponse<List<StadiumResponse>> {
        val stadiums = stadiumService.searchStadiums(latitude, longitude, radiusKm)
        return ApiResponse.success(stadiums.map { StadiumResponse.from(it) })
    }

    /**
     * 구장 상세 정보를 조회합니다.
     */
    @GetMapping("/{id}")
    fun getStadium(
        @PathVariable id: Long,
    ): ApiResponse<StadiumResponse> {
        val stadium = stadiumService.getById(id)
        return ApiResponse.success(StadiumResponse.from(stadium))
    }

    /**
     * 구장의 특정 날짜에 사용 가능한 슬롯을 조회합니다.
     */
    @GetMapping("/{id}/slots")
    fun getAvailableSlots(
        @PathVariable id: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    ): ApiResponse<List<StadiumSlotResponse>> {
        val slots = stadiumService.getAvailableSlots(id, date)
        return ApiResponse.success(slots.map { StadiumSlotResponse.from(it) })
    }

    /**
     * 구장 슬롯을 예약합니다.
     */
    @PostMapping("/book")
    fun bookSlot(
        @RequestBody @Valid request: BookSlotRequest,
    ): ApiResponse<BookingResponse> {
        val booking = stadiumService.bookSlot(request)
        return ApiResponse.success(BookingResponse.from(booking))
    }
}
