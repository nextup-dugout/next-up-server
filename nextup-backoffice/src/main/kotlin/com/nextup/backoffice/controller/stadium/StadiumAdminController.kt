package com.nextup.backoffice.controller.stadium

import com.nextup.backoffice.dto.common.ApiResponse
import com.nextup.backoffice.dto.stadium.CreateSlotRequest
import com.nextup.backoffice.dto.stadium.CreateStadiumRequest
import com.nextup.backoffice.dto.stadium.StadiumResponse
import com.nextup.backoffice.dto.stadium.StadiumSlotResponse
import com.nextup.backoffice.dto.stadium.UpdateStadiumRequest
import com.nextup.core.service.stadium.StadiumAdminService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

/**
 * 구장 관리 API Controller (백오피스용)
 */
@RestController
@RequestMapping("/api/backoffice/stadiums")
class StadiumAdminController(
    private val stadiumAdminService: StadiumAdminService,
) {
    /**
     * 구장을 생성합니다.
     */
    @PostMapping
    fun createStadium(
        @RequestBody @Valid request: CreateStadiumRequest,
    ): ApiResponse<StadiumResponse> {
        val stadium =
            stadiumAdminService.createStadium(
                com.nextup.core.service.stadium.dto.CreateStadiumRequest(
                    name = request.name,
                    address = request.address,
                    latitude = request.latitude,
                    longitude = request.longitude,
                    capacity = request.capacity,
                    facilities = request.facilities,
                    contactInfo = request.contactInfo,
                    imageUrls = request.imageUrls,
                ),
            )
        return ApiResponse.success(StadiumResponse.from(stadium))
    }

    /**
     * 구장 정보를 수정합니다.
     */
    @PutMapping("/{id}")
    fun updateStadium(
        @PathVariable id: Long,
        @RequestBody @Valid request: UpdateStadiumRequest,
    ): ApiResponse<StadiumResponse> {
        val stadium =
            stadiumAdminService.updateStadium(
                id = id,
                address = request.address,
                latitude = request.latitude,
                longitude = request.longitude,
                capacity = request.capacity,
                facilities = request.facilities,
                contactInfo = request.contactInfo,
                imageUrls = request.imageUrls,
            )
        return ApiResponse.success(StadiumResponse.from(stadium))
    }

    /**
     * 구장 슬롯을 생성합니다.
     */
    @PostMapping("/slots")
    fun createSlots(
        @RequestBody @Valid requests: List<CreateSlotRequest>,
    ): ApiResponse<List<StadiumSlotResponse>> {
        val coreRequests =
            requests.map {
                com.nextup.core.service.stadium.dto.CreateSlotRequest(
                    stadiumId = it.stadiumId,
                    date = it.date,
                    startTime = it.startTime,
                    endTime = it.endTime,
                    price = it.price,
                )
            }
        val slots = stadiumAdminService.createSlots(coreRequests)
        return ApiResponse.success(slots.map { StadiumSlotResponse.from(it) })
    }

    /**
     * 구장을 비활성화합니다.
     */
    @PutMapping("/{id}/deactivate")
    fun deactivateStadium(
        @PathVariable id: Long,
    ): ApiResponse<StadiumResponse> {
        val stadium = stadiumAdminService.deactivateStadium(id)
        return ApiResponse.success(StadiumResponse.from(stadium))
    }

    /**
     * 구장을 활성화합니다.
     */
    @PutMapping("/{id}/activate")
    fun activateStadium(
        @PathVariable id: Long,
    ): ApiResponse<StadiumResponse> {
        val stadium = stadiumAdminService.activateStadium(id)
        return ApiResponse.success(StadiumResponse.from(stadium))
    }
}
