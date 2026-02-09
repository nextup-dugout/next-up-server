package com.nextup.backoffice.dto.bracket

data class BracketEntryResponse(
    val id: Long,
    val competitionId: Long,
    val roundNumber: Int,
    val matchNumber: Int,
    val team1: TeamBriefResponse?,
    val team2: TeamBriefResponse?,
    val winner: TeamBriefResponse?,
    val bracketType: String,
    val seed1: Int?,
    val seed2: Int?,
    val isBye: Boolean,
    val isCompleted: Boolean,
)

data class TeamBriefResponse(
    val id: Long,
    val name: String,
    val city: String,
)
