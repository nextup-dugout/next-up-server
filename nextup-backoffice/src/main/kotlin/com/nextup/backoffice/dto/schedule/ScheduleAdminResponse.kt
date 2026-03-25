package com.nextup.backoffice.dto.schedule

import com.nextup.core.domain.schedule.ScheduleStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * 대진표 관리자 응답 DTO
 *
 * backoffice 모듈에 독립적으로 존재
 * 변환 로직은 ScheduleExtensions.kt의 Extension Function을 사용합니다.
 */
data class ScheduleAdminResponse(
    val id: Long,
    val competitionId: Long,
    val round: Int,
    val matchNumber: Int,
    val homeTeamId: Long,
    val homeTeamName: String,
    val awayTeamId: Long,
    val awayTeamName: String,
    val scheduledDate: LocalDate,
    val scheduledTime: LocalTime?,
    val venue: String?,
    val status: ScheduleStatus,
    val gameId: Long?,
    val postponedReason: String?,
    val originalDate: LocalDate?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
