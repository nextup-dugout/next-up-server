package com.nextup.core.port.repository

import com.nextup.core.domain.player.PlayerTeamHistory
import java.time.LocalDate
import java.util.Optional

/**
 * PlayerTeamHistory Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface PlayerTeamHistoryRepositoryPort {

    fun save(playerTeamHistory: PlayerTeamHistory): PlayerTeamHistory

    fun findAll(): List<PlayerTeamHistory>

    fun delete(playerTeamHistory: PlayerTeamHistory)

    fun deleteById(id: Long)

    fun findByPlayerId(playerId: Long): List<PlayerTeamHistory>

    fun findByTeamId(teamId: Long): List<PlayerTeamHistory>

    fun findCurrentByPlayerId(playerId: Long): PlayerTeamHistory?

    fun findCurrentByTeamId(teamId: Long): List<PlayerTeamHistory>

    fun findByPlayerIdWithDetails(playerId: Long): List<PlayerTeamHistory>

    fun findByTeamIdAtDate(teamId: Long, date: LocalDate): List<PlayerTeamHistory>
}
