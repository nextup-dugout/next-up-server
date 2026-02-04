package com.nextup.api.controller.oauth

import com.nextup.common.exception.OAuthAccountNotFoundException
import com.nextup.core.domain.user.OAuthProvider
import com.nextup.infrastructure.service.oauth.OAuthLinkService
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("OAuthController 테스트")
class OAuthControllerTest {
    private lateinit var oauthLinkService: OAuthLinkService
    private lateinit var oauthController: OAuthController

    private val testUserId = 1L
    private val baseUrl = "http://localhost:8080"

    @BeforeEach
    fun setUp() {
        oauthLinkService = mockk()
        oauthController = OAuthController(oauthLinkService, baseUrl)
    }

    @Test
    @DisplayName("GET /api/me/oauth-accounts - 연동된 소셜 계정 목록 조회 성공")
    fun getLinkedOAuthAccounts_Success() {
        // given
        val linkedProviders = listOf(OAuthProvider.KAKAO, OAuthProvider.GOOGLE)
        every { oauthLinkService.getLinkedProviders(testUserId) } returns linkedProviders

        // when
        val response = oauthController.getLinkedOAuthAccounts(testUserId)

        // then
        assertThat(response.statusCode.value()).isEqualTo(200)
        assertThat(response.body?.success).isTrue()
        assertThat(response.body?.data?.providers).containsExactly(
            OAuthProvider.KAKAO,
            OAuthProvider.GOOGLE,
        )
    }

    @Test
    @DisplayName("GET /api/me/oauth-accounts - 연동된 계정이 없는 경우")
    fun getLinkedOAuthAccounts_Empty() {
        // given
        every { oauthLinkService.getLinkedProviders(testUserId) } returns emptyList()

        // when
        val response = oauthController.getLinkedOAuthAccounts(testUserId)

        // then
        assertThat(response.statusCode.value()).isEqualTo(200)
        assertThat(response.body?.success).isTrue()
        assertThat(response.body?.data?.providers).isEmpty()
    }

    @Test
    @DisplayName("POST /api/me/oauth-accounts/{provider}/link - 소셜 계정 연동 시작 성공")
    fun startOAuthLink_Success() {
        // when
        val response = oauthController.startOAuthLink(OAuthProvider.KAKAO, testUserId)

        // then
        assertThat(response.statusCode.value()).isEqualTo(200)
        assertThat(response.body?.success).isTrue()
        assertThat(response.body?.data?.provider).isEqualTo(OAuthProvider.KAKAO)
        assertThat(response.body?.data?.authorizationUrl)
            .isEqualTo("$baseUrl/oauth2/authorization/kakao")
    }

    @Test
    @DisplayName("DELETE /api/me/oauth-accounts/{provider} - 소셜 계정 연결 해제 성공")
    fun unlinkOAuthAccount_Success() {
        // given
        every { oauthLinkService.unlinkOAuthAccount(testUserId, OAuthProvider.KAKAO) } returns Unit

        // when
        val response = oauthController.unlinkOAuthAccount(OAuthProvider.KAKAO, testUserId)

        // then
        assertThat(response.statusCode.value()).isEqualTo(200)
        assertThat(response.body?.success).isTrue()
    }

    @Test
    @DisplayName("DELETE /api/me/oauth-accounts/{provider} - 연결되지 않은 계정 해제 시도 시 예외 발생")
    fun unlinkOAuthAccount_NotFound() {
        // given
        every {
            oauthLinkService.unlinkOAuthAccount(testUserId, OAuthProvider.NAVER)
        } throws OAuthAccountNotFoundException("NAVER", "unknown")

        // when & then
        try {
            oauthController.unlinkOAuthAccount(OAuthProvider.NAVER, testUserId)
        } catch (e: OAuthAccountNotFoundException) {
            assertThat(e.code).isEqualTo("OAUTH_ACCOUNT_NOT_FOUND")
        }
    }
}
