package com.nextup.core.domain.event

import com.nextup.core.domain.game.CorrectionType

/**
 * 기록 정정 도메인 이벤트
 *
 * 관리자가 타격/투수 기록을 정정했을 때 발행됩니다.
 * 이벤트 리스너에서 해당 선수의 시즌/커리어 스탯을 재집계하고,
 * BATTING 정정의 경우 GameEvent 타임라인도 갱신합니다(H6).
 *
 * @param gameEventId 정정 대상 타석의 GameEvent ID. BATTING 정정 시 GameEvent 갱신에 사용.
 */
data class RecordCorrectedEvent(
    val gameId: Long,
    val correctionType: CorrectionType,
    val playerId: Long,
    val fieldName: String,
    val oldValue: String,
    val newValue: String,
    val gameEventId: Long? = null,
)
