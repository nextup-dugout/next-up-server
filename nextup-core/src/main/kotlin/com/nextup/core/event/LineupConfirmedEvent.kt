package com.nextup.core.event

/**
 * 라인업 확정 이벤트
 *
 * 라인업이 기록원에 의해 확정되었을 때 발행됩니다.
 */
data class LineupConfirmedEvent(
    val gameId: Long,
    val teamId: Long,
)
