package com.nextup.api.dto.team

data class TeamSummaryResponse(
    val teamId: Long,
    val name: String,
    val city: String,
    val abbreviation: String?,
    val memberCount: Int,
)

data class TeamDetailResponse(
    val teamId: Long,
    val name: String,
    val city: String,
    val abbreviation: String?,
    val leagueName: String?,
    val foundedYear: Int,
    val memberCount: Int,
)
