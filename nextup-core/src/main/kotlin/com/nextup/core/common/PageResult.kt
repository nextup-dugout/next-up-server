package com.nextup.core.common

/**
 * Core 모듈용 페이징 결과 객체.
 * Spring Data의 Page를 대체하여 Core가 Spring에 의존하지 않도록 합니다.
 */
data class PageResult<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    val hasNext: Boolean get() = page + 1 < totalPages
    val hasPrevious: Boolean get() = page > 0

    fun <R> map(transform: (T) -> R): PageResult<R> =
        PageResult(
            content = content.map(transform),
            page = page,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages,
        )
}
