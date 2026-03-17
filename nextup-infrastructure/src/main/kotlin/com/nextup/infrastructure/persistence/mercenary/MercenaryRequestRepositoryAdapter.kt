package com.nextup.infrastructure.persistence.mercenary

import com.nextup.core.domain.mercenary.MercenaryRequest
import com.nextup.core.domain.mercenary.MercenaryRequestStatus
import com.nextup.core.port.repository.MercenaryRequestRepositoryPort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class MercenaryRequestRepositoryAdapter(
    private val jpaRepository: MercenaryRequestJpaRepository,
) : MercenaryRequestRepositoryPort {
    override fun save(mercenaryRequest: MercenaryRequest): MercenaryRequest = jpaRepository.save(mercenaryRequest)

    override fun findByIdOrNull(id: Long): MercenaryRequest? = jpaRepository.findByIdOrNull(id)

    override fun findByStatus(status: MercenaryRequestStatus): List<MercenaryRequest> =
        jpaRepository.findByStatus(status)

    override fun findByRequestingTeamId(teamId: Long): List<MercenaryRequest> =
        jpaRepository.findByRequestingTeamId(teamId)

    override fun findByGameId(gameId: Long): List<MercenaryRequest> = jpaRepository.findByGameId(gameId)
}
