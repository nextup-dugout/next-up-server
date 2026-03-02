package com.nextup.api.dto.common

import com.nextup.core.common.PageResult
import org.springframework.data.domain.Page

/**
 * 페이징된 목록 응답 래퍼
 *
 * Spring Data [Page] 또는 Core [PageResult]를 클라이언트 친화적인 형태로 변환합니다.
 *
 * @param T 응답 데이터 원소 타입
 * @property content 현재 페이지의 데이터 목록
 * @property totalElements 전체 데이터 수
 * @property totalPages 전체 페이지 수
 * @property currentPage 현재 페이지 번호 (0-based)
 * @property size 페이지당 데이터 수
 * @property hasNext 다음 페이지 존재 여부
 */
data class PagedResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val size: Int,
    val hasNext: Boolean,
) {
    companion object {
        /**
         * Spring Data [Page]로부터 [PagedResponse]를 생성합니다.
         */
        fun <T> from(page: Page<T>): PagedResponse<T> =
            PagedResponse(
                content = page.content,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                currentPage = page.number,
                size = page.size,
                hasNext = page.hasNext(),
            )

        /**
         * Core [PageResult]로부터 [PagedResponse]를 생성합니다.
         */
        fun <T> from(pageResult: PageResult<T>): PagedResponse<T> =
            PagedResponse(
                content = pageResult.content,
                totalElements = pageResult.totalElements,
                totalPages = pageResult.totalPages,
                currentPage = pageResult.page,
                size = pageResult.size,
                hasNext = pageResult.hasNext,
            )
    }
}
