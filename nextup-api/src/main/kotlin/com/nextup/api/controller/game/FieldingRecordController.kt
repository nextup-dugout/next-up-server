package com.nextup.api.controller.game

import com.nextup.api.dto.game.FieldingRecordResponse
import com.nextup.api.mapper.game.toFieldingResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.game.FieldingRecordService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 수비 기록 조회 Controller (일반 사용자용)
 *
 * 경기별 수비 기록을 조회하는 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/games/{gameId}/fielding-records")
class FieldingRecordController(
    private val fieldingRecordService: FieldingRecordService,
) {
    /**
     * 경기의 모든 수비 기록을 조회합니다.
     *
     * GET /api/v1/games/{gameId}/fielding-records
     */
    @GetMapping
    fun getFieldingRecordsByGame(
        @PathVariable gameId: Long,
    ): ApiResponse<List<FieldingRecordResponse>> {
        val records = fieldingRecordService.getAllByGameId(gameId)
        return ApiResponse.success(records.toFieldingResponse())
    }
}
