package com.nextup.api.dto.stats

import java.time.Instant

/**
 * 시즌 타이틀(개인상) 응답 DTO
 */
data class SeasonAwardResponse(
    val id: Long,
    val playerId: Long,
    val playerName: String,
    val year: Int,
    val competitionId: Long?,
    val title: String,
    val titleDisplayName: String,
    val titleDescription: String,
    val statValue: String?,
    val createdAt: Instant,
)
