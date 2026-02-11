package com.nextup.infrastructure.security.oauth2

import com.nextup.common.exception.UnsupportedOAuth2ProviderException
import com.nextup.core.domain.user.OAuthProvider
import com.nextup.infrastructure.security.oauth2.impl.GoogleOAuth2UserInfo
import com.nextup.infrastructure.security.oauth2.impl.KakaoOAuth2UserInfo
import com.nextup.infrastructure.security.oauth2.impl.NaverOAuth2UserInfo
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("OAuth2UserInfoFactory 테스트")
class OAuth2UserInfoFactoryTest {
    @Nested
    @DisplayName("create")
    inner class Create {
        @Test
        fun `should create KakaoOAuth2UserInfo for kakao`() {
            // given
            val attributes = mapOf("id" to 123L)

            // when
            val result = OAuth2UserInfoFactory.create("kakao", attributes)

            // then
            assertThat(result).isInstanceOf(KakaoOAuth2UserInfo::class.java)
            assertThat(result.provider).isEqualTo(OAuthProvider.KAKAO)
        }

        @Test
        fun `should create GoogleOAuth2UserInfo for google`() {
            // given
            val attributes = mapOf("sub" to "123")

            // when
            val result = OAuth2UserInfoFactory.create("google", attributes)

            // then
            assertThat(result).isInstanceOf(GoogleOAuth2UserInfo::class.java)
            assertThat(result.provider).isEqualTo(OAuthProvider.GOOGLE)
        }

        @Test
        fun `should create NaverOAuth2UserInfo for naver`() {
            // given
            val attributes = mapOf("response" to mapOf("id" to "123"))

            // when
            val result = OAuth2UserInfoFactory.create("naver", attributes)

            // then
            assertThat(result).isInstanceOf(NaverOAuth2UserInfo::class.java)
            assertThat(result.provider).isEqualTo(OAuthProvider.NAVER)
        }

        @ParameterizedTest
        @ValueSource(strings = ["KAKAO", "Kakao", "GOOGLE", "Google", "NAVER", "Naver"])
        fun `should be case insensitive`(registrationId: String) {
            // when
            val result = OAuth2UserInfoFactory.create(registrationId, emptyMap())

            // then
            assertThat(result).isNotNull
        }

        @Test
        fun `should throw exception for unsupported provider`() {
            // when & then
            assertThatThrownBy {
                OAuth2UserInfoFactory.create("facebook", emptyMap())
            }.isInstanceOf(UnsupportedOAuth2ProviderException::class.java)
        }
    }

    @Nested
    @DisplayName("toOAuthProvider")
    inner class ToOAuthProvider {
        @Test
        fun `should return KAKAO for kakao`() {
            // when
            val result = OAuth2UserInfoFactory.toOAuthProvider("kakao")

            // then
            assertThat(result).isEqualTo(OAuthProvider.KAKAO)
        }

        @Test
        fun `should return GOOGLE for google`() {
            // when
            val result = OAuth2UserInfoFactory.toOAuthProvider("google")

            // then
            assertThat(result).isEqualTo(OAuthProvider.GOOGLE)
        }

        @Test
        fun `should return NAVER for naver`() {
            // when
            val result = OAuth2UserInfoFactory.toOAuthProvider("naver")

            // then
            assertThat(result).isEqualTo(OAuthProvider.NAVER)
        }

        @ParameterizedTest
        @ValueSource(strings = ["KAKAO", "Kakao", "GOOGLE", "Google", "NAVER", "Naver"])
        fun `should be case insensitive`(registrationId: String) {
            // when
            val result = OAuth2UserInfoFactory.toOAuthProvider(registrationId)

            // then
            assertThat(result).isNotNull
        }

        @Test
        fun `should throw exception for unsupported provider`() {
            // when & then
            assertThatThrownBy {
                OAuth2UserInfoFactory.toOAuthProvider("twitter")
            }.isInstanceOf(UnsupportedOAuth2ProviderException::class.java)
        }
    }
}
