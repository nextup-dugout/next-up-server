package com.nextup.api.dto.election

import com.nextup.core.domain.election.ElectionType
import com.nextup.core.service.election.dto.CreateElectionRequest
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant

/**
 * 선거 생성 API 요청 DTO
 */
data class CreateElectionApiRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    val title: String,
    val description: String?,
    @field:NotNull(message = "선거 유형은 필수입니다")
    val electionType: ElectionType,
    @field:NotNull(message = "시작 시간은 필수입니다")
    @field:Future(message = "시작 시간은 미래여야 합니다")
    val startAt: Instant,
    @field:NotNull(message = "종료 시간은 필수입니다")
    @field:Future(message = "종료 시간은 미래여야 합니다")
    val endAt: Instant,
)

/**
 * API 요청을 Service 요청으로 변환합니다.
 */
fun CreateElectionApiRequest.toServiceRequest(teamId: Long): CreateElectionRequest =
    CreateElectionRequest(
        teamId = teamId,
        title = this.title,
        description = this.description,
        electionType = this.electionType,
        startAt = this.startAt,
        endAt = this.endAt,
    )
