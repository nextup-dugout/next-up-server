package com.nextup.core.domain.user

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("OAuthAccount 테스트")
class OAuthAccountTest {

    private fun createTestUser(): User {
        return User.createLocalUser(
            email = "test@example.com",
            encodedPassword = "password",
            nickname = "테스터"
        )
    }

    @Nested
    @DisplayName("OAuth 계정 생성")
    inner class Create {

        @Test
        fun `should create oauth account successfully`() {
            // given
            val user = createTestUser()

            // when
            val account = OAuthAccount.create(
                user = user,
                provider = OAuthProvider.KAKAO,
                oauthId = "kakao_12345",
                email = "kakao@example.com"
            )

            // then
            assertThat(account.user).isEqualTo(user)
            assertThat(account.provider).isEqualTo(OAuthProvider.KAKAO)
            assertThat(account.oauthId).isEqualTo("kakao_12345")
            assertThat(account.email).isEqualTo("kakao@example.com")
            assertThat(account.connectedAt).isNotNull()
        }

        @Test
        fun `should create oauth account without email`() {
            // given
            val user = createTestUser()

            // when
            val account = OAuthAccount.create(
                user = user,
                provider = OAuthProvider.GOOGLE,
                oauthId = "google_12345"
            )

            // then
            assertThat(account.email).isNull()
        }

        @Test
        fun `should throw exception when provider is LOCAL`() {
            // given
            val user = createTestUser()

            // when & then
            assertThrows<IllegalArgumentException> {
                OAuthAccount.create(
                    user = user,
                    provider = OAuthProvider.LOCAL,
                    oauthId = "local_123"
                )
            }
        }

        @Test
        fun `should throw exception when oauthId is blank`() {
            // given
            val user = createTestUser()

            // when & then
            assertThrows<IllegalArgumentException> {
                OAuthAccount.create(
                    user = user,
                    provider = OAuthProvider.KAKAO,
                    oauthId = ""
                )
            }
        }

        @Test
        fun `should throw exception when oauthId is whitespace only`() {
            // given
            val user = createTestUser()

            // when & then
            assertThrows<IllegalArgumentException> {
                OAuthAccount.create(
                    user = user,
                    provider = OAuthProvider.NAVER,
                    oauthId = "   "
                )
            }
        }
    }

    @Nested
    @DisplayName("OAuth Provider 종류")
    inner class ProviderTypes {

        @Test
        fun `should create account for each provider`() {
            // given
            val user = createTestUser()
            val providers = listOf(
                OAuthProvider.KAKAO,
                OAuthProvider.GOOGLE,
                OAuthProvider.NAVER,
                OAuthProvider.APPLE
            )

            // when & then
            providers.forEach { provider ->
                val account = OAuthAccount.create(
                    user = user,
                    provider = provider,
                    oauthId = "${provider.name.lowercase()}_123"
                )
                assertThat(account.provider).isEqualTo(provider)
            }
        }

        @Test
        fun `should have correct display names for providers`() {
            // then
            assertThat(OAuthProvider.KAKAO.displayName).isEqualTo("카카오")
            assertThat(OAuthProvider.GOOGLE.displayName).isEqualTo("구글")
            assertThat(OAuthProvider.NAVER.displayName).isEqualTo("네이버")
            assertThat(OAuthProvider.APPLE.displayName).isEqualTo("애플")
            assertThat(OAuthProvider.LOCAL.displayName).isEqualTo("일반")
        }
    }
}
