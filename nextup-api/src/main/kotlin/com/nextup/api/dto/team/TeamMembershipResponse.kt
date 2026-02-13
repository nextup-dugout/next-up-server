package com.nextup.api.dto.team

import com.nextup.core.domain.team.JoinRequestStatus
import com.nextup.core.domain.team.TeamMemberRole
import com.nextup.core.domain.team.TeamMemberStatus
import java.time.LocalDateTime

/**
 * 가입 신청 응답 DTO
 */
data class JoinRequestResponse(
    val requestId: Long,
    val teamId: Long,
    val userId: Long,
    val playerId: Long,
    val desiredUniformNumber: Int,
    val requestMessage: String?,
    val status: JoinRequestStatus,
    val requestedAt: LocalDateTime,
    val processedAt: LocalDateTime? = null,
    val processedBy: Long? = null,
    val responseMessage: String? = null,
)

/**
 * 팀 멤버 응답 DTO
 */
data class TeamMemberResponse(
    val memberId: Long,
    val teamId: Long,
    val userId: Long,
    val playerId: Long,
    val playerName: String,
    val role: TeamMemberRole,
    val uniformNumber: Int,
    val status: TeamMemberStatus,
    val joinedAt: LocalDateTime,
    val leftAt: LocalDateTime? = null,
)

/**
 * 팀 멤버 상세 응답 DTO (사용자 정보 포함)
 */
data class TeamMemberDetailResponse(
    val memberId: Long,
    val user: UserSummary,
    val player: PlayerSummary,
    val role: TeamMemberRole,
    val uniformNumber: Int,
    val status: TeamMemberStatus,
    val joinedAt: LocalDateTime,
)

/**
 * 사용자 요약 정보
 */
data class UserSummary(
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String? = null,
)

/**
 * 선수 요약 정보
 */
data class PlayerSummary(
    val playerId: Long,
    val name: String,
    val primaryPosition: String,
)

/**
 * 블랙리스트 응답 DTO
 */
data class BlacklistResponse(
    val blacklistId: Long,
    val userId: Long,
    val userNickname: String,
    val reason: String,
    val registeredBy: Long,
    val registeredByNickname: String,
    val registeredAt: LocalDateTime,
    val expiresAt: LocalDateTime? = null,
    val isPermanent: Boolean,
    val isActive: Boolean,
)
