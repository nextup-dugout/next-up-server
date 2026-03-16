package com.nextup.core.service.recruitment.dto

/**
 * 모집 공고 지원 요청 DTO (Core Service용)
 */
data class ApplyRecruitmentRequest(
    val recruitmentId: Long,
    val applicantId: Long,
    val message: String,
    val preferredPositions: String,
)
