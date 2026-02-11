package com.nextup.core.port.repository

import com.nextup.core.domain.election.ElectionVote

/**
 * ElectionVote Repository Port
 */
interface ElectionVoteRepositoryPort {
    /**
     * ElectionVote를 저장합니다.
     */
    fun save(electionVote: ElectionVote): ElectionVote

    /**
     * 선거 ID로 모든 ElectionVote를 조회합니다.
     */
    fun findAllByElectionId(electionId: Long): List<ElectionVote>

    /**
     * 선거 ID와 투표자 ID로 ElectionVote를 조회합니다.
     */
    fun findByElectionIdAndVoterId(
        electionId: Long,
        voterId: Long,
    ): ElectionVote?

    /**
     * 선거 ID로 투표 수를 집계합니다 (후보자별).
     */
    fun countByElectionIdGroupByCandidateId(electionId: Long): Map<Long, Long>
}
