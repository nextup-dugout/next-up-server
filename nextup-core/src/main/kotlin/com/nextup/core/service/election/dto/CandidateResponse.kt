package com.nextup.core.service.election.dto

import com.nextup.core.domain.election.Candidate
import java.time.Instant

/**
 * 후보자 응답 DTO
 */
data class CandidateResponse(
    val id: Long,
    val electionId: Long,
    val memberId: Long,
    val memberName: String,
    val statement: String?,
    val createdAt: Instant,
)

/**
 * Candidate를 CandidateResponse로 변환합니다.
 */
fun Candidate.toResponse(): CandidateResponse =
    CandidateResponse(
        id = this.id,
        electionId = this.electionId,
        memberId = this.memberId,
        memberName = this.memberName,
        statement = this.statement,
        createdAt = this.createdAt,
    )

/**
 * Candidate 리스트를 CandidateResponse 리스트로 변환합니다.
 */
fun List<Candidate>.toResponse(): List<CandidateResponse> = this.map { it.toResponse() }
