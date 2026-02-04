package com.nextup.backoffice.dto.user

import com.nextup.core.domain.user.User
import java.time.Instant

/**
 * 사용자 관리자 응답 DTO
 *
 * 관리자가 사용자 정보를 조회할 때 사용합니다.
 */
data class UserAdminResponse(
    val id: Long,
    val email: String,
    val nickname: String,
    val profileImageUrl: String?,
    val roles: Set<String>,
    val isLocalUser: Boolean,
    val linkedOAuthProviders: List<String>,
    val hasLinkedPlayer: Boolean,
    val playerId: Long?,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(user: User): UserAdminResponse =
            UserAdminResponse(
                id = user.id,
                email = user.email,
                nickname = user.nickname,
                profileImageUrl = user.profileImageUrl,
                roles = user.roles.map { it.name }.toSet(),
                isLocalUser = user.isLocalUser,
                linkedOAuthProviders = user.oauthAccounts.map { it.provider.name },
                hasLinkedPlayer = user.hasLinkedPlayer,
                playerId = user.player?.id,
                isActive = user.isActive,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt,
            )
    }
}

/**
 * 사용자 목록 응답 DTO (간략 정보)
 */
data class UserListResponse(
    val id: Long,
    val email: String,
    val nickname: String,
    val roles: Set<String>,
    val isActive: Boolean,
    val createdAt: Instant,
) {
    companion object {
        fun from(user: User): UserListResponse =
            UserListResponse(
                id = user.id,
                email = user.email,
                nickname = user.nickname,
                roles = user.roles.map { it.name }.toSet(),
                isActive = user.isActive,
                createdAt = user.createdAt,
            )
    }
}

/**
 * OAuth 계정 정보 응답 DTO
 */
data class OAuthAccountResponse(
    val provider: String,
    val providerDisplayName: String,
    val email: String?,
    val connectedAt: Instant,
)
