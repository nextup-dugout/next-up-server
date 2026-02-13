package com.nextup.api.controller.attendance

import com.nextup.api.dto.attendance.NudgeRequest
import com.nextup.api.dto.attendance.NudgeResponse
import com.nextup.api.dto.common.ApiResponse
import com.nextup.core.service.attendance.NudgeService
import org.springframework.web.bind.annotation.*

/**
 * 출석 관리 API Controller
 *
 * 경기 출석 투표 및 재촉 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/games/{gameId}/attendance")
class AttendanceController(
    private val nudgeService: NudgeService,
) {
    /**
     * 미투표자에게 출석 투표 독려 알림을 전송합니다.
     *
     * @param gameId 경기 ID
     * @param request 재촉 요청 (선택적 메시지 포함)
     * @return 알림 전송 결과
     */
    @PostMapping("/nudge")
    fun nudgeNonVoters(
        @PathVariable gameId: Long,
        @RequestBody request: NudgeRequest = NudgeRequest(),
    ): ApiResponse<NudgeResponse> {
        val result = nudgeService.nudgeNonVoters(gameId, request.message)
        return ApiResponse.success(NudgeResponse.from(result))
    }
}
