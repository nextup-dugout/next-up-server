package com.nextup.infrastructure.persistence.election

import com.nextup.core.domain.election.Election
import com.nextup.core.port.repository.ElectionRepositoryPort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

/**
 * Election Repository Adapter
 */
@Repository
class ElectionRepositoryAdapter(
    private val jpaRepository: ElectionJpaRepository,
) : ElectionRepositoryPort {
    override fun save(election: Election): Election = jpaRepository.save(election)

    override fun findById(id: Long): Election? = jpaRepository.findByIdOrNull(id)

    override fun findAllByTeamId(teamId: Long): List<Election> = jpaRepository.findAllByTeamId(teamId)

    override fun delete(election: Election) {
        jpaRepository.delete(election)
    }

    override fun countByParentElectionId(parentElectionId: Long): Long =
        jpaRepository.countByParentElectionId(parentElectionId)
}
