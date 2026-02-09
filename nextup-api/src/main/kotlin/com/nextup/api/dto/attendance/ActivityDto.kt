package com.nextup.api.dto.attendance

import com.nextup.core.domain.attendance.ActivityScore
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

/**
 * 활동 점수 응답 DTO
 */
data class ActivityScoreResponse(
    val id: Long,
    val teamId: Long,
    val memberId: Long,
    val memberName: String,
    val gameParticipationRate: BigDecimal,
    val practiceAttendanceRate: BigDecimal,
    val contributionScore: BigDecimal,
    val totalScore: BigDecimal,
    val createdAt: String,
    val updatedAt: String,
)

/**
 * 활동 점수 업데이트 요청 DTO
 */
data class UpdateActivityScoreRequest(
    @field:NotNull(message = "점수는 필수입니다")
    @field:DecimalMin(value = "0.00", message = "점수는 0 이상이어야 합니다")
    @field:DecimalMax(value = "100.00", message = "점수는 100 이하여야 합니다")
    val score: BigDecimal,
)

/**
 * Extension Functions for Mapping
 */
fun ActivityScore.toResponse(): ActivityScoreResponse =
    ActivityScoreResponse(
        id = this.id,
        teamId = this.team.id,
        memberId = this.member.id,
        memberName = this.member.player.name,
        gameParticipationRate = this.gameParticipationRate,
        practiceAttendanceRate = this.practiceAttendanceRate,
        contributionScore = this.contributionScore,
        totalScore = this.calculateTotalScore(),
        createdAt = this.createdAt.toString(),
        updatedAt = this.updatedAt.toString(),
    )
