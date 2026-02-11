package com.nextup.infrastructure.security.oauth2.impl

import com.nextup.core.domain.user.OAuthProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("OAuth2UserInfo 구현체 테스트")
class OAuth2UserInfoImplTest {
    @Nested
    @DisplayName("KakaoOAuth2UserInfo")
    inner class KakaoOAuth2UserInfoTest {
        @Test
        fun `should parse kakao user info correctly`() {
            // given
            val attributes =
                mapOf<String, Any>(
                    "id" to 123456789L,
                    "kakao_account" to
                        mapOf(
                            "email" to "user@kakao.com",
                            "profile" to
                                mapOf(
                                    "nickname" to "홍길동",
                                    "profile_image_url" to "http://example.com/image.jpg",
                                ),
                        ),
                )

            // when
            val userInfo = KakaoOAuth2UserInfo(attributes)

            // then
            assertThat(userInfo.provider).isEqualTo(OAuthProvider.KAKAO)
            assertThat(userInfo.id).isEqualTo("123456789")
            assertThat(userInfo.email).isEqualTo("user@kakao.com")
            assertThat(userInfo.name).isEqualTo("홍길동")
            assertThat(userInfo.profileImageUrl).isEqualTo("http://example.com/image.jpg")
            assertThat(userInfo.attributes).isEqualTo(attributes)
        }

        @Test
        fun `should return null when kakao_account is missing`() {
            // given
            val attributes = mapOf<String, Any>("id" to 123456789L)

            // when
            val userInfo = KakaoOAuth2UserInfo(attributes)

            // then
            assertThat(userInfo.id).isEqualTo("123456789")
            assertThat(userInfo.email).isNull()
            assertThat(userInfo.name).isNull()
            assertThat(userInfo.profileImageUrl).isNull()
        }

        @Test
        fun `should return null when profile is missing`() {
            // given
            val attributes =
                mapOf<String, Any>(
                    "id" to 123456789L,
                    "kakao_account" to
                        mapOf(
                            "email" to "user@kakao.com",
                        ),
                )

            // when
            val userInfo = KakaoOAuth2UserInfo(attributes)

            // then
            assertThat(userInfo.email).isEqualTo("user@kakao.com")
            assertThat(userInfo.name).isNull()
            assertThat(userInfo.profileImageUrl).isNull()
        }
    }

    @Nested
    @DisplayName("GoogleOAuth2UserInfo")
    inner class GoogleOAuth2UserInfoTest {
        @Test
        fun `should parse google user info correctly`() {
            // given
            val attributes =
                mapOf<String, Any>(
                    "sub" to "google_123456789",
                    "email" to "user@gmail.com",
                    "name" to "홍길동",
                    "picture" to "http://example.com/image.jpg",
                )

            // when
            val userInfo = GoogleOAuth2UserInfo(attributes)

            // then
            assertThat(userInfo.provider).isEqualTo(OAuthProvider.GOOGLE)
            assertThat(userInfo.id).isEqualTo("google_123456789")
            assertThat(userInfo.email).isEqualTo("user@gmail.com")
            assertThat(userInfo.name).isEqualTo("홍길동")
            assertThat(userInfo.profileImageUrl).isEqualTo("http://example.com/image.jpg")
            assertThat(userInfo.attributes).isEqualTo(attributes)
        }

        @Test
        fun `should return empty string when sub is missing`() {
            // given
            val attributes = mapOf<String, Any>("email" to "user@gmail.com")

            // when
            val userInfo = GoogleOAuth2UserInfo(attributes)

            // then
            assertThat(userInfo.id).isEqualTo("")
            assertThat(userInfo.email).isEqualTo("user@gmail.com")
        }

        @Test
        fun `should return null for optional fields when missing`() {
            // given
            val attributes = mapOf<String, Any>("sub" to "google_123")

            // when
            val userInfo = GoogleOAuth2UserInfo(attributes)

            // then
            assertThat(userInfo.id).isEqualTo("google_123")
            assertThat(userInfo.email).isNull()
            assertThat(userInfo.name).isNull()
            assertThat(userInfo.profileImageUrl).isNull()
        }
    }

    @Nested
    @DisplayName("NaverOAuth2UserInfo")
    inner class NaverOAuth2UserInfoTest {
        @Test
        fun `should parse naver user info correctly`() {
            // given
            val attributes =
                mapOf<String, Any>(
                    "resultcode" to "00",
                    "message" to "success",
                    "response" to
                        mapOf(
                            "id" to "naver_123456789",
                            "email" to "user@naver.com",
                            "name" to "홍길동",
                            "profile_image" to "http://example.com/image.jpg",
                        ),
                )

            // when
            val userInfo = NaverOAuth2UserInfo(attributes)

            // then
            assertThat(userInfo.provider).isEqualTo(OAuthProvider.NAVER)
            assertThat(userInfo.id).isEqualTo("naver_123456789")
            assertThat(userInfo.email).isEqualTo("user@naver.com")
            assertThat(userInfo.name).isEqualTo("홍길동")
            assertThat(userInfo.profileImageUrl).isEqualTo("http://example.com/image.jpg")
            assertThat(userInfo.attributes).isEqualTo(attributes)
        }

        @Test
        fun `should return empty string when response is missing`() {
            // given
            val attributes =
                mapOf<String, Any>(
                    "resultcode" to "00",
                    "message" to "success",
                )

            // when
            val userInfo = NaverOAuth2UserInfo(attributes)

            // then
            assertThat(userInfo.id).isEqualTo("")
            assertThat(userInfo.email).isNull()
            assertThat(userInfo.name).isNull()
            assertThat(userInfo.profileImageUrl).isNull()
        }

        @Test
        fun `should return null for optional fields when missing in response`() {
            // given
            val attributes =
                mapOf<String, Any>(
                    "response" to mapOf("id" to "naver_123"),
                )

            // when
            val userInfo = NaverOAuth2UserInfo(attributes)

            // then
            assertThat(userInfo.id).isEqualTo("naver_123")
            assertThat(userInfo.email).isNull()
            assertThat(userInfo.name).isNull()
            assertThat(userInfo.profileImageUrl).isNull()
        }
    }
}
