package com.nextup.backoffice.dto.user

import com.nextup.core.domain.user.User

/**
 * User Entity를 UserAdminResponse DTO로 변환하는 Extension Function
 */
fun User.toAdminResponse(): UserAdminResponse =
    UserAdminResponse(
        id = this.id,
        email = this.email,
        nickname = this.nickname,
        profileImageUrl = this.profileImageUrl,
        roles = this.roles.map { it.name }.toSet(),
        isLocalUser = this.isLocalUser,
        linkedOAuthProviders = this.oauthAccounts.map { it.provider.name },
        hasLinkedPlayer = this.hasLinkedPlayer,
        playerId = this.player?.id,
        isActive = this.isActive,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )

/**
 * User Entity를 UserListResponse DTO로 변환하는 Extension Function
 */
fun User.toListResponse(): UserListResponse =
    UserListResponse(
        id = this.id,
        email = this.email,
        nickname = this.nickname,
        roles = this.roles.map { it.name }.toSet(),
        isActive = this.isActive,
        createdAt = this.createdAt,
    )
