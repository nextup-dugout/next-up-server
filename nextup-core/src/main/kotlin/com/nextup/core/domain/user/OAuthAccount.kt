package com.nextup.core.domain.user

import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.*
import java.time.Instant

/**
 * OAuth 계정 엔티티
 *
 * 사용자의 소셜 로그인 연동 정보를 관리합니다.
 * 한 User가 여러 OAuthAccount를 가질 수 있습니다 (1:N).
 */
@Entity
@Table(
    name = "oauth_accounts",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_oauth_provider_id",
            columnNames = ["provider", "oauth_id"],
        ),
    ],
    indexes = [
        Index(name = "idx_oauth_user", columnList = "user_id"),
        Index(name = "idx_oauth_provider", columnList = "provider"),
    ],
)
class OAuthAccount private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val provider: OAuthProvider,
    @Column(name = "oauth_id", nullable = false, length = 100)
    val oauthId: String,
    @Column(length = 100)
    val email: String? = null,
    @Column(name = "connected_at", nullable = false)
    val connectedAt: Instant = Instant.now(),
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    companion object {
        /**
         * OAuthAccount를 생성합니다.
         *
         * @param user 연결할 사용자
         * @param provider OAuth 제공자 (LOCAL은 허용되지 않음)
         * @param oauthId OAuth 제공자에서 발급한 사용자 ID
         * @param email OAuth 제공자에서 제공하는 이메일 (선택)
         * @return 생성된 OAuthAccount
         * @throws IllegalArgumentException LOCAL provider인 경우
         */
        fun create(
            user: User,
            provider: OAuthProvider,
            oauthId: String,
            email: String? = null,
        ): OAuthAccount {
            require(provider != OAuthProvider.LOCAL) {
                "LOCAL은 OAuthAccount로 관리하지 않습니다."
            }
            require(oauthId.isNotBlank()) {
                "OAuth ID는 비어있을 수 없습니다."
            }
            return OAuthAccount(
                user = user,
                provider = provider,
                oauthId = oauthId,
                email = email,
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OAuthAccount) return false
        if (id == 0L) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "OAuthAccount(id=$id, provider=$provider, oauthId=$oauthId, userId=${user.id})"
}
