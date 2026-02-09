package com.nextup.infrastructure.persistence.match

import com.nextup.core.domain.match.MatchRequest
import com.nextup.core.domain.match.MatchRequestStatus
import com.nextup.core.port.repository.MatchRequestRepositoryPort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class MatchRequestRepositoryAdapter(
    private val jpaRepository: MatchRequestJpaRepository,
) : MatchRequestRepositoryPort {
    override fun save(matchRequest: MatchRequest): MatchRequest = jpaRepository.save(matchRequest)

    override fun findByIdOrNull(id: Long): MatchRequest? = jpaRepository.findByIdOrNull(id)

    override fun findByTeamId(teamId: Long): List<MatchRequest> = jpaRepository.findByTeamId(teamId)

    override fun findAllOpen(): List<MatchRequest> = jpaRepository.findAllOpen()

    override fun findByStatus(status: MatchRequestStatus): List<MatchRequest> = jpaRepository.findByStatus(status)
}
