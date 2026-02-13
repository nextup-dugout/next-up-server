package com.nextup.api.dto.pitch

import com.nextup.core.service.PitcherPitchStats

/**
 * 투수 투구 통계 응답 DTO
 */
data class PitcherStatsResponse(
    val totalPitches: Int,
    val strikes: Int,
    val balls: Int,
    val fouls: Int,
    val inPlayPitches: Int,
    val strikePercentage: Double,
    val avgPitchesPerAtBat: Double,
)

/**
 * PitcherPitchStats → Response DTO 변환
 */
fun PitcherPitchStats.toResponse(): PitcherStatsResponse =
    PitcherStatsResponse(
        totalPitches = this.totalPitches,
        strikes = this.strikes,
        balls = this.balls,
        fouls = this.fouls,
        inPlayPitches = this.inPlayPitches,
        strikePercentage = this.strikePercentage,
        avgPitchesPerAtBat = this.avgPitchesPerAtBat,
    )
