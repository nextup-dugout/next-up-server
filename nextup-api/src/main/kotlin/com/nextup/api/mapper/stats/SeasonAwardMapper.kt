package com.nextup.api.mapper.stats

import com.nextup.api.dto.stats.SeasonAwardResponse
import com.nextup.core.domain.stats.SeasonAward

/**
 * SeasonAward Entity를 SeasonAwardResponse DTO로 변환하는 Extension Function
 */
fun SeasonAward.toResponse(): SeasonAwardResponse =
    SeasonAwardResponse(
        id = this.id,
        playerId = this.player.id,
        playerName = this.player.name,
        year = this.year,
        competitionId = this.competitionId,
        title = this.title.name,
        titleDisplayName = this.title.displayName,
        titleDescription = this.title.description,
        statValue = this.statValue?.toPlainString(),
        createdAt = this.createdAt,
    )

/**
 * List<SeasonAward>를 List<SeasonAwardResponse>로 변환
 */
fun List<SeasonAward>.toSeasonAwardResponse(): List<SeasonAwardResponse> = this.map { it.toResponse() }
