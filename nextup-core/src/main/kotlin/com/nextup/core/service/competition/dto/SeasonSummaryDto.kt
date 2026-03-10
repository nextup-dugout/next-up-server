package com.nextup.core.service.competition.dto

import com.nextup.core.service.standings.dto.TeamStandingDto
import java.time.LocalDate

/**
 * 시즌 종료 요약 DTO
 *
 * 완료된 대회의 최종 순위, 참가 팀/선수 수 등을 포함합니다.
 */
data class SeasonSummaryDto(
    val competitionId: Long,
    val competitionName: String,
    val year: Int,
    val season: Int,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val totalTeams: Int,
    val totalPlayers: Int,
    val finalStandings: List<TeamStandingDto>,
)
