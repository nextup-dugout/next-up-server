package com.nextup.core.domain.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("OrganizationRole 테스트")
class OrganizationRoleTest {
    @Nested
    @DisplayName("enum 값")
    inner class EnumValues {
        @Test
        fun `should have correct ADMIN properties`() {
            // given
            val role = OrganizationRole.ADMIN

            // then
            assertThat(role.displayName).isEqualTo("관리자")
            assertThat(role.description).isEqualTo("조직의 모든 권한을 가진 최고 관리자")
            assertThat(role.level).isEqualTo(100)
        }

        @Test
        fun `should have correct MANAGER properties`() {
            // given
            val role = OrganizationRole.MANAGER

            // then
            assertThat(role.displayName).isEqualTo("매니저")
            assertThat(role.description).isEqualTo("조직 운영 관련 권한을 가진 관리자")
            assertThat(role.level).isEqualTo(50)
        }

        @Test
        fun `should have correct SCORER properties`() {
            // given
            val role = OrganizationRole.SCORER

            // then
            assertThat(role.displayName).isEqualTo("기록원")
            assertThat(role.description).isEqualTo("경기 기록 입력 권한만 가진 관리자")
            assertThat(role.level).isEqualTo(10)
        }
    }

    @Nested
    @DisplayName("isHigherThan 메서드")
    inner class IsHigherThan {
        @Test
        fun `ADMIN should be higher than MANAGER`() {
            assertThat(OrganizationRole.ADMIN.isHigherThan(OrganizationRole.MANAGER)).isTrue()
        }

        @Test
        fun `ADMIN should be higher than SCORER`() {
            assertThat(OrganizationRole.ADMIN.isHigherThan(OrganizationRole.SCORER)).isTrue()
        }

        @Test
        fun `MANAGER should be higher than SCORER`() {
            assertThat(OrganizationRole.MANAGER.isHigherThan(OrganizationRole.SCORER)).isTrue()
        }

        @Test
        fun `MANAGER should not be higher than ADMIN`() {
            assertThat(OrganizationRole.MANAGER.isHigherThan(OrganizationRole.ADMIN)).isFalse()
        }

        @Test
        fun `SCORER should not be higher than MANAGER`() {
            assertThat(OrganizationRole.SCORER.isHigherThan(OrganizationRole.MANAGER)).isFalse()
        }

        @Test
        fun `same role should not be higher than itself`() {
            assertThat(OrganizationRole.ADMIN.isHigherThan(OrganizationRole.ADMIN)).isFalse()
            assertThat(OrganizationRole.MANAGER.isHigherThan(OrganizationRole.MANAGER)).isFalse()
            assertThat(OrganizationRole.SCORER.isHigherThan(OrganizationRole.SCORER)).isFalse()
        }
    }

    @Nested
    @DisplayName("isHigherOrEqual 메서드")
    inner class IsHigherOrEqual {
        @Test
        fun `ADMIN should be higher or equal to all roles`() {
            assertThat(OrganizationRole.ADMIN.isHigherOrEqual(OrganizationRole.ADMIN)).isTrue()
            assertThat(OrganizationRole.ADMIN.isHigherOrEqual(OrganizationRole.MANAGER)).isTrue()
            assertThat(OrganizationRole.ADMIN.isHigherOrEqual(OrganizationRole.SCORER)).isTrue()
        }

        @Test
        fun `MANAGER should be higher or equal to MANAGER and SCORER`() {
            assertThat(OrganizationRole.MANAGER.isHigherOrEqual(OrganizationRole.ADMIN)).isFalse()
            assertThat(OrganizationRole.MANAGER.isHigherOrEqual(OrganizationRole.MANAGER)).isTrue()
            assertThat(OrganizationRole.MANAGER.isHigherOrEqual(OrganizationRole.SCORER)).isTrue()
        }

        @Test
        fun `SCORER should only be higher or equal to itself`() {
            assertThat(OrganizationRole.SCORER.isHigherOrEqual(OrganizationRole.ADMIN)).isFalse()
            assertThat(OrganizationRole.SCORER.isHigherOrEqual(OrganizationRole.MANAGER)).isFalse()
            assertThat(OrganizationRole.SCORER.isHigherOrEqual(OrganizationRole.SCORER)).isTrue()
        }
    }

    @Nested
    @DisplayName("fromValue 메서드")
    inner class FromValue {
        @ParameterizedTest
        @CsvSource(
            "ADMIN, ADMIN",
            "admin, ADMIN",
            "Admin, ADMIN",
            "MANAGER, MANAGER",
            "manager, MANAGER",
            "SCORER, SCORER",
            "scorer, SCORER",
        )
        fun `should return correct role for valid values`(
            input: String,
            expected: OrganizationRole,
        ) {
            // when
            val result = OrganizationRole.fromValue(input)

            // then
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `should throw exception for invalid value`() {
            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    OrganizationRole.fromValue("INVALID")
                }
            assertThat(exception.message).contains("Invalid organization role")
        }

        @Test
        fun `should throw exception for empty value`() {
            // when & then
            assertThrows<IllegalArgumentException> {
                OrganizationRole.fromValue("")
            }
        }
    }
}
