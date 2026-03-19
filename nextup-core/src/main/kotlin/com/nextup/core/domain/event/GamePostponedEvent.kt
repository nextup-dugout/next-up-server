package com.nextup.core.domain.event

import java.time.LocalDateTime

/**
 * 경기 연기 도메인 이벤트
 *
 * 경기가 연기될 때 발행됩니다.
 * 해당 경기에 참여하는 양 팀의 멤버에게 알림을 전송하기 위해 사용됩니다.
 */
data class GamePostponedEvent(
    val gameId: Long,
    val homeTeamId: Long,
    val awayTeamId: Long,
    val newScheduledAt: LocalDateTime,
)
