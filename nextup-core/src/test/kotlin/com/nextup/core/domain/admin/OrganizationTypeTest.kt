package com.nextup.core.domain.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("OrganizationType 테스트")
class OrganizationTypeTest {

    @Nested
    @DisplayName("enum 값")
    inner class EnumValues {

        @Test
        fun `should have correct ASSOCIATION properties`() {
            // given
            val type = OrganizationType.ASSOCIATION

            // then
            assertThat(type.displayName).isEqualTo("협회")
            assertThat(type.description).isEqualTo("사회인 야구 협회")
        }

        @Test
        fun `should have correct LEAGUE properties`() {
            // given
            val type = OrganizationType.LEAGUE

            // then
            assertThat(type.displayName).isEqualTo("리그")
            assertThat(type.description).isEqualTo("협회에 속한 리그")
        }

        @Test
        fun `should have correct TEAM properties`() {
            // given
            val type = OrganizationType.TEAM

            // then
            assertThat(type.displayName).isEqualTo("팀")
            assertThat(type.description).isEqualTo("리그에 속한 팀")
        }
    }

    @Nested
    @DisplayName("fromValue 메서드")
    inner class FromValue {

        @ParameterizedTest
        @CsvSource(
            "ASSOCIATION, ASSOCIATION",
            "association, ASSOCIATION",
            "Association, ASSOCIATION",
            "LEAGUE, LEAGUE",
            "league, LEAGUE",
            "League, LEAGUE",
            "TEAM, TEAM",
            "team, TEAM",
            "Team, TEAM"
        )
        fun `should return correct type for valid values`(input: String, expected: OrganizationType) {
            // when
            val result = OrganizationType.fromValue(input)

            // then
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `should throw exception for invalid value`() {
            // when & then
            val exception = assertThrows<IllegalArgumentException> {
                OrganizationType.fromValue("INVALID")
            }
            assertThat(exception.message).contains("Invalid organization type")
        }

        @Test
        fun `should throw exception for empty value`() {
            // when & then
            assertThrows<IllegalArgumentException> {
                OrganizationType.fromValue("")
            }
        }
    }

    @Nested
    @DisplayName("전체 enum 값")
    inner class AllValues {

        @Test
        fun `should have exactly three types`() {
            // when
            val allTypes = OrganizationType.entries

            // then
            assertThat(allTypes).hasSize(3)
            assertThat(allTypes).containsExactly(
                OrganizationType.ASSOCIATION,
                OrganizationType.LEAGUE,
                OrganizationType.TEAM
            )
        }
    }
}
