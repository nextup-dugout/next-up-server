package com.nextup.core.domain.association

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Association 엔티티 테스트")
class AssociationTest {
    private fun createAssociation(isActive: Boolean = true): Association =
        Association(
            name = "서울시야구협회",
            abbreviation = "SBA",
            region = "서울",
            description = "서울 지역 사회인 야구 협회",
            logoUrl = "https://example.com/logo.png",
            websiteUrl = "https://example.com",
            isActive = isActive,
        )

    @Nested
    @DisplayName("활성화/비활성화")
    inner class ActivationTests {
        @Test
        fun `협회를 비활성화할 수 있다`() {
            // given
            val association = createAssociation(isActive = true)

            // when
            association.deactivate()

            // then
            assertThat(association.isActive).isFalse()
        }

        @Test
        fun `협회를 활성화할 수 있다`() {
            // given
            val association = createAssociation(isActive = false)

            // when
            association.activate()

            // then
            assertThat(association.isActive).isTrue()
        }
    }

    @Nested
    @DisplayName("정보 업데이트")
    inner class UpdateInfoTests {
        @Test
        fun `설명을 업데이트할 수 있다`() {
            // given
            val association = createAssociation()
            val newDescription = "수정된 설명"

            // when
            association.updateInfo(description = newDescription)

            // then
            assertThat(association.description).isEqualTo(newDescription)
        }

        @Test
        fun `로고 URL을 업데이트할 수 있다`() {
            // given
            val association = createAssociation()
            val newLogoUrl = "https://example.com/new-logo.png"

            // when
            association.updateInfo(logoUrl = newLogoUrl)

            // then
            assertThat(association.logoUrl).isEqualTo(newLogoUrl)
        }

        @Test
        fun `웹사이트 URL을 업데이트할 수 있다`() {
            // given
            val association = createAssociation()
            val newWebsiteUrl = "https://new-website.com"

            // when
            association.updateInfo(websiteUrl = newWebsiteUrl)

            // then
            assertThat(association.websiteUrl).isEqualTo(newWebsiteUrl)
        }

        @Test
        fun `모든 정보를 동시에 업데이트할 수 있다`() {
            // given
            val association = createAssociation()

            // when
            association.updateInfo(
                description = "새로운 설명",
                logoUrl = "https://example.com/updated.png",
                websiteUrl = "https://updated-website.com",
            )

            // then
            assertThat(association.description).isEqualTo("새로운 설명")
            assertThat(association.logoUrl).isEqualTo("https://example.com/updated.png")
            assertThat(association.websiteUrl).isEqualTo("https://updated-website.com")
        }
    }

    @Nested
    @DisplayName("기본 속성")
    inner class PropertyTests {
        @Test
        fun `협회 생성 시 기본값으로 활성 상태이다`() {
            // given & when
            val association =
                Association(
                    name = "테스트 협회",
                    region = "테스트",
                )

            // then
            assertThat(association.isActive).isTrue()
        }

        @Test
        fun `협회 속성이 올바르게 설정된다`() {
            // given & when
            val association = createAssociation()

            // then
            assertThat(association.name).isEqualTo("서울시야구협회")
            assertThat(association.abbreviation).isEqualTo("SBA")
            assertThat(association.region).isEqualTo("서울")
        }
    }
}
