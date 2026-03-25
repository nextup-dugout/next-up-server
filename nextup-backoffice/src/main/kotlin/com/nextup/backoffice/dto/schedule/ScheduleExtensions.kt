package com.nextup.backoffice.dto.schedule

import com.nextup.core.domain.schedule.LeagueSchedule
import com.nextup.core.domain.schedule.ScheduleConflict

/**
 * LeagueSchedule Entity를 ScheduleAdminResponse DTO로 변환하는 Extension Function
 */
fun LeagueSchedule.toAdminResponse(): ScheduleAdminResponse =
    ScheduleAdminResponse(
        id = this.id,
        competitionId = this.competition.id,
        round = this.round,
        matchNumber = this.matchNumber,
        homeTeamId = this.homeTeam.id,
        homeTeamName = this.homeTeam.name,
        awayTeamId = this.awayTeam.id,
        awayTeamName = this.awayTeam.name,
        scheduledDate = this.scheduledDate,
        scheduledTime = this.scheduledTime,
        venue = this.venue,
        status = this.status,
        gameId = this.game?.id,
        postponedReason = this.postponedReason,
        originalDate = this.originalDate,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )

/**
 * ScheduleConflict를 ScheduleConflictResponse DTO로 변환하는 Extension Function
 */
fun ScheduleConflict.toResponse(): ScheduleConflictResponse =
    ScheduleConflictResponse(
        type = this.type,
        conflictingScheduleId = this.conflictingScheduleId,
        description = this.description,
    )
