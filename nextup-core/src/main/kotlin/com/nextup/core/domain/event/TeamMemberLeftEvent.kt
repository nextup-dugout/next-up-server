package com.nextup.core.domain.event

/**
 * 팀원 탈퇴 이벤트
 *
 * 팀원이 자진 탈퇴했을 때 발행됩니다.
 */
data class TeamMemberLeftEvent(
    val teamId: Long,
    val playerId: Long,
    val memberId: Long,
)
