package com.nextup.api.dto.team

import com.nextup.core.domain.team.TeamSchedule
import com.nextup.core.domain.team.TeamScheduleType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.time.LocalDateTime

/**
 * 팀 일정 생성 요청 DTO
 */
data class CreateTeamScheduleRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    val title: String,
    val description: String? = null,
    @field:NotNull(message = "일정 유형은 필수입니다")
    val scheduleType: TeamScheduleType,
    @field:NotNull(message = "시작 시간은 필수입니다")
    val startAt: LocalDateTime,
    val endAt: LocalDateTime? = null,
    val location: String? = null,
)

/**
 * 팀 일정 수정 요청 DTO
 */
data class UpdateTeamScheduleRequest(
    val title: String? = null,
    val description: String? = null,
    val scheduleType: TeamScheduleType? = null,
    val startAt: LocalDateTime? = null,
    val endAt: LocalDateTime? = null,
    val location: String? = null,
)

/**
 * 팀 일정 응답 DTO
 */
data class TeamScheduleResponse(
    val id: Long,
    val teamId: Long,
    val teamName: String,
    val title: String,
    val description: String?,
    val scheduleType: TeamScheduleType,
    val scheduleTypeDisplayName: String,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime?,
    val location: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(schedule: TeamSchedule): TeamScheduleResponse =
            TeamScheduleResponse(
                id = schedule.id,
                teamId = schedule.team.id,
                teamName = schedule.team.name,
                title = schedule.title,
                description = schedule.description,
                scheduleType = schedule.scheduleType,
                scheduleTypeDisplayName = schedule.scheduleType.displayName,
                startAt = schedule.startAt,
                endAt = schedule.endAt,
                location = schedule.location,
                createdAt = schedule.createdAt,
                updatedAt = schedule.updatedAt,
            )
    }
}
