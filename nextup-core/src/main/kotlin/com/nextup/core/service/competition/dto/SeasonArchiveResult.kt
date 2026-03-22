package com.nextup.core.service.competition.dto

/**
 * 시즌 통계 아카이브/해제 결과 DTO
 */
data class SeasonArchiveResult(
    val competitionId: Long,
    val competitionName: String,
    val year: Int,
    val battingStatsFinalized: Int,
    val pitchingStatsFinalized: Int,
    val fieldingStatsFinalized: Int,
) {
    val totalStatsFinalized: Int
        get() = battingStatsFinalized + pitchingStatsFinalized + fieldingStatsFinalized
}
