package com.nextup.backoffice.dto.bracket

import com.nextup.core.domain.competition.TournamentType
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class GenerateBracketRequest(
    @field:NotNull(message = "토너먼트 타입은 필수입니다")
    val tournamentType: TournamentType,
    @field:NotEmpty(message = "팀 목록은 비어있을 수 없습니다")
    val seededTeamIds: List<Long>,
)
