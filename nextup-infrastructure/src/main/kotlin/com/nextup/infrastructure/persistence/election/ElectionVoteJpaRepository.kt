package com.nextup.infrastructure.persistence.election

import com.nextup.core.domain.election.ElectionVote
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

/**
 * ElectionVote JPA Repository
 */
interface ElectionVoteJpaRepository : JpaRepository<ElectionVote, Long> {
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
    @Query(
        """
        SELECT v.candidateId as candidateId, COUNT(v) as voteCount
        FROM ElectionVote v
        WHERE v.electionId = :electionId
        GROUP BY v.candidateId
    """,
    )
    fun countByElectionIdGroupByCandidateId(electionId: Long): List<VoteCountProjection>
}

/**
 * 투표 집계 결과 Projection
 */
interface VoteCountProjection {
    val candidateId: Long
    val voteCount: Long
}
