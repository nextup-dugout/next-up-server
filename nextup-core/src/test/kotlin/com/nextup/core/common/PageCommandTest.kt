package com.nextup.core.common

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PageCommand 테스트")
class PageCommandTest {
    @Nested
    @DisplayName("기본 생성")
    inner class Creation {
        @Test
        fun `기본값으로 생성 시 page=0, size=20, sorts 빈 목록`() {
            val command = PageCommand()

            assertThat(command.page).isEqualTo(0)
            assertThat(command.size).isEqualTo(20)
            assertThat(command.sorts).isEmpty()
        }

        @Test
        fun `커스텀 값으로 생성 가능`() {
            val command = PageCommand(page = 2, size = 10)

            assertThat(command.page).isEqualTo(2)
            assertThat(command.size).isEqualTo(10)
        }

        @Test
        fun `sorts가 있는 경우 정상 생성`() {
            val sorts =
                listOf(
                    SortOrder(property = "name", direction = SortDirection.ASC),
                    SortOrder(property = "createdAt", direction = SortDirection.DESC),
                )
            val command = PageCommand(page = 0, size = 10, sorts = sorts)

            assertThat(command.sorts).hasSize(2)
            assertThat(command.sorts[0].property).isEqualTo("name")
            assertThat(command.sorts[0].direction).isEqualTo(SortDirection.ASC)
            assertThat(command.sorts[1].property).isEqualTo("createdAt")
            assertThat(command.sorts[1].direction).isEqualTo(SortDirection.DESC)
        }
    }

    @Nested
    @DisplayName("유효성 검증")
    inner class Validation {
        @Test
        fun `page가 음수이면 예외 발생`() {
            assertThatThrownBy {
                PageCommand(page = -1, size = 20)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Page must be non-negative")
        }

        @Test
        fun `size가 0이면 예외 발생`() {
            assertThatThrownBy {
                PageCommand(page = 0, size = 0)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Size must be positive")
        }

        @Test
        fun `size가 음수이면 예외 발생`() {
            assertThatThrownBy {
                PageCommand(page = 0, size = -5)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Size must be positive")
        }

        @Test
        fun `page가 0이면 정상 생성`() {
            val command = PageCommand(page = 0, size = 1)

            assertThat(command.page).isEqualTo(0)
        }

        @Test
        fun `size가 1이면 정상 생성`() {
            val command = PageCommand(page = 0, size = 1)

            assertThat(command.size).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("SortOrder 생성")
    inner class SortOrderCreation {
        @Test
        fun `SortOrder 기본 방향은 ASC`() {
            val sortOrder = SortOrder(property = "name")

            assertThat(sortOrder.property).isEqualTo("name")
            assertThat(sortOrder.direction).isEqualTo(SortDirection.ASC)
        }

        @Test
        fun `SortOrder DESC 방향으로 생성 가능`() {
            val sortOrder = SortOrder(property = "createdAt", direction = SortDirection.DESC)

            assertThat(sortOrder.property).isEqualTo("createdAt")
            assertThat(sortOrder.direction).isEqualTo(SortDirection.DESC)
        }
    }

    @Nested
    @DisplayName("SortDirection enum")
    inner class SortDirectionEnum {
        @Test
        fun `SortDirection은 ASC와 DESC 두 값을 가진다`() {
            val values = SortDirection.entries

            assertThat(values).containsExactlyInAnyOrder(SortDirection.ASC, SortDirection.DESC)
        }

        @Test
        fun `SortDirection ASC 이름 확인`() {
            assertThat(SortDirection.ASC.name).isEqualTo("ASC")
        }

        @Test
        fun `SortDirection DESC 이름 확인`() {
            assertThat(SortDirection.DESC.name).isEqualTo("DESC")
        }
    }
}
