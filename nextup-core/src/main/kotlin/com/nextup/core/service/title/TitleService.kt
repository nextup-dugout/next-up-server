package com.nextup.core.service.title

import com.nextup.core.service.title.dto.TitleDto

/**
 * 타이틀 서비스 인터페이스 (Port)
 *
 * 대회별 개인 타이틀(타격왕, 홈런왕 등)을 조회합니다.
 */
interface TitleService {
    /**
     * 대회의 모든 타이틀 정보를 조회합니다.
     *
     * @param competitionId 대회 ID
     * @return 타이틀 목록
     */
    fun getTitles(competitionId: Long): List<TitleDto>

    /**
     * 대회의 특정 카테고리 타이틀을 조회합니다.
     *
     * @param competitionId 대회 ID
     * @param category 타이틀 카테고리
     * @return 타이틀 정보
     */
    fun getTitleByCategory(
        competitionId: Long,
        category: TitleCategory,
    ): TitleDto
}
