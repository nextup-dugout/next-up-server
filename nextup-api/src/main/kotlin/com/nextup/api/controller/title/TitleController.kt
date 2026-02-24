package com.nextup.api.controller.title

import com.nextup.api.dto.title.TitleListResponse
import com.nextup.api.dto.title.TitleResponse
import com.nextup.api.dto.title.toListResponse
import com.nextup.api.dto.title.toResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.title.TitleCategory
import com.nextup.core.service.title.TitleService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 타이틀 컨트롤러
 *
 * 대회별 개인 타이틀(타격왕, 홈런왕 등) 조회 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/competitions/{competitionId}/titles")
class TitleController(
    private val titleService: TitleService,
) {
    /**
     * 대회의 모든 타이틀을 조회합니다.
     *
     * @param competitionId 대회 ID
     * @return 타이틀 목록
     */
    @GetMapping
    fun getTitles(
        @PathVariable competitionId: Long,
    ): ApiResponse<TitleListResponse> {
        val titles = titleService.getTitles(competitionId)
        return ApiResponse.success(titles.toListResponse(competitionId))
    }

    /**
     * 대회의 특정 카테고리 타이틀을 조회합니다.
     *
     * @param competitionId 대회 ID
     * @param category 타이틀 카테고리 (BATTING_AVG, HOME_RUNS, RBI, STOLEN_BASES, HITS, WINS, ERA, SAVES, STRIKEOUTS)
     * @return 타이틀 정보
     */
    @GetMapping("/{category}")
    fun getTitleByCategory(
        @PathVariable competitionId: Long,
        @PathVariable category: String,
    ): ApiResponse<TitleResponse> {
        val titleCategory = TitleCategory.valueOf(category.uppercase())
        val title = titleService.getTitleByCategory(competitionId, titleCategory)
        return ApiResponse.success(title.toResponse())
    }
}
