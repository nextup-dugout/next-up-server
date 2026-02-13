package com.nextup.infrastructure.persistence.election

import com.nextup.core.domain.election.ElectionVote
import com.nextup.core.port.repository.ElectionVoteRepositoryPort
import org.springframework.stereotype.Repository

/**
 * ElectionVote Repository Adapter
 */
@Repository
class ElectionVoteRepositoryAdapter(
    private val jpaRepository: ElectionVoteJpaRepository,
) : ElectionVoteRepositoryPort {
    override fun save(electionVote: ElectionVote): ElectionVote = jpaRepository.save(electionVote)

    override fun findAllByElectionId(electionId: Long): List<ElectionVote> =
        jpaRepository.findAllByElectionId(electionId)

    override fun findByElectionIdAndVoterId(
        electionId: Long,
        voterId: Long,
    ): ElectionVote? = jpaRepository.findByElectionIdAndVoterId(electionId, voterId)

    override fun countByElectionIdGroupByCandidateId(electionId: Long): Map<Long, Long> =
        jpaRepository.countByElectionIdGroupByCandidateId(electionId)
            .associate { it.candidateId to it.voteCount }
}
