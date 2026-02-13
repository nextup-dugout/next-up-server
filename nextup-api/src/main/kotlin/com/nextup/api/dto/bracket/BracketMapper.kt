package com.nextup.api.dto.bracket

import com.nextup.core.domain.competition.BracketEntry
import com.nextup.core.domain.team.Team

fun BracketEntry.toResponse(): BracketEntryResponse =
    BracketEntryResponse(
        id = this.id,
        competitionId = this.competition.id,
        roundNumber = this.roundNumber,
        matchNumber = this.matchNumber,
        team1 = this.team1?.toBriefResponse(),
        team2 = this.team2?.toBriefResponse(),
        winner = this.winner?.toBriefResponse(),
        bracketType = this.bracketType,
        seed1 = this.seed1,
        seed2 = this.seed2,
        isBye = this.isBye(),
        isCompleted = this.isCompleted(),
    )

fun Team.toBriefResponse(): TeamBriefResponse =
    TeamBriefResponse(
        id = this.id,
        name = this.name,
        city = this.city,
    )

fun List<BracketEntry>.toBracketResponse(competitionId: Long): BracketResponse =
    BracketResponse(
        competitionId = competitionId,
        entries = this.map { it.toResponse() },
    )
