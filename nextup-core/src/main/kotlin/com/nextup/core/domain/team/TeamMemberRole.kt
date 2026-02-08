package com.nextup.core.domain.team

/**
 * 팀 멤버 역할
 *
 * 팀 내에서 회원이 가지는 권한 레벨을 정의합니다.
 */
enum class TeamMemberRole(
    val displayName: String,
    val level: Int,
) {
    /** 감독 - 팀 전체 관리, 최고 권한 */
    OWNER("감독", 100),

    /** 운영진 - 가입 승인, 라인업 제출 */
    MANAGER("운영진", 50),

    /** 일반 회원 - 투표 참여, 게시글 작성 */
    MEMBER("일반 회원", 10),
    ;

    /**
     * 가입 승인 권한이 있는지 확인합니다.
     */
    fun canApproveJoin(): Boolean = this in listOf(OWNER, MANAGER)

    /**
     * 강퇴 권한이 있는지 확인합니다.
     */
    fun canKickMember(): Boolean = this == OWNER

    /**
     * 역할 변경 권한이 있는지 확인합니다.
     */
    fun canChangeRole(): Boolean = this == OWNER

    /**
     * 다른 역할보다 높은 권한인지 확인합니다.
     */
    fun isHigherThan(other: TeamMemberRole): Boolean = this.level > other.level

    /**
     * 다른 역할과 같거나 높은 권한인지 확인합니다.
     */
    fun isHigherOrEqual(other: TeamMemberRole): Boolean = this.level >= other.level
}
