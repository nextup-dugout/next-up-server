package com.nextup.infrastructure.persistence.election

import com.nextup.core.domain.election.Candidate
import com.nextup.core.port.repository.CandidateRepositoryPort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

/**
 * Candidate Repository Adapter
 */
@Repository
class CandidateRepositoryAdapter(
    private val jpaRepository: CandidateJpaRepository,
) : CandidateRepositoryPort {
    override fun save(candidate: Candidate): Candidate = jpaRepository.save(candidate)

    override fun findById(id: Long): Candidate? = jpaRepository.findByIdOrNull(id)

    override fun findAllByElectionId(electionId: Long): List<Candidate> = jpaRepository.findAllByElectionId(electionId)

    override fun findByElectionIdAndMemberId(
        electionId: Long,
        memberId: Long,
    ): Candidate? = jpaRepository.findByElectionIdAndMemberId(electionId, memberId)

    override fun delete(candidate: Candidate) {
        jpaRepository.delete(candidate)
    }
}
