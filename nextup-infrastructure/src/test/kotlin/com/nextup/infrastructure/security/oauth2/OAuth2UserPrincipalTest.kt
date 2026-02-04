package com.nextup.infrastructure.security.oauth2

import com.nextup.core.domain.user.OAuthProvider
import com.nextup.core.domain.user.Role
import com.nextup.core.domain.user.User
import com.nextup.infrastructure.security.oauth2.impl.KakaoOAuth2UserInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("OAuth2UserPrincipal 테스트")
class OAuth2UserPrincipalTest {
    private fun createTestUser(id: Long = 1L): User {
        val user =
            User.createOAuthUser(
                email = "test@example.com",
                nickname = "테스터",
                provider = OAuthProvider.KAKAO,
                oauthId = "kakao_123",
            )
        val idField = User::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(user, id)
        return user
    }

    private fun createOAuth2UserInfo(): OAuth2UserInfo =
        KakaoOAuth2UserInfo(
            mapOf(
                "id" to 123456789L,
                "kakao_account" to
                    mapOf(
                        "email" to "test@kakao.com",
                        "profile" to
                            mapOf(
                                "nickname" to "카카오유저",
                            ),
                    ),
            ),
        )

    @Nested
    @DisplayName("기본 속성")
    inner class BasicProperties {
        @Test
        fun `should return user id`() {
            // given
            val user = createTestUser(id = 42L)
            val oAuth2UserInfo = createOAuth2UserInfo()
            val principal = OAuth2UserPrincipal(user, oAuth2UserInfo)

            // then
            assertThat(principal.userId).isEqualTo(42L)
        }

        @Test
        fun `should return user email`() {
            // given
            val user = createTestUser()
            val oAuth2UserInfo = createOAuth2UserInfo()
            val principal = OAuth2UserPrincipal(user, oAuth2UserInfo)

            // then
            assertThat(principal.email).isEqualTo("test@example.com")
        }

        @Test
        fun `should return user nickname`() {
            // given
            val user = createTestUser()
            val oAuth2UserInfo = createOAuth2UserInfo()
            val principal = OAuth2UserPrincipal(user, oAuth2UserInfo)

            // then
            assertThat(principal.nickname).isEqualTo("테스터")
        }

        @Test
        fun `should return isNewUser flag`() {
            // given
            val user = createTestUser()
            val oAuth2UserInfo = createOAuth2UserInfo()

            // when
            val newUserPrincipal = OAuth2UserPrincipal(user, oAuth2UserInfo, isNewUser = true)
            val existingUserPrincipal = OAuth2UserPrincipal(user, oAuth2UserInfo, isNewUser = false)

            // then
            assertThat(newUserPrincipal.isNewUser).isTrue()
            assertThat(existingUserPrincipal.isNewUser).isFalse()
        }
    }

    @Nested
    @DisplayName("OAuth2User 인터페이스")
    inner class OAuth2UserInterface {
        @Test
        fun `getName should return user id as string`() {
            // given
            val user = createTestUser(id = 123L)
            val oAuth2UserInfo = createOAuth2UserInfo()
            val principal = OAuth2UserPrincipal(user, oAuth2UserInfo)

            // when
            val name = principal.name

            // then
            assertThat(name).isEqualTo("123")
        }

        @Test
        fun `getAttributes should return OAuth2UserInfo attributes`() {
            // given
            val user = createTestUser()
            val oAuth2UserInfo = createOAuth2UserInfo()
            val principal = OAuth2UserPrincipal(user, oAuth2UserInfo)

            // when
            val attributes = principal.attributes

            // then
            assertThat(attributes).containsKey("id")
            assertThat(attributes).containsKey("kakao_account")
        }

        @Test
        fun `getAuthorities should return user roles with ROLE_ prefix`() {
            // given
            val user = createTestUser()
            user.addRole(Role.ADMIN)
            val oAuth2UserInfo = createOAuth2UserInfo()
            val principal = OAuth2UserPrincipal(user, oAuth2UserInfo)

            // when
            val authorities = principal.authorities.map { it.authority }

            // then
            assertThat(authorities).contains("ROLE_USER", "ROLE_ADMIN")
        }
    }

    @Nested
    @DisplayName("getRoleNames")
    inner class GetRoleNames {
        @Test
        fun `should return role names without ROLE_ prefix`() {
            // given
            val user = createTestUser()
            user.addRole(Role.SCORER)
            val oAuth2UserInfo = createOAuth2UserInfo()
            val principal = OAuth2UserPrincipal(user, oAuth2UserInfo)

            // when
            val roleNames = principal.getRoleNames()

            // then
            assertThat(roleNames).containsExactlyInAnyOrder("USER", "SCORER")
        }
    }
}
