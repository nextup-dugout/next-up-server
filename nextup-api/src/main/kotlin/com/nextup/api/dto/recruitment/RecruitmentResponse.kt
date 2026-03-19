package com.nextup.api.dto.recruitment

import com.nextup.core.domain.recruitment.RecruitmentStatus
import com.nextup.core.domain.recruitment.TeamRecruitment
import java.time.Instant
import java.time.LocalDate

/**
 * 팀 모집 공고 응답 DTO (공개 API용)
 */
data class RecruitmentResponse(
    val id: Long,
    val teamId: Long,
    val teamName: String,
    val teamLogoUrl: String?,
    val title: String,
    val description: String,
    val positionsNeeded: String,
    val ageRange: String?,
    val skillLevel: String?,
    val location: String?,
    val deadline: LocalDate,
    val status: RecruitmentStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(recruitment: TeamRecruitment): RecruitmentResponse =
            RecruitmentResponse(
                id = recruitment.id,
                teamId = recruitment.team.id,
                teamName = recruitment.team.name,
                teamLogoUrl = recruitment.team.logoUrl,
                title = recruitment.title,
                description = recruitment.description,
                positionsNeeded = recruitment.positionsNeeded,
                ageRange = recruitment.ageRange,
                skillLevel = recruitment.skillLevel,
                location = recruitment.location,
                deadline = recruitment.deadline,
                status = recruitment.status,
                createdAt = recruitment.createdAt,
                updatedAt = recruitment.updatedAt,
            )
    }
}
