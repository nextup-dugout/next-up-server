package com.nextup.backoffice.dto.schedule

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalTime

/**
 * 대진표 생성 요청 DTO (관리자용)
 */
data class CreateScheduleRequest(
    @field:NotNull(message = "라운드는 필수입니다")
    @field:Positive(message = "라운드는 1 이상이어야 합니다")
    val round: Int,
    @field:NotNull(message = "경기 번호는 필수입니다")
    @field:Positive(message = "경기 번호는 1 이상이어야 합니다")
    val matchNumber: Int,
    @field:NotNull(message = "홈팀 ID는 필수입니다")
    @field:Positive(message = "홈팀 ID는 양수여야 합니다")
    val homeTeamId: Long,
    @field:NotNull(message = "원정팀 ID는 필수입니다")
    @field:Positive(message = "원정팀 ID는 양수여야 합니다")
    val awayTeamId: Long,
    @field:NotNull(message = "경기 날짜는 필수입니다")
    val scheduledDate: LocalDate,
    val scheduledTime: LocalTime? = null,
    @field:Size(max = 255, message = "경기장은 255자를 초과할 수 없습니다")
    val venue: String? = null,
)

/**
 * 대진표 수정 요청 DTO (관리자용)
 */
data class UpdateScheduleRequest(
    val scheduledDate: LocalDate? = null,
    val scheduledTime: LocalTime? = null,
    @field:Size(max = 255, message = "경기장은 255자를 초과할 수 없습니다")
    val venue: String? = null,
)
