package com.nextup.core.domain.event

/**
 * 경기 시작 도메인 이벤트
 *
 * 경기가 시작될 때 발행됩니다.
 * WebSocket 브로드캐스트를 통해 연결된 클라이언트에게 경기 시작을 알립니다.
 *
 * @param gameId 경기 ID
 */
data class GameStartedEvent(
    val gameId: Long,
)
