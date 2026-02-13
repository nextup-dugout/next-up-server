package com.nextup.infrastructure.persistence.election

import com.nextup.core.domain.election.Candidate
import org.springframework.data.jpa.repository.JpaRepository

/**
 * Candidate JPA Repository
 */
interface CandidateJpaRepository : JpaRepository<Candidate, Long> {
    /**
     * 선거 ID로 모든 Candidate를 조회합니다.
     */
    fun findAllByElectionId(electionId: Long): List<Candidate>

    /**
     * 선거 ID와 회원 ID로 Candidate를 조회합니다.
     */
    fun findByElectionIdAndMemberId(
        electionId: Long,
        memberId: Long,
    ): Candidate?
}
