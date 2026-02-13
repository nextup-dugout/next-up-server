package com.nextup.core.port.repository

import com.nextup.core.domain.election.Candidate

/**
 * Candidate Repository Port
 */
interface CandidateRepositoryPort {
    /**
     * Candidate를 저장합니다.
     */
    fun save(candidate: Candidate): Candidate

    /**
     * ID로 Candidate를 조회합니다.
     */
    fun findById(id: Long): Candidate?

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

    /**
     * Candidate를 삭제합니다.
     */
    fun delete(candidate: Candidate)
}
