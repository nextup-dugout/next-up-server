package com.nextup.core.domain.league

import com.nextup.core.domain.association.Association
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("League 엔티티 테스트")
class LeagueTest {
    private lateinit var association: Association

    @BeforeEach
    fun setUp() {
        association =
            Association(
                name = "서울시야구협회",
                abbreviation = "SBA",
                region = "서울",
            )
    }

    private fun createLeague(isActive: Boolean = true): League =
        League(
            association = association,
            name = "1부 리그",
            abbreviation = "1st",
            foundedYear = 2020,
            divisionLevel = 1,
            description = "최상위 리그",
            logoUrl = "https://example.com/logo.png",
            isActive = isActive,
        )

    @Nested
    @DisplayName("활성화/비활성화")
    inner class ActivationTests {
        @Test
        fun `리그를 비활성화할 수 있다`() {
            // given
            val league = createLeague(isActive = true)

            // when
            league.deactivate()

            // then
            assertThat(league.isActive).isFalse()
        }

        @Test
        fun `리그를 활성화할 수 있다`() {
            // given
            val league = createLeague(isActive = false)

            // when
            league.activate()

            // then
            assertThat(league.isActive).isTrue()
        }
    }

    @Nested
    @DisplayName("정보 업데이트")
    inner class UpdateInfoTests {
        @Test
        fun `설명을 업데이트할 수 있다`() {
            // given
            val league = createLeague()
            val newDescription = "수정된 설명"

            // when
            league.updateInfo(description = newDescription)

            // then
            assertThat(league.description).isEqualTo(newDescription)
        }

        @Test
        fun `로고 URL을 업데이트할 수 있다`() {
            // given
            val league = createLeague()
            val newLogoUrl = "https://example.com/new-logo.png"

            // when
            league.updateInfo(logoUrl = newLogoUrl)

            // then
            assertThat(league.logoUrl).isEqualTo(newLogoUrl)
        }

        @Test
        fun `설명과 로고 URL을 동시에 업데이트할 수 있다`() {
            // given
            val league = createLeague()

            // when
            league.updateInfo(
                description = "새로운 설명",
                logoUrl = "https://example.com/updated.png",
            )

            // then
            assertThat(league.description).isEqualTo("새로운 설명")
            assertThat(league.logoUrl).isEqualTo("https://example.com/updated.png")
        }
    }

    @Nested
    @DisplayName("기본 속성")
    inner class PropertyTests {
        @Test
        fun `리그 생성 시 기본값으로 활성 상태이다`() {
            // given & when
            val league =
                League(
                    association = association,
                    name = "테스트 리그",
                    foundedYear = 2025,
                    divisionLevel = 1,
                )

            // then
            assertThat(league.isActive).isTrue()
        }

        @Test
        fun `리그 속성이 올바르게 설정된다`() {
            // given & when
            val league = createLeague()

            // then
            assertThat(league.name).isEqualTo("1부 리그")
            assertThat(league.abbreviation).isEqualTo("1st")
            assertThat(league.foundedYear).isEqualTo(2020)
            assertThat(league.divisionLevel).isEqualTo(1)
        }
    }
}
