package com.nextup.core.service.user

import com.nextup.common.exception.*
import com.nextup.core.domain.user.OAuthAccount
import com.nextup.core.domain.user.OAuthProvider
import com.nextup.core.domain.user.Role
import com.nextup.core.domain.user.User
import com.nextup.core.port.repository.OAuthAccountRepositoryPort
import com.nextup.core.port.repository.UserRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.*

@DisplayName("UserService")
class UserServiceTest {

    private lateinit var userRepository: UserRepositoryPort
    private lateinit var oauthAccountRepository: OAuthAccountRepositoryPort
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        oauthAccountRepository = mockk()
        userService = UserService(userRepository, oauthAccountRepository)
    }

    @Nested
    @DisplayName("createLocalUser")
    inner class CreateLocalUser {

        @Test
        fun `should create local user successfully`() {
            // given
            val email = "test@example.com"
            val password = "encoded_password"
            val nickname = "테스터"

            every { userRepository.existsByEmail(email) } returns false
            every { userRepository.save(any()) } answers { firstArg() }

            // when
            val result = userService.createLocalUser(email, password, nickname)

            // then
            assertThat(result.email).isEqualTo(email)
            assertThat(result.nickname).isEqualTo(nickname)
            assertThat(result.isLocalUser).isTrue()
            verify { userRepository.save(any()) }
        }

        @Test
        fun `should throw exception when email is duplicated`() {
            // given
            val email = "duplicate@example.com"
            every { userRepository.existsByEmail(email) } returns true

            // when & then
            assertThatThrownBy {
                userService.createLocalUser(email, "password", "테스터")
            }.isInstanceOf(EmailDuplicateException::class.java)
        }
    }

    @Nested
    @DisplayName("createOrLinkOAuthUser")
    inner class CreateOrLinkOAuthUser {

        @Test
        fun `should return existing user when oauth account exists`() {
            // given
            val user = createTestUser(1L, "existing@example.com")
            val oauthAccount = mockk<OAuthAccount>()
            every { oauthAccount.user } returns user
            every { oauthAccountRepository.findByProviderAndOauthId(OAuthProvider.KAKAO, "kakao_123") } returns oauthAccount

            // when
            val (resultUser, isNew) = userService.createOrLinkOAuthUser(
                email = "new@example.com",
                nickname = "새사용자",
                provider = OAuthProvider.KAKAO,
                oauthId = "kakao_123"
            )

            // then
            assertThat(resultUser.id).isEqualTo(1L)
            assertThat(isNew).isFalse()
        }

        @Test
        fun `should link oauth to existing user with same email`() {
            // given
            val existingUser = createTestUser(1L, "existing@example.com")
            every { oauthAccountRepository.findByProviderAndOauthId(OAuthProvider.GOOGLE, "google_123") } returns null
            every { userRepository.findByEmail("existing@example.com") } returns existingUser

            // when
            val (resultUser, isNew) = userService.createOrLinkOAuthUser(
                email = "existing@example.com",
                nickname = "새닉네임",
                provider = OAuthProvider.GOOGLE,
                oauthId = "google_123"
            )

            // then
            assertThat(resultUser.id).isEqualTo(1L)
            assertThat(resultUser.hasOAuthProvider(OAuthProvider.GOOGLE)).isTrue()
            assertThat(isNew).isFalse()
        }

        @Test
        fun `should create new user when no existing user found`() {
            // given
            every { oauthAccountRepository.findByProviderAndOauthId(OAuthProvider.NAVER, "naver_123") } returns null
            every { userRepository.findByEmail("new@example.com") } returns null
            every { userRepository.save(any()) } answers { firstArg() }

            // when
            val (resultUser, isNew) = userService.createOrLinkOAuthUser(
                email = "new@example.com",
                nickname = "새사용자",
                provider = OAuthProvider.NAVER,
                oauthId = "naver_123"
            )

            // then
            assertThat(resultUser.email).isEqualTo("new@example.com")
            assertThat(resultUser.hasOAuthProvider(OAuthProvider.NAVER)).isTrue()
            assertThat(isNew).isTrue()
            verify { userRepository.save(any()) }
        }

        @Test
        fun `should update profile image when linking oauth to existing user without profile image`() {
            // given
            val existingUser = createTestUser(1L, "existing@example.com")
            every { oauthAccountRepository.findByProviderAndOauthId(OAuthProvider.GOOGLE, "google_123") } returns null
            every { userRepository.findByEmail("existing@example.com") } returns existingUser

            // when
            val (resultUser, isNew) = userService.createOrLinkOAuthUser(
                email = "existing@example.com",
                nickname = "새닉네임",
                provider = OAuthProvider.GOOGLE,
                oauthId = "google_123",
                profileImageUrl = "http://example.com/profile.jpg"
            )

            // then
            assertThat(resultUser.profileImageUrl).isEqualTo("http://example.com/profile.jpg")
            assertThat(isNew).isFalse()
        }

        @Test
        fun `should not update profile image when existing user already has one`() {
            // given
            val existingUser = createTestUser(1L, "existing@example.com").apply {
                updateProfile(profileImageUrl = "http://example.com/existing.jpg")
            }
            every { oauthAccountRepository.findByProviderAndOauthId(OAuthProvider.GOOGLE, "google_123") } returns null
            every { userRepository.findByEmail("existing@example.com") } returns existingUser

            // when
            val (resultUser, isNew) = userService.createOrLinkOAuthUser(
                email = "existing@example.com",
                nickname = "새닉네임",
                provider = OAuthProvider.GOOGLE,
                oauthId = "google_123",
                profileImageUrl = "http://example.com/new.jpg"
            )

            // then
            assertThat(resultUser.profileImageUrl).isEqualTo("http://example.com/existing.jpg")
            assertThat(isNew).isFalse()
        }
    }

    @Nested
    @DisplayName("getById")
    inner class GetById {

        @Test
        fun `should return user when found`() {
            // given
            val user = createTestUser(1L, "test@example.com")
            every { userRepository.findByIdOrNull(1L) } returns user

            // when
            val result = userService.getById(1L)

            // then
            assertThat(result.id).isEqualTo(1L)
            assertThat(result.email).isEqualTo("test@example.com")
        }

        @Test
        fun `should throw exception when not found`() {
            // given
            every { userRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                userService.getById(999L)
            }.isInstanceOf(UserNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("getActiveById")
    inner class GetActiveById {

        @Test
        fun `should return active user`() {
            // given
            val user = createTestUser(1L, "active@example.com")
            every { userRepository.findByIdOrNull(1L) } returns user

            // when
            val result = userService.getActiveById(1L)

            // then
            assertThat(result.isActive).isTrue()
        }

        @Test
        fun `should throw exception when user is deactivated`() {
            // given
            val user = createTestUser(1L, "inactive@example.com").apply { deactivate() }
            every { userRepository.findByIdOrNull(1L) } returns user

            // when & then
            assertThatThrownBy {
                userService.getActiveById(1L)
            }.isInstanceOf(UserDeactivatedException::class.java)
        }
    }

    @Nested
    @DisplayName("getByEmail")
    inner class GetByEmail {

        @Test
        fun `should return user when found by email`() {
            // given
            val user = createTestUser(1L, "found@example.com")
            every { userRepository.findByEmail("found@example.com") } returns user

            // when
            val result = userService.getByEmail("found@example.com")

            // then
            assertThat(result.email).isEqualTo("found@example.com")
        }

        @Test
        fun `should throw exception when not found by email`() {
            // given
            every { userRepository.findByEmail("notfound@example.com") } returns null

            // when & then
            assertThatThrownBy {
                userService.getByEmail("notfound@example.com")
            }.isInstanceOf(UserNotFoundByEmailException::class.java)
        }
    }

    @Nested
    @DisplayName("findByEmail")
    inner class FindByEmail {

        @Test
        fun `should return user when found`() {
            // given
            val user = createTestUser(1L, "test@example.com")
            every { userRepository.findByEmail("test@example.com") } returns user

            // when
            val result = userService.findByEmail("test@example.com")

            // then
            assertThat(result).isNotNull
            assertThat(result?.email).isEqualTo("test@example.com")
        }

        @Test
        fun `should return null when not found`() {
            // given
            every { userRepository.findByEmail("notexists@example.com") } returns null

            // when
            val result = userService.findByEmail("notexists@example.com")

            // then
            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("findByOAuth")
    inner class FindByOAuth {

        @Test
        fun `should return user when oauth account found`() {
            // given
            val user = createTestUser(1L, "oauth@example.com")
            val oauthAccount = mockk<OAuthAccount>()
            every { oauthAccount.user } returns user
            every { oauthAccountRepository.findByProviderAndOauthId(OAuthProvider.KAKAO, "kakao_123") } returns oauthAccount

            // when
            val result = userService.findByOAuth(OAuthProvider.KAKAO, "kakao_123")

            // then
            assertThat(result).isNotNull
            assertThat(result?.id).isEqualTo(1L)
        }

        @Test
        fun `should return null when oauth account not found`() {
            // given
            every { oauthAccountRepository.findByProviderAndOauthId(OAuthProvider.GOOGLE, "google_999") } returns null

            // when
            val result = userService.findByOAuth(OAuthProvider.GOOGLE, "google_999")

            // then
            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("getAllActive")
    inner class GetAllActive {

        @Test
        fun `should return paginated active users`() {
            // given
            val users = listOf(
                createTestUser(1L, "user1@example.com"),
                createTestUser(2L, "user2@example.com")
            )
            val pageable = PageRequest.of(0, 10)
            every { userRepository.findAllActive(pageable) } returns PageImpl(users)

            // when
            val result = userService.getAllActive(pageable)

            // then
            assertThat(result.content).hasSize(2)
        }
    }

    @Nested
    @DisplayName("getAll")
    inner class GetAll {

        @Test
        fun `should return all users paginated`() {
            // given
            val users = listOf(
                createTestUser(1L, "user1@example.com"),
                createTestUser(2L, "user2@example.com"),
                createTestUser(3L, "user3@example.com")
            )
            val pageable = PageRequest.of(0, 10)
            every { userRepository.findAllByIsActive(true, pageable) } returns PageImpl(users)

            // when
            val result = userService.getAll(pageable)

            // then
            assertThat(result.content).hasSize(3)
        }
    }

    @Nested
    @DisplayName("getAllByStatus")
    inner class GetAllByStatus {

        @Test
        fun `should return active users only`() {
            // given
            val activeUsers = listOf(createTestUser(1L, "active@example.com"))
            val pageable = PageRequest.of(0, 10)
            every { userRepository.findAllByIsActive(true, pageable) } returns PageImpl(activeUsers)

            // when
            val result = userService.getAllByStatus(true, pageable)

            // then
            assertThat(result.content).hasSize(1)
        }

        @Test
        fun `should return inactive users only`() {
            // given
            val inactiveUsers = listOf(
                createTestUser(1L, "inactive@example.com").apply { deactivate() }
            )
            val pageable = PageRequest.of(0, 10)
            every { userRepository.findAllByIsActive(false, pageable) } returns PageImpl(inactiveUsers)

            // when
            val result = userService.getAllByStatus(false, pageable)

            // then
            assertThat(result.content).hasSize(1)
        }
    }

    @Nested
    @DisplayName("search")
    inner class Search {

        @Test
        fun `should search users by keyword`() {
            // given
            val users = listOf(createTestUser(1L, "test@example.com"))
            val pageable = PageRequest.of(0, 10)
            every { userRepository.searchByKeyword("test", pageable) } returns PageImpl(users)

            // when
            val result = userService.search("test", pageable)

            // then
            assertThat(result.content).hasSize(1)
        }
    }

    @Nested
    @DisplayName("getAllByRole")
    inner class GetAllByRole {

        @Test
        fun `should return users with specific role`() {
            // given
            val admins = listOf(
                createTestUser(1L, "admin@example.com").apply { addRole(Role.ADMIN) }
            )
            val pageable = PageRequest.of(0, 10)
            every { userRepository.findAllByRole(Role.ADMIN, pageable) } returns PageImpl(admins)

            // when
            val result = userService.getAllByRole(Role.ADMIN, pageable)

            // then
            assertThat(result.content).hasSize(1)
        }
    }

    @Nested
    @DisplayName("updateProfile")
    inner class UpdateProfile {

        @Test
        fun `should update profile successfully`() {
            // given
            val user = createTestUser(1L, "test@example.com")
            every { userRepository.findByIdOrNull(1L) } returns user

            // when
            val result = userService.updateProfile(
                userId = 1L,
                nickname = "새닉네임",
                profileImageUrl = "http://example.com/new.jpg"
            )

            // then
            assertThat(result.nickname).isEqualTo("새닉네임")
            assertThat(result.profileImageUrl).isEqualTo("http://example.com/new.jpg")
        }

        @Test
        fun `should keep existing nickname when null provided`() {
            // given
            val user = createTestUser(1L, "test@example.com")
            every { userRepository.findByIdOrNull(1L) } returns user

            // when
            val result = userService.updateProfile(
                userId = 1L,
                nickname = null,
                profileImageUrl = "http://example.com/new.jpg"
            )

            // then
            assertThat(result.nickname).isEqualTo("테스터")
            assertThat(result.profileImageUrl).isEqualTo("http://example.com/new.jpg")
        }
    }

    @Nested
    @DisplayName("changePassword")
    inner class ChangePassword {

        @Test
        fun `should change password successfully`() {
            // given
            val user = createTestUser(1L, "test@example.com")
            every { userRepository.findByIdOrNull(1L) } returns user

            // when
            val result = userService.changePassword(1L, "new_encoded_password")

            // then
            assertThat(result.password).isEqualTo("new_encoded_password")
        }
    }

    @Nested
    @DisplayName("addRole / removeRole")
    inner class RoleManagement {

        @Test
        fun `should add role to user`() {
            // given
            val user = createTestUser(1L, "test@example.com")
            every { userRepository.findByIdOrNull(1L) } returns user

            // when
            val result = userService.addRole(1L, Role.ADMIN)

            // then
            assertThat(result.hasRole(Role.ADMIN)).isTrue()
        }

        @Test
        fun `should remove role from user`() {
            // given
            val user = createTestUser(1L, "test@example.com").apply { addRole(Role.SCORER) }
            every { userRepository.findByIdOrNull(1L) } returns user

            // when
            val result = userService.removeRole(1L, Role.SCORER)

            // then
            assertThat(result.hasRole(Role.SCORER)).isFalse()
        }
    }

    @Nested
    @DisplayName("linkOAuthAccount")
    inner class LinkOAuthAccount {

        @Test
        fun `should link oauth account successfully`() {
            // given
            val user = createTestUser(1L, "test@example.com")
            every { userRepository.findByIdOrNull(1L) } returns user
            every { oauthAccountRepository.findByProviderAndOauthId(OAuthProvider.KAKAO, "kakao_new") } returns null

            // when
            val result = userService.linkOAuthAccount(1L, OAuthProvider.KAKAO, "kakao_new")

            // then
            assertThat(result.hasOAuthProvider(OAuthProvider.KAKAO)).isTrue()
        }

        @Test
        fun `should throw exception when oauth already linked to another user`() {
            // given
            val user = createTestUser(1L, "test@example.com")
            val anotherUser = createTestUser(2L, "another@example.com")
            val existingOAuth = mockk<OAuthAccount>()
            every { existingOAuth.user } returns anotherUser

            every { userRepository.findByIdOrNull(1L) } returns user
            every { oauthAccountRepository.findByProviderAndOauthId(OAuthProvider.KAKAO, "kakao_used") } returns existingOAuth

            // when & then
            assertThatThrownBy {
                userService.linkOAuthAccount(1L, OAuthProvider.KAKAO, "kakao_used")
            }.isInstanceOf(OAuthAccountAlreadyLinkedException::class.java)
        }

        @Test
        fun `should allow linking oauth account already linked to same user`() {
            // given
            val user = createTestUser(1L, "test@example.com")
            val existingOAuth = mockk<OAuthAccount>()
            every { existingOAuth.user } returns user

            every { userRepository.findByIdOrNull(1L) } returns user
            every { oauthAccountRepository.findByProviderAndOauthId(OAuthProvider.KAKAO, "kakao_123") } returns existingOAuth

            // when - no exception should be thrown
            val result = userService.linkOAuthAccount(1L, OAuthProvider.KAKAO, "kakao_123")

            // then
            assertThat(result.id).isEqualTo(1L)
        }
    }

    @Nested
    @DisplayName("unlinkOAuthAccount")
    inner class UnlinkOAuthAccount {

        @Test
        fun `should unlink oauth account when multiple auth methods exist`() {
            // given
            val user = createTestUser(1L, "test@example.com") // local user with password
            user.addOAuthAccount(OAuthProvider.KAKAO, "kakao_123")
            every { userRepository.findByIdOrNull(1L) } returns user

            // when
            val result = userService.unlinkOAuthAccount(1L, OAuthProvider.KAKAO)

            // then
            assertThat(result.hasOAuthProvider(OAuthProvider.KAKAO)).isFalse()
        }

        @Test
        fun `should throw exception when trying to remove last auth method`() {
            // given - OAuth only user (no password)
            val user = User.createOAuthUser(
                email = "oauth@example.com",
                nickname = "OAuth사용자",
                provider = OAuthProvider.KAKAO,
                oauthId = "kakao_only"
            )
            setUserId(user, 1L)
            every { userRepository.findByIdOrNull(1L) } returns user

            // when & then
            assertThatThrownBy {
                userService.unlinkOAuthAccount(1L, OAuthProvider.KAKAO)
            }.isInstanceOf(InsufficientAuthMethodException::class.java)
        }
    }

    @Nested
    @DisplayName("getLinkedProviders")
    inner class GetLinkedProviders {

        @Test
        fun `should return list of linked providers`() {
            // given
            val providers = listOf(OAuthProvider.KAKAO, OAuthProvider.GOOGLE)
            every { oauthAccountRepository.findProvidersByUserId(1L) } returns providers

            // when
            val result = userService.getLinkedProviders(1L)

            // then
            assertThat(result).hasSize(2)
            assertThat(result).containsExactly(OAuthProvider.KAKAO, OAuthProvider.GOOGLE)
        }

        @Test
        fun `should return empty list when no providers linked`() {
            // given
            every { oauthAccountRepository.findProvidersByUserId(1L) } returns emptyList()

            // when
            val result = userService.getLinkedProviders(1L)

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("deactivate / activate")
    inner class DeactivateActivate {

        @Test
        fun `should deactivate user`() {
            // given
            val user = createTestUser(1L, "test@example.com")
            every { userRepository.findByIdOrNull(1L) } returns user

            // when
            val result = userService.deactivate(1L)

            // then
            assertThat(result.isActive).isFalse()
        }

        @Test
        fun `should activate user`() {
            // given
            val user = createTestUser(1L, "test@example.com").apply { deactivate() }
            every { userRepository.findByIdOrNull(1L) } returns user

            // when
            val result = userService.activate(1L)

            // then
            assertThat(result.isActive).isTrue()
        }
    }

    @Nested
    @DisplayName("utility methods")
    inner class UtilityMethods {

        @Test
        fun `should validate email not duplicate`() {
            // given
            every { userRepository.existsByEmail("unique@example.com") } returns false

            // when & then (no exception)
            userService.validateEmailNotDuplicate("unique@example.com")
        }

        @Test
        fun `should throw exception when email is duplicate`() {
            // given
            every { userRepository.existsByEmail("duplicate@example.com") } returns true

            // when & then
            assertThatThrownBy {
                userService.validateEmailNotDuplicate("duplicate@example.com")
            }.isInstanceOf(EmailDuplicateException::class.java)
        }

        @Test
        fun `should check if email exists`() {
            // given
            every { userRepository.existsByEmail("exists@example.com") } returns true
            every { userRepository.existsByEmail("notexists@example.com") } returns false

            // when & then
            assertThat(userService.existsByEmail("exists@example.com")).isTrue()
            assertThat(userService.existsByEmail("notexists@example.com")).isFalse()
        }
    }

    private fun createTestUser(id: Long, email: String): User {
        val user = User.createLocalUser(
            email = email,
            encodedPassword = "encoded_password",
            nickname = "테스터"
        )
        setUserId(user, id)
        return user
    }

    private fun setUserId(user: User, id: Long) {
        val idField = User::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(user, id)
    }
}
