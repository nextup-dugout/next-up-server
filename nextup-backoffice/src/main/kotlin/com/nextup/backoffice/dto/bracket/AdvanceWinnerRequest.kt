package com.nextup.backoffice.dto.bracket

import jakarta.validation.constraints.NotNull

data class AdvanceWinnerRequest(
    @field:NotNull(message = "승자 팀 ID는 필수입니다")
    val winnerTeamId: Long,
)
