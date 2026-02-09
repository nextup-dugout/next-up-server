package com.nextup.scorer.dto.pitch

import com.nextup.core.domain.game.PitchResult
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

/**
 * 투구 기록 요청 DTO
 */
data class RecordPitchRequest(
    @field:NotNull(message = "투수 ID는 필수입니다")
    @field:Positive(message = "투수 ID는 양수여야 합니다")
    val pitcherId: Long,
    @field:NotNull(message = "타자 ID는 필수입니다")
    @field:Positive(message = "타자 ID는 양수여야 합니다")
    val batterId: Long,
    @field:NotNull(message = "투구 결과는 필수입니다")
    val result: PitchResult,
    val description: String? = null,
)
