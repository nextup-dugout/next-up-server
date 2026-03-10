package com.nextup.core.service.election.dto

import com.nextup.core.domain.election.Candidate
import com.nextup.core.domain.election.Election

/**
 * 선거 결과 (Service → Controller 전달용)
 */
data class ElectionResult(
    val election: Election,
    val candidateResults: List<CandidateResult>,
    val totalVotes: Long,
)

/**
 * 후보자별 결과 (Service → Controller 전달용)
 */
data class CandidateResult(
    val candidate: Candidate,
    val voteCount: Long,
)
