package com.nextup.infrastructure.persistence.mercenary

import com.nextup.core.domain.mercenary.MercenaryParticipation
import com.nextup.core.port.repository.MercenaryParticipationRepositoryPort
import org.springframework.stereotype.Repository

@Repository
class MercenaryParticipationRepositoryAdapter(
    private val jpaRepository: MercenaryParticipationJpaRepository,
) : MercenaryParticipationRepositoryPort {
    override fun save(participation: MercenaryParticipation): MercenaryParticipation = jpaRepository.save(participation)

    override fun findByGameId(gameId: Long): List<MercenaryParticipation> = jpaRepository.findByGameId(gameId)

    override fun findByPlayerId(playerId: Long): List<MercenaryParticipation> = jpaRepository.findByPlayerId(playerId)

    override fun findByTeamId(teamId: Long): List<MercenaryParticipation> = jpaRepository.findByTeamId(teamId)

    override fun existsByGameIdAndPlayerId(
        gameId: Long,
        playerId: Long,
    ): Boolean = jpaRepository.existsByGameIdAndPlayerId(gameId, playerId)
}
