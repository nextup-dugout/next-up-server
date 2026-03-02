package com.nextup.core.domain.event

/**
 * 팀 해산 이벤트
 *
 * 팀이 해산되었을 때 발행됩니다.
 */
data class TeamDisbandedEvent(
    val teamId: Long,
)
