package com.nextup.infrastructure.service.oauth

import com.nextup.common.exception.UserNotFoundException
import com.nextup.core.domain.user.OAuthAccount
import com.nextup.core.domain.user.OAuthProvider
import com.nextup.core.domain.user.User
import com.nextup.infrastructure.repository.user.OAuthAccountRepository
import com.nextup.infrastructure.security.userdetails.UserJpaRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

@DisplayName("OAuthLinkService")
class OAuthLinkServiceTest {

    private lateinit var userJpaRepository: UserJpaRepository
    private lateinit var oauthAccountRepository: OAuthAccountRepository
    private lateinit var oauthLinkService: OAuthLinkService

    @BeforeEach
    fun setUp() {
        userJpaRepository = mockk()
        oauthAccountRepository = mockk()
        oauthLinkService = OAuthLinkService(userJpaRepository, oauthAccountRepository)
    }

    @Nested
    @DisplayName("linkOAuthAccount")
    inner class LinkOAuthAccount {

        @Test
        fun `should link oauth account to existing user`() {
            // given
            val userId = 1L
            val provider = OAuthProvider.KAKAO
            val oauthId = "kakao_123"
            val user = createLocalUser(userId)

            every { userJpaRepository.findById(userId) } returns Optional.of(user)
            every { userJpaRepository.save(any()) } returns user

            // when
            val result = oauthLinkService.linkOAuthAccount(userId, provider, oauthId)

            // then
            assertThat(result.provider).isEqualTo(provider)
            assertThat(result.oauthId).isEqualTo(oauthId)
            verify { userJpaRepository.findById(userId) }
            verify { userJpaRepository.save(user) }
        }

        @Test
        fun `should throw UserNotFoundException when user not found`() {
            // given
            val userId = 999L
            every { userJpaRepository.findById(userId) } returns Optional.empty()

            // when & then
            assertThatThrownBy {
                oauthLinkService.linkOAuthAccount(userId, OAuthProvider.KAKAO, "kakao_123")
            }.isInstanceOf(UserNotFoundException::class.java)
        }

        @Test
        fun `should throw IllegalArgumentException when provider already linked`() {
            // given
            val userId = 1L
            val provider = OAuthProvider.KAKAO
            val oauthId = "kakao_123"
            val user = createOAuthUser(userId)

            every { userJpaRepository.findById(userId) } returns Optional.of(user)

            // when & then
            assertThatThrownBy {
                oauthLinkService.linkOAuthAccount(userId, provider, oauthId)
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("unlinkOAuthAccount")
    inner class UnlinkOAuthAccount {

        @Test
        fun `should unlink oauth account from user`() {
            // given
            val userId = 1L
            val provider = OAuthProvider.KAKAO
            val user = createOAuthUser(userId)

            every { userJpaRepository.findById(userId) } returns Optional.of(user)
            every { userJpaRepository.save(any()) } returns user

            // when
            oauthLinkService.unlinkOAuthAccount(userId, provider)

            // then
            verify { userJpaRepository.findById(userId) }
            verify { userJpaRepository.save(user) }
        }

        @Test
        fun `should throw UserNotFoundException when user not found`() {
            // given
            val userId = 999L
            every { userJpaRepository.findById(userId) } returns Optional.empty()

            // when & then
            assertThatThrownBy {
                oauthLinkService.unlinkOAuthAccount(userId, OAuthProvider.KAKAO)
            }.isInstanceOf(UserNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("getLinkedProviders")
    inner class GetLinkedProviders {

        @Test
        fun `should return list of linked providers`() {
            // given
            val userId = 1L
            val providers = listOf(OAuthProvider.KAKAO, OAuthProvider.GOOGLE)

            every { oauthAccountRepository.findProvidersByUserId(userId) } returns providers

            // when
            val result = oauthLinkService.getLinkedProviders(userId)

            // then
            assertThat(result).containsExactlyInAnyOrder(OAuthProvider.KAKAO, OAuthProvider.GOOGLE)
        }

        @Test
        fun `should return empty list when no providers linked`() {
            // given
            val userId = 1L
            every { oauthAccountRepository.findProvidersByUserId(userId) } returns emptyList()

            // when
            val result = oauthLinkService.getLinkedProviders(userId)

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("isLinked")
    inner class IsLinked {

        @Test
        fun `should return true when oauth account is linked`() {
            // given
            val userId = 1L
            val provider = OAuthProvider.KAKAO
            val oauthAccount = mockk<OAuthAccount>()

            every { oauthAccountRepository.findByUserIdAndProvider(userId, provider) } returns oauthAccount

            // when
            val result = oauthLinkService.isLinked(userId, provider)

            // then
            assertThat(result).isTrue()
        }

        @Test
        fun `should return false when oauth account is not linked`() {
            // given
            val userId = 1L
            val provider = OAuthProvider.KAKAO

            every { oauthAccountRepository.findByUserIdAndProvider(userId, provider) } returns null

            // when
            val result = oauthLinkService.isLinked(userId, provider)

            // then
            assertThat(result).isFalse()
        }
    }

    @Nested
    @DisplayName("findUserByOAuth")
    inner class FindUserByOAuth {

        @Test
        fun `should return user when oauth account found`() {
            // given
            val provider = OAuthProvider.KAKAO
            val oauthId = "kakao_123"
            val user = createOAuthUser(1L)
            val oauthAccount = mockk<OAuthAccount>()

            every { oauthAccountRepository.findByProviderAndOauthId(provider, oauthId) } returns oauthAccount
            every { oauthAccount.user } returns user

            // when
            val result = oauthLinkService.findUserByOAuth(provider, oauthId)

            // then
            assertThat(result).isEqualTo(user)
        }

        @Test
        fun `should return null when oauth account not found`() {
            // given
            val provider = OAuthProvider.KAKAO
            val oauthId = "kakao_123"

            every { oauthAccountRepository.findByProviderAndOauthId(provider, oauthId) } returns null

            // when
            val result = oauthLinkService.findUserByOAuth(provider, oauthId)

            // then
            assertThat(result).isNull()
        }
    }

    // Helper methods
    private fun createLocalUser(id: Long): User {
        val user = User.createLocalUser(
            email = "test@example.com",
            encodedPassword = "encoded_password",
            nickname = "testuser"
        )
        setUserId(user, id)
        return user
    }

    private fun createOAuthUser(id: Long): User {
        val user = User.createOAuthUser(
            email = "test@kakao.com",
            nickname = "kakao_user",
            provider = OAuthProvider.KAKAO,
            oauthId = "kakao_123"
        )
        // Add password so user can remove OAuth account
        user.changePassword("encoded_password")
        setUserId(user, id)
        return user
    }

    private fun setUserId(user: User, id: Long) {
        val idField = User::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(user, id)
    }
}
