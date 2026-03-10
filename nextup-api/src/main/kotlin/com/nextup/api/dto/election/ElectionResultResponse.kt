package com.nextup.api.dto.election

import com.nextup.core.service.election.dto.ElectionResult

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

/**
 * ElectionResult를 ElectionResultResponse로 변환합니다.
 */
fun ElectionResult.toResponse(): ElectionResultResponse =
    ElectionResultResponse(
        election = this.election.toResponse(),
        candidates =
            this.candidateResults.map { candidateResult ->
                CandidateResultResponse(
                    candidate = candidateResult.candidate.toResponse(),
                    voteCount = candidateResult.voteCount,
                )
            },
        totalVotes = this.totalVotes,
    )
