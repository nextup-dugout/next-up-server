package com.nextup.core.domain.team

/**
 * 팀 멤버 상태
 *
 * 팀 내에서 회원의 활동 상태를 나타냅니다.
 */
enum class TeamMemberStatus(
    val displayName: String,
) {
    /** 활동 중 */
    ACTIVE("활동 중"),

    /** 활동 정지 (회비 미납 등) */
    SUSPENDED("활동 정지"),

    /** 자진 탈퇴 */
    LEFT("탈퇴"),

    /** 강퇴됨 */
    KICKED("강퇴"),
}
