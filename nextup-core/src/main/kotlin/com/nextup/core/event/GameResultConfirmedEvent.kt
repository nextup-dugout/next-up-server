package com.nextup.core.event

/**
 * 경기 결과 확정 이벤트
 *
 * 경기가 종료되어 결과가 확정되었을 때 발행됩니다.
 */
data class GameResultConfirmedEvent(
    val gameId: Long,
    val homeTeamId: Long,
    val awayTeamId: Long,
    val homeScore: Int,
    val awayScore: Int,
)
