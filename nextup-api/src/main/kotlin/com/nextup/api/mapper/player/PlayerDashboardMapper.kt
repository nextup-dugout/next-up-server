package com.nextup.api.mapper.player

import com.nextup.api.dto.player.PlayerDashboardResponse
import com.nextup.api.dto.player.PlayerProfileResponse
import com.nextup.api.dto.player.TeamHistoryItemResponse
import com.nextup.api.mapper.stats.toResponse
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.PlayerTeamHistory
import com.nextup.core.service.player.dto.PlayerDashboardDto

/**
 * PlayerDashboardDto -> PlayerDashboardResponse 변환
 */
fun PlayerDashboardDto.toResponse(): PlayerDashboardResponse =
    PlayerDashboardResponse(
        profile = player.toProfileResponse(currentHistory),
        currentTeam = currentHistory?.toHistoryItemResponse(),
        seasonBattingStats = seasonBattingStats?.toResponse(),
        seasonPitchingStats = seasonPitchingStats?.toResponse(),
        careerBattingStats = careerBattingStats?.toResponse(),
        careerPitchingStats = careerPitchingStats?.toResponse(),
        recentBattingForm = recentBattingForm?.toResponse(),
        recentPitchingForm = recentPitchingForm?.toResponse(),
        teamHistory = teamHistory.map { it.toHistoryItemResponse() },
    )

/**
 * Player -> PlayerProfileResponse 변환
 */
fun Player.toProfileResponse(currentHistory: PlayerTeamHistory?): PlayerProfileResponse =
    PlayerProfileResponse(
        id = this.id,
        name = this.name,
        backNumber = currentHistory?.uniformNumber,
        position = currentHistory?.position?.displayName ?: this.primaryPosition.displayName,
        profileImageUrl = this.profileImageUrl,
        birthDate = this.birthDate,
        birthPlace = this.birthPlace,
        nationality = this.nationality,
        height = this.height,
        weight = this.weight,
        throwingHand = this.throwingHand?.name,
        battingHand = this.battingHand?.name,
        debutYear = this.debutYear,
        isActive = this.isActive,
    )

/**
 * PlayerTeamHistory -> TeamHistoryItemResponse 변환
 */
fun PlayerTeamHistory.toHistoryItemResponse(): TeamHistoryItemResponse =
    TeamHistoryItemResponse(
        teamId = this.team.id,
        teamName = this.team.name,
        position = this.position.displayName,
        uniformNumber = this.uniformNumber,
        startDate = this.startDate,
        endDate = this.endDate,
        isActive = this.isActive,
    )
