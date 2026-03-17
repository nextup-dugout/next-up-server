package com.nextup.infrastructure.persistence.mercenary

import com.nextup.core.domain.mercenary.MercenaryApplication
import com.nextup.core.domain.mercenary.MercenaryApplicationStatus
import com.nextup.core.port.repository.MercenaryApplicationRepositoryPort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class MercenaryApplicationRepositoryAdapter(
    private val jpaRepository: MercenaryApplicationJpaRepository,
) : MercenaryApplicationRepositoryPort {
    override fun save(application: MercenaryApplication): MercenaryApplication = jpaRepository.save(application)

    override fun findByIdOrNull(id: Long): MercenaryApplication? = jpaRepository.findByIdOrNull(id)

    override fun findByRequestId(requestId: Long): List<MercenaryApplication> = jpaRepository.findByRequestId(requestId)

    override fun findByPlayerId(playerId: Long): List<MercenaryApplication> = jpaRepository.findByPlayerId(playerId)

    override fun existsByRequestIdAndPlayerId(
        requestId: Long,
        playerId: Long,
    ): Boolean = jpaRepository.existsByRequestIdAndPlayerId(requestId, playerId)

    override fun countAcceptedByRequestId(requestId: Long): Long =
        jpaRepository.countByRequestIdAndStatus(requestId, MercenaryApplicationStatus.ACCEPTED)
}
