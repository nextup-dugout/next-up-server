package com.nextup.api.dto.team

import com.nextup.core.domain.team.TeamMemberRole
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

/**
 * 팀 가입 신청 요청 DTO
 */
data class JoinRequestDto(
    @field:NotNull(message = "희망 등번호는 필수입니다")
    @field:Min(value = 1, message = "등번호는 1 이상이어야 합니다")
    @field:Max(value = 99, message = "등번호는 99 이하여야 합니다")
    val desiredUniformNumber: Int,
    val requestMessage: String? = null,
)

/**
 * 가입 승인 요청 DTO
 */
data class ApproveJoinRequestDto(
    @field:Min(value = 1, message = "등번호는 1 이상이어야 합니다")
    @field:Max(value = 99, message = "등번호는 99 이하여야 합니다")
    val uniformNumber: Int? = null,
    val responseMessage: String? = null,
)

/**
 * 가입 거부 요청 DTO
 */
data class RejectJoinRequestDto(
    val responseMessage: String? = null,
)

/**
 * 멤버 강퇴 요청 DTO
 */
data class KickMemberRequest(
    @field:NotNull(message = "강퇴 사유는 필수입니다")
    val reason: String,
    val addToBlacklist: Boolean = false,
)

/**
 * 역할 변경 요청 DTO
 */
data class ChangeRoleRequest(
    @field:NotNull(message = "새로운 역할은 필수입니다")
    val newRole: TeamMemberRole,
)
