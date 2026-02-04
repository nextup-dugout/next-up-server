package com.nextup.core.port.repository

import com.nextup.core.domain.user.OAuthAccount
import com.nextup.core.domain.user.OAuthProvider

/**
 * OAuthAccount Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface OAuthAccountRepositoryPort {
    fun save(oAuthAccount: OAuthAccount): OAuthAccount

    fun delete(oAuthAccount: OAuthAccount)

    fun findByProviderAndOauthId(
        provider: OAuthProvider,
        oauthId: String,
    ): OAuthAccount?

    fun existsByProviderAndOauthId(
        provider: OAuthProvider,
        oauthId: String,
    ): Boolean

    fun findAllByUserId(userId: Long): List<OAuthAccount>

    fun findByUserIdAndProvider(
        userId: Long,
        provider: OAuthProvider,
    ): OAuthAccount?

    fun findProvidersByUserId(userId: Long): List<OAuthProvider>
}
