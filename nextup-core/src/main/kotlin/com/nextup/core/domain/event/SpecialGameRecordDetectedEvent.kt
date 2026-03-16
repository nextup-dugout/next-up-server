package com.nextup.core.domain.event

import com.nextup.core.domain.game.SpecialGameRecord

/**
 * 특수 경기 기록 감지 이벤트
 *
 * 경기 종료 후 노히트/퍼펙트게임이 감지되었을 때 발행됩니다.
 */
data class SpecialGameRecordDetectedEvent(
    val gameId: Long,
    val teamId: Long,
    val opponentTeamId: Long,
    val record: SpecialGameRecord,
)
