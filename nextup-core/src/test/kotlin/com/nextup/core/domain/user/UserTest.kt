package com.nextup.core.domain.user

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("User 테스트")
class UserTest {
    @Nested
    @DisplayName("LOCAL 사용자 생성")
    inner class CreateLocalUser {
        @Test
        fun `should create local user successfully`() {
            // when
            val user =
                User.createLocalUser(
                    email = "test@example.com",
                    encodedPassword = "encoded_password",
                    nickname = "테스터",
                )

            // then
            assertThat(user.email).isEqualTo("test@example.com")
            assertThat(user.password).isEqualTo("encoded_password")
            assertThat(user.nickname).isEqualTo("테스터")
            assertThat(user.isLocalUser).isTrue()
            assertThat(user.hasOAuthAccount).isFalse()
            assertThat(user.roles).contains(Role.USER)
            assertThat(user.isActive).isTrue()
        }

        @Test
        fun `should throw exception when email is blank`() {
            // when & then
            assertThrows<IllegalArgumentException> {
                User.createLocalUser(
                    email = "",
                    encodedPassword = "password",
                    nickname = "테스터",
                )
            }
        }

        @Test
        fun `should throw exception when password is blank`() {
            // when & then
            assertThrows<IllegalArgumentException> {
                User.createLocalUser(
                    email = "test@example.com",
                    encodedPassword = "",
                    nickname = "테스터",
                )
            }
        }

        @Test
        fun `should throw exception when nickname is blank`() {
            // when & then
            assertThrows<IllegalArgumentException> {
                User.createLocalUser(
                    email = "test@example.com",
                    encodedPassword = "password",
                    nickname = "",
                )
            }
        }
    }

    @Nested
    @DisplayName("OAuth 사용자 생성")
    inner class CreateOAuthUser {
        @Test
        fun `should create oauth user successfully`() {
            // when
            val user =
                User.createOAuthUser(
                    email = "oauth@example.com",
                    nickname = "OAuth테스터",
                    provider = OAuthProvider.KAKAO,
                    oauthId = "kakao_12345",
                    profileImageUrl = "http://example.com/profile.jpg",
                )

            // then
            assertThat(user.email).isEqualTo("oauth@example.com")
            assertThat(user.password).isNull()
            assertThat(user.nickname).isEqualTo("OAuth테스터")
            assertThat(user.isLocalUser).isFalse()
            assertThat(user.hasOAuthAccount).isTrue()
            assertThat(user.oauthAccounts).hasSize(1)
            assertThat(user.hasOAuthProvider(OAuthProvider.KAKAO)).isTrue()
            assertThat(user.profileImageUrl).isEqualTo("http://example.com/profile.jpg")
        }

        @Test
        fun `should throw exception when provider is LOCAL`() {
            // when & then
            assertThrows<IllegalArgumentException> {
                User.createOAuthUser(
                    email = "test@example.com",
                    nickname = "테스터",
                    provider = OAuthProvider.LOCAL,
                    oauthId = "local_123",
                )
            }
        }
    }

    @Nested
    @DisplayName("OAuth 계정 연동")
    inner class OAuthAccountLink {
        @Test
        fun `should add oauth account to local user`() {
            // given
            val user =
                User.createLocalUser(
                    email = "test@example.com",
                    encodedPassword = "password",
                    nickname = "테스터",
                )

            // when
            user.addOAuthAccount(OAuthProvider.GOOGLE, "google_123", "google@example.com")

            // then
            assertThat(user.hasOAuthAccount).isTrue()
            assertThat(user.hasOAuthProvider(OAuthProvider.GOOGLE)).isTrue()
            assertThat(user.oauthAccounts).hasSize(1)
        }

        @Test
        fun `should add multiple oauth accounts`() {
            // given
            val user =
                User.createLocalUser(
                    email = "test@example.com",
                    encodedPassword = "password",
                    nickname = "테스터",
                )

            // when
            user.addOAuthAccount(OAuthProvider.KAKAO, "kakao_123")
            user.addOAuthAccount(OAuthProvider.GOOGLE, "google_123")

            // then
            assertThat(user.oauthAccounts).hasSize(2)
            assertThat(user.hasOAuthProvider(OAuthProvider.KAKAO)).isTrue()
            assertThat(user.hasOAuthProvider(OAuthProvider.GOOGLE)).isTrue()
        }

        @Test
        fun `should throw exception when adding duplicate provider`() {
            // given
            val user =
                User.createOAuthUser(
                    email = "test@example.com",
                    nickname = "테스터",
                    provider = OAuthProvider.KAKAO,
                    oauthId = "kakao_123",
                )

            // when & then
            assertThrows<IllegalArgumentException> {
                user.addOAuthAccount(OAuthProvider.KAKAO, "kakao_456")
            }
        }

        @Test
        fun `should remove oauth account when multiple auth methods exist`() {
            // given
            val user =
                User.createLocalUser(
                    email = "test@example.com",
                    encodedPassword = "password",
                    nickname = "테스터",
                )
            user.addOAuthAccount(OAuthProvider.KAKAO, "kakao_123")
            user.addOAuthAccount(OAuthProvider.GOOGLE, "google_123")

            // when
            user.removeOAuthAccount(OAuthProvider.KAKAO)

            // then
            assertThat(user.hasOAuthProvider(OAuthProvider.KAKAO)).isFalse()
            assertThat(user.hasOAuthProvider(OAuthProvider.GOOGLE)).isTrue()
            assertThat(user.oauthAccounts).hasSize(1)
        }

        @Test
        fun `should throw exception when removing last auth method`() {
            // given
            val user =
                User.createOAuthUser(
                    email = "test@example.com",
                    nickname = "테스터",
                    provider = OAuthProvider.KAKAO,
                    oauthId = "kakao_123",
                )

            // when & then
            assertThrows<IllegalArgumentException> {
                user.removeOAuthAccount(OAuthProvider.KAKAO)
            }
        }
    }

    @Nested
    @DisplayName("인증 수단 관리")
    inner class AuthMethodManagement {
        @Test
        fun `should allow removing auth method when multiple exist`() {
            // given
            val user =
                User.createLocalUser(
                    email = "test@example.com",
                    encodedPassword = "password",
                    nickname = "테스터",
                )
            user.addOAuthAccount(OAuthProvider.KAKAO, "kakao_123")

            // when & then
            assertThat(user.canRemoveAuthMethod()).isTrue()
        }

        @Test
        fun `should not allow removing last auth method`() {
            // given
            val user =
                User.createLocalUser(
                    email = "test@example.com",
                    encodedPassword = "password",
                    nickname = "테스터",
                )

            // when & then
            assertThat(user.canRemoveAuthMethod()).isFalse()
        }

        @Test
        fun `should remove password when oauth account exists`() {
            // given
            val user =
                User.createLocalUser(
                    email = "test@example.com",
                    encodedPassword = "password",
                    nickname = "테스터",
                )
            user.addOAuthAccount(OAuthProvider.KAKAO, "kakao_123")

            // when
            user.removePassword()

            // then
            assertThat(user.password).isNull()
            assertThat(user.isLocalUser).isFalse()
        }

        @Test
        fun `should throw exception when removing password without oauth`() {
            // given
            val user =
                User.createLocalUser(
                    email = "test@example.com",
                    encodedPassword = "password",
                    nickname = "테스터",
                )

            // when & then
            assertThrows<IllegalArgumentException> {
                user.removePassword()
            }
        }
    }

    @Nested
    @DisplayName("역할 관리")
    inner class RoleManagement {
        @Test
        fun `should add role successfully`() {
            // given
            val user =
                User.createLocalUser(
                    email = "test@example.com",
                    encodedPassword = "password",
                    nickname = "테스터",
                )

            // when
            user.addRole(Role.ADMIN)

            // then
            assertThat(user.hasRole(Role.ADMIN)).isTrue()
            assertThat(user.hasRole(Role.USER)).isTrue()
        }

        @Test
        fun `should remove role successfully`() {
            // given
            val user =
                User.createLocalUser(
                    email = "test@example.com",
                    encodedPassword = "password",
                    nickname = "테스터",
                )
            user.addRole(Role.SCORER)

            // when
            user.removeRole(Role.SCORER)

            // then
            assertThat(user.hasRole(Role.SCORER)).isFalse()
        }

        @Test
        fun `should throw exception when removing USER role`() {
            // given
            val user =
                User.createLocalUser(
                    email = "test@example.com",
                    encodedPassword = "password",
                    nickname = "테스터",
                )

            // when & then
            assertThrows<IllegalArgumentException> {
                user.removeRole(Role.USER)
            }
        }
    }

    @Nested
    @DisplayName("프로필 업데이트")
    inner class ProfileUpdate {
        @Test
        fun `should update profile successfully`() {
            // given
            val user =
                User.createLocalUser(
                    email = "test@example.com",
                    encodedPassword = "password",
                    nickname = "원래닉네임",
                )

            // when
            user.updateProfile(
                nickname = "새닉네임",
                profileImageUrl = "http://example.com/new-profile.jpg",
            )

            // then
            assertThat(user.nickname).isEqualTo("새닉네임")
            assertThat(user.profileImageUrl).isEqualTo("http://example.com/new-profile.jpg")
        }

        @Test
        fun `should change password successfully`() {
            // given
            val user =
                User.createLocalUser(
                    email = "test@example.com",
                    encodedPassword = "old_password",
                    nickname = "테스터",
                )

            // when
            user.changePassword("new_password")

            // then
            assertThat(user.password).isEqualTo("new_password")
        }
    }

    @Nested
    @DisplayName("계정 상태 관리")
    inner class AccountStatus {
        @Test
        fun `should deactivate account`() {
            // given
            val user =
                User.createLocalUser(
                    email = "test@example.com",
                    encodedPassword = "password",
                    nickname = "테스터",
                )

            // when
            user.deactivate()

            // then
            assertThat(user.isActive).isFalse()
        }

        @Test
        fun `should activate account`() {
            // given
            val user =
                User.createLocalUser(
                    email = "test@example.com",
                    encodedPassword = "password",
                    nickname = "테스터",
                )
            user.deactivate()

            // when
            user.activate()

            // then
            assertThat(user.isActive).isTrue()
        }
    }
}
