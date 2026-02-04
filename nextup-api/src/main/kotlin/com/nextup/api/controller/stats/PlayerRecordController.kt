package com.nextup.api.controller.stats

import com.nextup.api.dto.common.ApiResponse
import com.nextup.api.dto.stats.PlayerRecordResponse
import com.nextup.api.dto.stats.RecentFormResponse
import com.nextup.api.mapper.stats.toResponse
import com.nextup.core.service.stats.PlayerRecordService
import com.nextup.core.service.stats.RecentFormService
import com.nextup.core.service.stats.dto.FormType
import com.nextup.core.service.stats.dto.RecordScope
import com.nextup.core.service.stats.dto.RecordType
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 선수 기록 컨트롤러
 *
 * 선수의 타격/투수 기록을 조회하는 API를 제공합니다.
 */
@Validated
@RestController
@RequestMapping("/api/v1/players/{playerId}/records")
class PlayerRecordController(
    private val playerRecordService: PlayerRecordService,
    private val recentFormService: RecentFormService,
) {
    /**
     * 선수 기록을 조회합니다.
     *
     * @param playerId 선수 ID
     * @param scope 조회 범위 (SEASON, CAREER, COMPETITION)
     * @param type 조회 타입 (BATTING, PITCHING, ALL)
     * @param year 시즌 연도 (scope=SEASON일 때 사용, 미지정 시 현재 연도)
     * @param competitionId 대회 ID (scope=COMPETITION일 때 필수)
     * @return 선수 기록 응답
     */
    @GetMapping
    fun getPlayerRecord(
        @PathVariable playerId: Long,
        @RequestParam(defaultValue = "CAREER") scope: RecordScope,
        @RequestParam(defaultValue = "ALL") type: RecordType,
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) competitionId: Long?,
    ): ApiResponse<PlayerRecordResponse> {
        val record = playerRecordService.getPlayerRecord(playerId, scope, type, year, competitionId)
        return ApiResponse.success(record.toResponse())
    }

    /**
     * 선수의 최근 N경기 폼을 조회합니다.
     *
     * @param playerId 선수 ID
     * @param games 조회할 경기 수 (기본값: 5, 최대: 20)
     * @param type 조회 타입 (BATTING, PITCHING)
     * @return 최근 폼 분석 응답
     */
    @GetMapping("/form")
    fun getRecentForm(
        @PathVariable playerId: Long,
        @RequestParam(defaultValue = "5") @Min(1) @Max(20) games: Int,
        @RequestParam(defaultValue = "BATTING") type: FormType,
    ): ApiResponse<RecentFormResponse> {
        val form = recentFormService.getRecentForm(playerId, games, type)
        return ApiResponse.success(form.toResponse())
    }
}
