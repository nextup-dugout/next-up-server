package com.nextup.api.dto.recruitment

import com.nextup.core.domain.recruitment.ApplicationStatus
import com.nextup.core.domain.recruitment.RecruitmentApplication
import java.time.Instant

/**
 * 모집 공고 지원 응답 DTO (공개 API용)
 */
data class RecruitmentApplicationResponse(
    val id: Long,
    val recruitmentId: Long,
    val recruitmentTitle: String,
    val applicantId: Long,
    val message: String,
    val preferredPositions: String,
    val status: ApplicationStatus,
    val appliedAt: Instant,
    val processedAt: Instant?,
    val processedBy: Long?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(application: RecruitmentApplication): RecruitmentApplicationResponse =
            RecruitmentApplicationResponse(
                id = application.id,
                recruitmentId = application.recruitment.id,
                recruitmentTitle = application.recruitment.title,
                applicantId = application.applicantId,
                message = application.message,
                preferredPositions = application.preferredPositions,
                status = application.status,
                appliedAt = application.appliedAt,
                processedAt = application.processedAt,
                processedBy = application.processedBy,
                createdAt = application.createdAt,
                updatedAt = application.updatedAt,
            )
    }
}
