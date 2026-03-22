package com.nextup.core.domain.event

/**
 * 선수 교체 도메인 이벤트
 *
 * 경기 중 선수 교체가 발생할 때 발행됩니다.
 * WebSocket 브로드캐스트를 통해 교체 이벤트와 갱신된 경기 상태를 전송합니다.
 *
 * @param gameId 경기 ID
 * @param gameEventId 교체 기록이 저장된 GameEvent ID
 */
data class PlayerSubstitutedEvent(
    val gameId: Long,
    val gameEventId: Long,
)
