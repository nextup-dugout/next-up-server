package com.nextup.api.dto.schedule

import com.nextup.core.domain.schedule.LeagueSchedule
import com.nextup.core.domain.schedule.ScheduleStatus
import java.time.LocalDate
import java.time.LocalTime

/**
 * 대진표 응답 DTO (API - 조회 전용)
 *
 * 일반 사용자용 대진표 정보
 */
data class ScheduleResponse(
    val id: Long,
    val competitionId: Long,
    val round: Int,
    val matchNumber: Int,
    val homeTeam: ScheduleTeamResponse,
    val awayTeam: ScheduleTeamResponse,
    val scheduledDate: LocalDate,
    val scheduledTime: LocalTime?,
    val venue: String?,
    val status: ScheduleStatus,
    val gameId: Long?,
) {
    companion object {
        fun from(schedule: LeagueSchedule): ScheduleResponse =
            ScheduleResponse(
                id = schedule.id,
                competitionId = schedule.competition.id,
                round = schedule.round,
                matchNumber = schedule.matchNumber,
                homeTeam =
                    ScheduleTeamResponse(
                        id = schedule.homeTeam.id,
                        name = schedule.homeTeam.name,
                        abbreviation = schedule.homeTeam.abbreviation,
                    ),
                awayTeam =
                    ScheduleTeamResponse(
                        id = schedule.awayTeam.id,
                        name = schedule.awayTeam.name,
                        abbreviation = schedule.awayTeam.abbreviation,
                    ),
                scheduledDate = schedule.scheduledDate,
                scheduledTime = schedule.scheduledTime,
                venue = schedule.venue,
                status = schedule.status,
                gameId = schedule.game?.id,
            )
    }
}

/**
 * 대진표 내 팀 정보
 */
data class ScheduleTeamResponse(
    val id: Long,
    val name: String,
    val abbreviation: String?,
)
