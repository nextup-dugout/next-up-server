package com.nextup.core.service.search

import com.nextup.core.service.search.dto.SearchResultDto

/**
 * 통합 검색 서비스 인터페이스
 *
 * 선수/팀/대회를 키워드로 동시 검색합니다.
 */
interface SearchService {
    /**
     * 키워드로 선수, 팀, 대회를 검색합니다.
     *
     * @param keyword 검색 키워드 (최소 1자)
     * @param limit 각 카테고리별 최대 결과 수 (기본값: 5)
     */
    fun search(
        keyword: String,
        limit: Int = 5,
    ): SearchResultDto
}
