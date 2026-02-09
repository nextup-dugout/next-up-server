package com.nextup.backoffice.dto.schedule

import com.nextup.core.domain.schedule.LeagueSchedule
import com.nextup.core.domain.schedule.ScheduleStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * 대진표 관리자 응답 DTO
 *
 * backoffice 모듈에 독립적으로 존재
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
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(schedule: LeagueSchedule): ScheduleAdminResponse =
            ScheduleAdminResponse(
                id = schedule.id,
                competitionId = schedule.competition.id,
                round = schedule.round,
                matchNumber = schedule.matchNumber,
                homeTeamId = schedule.homeTeam.id,
                homeTeamName = schedule.homeTeam.name,
                awayTeamId = schedule.awayTeam.id,
                awayTeamName = schedule.awayTeam.name,
                scheduledDate = schedule.scheduledDate,
                scheduledTime = schedule.scheduledTime,
                venue = schedule.venue,
                status = schedule.status,
                gameId = schedule.game?.id,
                createdAt = schedule.createdAt,
                updatedAt = schedule.updatedAt,
            )
    }
}
