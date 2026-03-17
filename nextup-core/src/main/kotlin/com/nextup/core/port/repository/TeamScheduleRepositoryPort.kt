package com.nextup.core.port.repository

import com.nextup.core.domain.team.TeamSchedule
import java.time.LocalDateTime

/**
 * TeamSchedule Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface TeamScheduleRepositoryPort {
    fun save(teamSchedule: TeamSchedule): TeamSchedule

    fun findByIdOrNull(id: Long): TeamSchedule?

    fun findByTeamId(teamId: Long): List<TeamSchedule>

    fun findByTeamIdAndDateRange(
        teamId: Long,
        from: LocalDateTime,
        to: LocalDateTime,
    ): List<TeamSchedule>

    fun deleteById(id: Long)
}
