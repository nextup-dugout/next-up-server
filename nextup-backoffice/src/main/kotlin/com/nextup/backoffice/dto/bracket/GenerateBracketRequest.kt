package com.nextup.backoffice.dto.bracket

import jakarta.validation.constraints.NotEmpty

data class GenerateBracketRequest(
    @field:NotEmpty(message = "팀 목록은 비어있을 수 없습니다")
    val seededTeamIds: List<Long>,
)
