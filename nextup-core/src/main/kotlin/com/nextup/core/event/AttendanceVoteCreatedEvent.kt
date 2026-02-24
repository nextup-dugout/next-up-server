package com.nextup.core.event

import java.time.LocalDateTime

/**
 * 출석 투표 생성 이벤트
 *
 * 출석 투표가 생성되었을 때 발행됩니다.
 */
data class AttendanceVoteCreatedEvent(
    val teamId: Long,
    val pollId: Long,
    val eventDate: LocalDateTime,
)
