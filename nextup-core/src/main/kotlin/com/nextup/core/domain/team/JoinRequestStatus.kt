package com.nextup.core.domain.team

/**
 * 팀 가입 신청 상태
 */
enum class JoinRequestStatus(
    val displayName: String,
) {
    /** 승인 대기 */
    PENDING("승인 대기"),

    /** 승인됨 */
    APPROVED("승인됨"),

    /** 거부됨 */
    REJECTED("거부됨"),
}
