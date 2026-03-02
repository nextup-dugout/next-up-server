package com.nextup.api.controller.search

import com.nextup.api.dto.search.SearchResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.search.SearchService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 통합 검색 API Controller (A-08)
 *
 * 선수/팀/대회를 동시 검색합니다.
 */
@RestController
@RequestMapping("/api/v1")
class SearchController(
    private val searchService: SearchService,
) {
    /**
     * 키워드로 선수, 팀, 대회를 검색합니다.
     *
     * GET /api/v1/search?q={keyword}&limit={limit}
     */
    @GetMapping("/search")
    fun search(
        @RequestParam q: String,
        @RequestParam(defaultValue = "5") limit: Int,
    ): ApiResponse<SearchResponse> {
        val result = searchService.search(keyword = q, limit = limit)
        return ApiResponse.success(SearchResponse.from(result))
    }
}
