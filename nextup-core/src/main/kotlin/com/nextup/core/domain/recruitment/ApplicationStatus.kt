package com.nextup.core.domain.recruitment

/**
 * 모집 지원 상태
 */
enum class ApplicationStatus {
    /** 대기 중 */
    PENDING,

    /** 수락됨 */
    ACCEPTED,

    /** 거절됨 */
    REJECTED,

    /** 지원자가 취소함 */
    WITHDRAWN,
}
