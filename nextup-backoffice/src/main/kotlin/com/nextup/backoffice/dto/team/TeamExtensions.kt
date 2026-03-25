package com.nextup.backoffice.dto.team

import com.nextup.core.domain.team.TeamMember

/**
 * TeamMember Entity를 TeamMemberAdminResponse DTO로 변환하는 Extension Function
 */
fun TeamMember.toAdminResponse(): TeamMemberAdminResponse =
    TeamMemberAdminResponse(
        memberId = this.id,
        teamId = this.team.id,
        teamName = this.team.name,
        userId = this.user.id,
        userNickname = this.user.nickname,
        userEmail = this.user.email,
        playerId = this.player.id,
        playerName = this.player.name,
        role = this.role,
        uniformNumber = this.uniformNumber,
        status = this.status,
        joinedAt = this.joinedAt,
        leftAt = this.leftAt,
        memo = this.memo,
    )
