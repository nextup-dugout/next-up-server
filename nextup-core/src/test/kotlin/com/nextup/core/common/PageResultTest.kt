package com.nextup.core.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PageResult 테스트")
class PageResultTest {
    @Nested
    @DisplayName("기본 생성 및 필드 확인")
    inner class Creation {
        @Test
        fun `PageResult 생성 시 모든 필드가 설정된다`() {
            val content = listOf("a", "b", "c")
            val result =
                PageResult(
                    content = content,
                    page = 0,
                    size = 10,
                    totalElements = 3L,
                    totalPages = 1,
                )

            assertThat(result.content).isEqualTo(content)
            assertThat(result.page).isEqualTo(0)
            assertThat(result.size).isEqualTo(10)
            assertThat(result.totalElements).isEqualTo(3L)
            assertThat(result.totalPages).isEqualTo(1)
        }

        @Test
        fun `빈 content로 PageResult 생성 가능`() {
            val result =
                PageResult(
                    content = emptyList<String>(),
                    page = 0,
                    size = 20,
                    totalElements = 0L,
                    totalPages = 0,
                )

            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0L)
        }
    }

    @Nested
    @DisplayName("hasNext 계산")
    inner class HasNext {
        @Test
        fun `현재 페이지가 마지막 페이지보다 작으면 hasNext = true`() {
            val result =
                PageResult(
                    content = listOf(1, 2, 3),
                    page = 0,
                    size = 3,
                    totalElements = 9L,
                    totalPages = 3,
                )

            assertThat(result.hasNext).isTrue()
        }

        @Test
        fun `현재 페이지가 마지막 페이지이면 hasNext = false`() {
            val result =
                PageResult(
                    content = listOf(7, 8, 9),
                    page = 2,
                    size = 3,
                    totalElements = 9L,
                    totalPages = 3,
                )

            assertThat(result.hasNext).isFalse()
        }

        @Test
        fun `totalPages가 0이면 hasNext = false`() {
            val result =
                PageResult(
                    content = emptyList<Int>(),
                    page = 0,
                    size = 10,
                    totalElements = 0L,
                    totalPages = 0,
                )

            assertThat(result.hasNext).isFalse()
        }
    }

    @Nested
    @DisplayName("hasPrevious 계산")
    inner class HasPrevious {
        @Test
        fun `첫 번째 페이지이면 hasPrevious = false`() {
            val result =
                PageResult(
                    content = listOf(1, 2, 3),
                    page = 0,
                    size = 3,
                    totalElements = 9L,
                    totalPages = 3,
                )

            assertThat(result.hasPrevious).isFalse()
        }

        @Test
        fun `두 번째 이후 페이지이면 hasPrevious = true`() {
            val result =
                PageResult(
                    content = listOf(4, 5, 6),
                    page = 1,
                    size = 3,
                    totalElements = 9L,
                    totalPages = 3,
                )

            assertThat(result.hasPrevious).isTrue()
        }
    }

    @Nested
    @DisplayName("map 변환")
    inner class MapTransform {
        @Test
        fun `map으로 content 타입을 변환할 수 있다`() {
            val result =
                PageResult(
                    content = listOf(1, 2, 3),
                    page = 0,
                    size = 10,
                    totalElements = 3L,
                    totalPages = 1,
                )

            val mapped = result.map { it.toString() }

            assertThat(mapped.content).containsExactly("1", "2", "3")
            assertThat(mapped.page).isEqualTo(result.page)
            assertThat(mapped.size).isEqualTo(result.size)
            assertThat(mapped.totalElements).isEqualTo(result.totalElements)
            assertThat(mapped.totalPages).isEqualTo(result.totalPages)
        }

        @Test
        fun `map으로 변환해도 페이징 메타데이터는 유지된다`() {
            val result =
                PageResult(
                    content = listOf("hello", "world"),
                    page = 2,
                    size = 5,
                    totalElements = 12L,
                    totalPages = 3,
                )

            val mapped = result.map { it.length }

            assertThat(mapped.content).containsExactly(5, 5)
            assertThat(mapped.hasNext).isEqualTo(result.hasNext)
            assertThat(mapped.hasPrevious).isEqualTo(result.hasPrevious)
        }

        @Test
        fun `빈 content에 map 적용 시 빈 목록 반환`() {
            val result =
                PageResult(
                    content = emptyList<String>(),
                    page = 0,
                    size = 10,
                    totalElements = 0L,
                    totalPages = 0,
                )

            val mapped = result.map { it.length }

            assertThat(mapped.content).isEmpty()
        }
    }
}
