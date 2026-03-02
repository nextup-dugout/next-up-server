package com.nextup.core.service.player.dto

import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.PlayerTeamHistory
import com.nextup.core.domain.stats.CareerBattingStats
import com.nextup.core.domain.stats.CareerPitchingStats
import com.nextup.core.domain.stats.SeasonBattingStats
import com.nextup.core.domain.stats.SeasonPitchingStats
import com.nextup.core.service.stats.dto.RecentFormDto

/**
 * 선수 대시보드 통합 DTO
 *
 * 선수 프로필 화면에 필요한 모든 데이터를 단일 구조로 제공합니다.
 */
data class PlayerDashboardDto(
    val player: Player,
    val currentHistory: PlayerTeamHistory?,
    val seasonBattingStats: SeasonBattingStats?,
    val seasonPitchingStats: SeasonPitchingStats?,
    val careerBattingStats: CareerBattingStats?,
    val careerPitchingStats: CareerPitchingStats?,
    val recentBattingForm: RecentFormDto?,
    val recentPitchingForm: RecentFormDto?,
    val teamHistory: List<PlayerTeamHistory>,
)
