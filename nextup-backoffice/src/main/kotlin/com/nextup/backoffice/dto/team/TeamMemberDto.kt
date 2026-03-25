package com.nextup.backoffice.dto.team

import com.nextup.core.domain.team.TeamMemberRole
import com.nextup.core.domain.team.TeamMemberStatus
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

/**
 * 팀 멤버 관리자용 응답 DTO
 *
 * 변환 로직은 TeamExtensions.kt의 Extension Function을 사용합니다.
 */
data class TeamMemberAdminResponse(
    val memberId: Long,
    val teamId: Long,
    val teamName: String,
    val userId: Long,
    val userNickname: String,
    val userEmail: String,
    val playerId: Long,
    val playerName: String,
    val role: TeamMemberRole,
    val uniformNumber: Int,
    val status: TeamMemberStatus,
    val joinedAt: LocalDateTime,
    val leftAt: LocalDateTime?,
    val memo: String?,
)

/**
 * 멤버 상태 변경 요청 DTO
 */
data class UpdateMemberStatusRequest(
    @field:NotNull(message = "상태는 필수입니다")
    val status: TeamMemberStatus,
    val reason: String? = null,
)
