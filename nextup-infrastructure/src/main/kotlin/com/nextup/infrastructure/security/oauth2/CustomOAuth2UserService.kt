package com.nextup.infrastructure.security.oauth2

import com.nextup.common.exception.OAuth2AuthenticationProcessingException
import com.nextup.core.domain.user.User
import com.nextup.infrastructure.repository.user.OAuthAccountRepository
import com.nextup.infrastructure.security.userdetails.UserJpaRepository
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * OAuth2 로그인 시 사용자 정보를 처리하는 서비스
 *
 * 1. 기존 OAuthAccount 존재 → User 조회
 * 2. 이메일로 User 조회 → 기존 User에 OAuthAccount 추가
 * 3. 신규 User + OAuthAccount 생성
 */
@Service
@Transactional
class CustomOAuth2UserService(
    private val userJpaRepository: UserJpaRepository,
    private val oAuthAccountRepository: OAuthAccountRepository,
) : DefaultOAuth2UserService() {
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        return processOAuth2User(userRequest, oAuth2User)
    }

    private fun processOAuth2User(
        userRequest: OAuth2UserRequest,
        oAuth2User: OAuth2User,
    ): OAuth2UserPrincipal {
        val registrationId = userRequest.clientRegistration.registrationId
        val attributes = oAuth2User.attributes

        val oAuth2UserInfo = OAuth2UserInfoFactory.create(registrationId, attributes)
        val provider = oAuth2UserInfo.provider

        if (oAuth2UserInfo.id.isBlank()) {
            throw OAuth2AuthenticationProcessingException(
                "OAuth2 provider did not return user ID",
            )
        }

        // 1. 기존 OAuthAccount로 User 조회
        val existingOAuthAccount =
            oAuthAccountRepository.findByProviderAndOauthId(
                provider,
                oAuth2UserInfo.id,
            )

        if (existingOAuthAccount != null) {
            return OAuth2UserPrincipal(
                user = existingOAuthAccount.user,
                oAuth2UserInfo = oAuth2UserInfo,
                isNewUser = false,
            )
        }

        // 2. 이메일로 기존 User 조회 후 OAuthAccount 연동
        val email = oAuth2UserInfo.email
        if (!email.isNullOrBlank()) {
            val existingUser = userJpaRepository.findByEmail(email)
            if (existingUser != null) {
                existingUser.addOAuthAccount(
                    provider = provider,
                    oauthId = oAuth2UserInfo.id,
                    email = email,
                )
                userJpaRepository.save(existingUser)

                return OAuth2UserPrincipal(
                    user = existingUser,
                    oAuth2UserInfo = oAuth2UserInfo,
                    isNewUser = false,
                )
            }
        }

        // 3. 신규 User 생성
        val nickname =
            oAuth2UserInfo.name
                ?: email?.substringBefore("@")
                ?: "${provider.displayName}사용자"

        val userEmail = email ?: "${provider.name.lowercase()}_${oAuth2UserInfo.id}@oauth.local"

        val newUser =
            User.createOAuthUser(
                email = userEmail,
                nickname = nickname,
                provider = provider,
                oauthId = oAuth2UserInfo.id,
                profileImageUrl = oAuth2UserInfo.profileImageUrl,
            )
        userJpaRepository.save(newUser)

        return OAuth2UserPrincipal(
            user = newUser,
            oAuth2UserInfo = oAuth2UserInfo,
            isNewUser = true,
        )
    }
}
