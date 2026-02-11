package com.nextup.api.dto.game

import com.nextup.core.domain.game.PlateAppearanceResult
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

/**
 * 타석 결과 기록 요청 DTO
 *
 * Entity의 recordPlateAppearance() 메서드 파라미터와 매핑됩니다.
 */
data class RecordPlateAppearanceRequest(
    @field:NotNull(message = "타석 결과는 필수입니다.")
    val result: PlateAppearanceResult,
    @field:Min(value = 0, message = "타점은 0 이상이어야 합니다.")
    val runsBattedIn: Int = 0,
    val runsScored: Boolean = false,
)
