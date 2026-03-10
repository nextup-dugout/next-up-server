package com.nextup.infrastructure.security.oauth2

import com.nextup.common.exception.OAuth2AuthenticationProcessingException
import com.nextup.core.domain.user.OAuthAccount
import com.nextup.core.domain.user.OAuthProvider
import com.nextup.core.domain.user.User
import com.nextup.infrastructure.repository.UserJpaRepository
import com.nextup.infrastructure.repository.user.OAuthAccountRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.user.OAuth2User
import java.time.Instant

@DisplayName("CustomOAuth2UserService 테스트")
class CustomOAuth2UserServiceTest {
    private lateinit var userJpaRepository: UserJpaRepository
    private lateinit var oAuthAccountRepository: OAuthAccountRepository
    private lateinit var customOAuth2UserService: TestableCustomOAuth2UserService

    /**
     * 테스트 가능한 서브클래스 - super.loadUser() 호출을 모킹 가능하게 함
     */
    private class TestableCustomOAuth2UserService(
        userJpaRepository: UserJpaRepository,
        oAuthAccountRepository: OAuthAccountRepository,
    ) : CustomOAuth2UserService(userJpaRepository, oAuthAccountRepository) {
        var mockOAuth2User: OAuth2User? = null

        override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
            val oAuth2User =
                mockOAuth2User
                    ?: throw IllegalStateException("mockOAuth2User must be set before calling loadUser")

            // processOAuth2User is private, so we need to access it via reflection
            val method =
                CustomOAuth2UserService::class.java.getDeclaredMethod(
                    "processOAuth2User",
                    OAuth2UserRequest::class.java,
                    OAuth2User::class.java,
                )
            method.isAccessible = true
            return method.invoke(this, userRequest, oAuth2User) as OAuth2User
        }
    }

    @BeforeEach
    fun setUp() {
        userJpaRepository = mockk(relaxed = true)
        oAuthAccountRepository = mockk(relaxed = true)
        customOAuth2UserService = TestableCustomOAuth2UserService(userJpaRepository, oAuthAccountRepository)
    }

    private fun createUserRequest(registrationId: String): OAuth2UserRequest {
        val clientRegistration =
            ClientRegistration
                .withRegistrationId(registrationId)
                .clientId("test-client-id")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/callback")
                .authorizationUri("http://auth.example.com/authorize")
                .tokenUri("http://auth.example.com/token")
                .userInfoUri("http://auth.example.com/userinfo")
                .userNameAttributeName("id")
                .clientName("Test Client")
                .build()

        val accessToken =
            OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "test-access-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
            )

        return OAuth2UserRequest(clientRegistration, accessToken)
    }

    private fun createOAuth2User(attributes: Map<String, Any>): OAuth2User =
        mockk {
            every { getAttributes() } returns attributes
            every { getName() } returns "test-user"
        }

    private fun createUser(
        id: Long,
        email: String,
        nickname: String,
    ): User {
        val user =
            User.createOAuthUser(
                email = email,
                nickname = nickname,
                provider = OAuthProvider.KAKAO,
                oauthId = "kakao_$id",
            )
        val idField = User::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(user, id)
        return user
    }

    @Nested
    @DisplayName("기존 OAuthAccount 존재 시")
    inner class ExistingOAuthAccountTest {
        @Test
        fun `should return existing user when OAuthAccount exists`() {
            // given
            val userRequest = createUserRequest("kakao")
            customOAuth2UserService.mockOAuth2User =
                createOAuth2User(
                    mapOf(
                        "id" to 123456L,
                        "kakao_account" to
                            mapOf(
                                "email" to "existing@kakao.com",
                                "profile" to mapOf("nickname" to "기존사용자"),
                            ),
                    ),
                )

            val existingUser = createUser(1L, "existing@kakao.com", "기존사용자")
            val existingOAuthAccount =
                mockk<OAuthAccount> {
                    every { user } returns existingUser
                }

            every { oAuthAccountRepository.findByProviderAndOauthId(OAuthProvider.KAKAO, "123456") } returns
                existingOAuthAccount

            // when
            val result = customOAuth2UserService.loadUser(userRequest)

            // then
            assertThat(result).isInstanceOf(OAuth2UserPrincipal::class.java)
            val principal = result as OAuth2UserPrincipal
            assertThat(principal.isNewUser).isFalse()
            assertThat(principal.userId).isEqualTo(1L)
        }
    }

    @Nested
    @DisplayName("이메일로 기존 User 조회")
    inner class ExistingUserByEmailTest {
        @Test
        fun `should link OAuthAccount to existing user when email matches`() {
            // given
            val userRequest = createUserRequest("google")
            customOAuth2UserService.mockOAuth2User =
                createOAuth2User(
                    mapOf(
                        "sub" to "google_123",
                        "email" to "existing@gmail.com",
                        "name" to "기존사용자",
                    ),
                )

            val existingUser = createUser(2L, "existing@gmail.com", "기존사용자")

            every { oAuthAccountRepository.findByProviderAndOauthId(OAuthProvider.GOOGLE, "google_123") } returns null
            every { userJpaRepository.findByEmail("existing@gmail.com") } returns existingUser
            every { userJpaRepository.save(existingUser) } returns existingUser

            // when
            val result = customOAuth2UserService.loadUser(userRequest)

            // then
            assertThat(result).isInstanceOf(OAuth2UserPrincipal::class.java)
            val principal = result as OAuth2UserPrincipal
            assertThat(principal.isNewUser).isFalse()
            assertThat(principal.userId).isEqualTo(2L)
            verify { userJpaRepository.save(existingUser) }
        }
    }

    @Nested
    @DisplayName("신규 User 생성")
    inner class NewUserCreationTest {
        @Test
        fun `should create new user when no existing account found`() {
            // given
            val userRequest = createUserRequest("naver")
            customOAuth2UserService.mockOAuth2User =
                createOAuth2User(
                    mapOf(
                        "response" to
                            mapOf(
                                "id" to "naver_new_123",
                                "email" to "newuser@naver.com",
                                "name" to "신규사용자",
                            ),
                    ),
                )

            val savedUserSlot = slot<User>()

            every { oAuthAccountRepository.findByProviderAndOauthId(OAuthProvider.NAVER, "naver_new_123") } returns null
            every { userJpaRepository.findByEmail("newuser@naver.com") } returns null
            every { userJpaRepository.save(capture(savedUserSlot)) } answers { savedUserSlot.captured }

            // when
            val result = customOAuth2UserService.loadUser(userRequest)

            // then
            assertThat(result).isInstanceOf(OAuth2UserPrincipal::class.java)
            val principal = result as OAuth2UserPrincipal
            assertThat(principal.isNewUser).isTrue()
            verify { userJpaRepository.save(any<User>()) }
        }

        @Test
        fun `should use email prefix as nickname when name is null`() {
            // given
            val userRequest = createUserRequest("google")
            customOAuth2UserService.mockOAuth2User =
                createOAuth2User(
                    mapOf(
                        "sub" to "google_456",
                        "email" to "testuser@gmail.com",
                        // name is missing
                    ),
                )

            val savedUserSlot = slot<User>()

            every { oAuthAccountRepository.findByProviderAndOauthId(OAuthProvider.GOOGLE, "google_456") } returns null
            every { userJpaRepository.findByEmail("testuser@gmail.com") } returns null
            every { userJpaRepository.save(capture(savedUserSlot)) } answers { savedUserSlot.captured }

            // when
            customOAuth2UserService.loadUser(userRequest)

            // then
            assertThat(savedUserSlot.captured.nickname).isEqualTo("testuser")
        }

        @Test
        fun `should use provider default nickname when both name and email are null`() {
            // given
            val userRequest = createUserRequest("kakao")
            customOAuth2UserService.mockOAuth2User =
                createOAuth2User(
                    mapOf(
                        "id" to 789L,
                        // No kakao_account, so no email or name
                    ),
                )

            val savedUserSlot = slot<User>()

            every { oAuthAccountRepository.findByProviderAndOauthId(OAuthProvider.KAKAO, "789") } returns null
            every { userJpaRepository.save(capture(savedUserSlot)) } answers { savedUserSlot.captured }

            // when
            customOAuth2UserService.loadUser(userRequest)

            // then
            assertThat(savedUserSlot.captured.nickname).isEqualTo("카카오사용자")
            assertThat(savedUserSlot.captured.email).isEqualTo("kakao_789@oauth.local")
        }
    }

    @Nested
    @DisplayName("예외 처리")
    inner class ExceptionHandlingTest {
        @Test
        fun `should throw exception when OAuth2 provider returns blank id`() {
            // given
            val userRequest = createUserRequest("google")
            customOAuth2UserService.mockOAuth2User =
                createOAuth2User(
                    mapOf(
                        "email" to "user@gmail.com",
                        // sub is missing, so id will be blank
                    ),
                )

            // when & then
            // 리플렉션으로 인해 InvocationTargetException으로 래핑됨
            assertThatThrownBy { customOAuth2UserService.loadUser(userRequest) }
                .hasCauseInstanceOf(OAuth2AuthenticationProcessingException::class.java)
        }
    }
}
