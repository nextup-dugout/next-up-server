package com.nextup.backoffice.dto.player

import com.nextup.core.domain.player.PlayerTeamStatus
import com.nextup.core.domain.player.Position
import java.time.Instant
import java.time.LocalDate

/**
 * 선수 팀 소속 응답 DTO
 *
 * backoffice 모듈에 독립적으로 존재
 * 변환 로직은 PlayerExtensions.kt의 Extension Function을 사용합니다.
 */
data class PlayerTeamResponse(
    val id: Long,
    val playerId: Long,
    val playerName: String,
    val teamId: Long,
    val teamName: String,
    val teamLogoUrl: String?,
    val leagueId: Long,
    val leagueName: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val uniformNumber: Int?,
    val position: Position,
    val status: PlayerTeamStatus,
    val isCurrentAffiliation: Boolean,
    val durationInDays: Long?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
