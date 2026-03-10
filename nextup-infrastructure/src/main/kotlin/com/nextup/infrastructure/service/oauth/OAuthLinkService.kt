package com.nextup.infrastructure.service.oauth

import com.nextup.common.exception.UserNotFoundException
import com.nextup.core.domain.user.OAuthAccount
import com.nextup.core.domain.user.OAuthProvider
import com.nextup.core.domain.user.User
import com.nextup.infrastructure.repository.UserJpaRepository
import com.nextup.infrastructure.repository.user.OAuthAccountRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * OAuth 계정 연동/해제 서비스
 */
@Service
@Transactional
class OAuthLinkService(
    private val userJpaRepository: UserJpaRepository,
    private val oAuthAccountRepository: OAuthAccountRepository,
) {
    /**     * 기존 사용자에게 OAuth 계정을 연동합니다.
     *
     * @param userId 사용자 ID
     * @param provider OAuth 제공자
     * @param oauthId OAuth 제공자에서 발급한 사용자 ID
     * @param email OAuth 제공자에서 제공하는 이메일 (선택)
     * @return 연동된 OAuthAccount
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     * @throws IllegalArgumentException 이미 연동된 Provider인 경우
     */
    fun linkOAuthAccount(
        userId: Long,
        provider: OAuthProvider,
        oauthId: String,
        email: String? = null,
    ): OAuthAccount {
        val user =
            userJpaRepository.findByIdOrNull(userId)
                ?: throw UserNotFoundException(userId)

        val oauthAccount = user.addOAuthAccount(provider, oauthId, email)
        userJpaRepository.save(user)

        return oauthAccount
    }

    /**
     * OAuth 계정 연동을 해제합니다.
     *
     * @param userId 사용자 ID
     * @param provider 해제할 OAuth 제공자
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     * @throws IllegalStateException 최소 1개의 인증 수단이 없는 경우
     */
    fun unlinkOAuthAccount(
        userId: Long,
        provider: OAuthProvider,
    ) {
        val user =
            userJpaRepository.findByIdOrNull(userId)
                ?: throw UserNotFoundException(userId)

        user.removeOAuthAccount(provider)
        userJpaRepository.save(user)
    }

    /**
     * 사용자의 연동된 OAuth 제공자 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 연동된 OAuth 제공자 목록
     */
    @Transactional(readOnly = true)
    fun getLinkedProviders(userId: Long): List<OAuthProvider> = oAuthAccountRepository.findProvidersByUserId(userId)

    /**
     * 사용자의 연동된 OAuth 계정 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 연동된 OAuth 계정 목록
     */
    @Transactional(readOnly = true)
    fun getLinkedAccounts(userId: Long): List<OAuthAccount> = oAuthAccountRepository.findAllByUserId(userId)

    /**
     * 특정 OAuth 제공자가 연동되어 있는지 확인합니다.
     *
     * @param userId 사용자 ID
     * @param provider OAuth 제공자
     * @return 연동 여부
     */
    @Transactional(readOnly = true)
    fun isLinked(
        userId: Long,
        provider: OAuthProvider,
    ): Boolean = oAuthAccountRepository.findByUserIdAndProvider(userId, provider) != null

    /**
     * OAuth 정보로 사용자를 조회합니다.
     *
     * @param provider OAuth 제공자
     * @param oauthId OAuth 제공자에서 발급한 사용자 ID
     * @return 사용자 (없으면 null)
     */
    @Transactional(readOnly = true)
    fun findUserByOAuth(
        provider: OAuthProvider,
        oauthId: String,
    ): User? = oAuthAccountRepository.findByProviderAndOauthId(provider, oauthId)?.user
}
