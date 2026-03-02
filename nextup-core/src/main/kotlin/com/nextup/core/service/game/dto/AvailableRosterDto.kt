package com.nextup.core.service.game.dto

import com.nextup.core.domain.competition.CompetitionPlayerStatus
import com.nextup.core.domain.player.Position

/**
 * 라인업 제출용 출전 가능 선수 DTO
 */
data class AvailableRosterDto(
    val players: List<RosterPlayerDto>,
)

/**
 * 로스터 선수 정보 DTO
 */
data class RosterPlayerDto(
    val playerId: Long,
    val playerName: String,
    val primaryPosition: Position,
    val profileImageUrl: String?,
    val competitionPlayerStatus: CompetitionPlayerStatus,
    val isEligible: Boolean,
)
