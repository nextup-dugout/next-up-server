package com.nextup.infrastructure.common

import com.nextup.core.common.PageCommand
import com.nextup.core.common.PageResult
import com.nextup.core.common.SortDirection
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

/**
 * Core의 PageCommand를 Spring Data의 Pageable로 변환합니다.
 */
fun PageCommand.toPageable(): Pageable {
    val sort =
        if (sorts.isEmpty()) {
            Sort.unsorted()
        } else {
            Sort.by(
                sorts.map {
                    Sort.Order(
                        if (it.direction == SortDirection.ASC) Sort.Direction.ASC else Sort.Direction.DESC,
                        it.property,
                    )
                },
            )
        }
    return PageRequest.of(page, size, sort)
}

/**
 * Spring Data의 Page를 Core의 PageResult로 변환합니다.
 */
fun <T> Page<T>.toPageResult(): PageResult<T> =
    PageResult(
        content = content,
        page = number,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
    )

/**
 * Spring Data의 Pageable을 Core의 PageCommand로 변환합니다.
 */
fun Pageable.toPageCommand(): PageCommand =
    PageCommand(
        page = pageNumber,
        size = pageSize,
        sorts =
            sort.map {
                com.nextup.core.common.SortOrder(
                    property = it.property,
                    direction = if (it.isAscending) SortDirection.ASC else SortDirection.DESC,
                )
            }.toList(),
    )
