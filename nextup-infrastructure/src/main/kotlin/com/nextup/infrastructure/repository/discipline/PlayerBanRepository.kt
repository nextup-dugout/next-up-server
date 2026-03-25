package com.nextup.infrastructure.repository.discipline

import com.nextup.core.domain.discipline.PlayerBan
import com.nextup.core.port.repository.PlayerBanRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository

interface PlayerBanRepository :
    JpaRepository<PlayerBan, Long>,
    PlayerBanRepositoryPort {
    override fun findByIdOrNull(id: Long): PlayerBan? = findById(id).orElse(null)

    override fun findByPlayerId(playerId: Long): List<PlayerBan>

    override fun findByCompetitionId(competitionId: Long): List<PlayerBan>

    override fun findByPlayerIdAndCompetitionId(
        playerId: Long,
        competitionId: Long,
    ): List<PlayerBan>

    override fun existsByPlayerIdAndCompetitionId(
        playerId: Long,
        competitionId: Long,
    ): Boolean
}
