package com.nextup.core.domain.event

/**
 * 팀 가입 승인 이벤트
 *
 * 팀 가입 신청이 승인되었을 때 발행됩니다.
 */
data class TeamJoinApprovedEvent(
    val teamId: Long,
    val userId: Long,
    val teamName: String,
)
