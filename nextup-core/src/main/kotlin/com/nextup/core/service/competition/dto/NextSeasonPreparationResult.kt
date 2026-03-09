package com.nextup.core.service.competition.dto

/**
 * 다음 시즌 준비 결과 DTO
 */
data class NextSeasonPreparationResult(
    val newCompetitionId: Long,
    val newCompetitionName: String,
    val year: Int,
    val season: Int,
    val previousCompetitionId: Long,
    val registeredTeamCount: Int,
    val registeredPlayerCount: Int,
    val skippedPlayerCount: Int,
)
