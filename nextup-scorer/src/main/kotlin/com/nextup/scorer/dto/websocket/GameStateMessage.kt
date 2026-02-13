package com.nextup.scorer.dto.websocket

/**
 * 경기 상태 메시지 DTO
 *
 * 경기 실시간 상태(이닝, 아웃, 주자)를 브로드캐스트합니다.
 * Topic: /topic/games/{gameId}/state
 */
data class GameStateMessage(
    val gameId: Long,
    val inning: Int,
    val isTopInning: Boolean,
    val outs: Int,
    val balls: Int,
    val strikes: Int,
    val runners: RunnersDto,
    val currentBatter: PlayerBriefDto?,
    val currentPitcher: PlayerBriefDto?
)

/**
 * 주자 상태 DTO
 */
data class RunnersDto(
    val first: PlayerBriefDto?,
    val second: PlayerBriefDto?,
    val third: PlayerBriefDto?
)
