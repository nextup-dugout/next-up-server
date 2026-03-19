package com.nextup.core.domain.event

/**
 * 모집 지원 수락 이벤트
 *
 * 모집 공고 지원이 수락되었을 때 발행됩니다.
 * 팀 자동 합류 처리를 위해 사용됩니다.
 */
data class RecruitmentApplicationAcceptedEvent(
    val applicationId: Long,
    val recruitmentId: Long,
    val teamId: Long,
    val applicantId: Long,
    val teamName: String,
)
