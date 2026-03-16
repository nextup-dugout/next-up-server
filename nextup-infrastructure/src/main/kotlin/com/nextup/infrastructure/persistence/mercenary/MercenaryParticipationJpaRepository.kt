package com.nextup.infrastructure.persistence.mercenary

import com.nextup.core.domain.mercenary.MercenaryParticipation
import org.springframework.data.jpa.repository.JpaRepository

interface MercenaryParticipationJpaRepository : JpaRepository<MercenaryParticipation, Long> {
    fun findByGameId(gameId: Long): List<MercenaryParticipation>

    fun findByPlayerId(playerId: Long): List<MercenaryParticipation>

    fun findByTeamId(teamId: Long): List<MercenaryParticipation>

    fun existsByGameIdAndPlayerId(
        gameId: Long,
        playerId: Long,
    ): Boolean
}
