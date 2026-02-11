package com.nextup.core.service.election.dto

/**
 * 후보자 등록 요청 DTO
 */
data class RegisterCandidateRequest(
    val electionId: Long,
    val memberId: Long,
    val memberName: String,
    val statement: String?,
)
