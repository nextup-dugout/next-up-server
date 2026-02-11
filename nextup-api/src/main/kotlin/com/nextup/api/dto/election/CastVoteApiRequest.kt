package com.nextup.api.dto.election

import com.nextup.core.service.election.dto.CastVoteRequest
import jakarta.validation.constraints.NotNull

/**
 * 투표 API 요청 DTO
 */
data class CastVoteApiRequest(
    @field:NotNull(message = "투표자 ID는 필수입니다")
    val voterId: Long,
    @field:NotNull(message = "후보자 ID는 필수입니다")
    val candidateId: Long,
)

/**
 * API 요청을 Service 요청으로 변환합니다.
 */
fun CastVoteApiRequest.toServiceRequest(electionId: Long): CastVoteRequest =
    CastVoteRequest(
        electionId = electionId,
        voterId = this.voterId,
        candidateId = this.candidateId,
    )
