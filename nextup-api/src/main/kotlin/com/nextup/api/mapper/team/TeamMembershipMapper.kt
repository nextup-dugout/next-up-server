package com.nextup.api.mapper.team

import com.nextup.api.dto.team.*
import com.nextup.core.domain.team.TeamJoinRequest
import com.nextup.core.domain.team.TeamMember

/**
 * TeamJoinRequest를 JoinRequestResponse로 변환합니다.
 */
fun TeamJoinRequest.toResponse(): JoinRequestResponse =
    JoinRequestResponse(
        requestId = this.id,
        teamId = this.team.id,
        userId = this.user.id,
        playerId = this.player.id,
        desiredUniformNumber = this.desiredUniformNumber,
        requestMessage = this.requestMessage,
        status = this.status,
        requestedAt = this.requestedAt,
        processedAt = this.processedAt,
        processedBy = this.processedBy?.id,
        responseMessage = this.responseMessage,
    )

/**
 * TeamMember를 TeamMemberResponse로 변환합니다.
 */
fun TeamMember.toResponse(): TeamMemberResponse =
    TeamMemberResponse(
        memberId = this.id,
        teamId = this.team.id,
        userId = this.user.id,
        playerId = this.player.id,
        playerName = this.player.name,
        role = this.role,
        uniformNumber = this.uniformNumber,
        status = this.status,
        joinedAt = this.joinedAt,
        leftAt = this.leftAt,
    )

/**
 * TeamMember를 TeamMemberDetailResponse로 변환합니다.
 */
fun TeamMember.toDetailResponse(): TeamMemberDetailResponse =
    TeamMemberDetailResponse(
        memberId = this.id,
        user =
            UserSummary(
                userId = this.user.id,
                nickname = this.user.nickname,
                profileImageUrl = null, // TODO: User entity에 profileImageUrl 추가 시 매핑
            ),
        player =
            PlayerSummary(
                playerId = this.player.id,
                name = this.player.name,
                primaryPosition = this.player.primaryPosition.abbreviation,
            ),
        role = this.role,
        uniformNumber = this.uniformNumber,
        status = this.status,
        joinedAt = this.joinedAt,
    )

/**
 * List<TeamMember>를 List<TeamMemberResponse>로 변환합니다.
 */
fun List<TeamMember>.toResponse(): List<TeamMemberResponse> = this.map { it.toResponse() }

/**
 * List<TeamMember>를 List<TeamMemberDetailResponse>로 변환합니다.
 */
fun List<TeamMember>.toDetailResponse(): List<TeamMemberDetailResponse> = this.map { it.toDetailResponse() }
