package com.nextup.api.dto.game

import jakarta.validation.constraints.NotNull

/**
 * 타격 기록 생성 요청 DTO
 *
 * @property playerId 선수 ID (Game ID는 PathVariable로 받음)
 */
data class CreateBattingRecordRequest(
    @field:NotNull(message = "playerId is required")
    val playerId: Long
)
