package com.nextup.core.domain.event

/**
 * 타순 위반 감지 이벤트
 *
 * 타석 기록 시 현재 타자가 올바른 타순이 아닐 때 발행됩니다.
 * 사회인 야구의 유연성을 고려하여 예외(Exception)가 아닌 경고(Warning) 이벤트로 처리합니다.
 * 기록원이 실수를 인지하고 수동 수정할 수 있도록 알림을 제공합니다.
 *
 * @property gameId 경기 ID
 * @property gamePlayerId 타자 GamePlayer ID
 * @property playerId 타자 Player ID
 * @property expectedBattingOrder 예상 타순
 * @property actualBattingOrder 실제 입력된 타자의 타순
 * @property inning 발생 이닝
 * @property isTopInning 초(true)/말(false) 여부
 */
data class BattingOrderViolationEvent(
    val gameId: Long,
    val gamePlayerId: Long,
    val playerId: Long,
    val expectedBattingOrder: Int,
    val actualBattingOrder: Int,
    val inning: Int,
    val isTopInning: Boolean,
)
