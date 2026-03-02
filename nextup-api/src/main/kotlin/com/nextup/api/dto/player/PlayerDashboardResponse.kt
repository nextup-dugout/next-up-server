package com.nextup.api.dto.player

import com.nextup.api.dto.stats.CareerBattingStatsResponse
import com.nextup.api.dto.stats.CareerPitchingStatsResponse
import com.nextup.api.dto.stats.RecentFormResponse
import com.nextup.api.dto.stats.SeasonBattingStatsResponse
import com.nextup.api.dto.stats.SeasonPitchingStatsResponse
import java.time.LocalDate

/**
 * 선수 대시보드 통합 응답 DTO
 *
 * GET /api/v1/players/{playerId}/dashboard
 */
data class PlayerDashboardResponse(
    val profile: PlayerProfileResponse,
    val currentTeam: TeamHistoryItemResponse?,
    val seasonBattingStats: SeasonBattingStatsResponse?,
    val seasonPitchingStats: SeasonPitchingStatsResponse?,
    val careerBattingStats: CareerBattingStatsResponse?,
    val careerPitchingStats: CareerPitchingStatsResponse?,
    val recentBattingForm: RecentFormResponse?,
    val recentPitchingForm: RecentFormResponse?,
    val teamHistory: List<TeamHistoryItemResponse>,
)

/**
 * 선수 프로필 응답
 */
data class PlayerProfileResponse(
    val id: Long,
    val name: String,
    val backNumber: Int?,
    val position: String?,
    val profileImageUrl: String?,
    val birthDate: LocalDate?,
    val birthPlace: String?,
    val nationality: String?,
    val height: Int?,
    val weight: Int?,
    val throwingHand: String?,
    val battingHand: String?,
    val debutYear: Int?,
    val isActive: Boolean,
)

/**
 * 팀 소속 이력 항목 응답
 */
data class TeamHistoryItemResponse(
    val teamId: Long,
    val teamName: String,
    val position: String?,
    val uniformNumber: Int?,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val isActive: Boolean,
)
