package com.nextup.scorer.dto.game

import com.nextup.core.domain.game.Base
import com.nextup.core.domain.game.BaseRunningResult
import com.nextup.core.service.game.dto.BaseRunningRequest
import jakarta.validation.constraints.NotNull

/**
 * 주루 플레이 기록 요청 DTO (scorer API 계층)
 */
data class BaseRunningRequestDto(
    @field:NotNull(message = "주자 ID는 필수입니다")
    val runnerId: Long?,
    @field:NotNull(message = "출발 베이스는 필수입니다")
    val fromBase: Base?,
    @field:NotNull(message = "도착 베이스는 필수입니다")
    val toBase: Base?,
    @field:NotNull(message = "주루 플레이 결과는 필수입니다")
    val result: BaseRunningResult?,
)

/**
 * BaseRunningRequestDto를 도메인 BaseRunningRequest로 변환합니다.
 */
fun BaseRunningRequestDto.toDomain(): BaseRunningRequest =
    BaseRunningRequest(
        runnerId = this.runnerId!!,
        fromBase = this.fromBase!!,
        toBase = this.toBase!!,
        result = this.result!!,
    )
