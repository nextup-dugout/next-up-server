package com.nextup.api.dto.match

import jakarta.validation.constraints.NotNull

data class RespondToMatchRequestApiRequest(
    @field:NotNull(message = "응답 팀 ID는 필수입니다")
    val respondTeamId: Long,
    val message: String?,
)
