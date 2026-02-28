package com.nextup.api.dto.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

@DisplayName("PagedResponse")
class PagedResponseTest {
    @Nested
    @DisplayName("from(page)")
    inner class From {
        @Test
        fun `단일 페이지 데이터를 올바르게 변환한다`() {
            // given
            val pageable = PageRequest.of(0, 20)
            val content = listOf("item1", "item2", "item3")
            val page = PageImpl(content, pageable, 3L)

            // when
            val result = PagedResponse.from(page)

            // then
            assertThat(result.content).isEqualTo(content)
            assertThat(result.totalElements).isEqualTo(3L)
            assertThat(result.totalPages).isEqualTo(1)
            assertThat(result.currentPage).isEqualTo(0)
            assertThat(result.size).isEqualTo(20)
            assertThat(result.hasNext).isFalse()
        }

        @Test
        fun `다음 페이지가 존재하는 경우 hasNext가 true이다`() {
            // given
            val pageable = PageRequest.of(0, 2)
            val content = listOf("item1", "item2")
            val page = PageImpl(content, pageable, 5L)

            // when
            val result = PagedResponse.from(page)

            // then
            assertThat(result.hasNext).isTrue()
            assertThat(result.totalPages).isEqualTo(3)
            assertThat(result.totalElements).isEqualTo(5L)
        }

        @Test
        fun `마지막 페이지인 경우 hasNext가 false이다`() {
            // given
            val pageable = PageRequest.of(2, 2)
            val content = listOf("item5")
            val page = PageImpl(content, pageable, 5L)

            // when
            val result = PagedResponse.from(page)

            // then
            assertThat(result.hasNext).isFalse()
            assertThat(result.currentPage).isEqualTo(2)
        }

        @Test
        fun `빈 페이지를 올바르게 변환한다`() {
            // given
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(emptyList<String>(), pageable, 0L)

            // when
            val result = PagedResponse.from(page)

            // then
            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0L)
            assertThat(result.totalPages).isEqualTo(0)
            assertThat(result.currentPage).isEqualTo(0)
            assertThat(result.size).isEqualTo(20)
            assertThat(result.hasNext).isFalse()
        }

        @Test
        fun `두 번째 페이지의 currentPage가 1이다`() {
            // given
            val pageable = PageRequest.of(1, 10)
            val content = listOf("item11")
            val page = PageImpl(content, pageable, 11L)

            // when
            val result = PagedResponse.from(page)

            // then
            assertThat(result.currentPage).isEqualTo(1)
            assertThat(result.size).isEqualTo(10)
            assertThat(result.hasNext).isFalse()
        }
    }
}
