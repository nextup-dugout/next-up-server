package com.nextup.infrastructure.repository.team

import com.nextup.core.domain.team.TeamSchedule
import com.nextup.core.port.repository.TeamScheduleRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface TeamScheduleRepository :
    JpaRepository<TeamSchedule, Long>,
    TeamScheduleRepositoryPort {
    override fun findByIdOrNull(id: Long): TeamSchedule? = findById(id).orElse(null)

    @Query("SELECT ts FROM TeamSchedule ts WHERE ts.team.id = :teamId ORDER BY ts.startAt")
    override fun findByTeamId(
        @Param("teamId") teamId: Long,
    ): List<TeamSchedule>

    @Query(
        "SELECT ts FROM TeamSchedule ts WHERE ts.team.id = :teamId " +
            "AND ts.startAt >= :from AND ts.startAt <= :to ORDER BY ts.startAt",
    )
    override fun findByTeamIdAndDateRange(
        @Param("teamId") teamId: Long,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
    ): List<TeamSchedule>
}
