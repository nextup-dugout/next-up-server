package com.nextup.core.service.election.dto

/**
 * 선거 결과 응답 DTO
 */
data class ElectionResultResponse(
    val election: ElectionResponse,
    val candidates: List<CandidateResultResponse>,
    val totalVotes: Long,
)

/**
 * 후보자 결과 응답 DTO
 */
data class CandidateResultResponse(
    val candidate: CandidateResponse,
    val voteCount: Long,
)
