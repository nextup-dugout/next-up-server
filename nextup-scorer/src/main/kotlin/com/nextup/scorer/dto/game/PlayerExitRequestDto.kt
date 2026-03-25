package com.nextup.scorer.dto.game

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

/**
 * 선수 퇴장 (교체 없음) 요청 DTO
 *
 * 교체 선수 없이 선수가 경기에서 빠지는 경우에 사용합니다.
 * 부상, 개인 사유 등으로 교체 선수를 투입할 수 없는 상황입니다.
 */
data class PlayerExitRequestDto(
    @field:NotNull(message = "퇴장할 선수의 GamePlayer ID는 필수입니다.")
    val gamePlayerId: Long,
    @field:NotNull(message = "퇴장 이닝은 필수입니다.")
    @field:Min(value = 1, message = "이닝은 1 이상이어야 합니다.")
    val inning: Int,
)
