package com.nextup.core.domain.event

/**
 * L-10: OWNER 강퇴 이벤트
 *
 * 팀 OWNER가 강퇴되었을 때 발행됩니다.
 * 자동으로 선거(Election)를 트리거하는 데 사용됩니다.
 */
data class OwnerKickedEvent(
    val teamId: Long,
    val kickedPlayerId: Long,
    val kickedMemberId: Long,
)
