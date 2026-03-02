package com.nextup.backoffice.controller.player

import com.nextup.backoffice.dto.player.PlayerBulkImportRequest
import com.nextup.backoffice.dto.player.PlayerBulkImportResponse
import com.nextup.backoffice.service.PlayerBulkImportService
import com.nextup.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 선수 벌크 임포트 Admin API Controller
 *
 * CSV 파싱 후 전달된 선수 목록을 일괄 등록합니다.
 */
@RestController
@RequestMapping("/api/backoffice/players/bulk-import")
class PlayerBulkImportController(
    private val playerBulkImportService: PlayerBulkImportService,
) {
    /**
     * 선수 목록을 일괄 등록합니다.
     *
     * 요청 본문의 players 배열을 순서대로 처리하며,
     * 일부 항목이 실패해도 성공한 항목은 저장됩니다.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun bulkImport(
        @Valid @RequestBody request: PlayerBulkImportRequest,
    ): ApiResponse<PlayerBulkImportResponse> {
        val result = playerBulkImportService.importPlayers(request.players)
        return ApiResponse.success(result)
    }
}
