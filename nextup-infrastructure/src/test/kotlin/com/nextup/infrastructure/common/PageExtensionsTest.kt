package com.nextup.infrastructure.common

import com.nextup.core.common.PageCommand
import com.nextup.core.common.SortDirection
import com.nextup.core.common.SortOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

@DisplayName("PageExtensions 테스트")
class PageExtensionsTest {
    @Nested
    @DisplayName("PageCommand.toPageable()")
    inner class ToPageable {
        @Test
        fun `sorts가 비어있으면 unsorted Pageable 반환`() {
            val command = PageCommand(page = 0, size = 20, sorts = emptyList())

            val pageable = command.toPageable()

            assertThat(pageable.pageNumber).isEqualTo(0)
            assertThat(pageable.pageSize).isEqualTo(20)
            assertThat(pageable.sort.isSorted).isFalse()
        }

        @Test
        fun `ASC sort가 있으면 ASC 방향의 Pageable 반환`() {
            val command =
                PageCommand(
                    page = 1,
                    size = 10,
                    sorts = listOf(SortOrder(property = "name", direction = SortDirection.ASC)),
                )

            val pageable = command.toPageable()

            assertThat(pageable.pageNumber).isEqualTo(1)
            assertThat(pageable.pageSize).isEqualTo(10)
            val order = pageable.sort.getOrderFor("name")
            assertThat(order).isNotNull
            assertThat(order!!.direction).isEqualTo(Sort.Direction.ASC)
        }

        @Test
        fun `DESC sort가 있으면 DESC 방향의 Pageable 반환`() {
            val command =
                PageCommand(
                    page = 0,
                    size = 5,
                    sorts = listOf(SortOrder(property = "createdAt", direction = SortDirection.DESC)),
                )

            val pageable = command.toPageable()

            val order = pageable.sort.getOrderFor("createdAt")
            assertThat(order).isNotNull
            assertThat(order!!.direction).isEqualTo(Sort.Direction.DESC)
        }

        @Test
        fun `복수의 sort 조건이 모두 변환된다`() {
            val command =
                PageCommand(
                    page = 0,
                    size = 20,
                    sorts =
                        listOf(
                            SortOrder(property = "name", direction = SortDirection.ASC),
                            SortOrder(property = "createdAt", direction = SortDirection.DESC),
                        ),
                )

            val pageable = command.toPageable()

            val nameOrder = pageable.sort.getOrderFor("name")
            val createdAtOrder = pageable.sort.getOrderFor("createdAt")
            assertThat(nameOrder!!.direction).isEqualTo(Sort.Direction.ASC)
            assertThat(createdAtOrder!!.direction).isEqualTo(Sort.Direction.DESC)
        }
    }

    @Nested
    @DisplayName("Page<T>.toPageResult()")
    inner class ToPageResult {
        @Test
        fun `Spring Page를 PageResult로 변환한다`() {
            val content = listOf("a", "b", "c")
            val pageable = PageRequest.of(0, 10)
            val springPage = PageImpl(content, pageable, 3L)

            val result = springPage.toPageResult()

            assertThat(result.content).isEqualTo(content)
            assertThat(result.page).isEqualTo(0)
            assertThat(result.size).isEqualTo(10)
            assertThat(result.totalElements).isEqualTo(3L)
            assertThat(result.totalPages).isEqualTo(1)
        }

        @Test
        fun `빈 Spring Page를 PageResult로 변환한다`() {
            val pageable = PageRequest.of(0, 20)
            val springPage = PageImpl(emptyList<String>(), pageable, 0L)

            val result = springPage.toPageResult()

            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0L)
            assertThat(result.hasNext).isFalse()
            assertThat(result.hasPrevious).isFalse()
        }

        @Test
        fun `다음 페이지가 있는 경우 hasNext = true`() {
            val content = listOf(1, 2, 3)
            val pageable = PageRequest.of(0, 3)
            val springPage = PageImpl(content, pageable, 9L)

            val result = springPage.toPageResult()

            assertThat(result.hasNext).isTrue()
        }
    }

    @Nested
    @DisplayName("Pageable.toPageCommand()")
    inner class ToPageCommand {
        @Test
        fun `정렬 없는 Pageable을 PageCommand로 변환한다`() {
            val pageable = PageRequest.of(2, 15)

            val command = pageable.toPageCommand()

            assertThat(command.page).isEqualTo(2)
            assertThat(command.size).isEqualTo(15)
            assertThat(command.sorts).isEmpty()
        }

        @Test
        fun `ASC 정렬 Pageable을 PageCommand로 변환하면 ASC SortOrder`() {
            val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name"))

            val command = pageable.toPageCommand()

            assertThat(command.sorts).hasSize(1)
            assertThat(command.sorts[0].property).isEqualTo("name")
            assertThat(command.sorts[0].direction).isEqualTo(SortDirection.ASC)
        }

        @Test
        fun `DESC 정렬 Pageable을 PageCommand로 변환하면 DESC SortOrder`() {
            val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))

            val command = pageable.toPageCommand()

            assertThat(command.sorts).hasSize(1)
            assertThat(command.sorts[0].property).isEqualTo("createdAt")
            assertThat(command.sorts[0].direction).isEqualTo(SortDirection.DESC)
        }

        @Test
        fun `복수 정렬 Pageable을 PageCommand로 변환한다`() {
            val pageable =
                PageRequest.of(
                    1,
                    5,
                    Sort.by(Sort.Order.asc("name"), Sort.Order.desc("createdAt")),
                )

            val command = pageable.toPageCommand()

            assertThat(command.sorts).hasSize(2)
            val nameSort = command.sorts.find { it.property == "name" }
            val createdAtSort = command.sorts.find { it.property == "createdAt" }
            assertThat(nameSort!!.direction).isEqualTo(SortDirection.ASC)
            assertThat(createdAtSort!!.direction).isEqualTo(SortDirection.DESC)
        }
    }
}
