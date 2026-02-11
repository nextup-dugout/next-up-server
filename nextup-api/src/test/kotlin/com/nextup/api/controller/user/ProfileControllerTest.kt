package com.nextup.api.controller.user

import com.nextup.api.dto.user.UpdateProfileRequest
import com.nextup.core.domain.user.OAuthProvider
import com.nextup.core.domain.user.User
import com.nextup.core.service.user.UserService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ProfileController 테스트")
class ProfileControllerTest {
    private lateinit var userService: UserService
    private lateinit var profileController: ProfileController

    @BeforeEach
    fun setUp() {
        userService = mockk()
        profileController = ProfileController(userService)
    }

    private fun createTestUser(
        id: Long = 1L,
        hasOAuth: Boolean = false,
    ): User {
        val user =
            User.createLocalUser(
                email = "test@example.com",
                encodedPassword = "encoded",
                nickname = "테스터",
            )
        // Use reflection to set ID
        val idField = user.javaClass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(user, id)

        if (hasOAuth) {
            user.addOAuthAccount(OAuthProvider.GOOGLE, "google123", "test@example.com")
        }

        return user
    }

    @Nested
    @DisplayName("내 프로필 조회")
    inner class GetMyProfile {
        @Test
        fun `should return my profile successfully`() {
            // given
            val userId = 1L
            val user = createTestUser(userId)
            every { userService.getActiveById(userId) } returns user

            // when
            val response = profileController.getMyProfile(userId)

            // then
            assertThat(response.success).isTrue()
            assertThat(response.data?.id).isEqualTo(userId)
            assertThat(response.data?.email).isEqualTo("test@example.com")
            assertThat(response.data?.nickname).isEqualTo("테스터")
            verify { userService.getActiveById(userId) }
        }

        @Test
        fun `should include oauth provider info when linked`() {
            // given
            val userId = 1L
            val user = createTestUser(userId, hasOAuth = true)
            every { userService.getActiveById(userId) } returns user

            // when
            val response = profileController.getMyProfile(userId)

            // then
            assertThat(response.success).isTrue()
            assertThat(response.data?.linkedOAuthProviders).hasSize(1)
            assertThat(
                response.data
                    ?.linkedOAuthProviders
                    ?.first()
                    ?.provider,
            ).isEqualTo("GOOGLE")
        }
    }

    @Nested
    @DisplayName("프로필 수정")
    inner class UpdateMyProfile {
        @Test
        fun `should update profile with new nickname`() {
            // given
            val userId = 1L
            val request = UpdateProfileRequest(nickname = "새닉네임", profileImageUrl = null)
            val updatedUser = createTestUser(userId)

            every { userService.updateProfile(userId, "새닉네임", null) } returns updatedUser

            // when
            val response = profileController.updateMyProfile(userId, request)

            // then
            assertThat(response.success).isTrue()
            verify { userService.updateProfile(userId, "새닉네임", null) }
        }

        @Test
        fun `should update profile with new profile image url`() {
            // given
            val userId = 1L
            val request = UpdateProfileRequest(nickname = null, profileImageUrl = "https://example.com/image.jpg")
            val updatedUser = createTestUser(userId)

            every { userService.updateProfile(userId, null, "https://example.com/image.jpg") } returns updatedUser

            // when
            val response = profileController.updateMyProfile(userId, request)

            // then
            assertThat(response.success).isTrue()
            verify { userService.updateProfile(userId, null, "https://example.com/image.jpg") }
        }
    }

    @Nested
    @DisplayName("연동된 OAuth 계정 조회")
    inner class GetLinkedOAuthAccounts {
        @Test
        fun `should return empty list when no oauth linked`() {
            // given
            val userId = 1L
            val user = createTestUser(userId, hasOAuth = false)
            every { userService.getActiveById(userId) } returns user

            // when
            val response = profileController.getLinkedOAuthAccounts(userId)

            // then
            assertThat(response.success).isTrue()
            assertThat(response.data).isEmpty()
        }

        @Test
        fun `should return oauth accounts when linked`() {
            // given
            val userId = 1L
            val user = createTestUser(userId, hasOAuth = true)
            every { userService.getActiveById(userId) } returns user

            // when
            val response = profileController.getLinkedOAuthAccounts(userId)

            // then
            assertThat(response.success).isTrue()
            assertThat(response.data).hasSize(1)
            assertThat(response.data?.first()?.provider).isEqualTo("GOOGLE")
            assertThat(response.data?.first()?.displayName).isEqualTo("구글")
        }
    }

    @Nested
    @DisplayName("회원 탈퇴")
    inner class DeactivateMyAccount {
        @Test
        fun `should deactivate account successfully`() {
            // given
            val userId = 1L
            val user = createTestUser(userId)
            every { userService.deactivate(userId) } returns user

            // when
            val response = profileController.deactivateMyAccount(userId)

            // then
            assertThat(response.success).isTrue()
            verify { userService.deactivate(userId) }
        }
    }
}

@DisplayName("PublicProfileController 테스트")
class PublicProfileControllerTest {
    private lateinit var userService: UserService
    private lateinit var publicProfileController: PublicProfileController

    @BeforeEach
    fun setUp() {
        userService = mockk()
        publicProfileController = PublicProfileController(userService)
    }

    private fun createTestUser(id: Long = 1L): User {
        val user =
            User.createLocalUser(
                email = "test@example.com",
                encodedPassword = "encoded",
                nickname = "테스터",
            )
        val idField = user.javaClass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(user, id)
        return user
    }

    @Nested
    @DisplayName("공개 프로필 조회")
    inner class GetPublicProfile {
        @Test
        fun `should return public profile successfully`() {
            // given
            val userId = 1L
            val user = createTestUser(userId)
            every { userService.getActiveById(userId) } returns user

            // when
            val response = publicProfileController.getPublicProfile(userId)

            // then
            assertThat(response.success).isTrue()
            assertThat(response.data?.id).isEqualTo(userId)
            assertThat(response.data?.nickname).isEqualTo("테스터")
            assertThat(response.data?.hasLinkedPlayer).isFalse()
            verify { userService.getActiveById(userId) }
        }
    }
}
