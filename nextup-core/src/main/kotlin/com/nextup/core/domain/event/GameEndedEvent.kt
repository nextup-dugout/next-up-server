package com.nextup.core.domain.event

/**
 * 경기 종료 도메인 이벤트
 *
 * 경기가 정규 종료, 콜드게임, 몰수, 취소 등으로 끝날 때 발행됩니다.
 * WebSocket 브로드캐스트를 통해 최종 스코어보드를 전송합니다.
 *
 * @param gameId 경기 ID
 * @param finalStatus 최종 경기 상태 (GameStatus.name 문자열)
 */
data class GameEndedEvent(
    val gameId: Long,
    val finalStatus: String,
)
