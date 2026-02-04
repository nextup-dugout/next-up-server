package com.nextup.scorer.dto.game

import com.nextup.core.domain.game.Base
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.service.game.dto.PlateAppearanceRequest
import com.nextup.core.service.game.dto.RunnerMovement
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

/**
 * 타석 결과 입력 요청 DTO
 */
data class PlateAppearanceRequestDto(
    @field:NotNull(message = "타자 ID는 필수입니다")
    val batterId: Long?,
    @field:NotNull(message = "투수 ID는 필수입니다")
    val pitcherId: Long?,
    @field:NotNull(message = "타석 결과는 필수입니다")
    val result: PlateAppearanceResult?,
    @field:Valid
    val runnerMovements: List<RunnerMovementDto> = emptyList(),
    @field:Min(value = 0, message = "타점은 0 이상이어야 합니다")
    val rbis: Int? = null,
    @field:Min(value = 0, message = "볼 카운트는 0 이상이어야 합니다")
    @field:Max(value = 4, message = "볼 카운트는 4 이하여야 합니다")
    val balls: Int = 0,
    @field:Min(value = 0, message = "스트라이크 카운트는 0 이상이어야 합니다")
    @field:Max(value = 3, message = "스트라이크 카운트는 3 이하여야 합니다")
    val strikes: Int = 0
)

/**
 * 주자 이동 DTO
 */
data class RunnerMovementDto(
    @field:NotNull(message = "주자 ID는 필수입니다")
    val runnerId: Long?,
    @field:NotNull(message = "출발 베이스는 필수입니다")
    val fromBase: Base?,
    @field:NotNull(message = "도착 베이스는 필수입니다")
    val toBase: Base?,
    val isOut: Boolean = false
)

/**
 * PlateAppearanceRequestDto를 PlateAppearanceRequest로 변환합니다.
 */
fun PlateAppearanceRequestDto.toDomain(): PlateAppearanceRequest {
    return PlateAppearanceRequest(
        batterId = this.batterId!!,
        pitcherId = this.pitcherId!!,
        result = this.result!!,
        runnerMovements = this.runnerMovements.map { it.toDomain() },
        rbis = this.rbis,
        balls = this.balls,
        strikes = this.strikes
    )
}

/**
 * RunnerMovementDto를 RunnerMovement로 변환합니다.
 */
fun RunnerMovementDto.toDomain(): RunnerMovement {
    return RunnerMovement(
        runnerId = this.runnerId!!,
        fromBase = this.fromBase!!,
        toBase = this.toBase!!,
        isOut = this.isOut
    )
}
