package com.nextup.api.dto.user

import com.nextup.core.domain.user.User
import java.time.Instant

/**
 * 사용자 프로필 응답 DTO
 *
 * 로그인한 사용자가 본인 정보를 조회할 때 사용합니다.
 */
data class ProfileResponse(
    val id: Long,
    val email: String,
    val nickname: String,
    val profileImageUrl: String?,
    val roles: Set<String>,
    val isLocalUser: Boolean,
    val linkedOAuthProviders: List<LinkedOAuthProvider>,
    val hasLinkedPlayer: Boolean,
    val playerId: Long?,
    val createdAt: Instant
) {
    companion object {
        fun from(user: User): ProfileResponse {
            return ProfileResponse(
                id = user.id,
                email = user.email,
                nickname = user.nickname,
                profileImageUrl = user.profileImageUrl,
                roles = user.roles.map { it.name }.toSet(),
                isLocalUser = user.isLocalUser,
                linkedOAuthProviders = user.oauthAccounts.map {
                    LinkedOAuthProvider(
                        provider = it.provider.name,
                        displayName = it.provider.displayName,
                        connectedAt = it.connectedAt
                    )
                },
                hasLinkedPlayer = user.hasLinkedPlayer,
                playerId = user.player?.id,
                createdAt = user.createdAt
            )
        }
    }
}

/**
 * 연동된 OAuth Provider 정보
 */
data class LinkedOAuthProvider(
    val provider: String,
    val displayName: String,
    val connectedAt: Instant
)

/**
 * 프로필 간략 정보 (공개용)
 */
data class PublicProfileResponse(
    val id: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val hasLinkedPlayer: Boolean,
    val playerId: Long?
) {
    companion object {
        fun from(user: User): PublicProfileResponse {
            return PublicProfileResponse(
                id = user.id,
                nickname = user.nickname,
                profileImageUrl = user.profileImageUrl,
                hasLinkedPlayer = user.hasLinkedPlayer,
                playerId = user.player?.id
            )
        }
    }
}
