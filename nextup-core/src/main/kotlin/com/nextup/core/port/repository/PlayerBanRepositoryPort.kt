package com.nextup.core.port.repository

import com.nextup.core.domain.discipline.PlayerBan

/**
 * PlayerBan Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface PlayerBanRepositoryPort {
    fun save(playerBan: PlayerBan): PlayerBan

    fun findByIdOrNull(id: Long): PlayerBan?

    fun findAll(): List<PlayerBan>

    fun findByPlayerId(playerId: Long): List<PlayerBan>

    fun findByCompetitionId(competitionId: Long): List<PlayerBan>

    fun findByPlayerIdAndCompetitionId(
        playerId: Long,
        competitionId: Long,
    ): List<PlayerBan>

    fun delete(playerBan: PlayerBan)

    fun deleteById(id: Long)

    fun existsByPlayerIdAndCompetitionId(
        playerId: Long,
        competitionId: Long,
    ): Boolean
}
