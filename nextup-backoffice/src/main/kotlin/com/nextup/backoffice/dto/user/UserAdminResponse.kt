package com.nextup.backoffice.dto.user

import java.time.Instant

/**
 * 사용자 관리자 응답 DTO
 *
 * 관리자가 사용자 정보를 조회할 때 사용합니다.
 * 변환 로직은 UserExtensions.kt의 Extension Function을 사용합니다.
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
)

/**
 * 사용자 목록 응답 DTO (간략 정보)
 *
 * 변환 로직은 UserExtensions.kt의 Extension Function을 사용합니다.
 */
data class UserListResponse(
    val id: Long,
    val email: String,
    val nickname: String,
    val roles: Set<String>,
    val isActive: Boolean,
    val createdAt: Instant,
)

/**
 * OAuth 계정 정보 응답 DTO
 */
data class OAuthAccountResponse(
    val provider: String,
    val providerDisplayName: String,
    val email: String?,
    val connectedAt: Instant,
)
