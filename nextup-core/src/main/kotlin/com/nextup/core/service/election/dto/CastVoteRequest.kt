package com.nextup.core.service.election.dto

/**
 * 투표 요청 DTO
 */
data class CastVoteRequest(
    val electionId: Long,
    val voterId: Long,
    val candidateId: Long,
)
