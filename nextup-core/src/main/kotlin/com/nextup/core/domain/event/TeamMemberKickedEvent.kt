package com.nextup.core.domain.event

/**
 * 팀원 강퇴 이벤트
 *
 * 팀원이 강퇴되었을 때 발행됩니다.
 */
data class TeamMemberKickedEvent(
    val teamId: Long,
    val playerId: Long,
    val memberId: Long,
)
