package com.nextup.core.domain.event

/**
 * 포지션 변경 도메인 이벤트
 *
 * 경기 중 선수 포지션 변경이 발생할 때 발행됩니다.
 * WebSocket 브로드캐스트를 통해 포지션 변경 이벤트와 갱신된 경기 상태를 전송합니다.
 *
 * @param gameId 경기 ID
 * @param gameEventId 포지션 변경 기록이 저장된 GameEvent ID
 */
data class PositionChangedEvent(
    val gameId: Long,
    val gameEventId: Long,
)
