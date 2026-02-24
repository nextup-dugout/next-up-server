package com.nextup.core.event

/**
 * 팀 가입 거절 이벤트
 *
 * 팀 가입 신청이 거절되었을 때 발행됩니다.
 */
data class TeamJoinRejectedEvent(
    val teamId: Long,
    val userId: Long,
    val teamName: String,
)
