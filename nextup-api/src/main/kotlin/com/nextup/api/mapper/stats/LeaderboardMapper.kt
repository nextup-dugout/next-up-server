package com.nextup.api.mapper.stats

import com.nextup.api.dto.stats.BattingLeaderResponse
import com.nextup.api.dto.stats.PitchingLeaderResponse
import com.nextup.core.service.stats.dto.BattingLeaderDto
import com.nextup.core.service.stats.dto.PitchingLeaderDto

fun BattingLeaderDto.toResponse(): BattingLeaderResponse =
    BattingLeaderResponse(
        rank = this.rank,
        playerId = this.playerId,
        playerName = this.playerName,
        teamName = this.teamName,
        value = this.value,
        games = this.games,
        plateAppearances = this.plateAppearances,
    )

fun PitchingLeaderDto.toResponse(): PitchingLeaderResponse =
    PitchingLeaderResponse(
        rank = this.rank,
        playerId = this.playerId,
        playerName = this.playerName,
        teamName = this.teamName,
        value = this.value,
        games = this.games,
        inningsPitched = this.inningsPitched,
    )

fun List<BattingLeaderDto>.toBattingLeaderResponse(): List<BattingLeaderResponse> = this.map { it.toResponse() }

fun List<PitchingLeaderDto>.toPitchingLeaderResponse(): List<PitchingLeaderResponse> = this.map { it.toResponse() }
