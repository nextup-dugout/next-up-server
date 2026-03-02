package com.nextup.api.dto.game

import com.nextup.core.domain.competition.CompetitionPlayerStatus
import com.nextup.core.domain.player.Position
import com.nextup.core.service.game.dto.AvailableRosterDto
import com.nextup.core.service.game.dto.RosterPlayerDto

/**
 * 라인업 제출용 출전 가능 선수 목록 응답 DTO
 */
data class AvailableRosterResponse(
    val players: List<RosterPlayerResponse>,
) {
    companion object {
        fun from(dto: AvailableRosterDto): AvailableRosterResponse =
            AvailableRosterResponse(
                players = dto.players.map { RosterPlayerResponse.from(it) },
            )
    }
}

/**
 * 로스터 선수 정보 응답 DTO
 */
data class RosterPlayerResponse(
    val playerId: Long,
    val playerName: String,
    val primaryPosition: Position,
    val profileImageUrl: String?,
    val competitionPlayerStatus: CompetitionPlayerStatus,
    val isEligible: Boolean,
) {
    companion object {
        fun from(dto: RosterPlayerDto): RosterPlayerResponse =
            RosterPlayerResponse(
                playerId = dto.playerId,
                playerName = dto.playerName,
                primaryPosition = dto.primaryPosition,
                profileImageUrl = dto.profileImageUrl,
                competitionPlayerStatus = dto.competitionPlayerStatus,
                isEligible = dto.isEligible,
            )
    }
}
