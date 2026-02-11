package com.nextup.infrastructure.repository.user

import com.nextup.core.domain.user.OAuthAccount
import com.nextup.core.domain.user.OAuthProvider
import com.nextup.core.port.repository.OAuthAccountRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface OAuthAccountRepository :
    JpaRepository<OAuthAccount, Long>,
    OAuthAccountRepositoryPort {
    override fun findByProviderAndOauthId(
        provider: OAuthProvider,
        oauthId: String,
    ): OAuthAccount?

    override fun existsByProviderAndOauthId(
        provider: OAuthProvider,
        oauthId: String,
    ): Boolean

    @Query("SELECT o FROM OAuthAccount o WHERE o.user.id = :userId")
    override fun findAllByUserId(userId: Long): List<OAuthAccount>

    @Query("SELECT o FROM OAuthAccount o WHERE o.user.id = :userId AND o.provider = :provider")
    override fun findByUserIdAndProvider(
        userId: Long,
        provider: OAuthProvider,
    ): OAuthAccount?

    @Query("SELECT o.provider FROM OAuthAccount o WHERE o.user.id = :userId")
    override fun findProvidersByUserId(userId: Long): List<OAuthProvider>
}
