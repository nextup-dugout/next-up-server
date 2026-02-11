package com.nextup.backoffice.dto.player

import com.nextup.core.domain.player.ContractType
import com.nextup.core.domain.player.PlayerTeamHistory
import com.nextup.core.domain.player.PlayerTeamStatus
import com.nextup.core.domain.player.Position
import java.time.Instant
import java.time.LocalDate

/**
 * 선수 팀 소속 응답 DTO
 *
 * backoffice 모듈에 독립적으로 존재
 */
data class PlayerTeamResponse(
    val id: Long,
    val playerId: Long,
    val playerName: String,
    val teamId: Long,
    val teamName: String,
    val leagueId: Long,
    val leagueName: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val uniformNumber: Int?,
    val position: Position,
    val contractType: ContractType,
    val status: PlayerTeamStatus,
    val isCurrentAffiliation: Boolean,
    val durationInDays: Long?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(history: PlayerTeamHistory): PlayerTeamResponse =
            PlayerTeamResponse(
                id = history.id,
                playerId = history.player.id,
                playerName = history.player.name,
                teamId = history.team.id,
                teamName = history.team.fullName,
                leagueId = history.team.league.id,
                leagueName = history.team.league.name,
                startDate = history.startDate,
                endDate = history.endDate,
                uniformNumber = history.uniformNumber,
                position = history.position,
                contractType = history.contractType,
                status = history.status,
                isCurrentAffiliation = history.isCurrentAffiliation,
                durationInDays = history.durationInDays,
                createdAt = history.createdAt,
                updatedAt = history.updatedAt,
            )
    }
}
