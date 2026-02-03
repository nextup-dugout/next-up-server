package com.nextup.scorer.dto.websocket

/**
 * 선수 간략 정보 DTO
 *
 * WebSocket 메시지에서 선수 정보를 간략하게 전달합니다.
 */
data class PlayerBriefDto(
    val id: Long,
    val name: String,
    val backNumber: Int?
)
