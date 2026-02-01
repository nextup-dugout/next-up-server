package com.nextup.api.dto.game

import jakarta.validation.constraints.NotNull

/**
 * 투수 기록 생성 요청 DTO
 *
 * @property playerId 선수 ID (Game ID는 PathVariable로 받음)
 * @property isStartingPitcher 선발 투수 여부 (기본값: false)
 */
data class CreatePitchingRecordRequest(
    @field:NotNull(message = "playerId is required")
    val playerId: Long,
    val isStartingPitcher: Boolean = false
)
