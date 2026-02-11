package com.nextup.api.dto.election

import com.nextup.core.service.election.dto.RegisterCandidateRequest
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

/**
 * 후보자 등록 API 요청 DTO
 */
data class RegisterCandidateApiRequest(
    @field:NotNull(message = "회원 ID는 필수입니다")
    val memberId: Long,
    @field:NotBlank(message = "회원 이름은 필수입니다")
    val memberName: String,
    val statement: String?,
)

/**
 * API 요청을 Service 요청으로 변환합니다.
 */
fun RegisterCandidateApiRequest.toServiceRequest(electionId: Long): RegisterCandidateRequest =
    RegisterCandidateRequest(
        electionId = electionId,
        memberId = this.memberId,
        memberName = this.memberName,
        statement = this.statement,
    )
