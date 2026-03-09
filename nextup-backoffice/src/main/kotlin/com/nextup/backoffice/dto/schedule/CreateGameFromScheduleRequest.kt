package com.nextup.backoffice.dto.schedule

import jakarta.validation.constraints.Size

/**
 * 대진표에서 경기 생성 요청 DTO
 *
 * 대진표의 기본 정보(대회, 홈팀, 원정팀, 일정)를 사용하되,
 * 장소와 구장명은 선택적으로 재지정할 수 있습니다.
 */
data class CreateGameFromScheduleRequest(
    @field:Size(max = 255)
    val location: String? = null,
    @field:Size(max = 255)
    val fieldName: String? = null,
)
