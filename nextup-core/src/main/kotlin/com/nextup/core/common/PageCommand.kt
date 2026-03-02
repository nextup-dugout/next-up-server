package com.nextup.core.common

/**
 * Core 모듈용 페이징 요청 객체.
 * Spring Data의 Pageable을 대체하여 Core가 Spring에 의존하지 않도록 합니다.
 */
data class PageCommand(
    val page: Int = 0,
    val size: Int = 20,
    val sorts: List<SortOrder> = emptyList(),
) {
    init {
        require(page >= 0) { "Page must be non-negative" }
        require(size > 0) { "Size must be positive" }
    }
}

data class SortOrder(
    val property: String,
    val direction: SortDirection = SortDirection.ASC,
)

enum class SortDirection {
    ASC,
    DESC,
}
