package com.nextup.backoffice.controller.correction

import com.nextup.backoffice.dto.correction.BattingRecordCorrectionResponse
import com.nextup.backoffice.dto.correction.CorrectBattingRecordRequest
import com.nextup.backoffice.dto.correction.CorrectPitchingRecordRequest
import com.nextup.backoffice.dto.correction.PitchingRecordCorrectionResponse
import com.nextup.backoffice.dto.correction.RecordCorrectionHistoryResponse
import com.nextup.backoffice.dto.correction.toCorrectionResponse
import com.nextup.backoffice.dto.correction.toHistoryResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.game.correction.BattingCorrectionRequest
import com.nextup.core.service.game.correction.PitchingCorrectionRequest
import com.nextup.core.service.game.correction.RecordCorrectionService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 기록 정정 관리자 API Controller
 *
 * 경기 타격/투수 기록을 관리자 권한으로 정정합니다.
 * 경기 상태(FINISHED 포함)와 무관하게 정정 가능합니다.
 */
@RestController
@RequestMapping("/api/backoffice/games/{gameId}")
class RecordCorrectionAdminController(
    private val recordCorrectionService: RecordCorrectionService,
) {
    /**
     * 타격 기록을 정정합니다.
     *
     * @param gameId 경기 ID
     * @param recordId 타격 기록 ID
     * @param request 정정 요청 정보
     */
    @PutMapping("/batting-records/{recordId}")
    fun correctBattingRecord(
        @PathVariable gameId: Long,
        @PathVariable recordId: Long,
        @Valid @RequestBody request: CorrectBattingRecordRequest,
    ): ApiResponse<BattingRecordCorrectionResponse> {
        val correctionRequest =
            BattingCorrectionRequest(
                adminUserId = request.adminUserId,
                fieldName = request.fieldName,
                newValue = request.newValue,
                reason = request.reason,
            )
        val updatedRecord = recordCorrectionService.correctBattingRecord(gameId, recordId, correctionRequest)
        return ApiResponse.success(updatedRecord.toCorrectionResponse())
    }

    /**
     * 투수 기록을 정정합니다.
     *
     * @param gameId 경기 ID
     * @param recordId 투수 기록 ID
     * @param request 정정 요청 정보
     */
    @PutMapping("/pitching-records/{recordId}")
    fun correctPitchingRecord(
        @PathVariable gameId: Long,
        @PathVariable recordId: Long,
        @Valid @RequestBody request: CorrectPitchingRecordRequest,
    ): ApiResponse<PitchingRecordCorrectionResponse> {
        val correctionRequest =
            PitchingCorrectionRequest(
                adminUserId = request.adminUserId,
                fieldName = request.fieldName,
                newValue = request.newValue,
                reason = request.reason,
            )
        val updatedRecord = recordCorrectionService.correctPitchingRecord(gameId, recordId, correctionRequest)
        return ApiResponse.success(updatedRecord.toCorrectionResponse())
    }

    /**
     * 경기의 기록 정정 이력을 조회합니다.
     *
     * @param gameId 경기 ID
     */
    @GetMapping("/corrections")
    fun getCorrectionHistory(
        @PathVariable gameId: Long,
    ): ApiResponse<List<RecordCorrectionHistoryResponse>> {
        val history = recordCorrectionService.getCorrectionHistory(gameId)
        return ApiResponse.success(history.map { it.toHistoryResponse() })
    }
}
