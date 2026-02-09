package com.nextup.infrastructure.repository

import com.nextup.core.domain.schedule.LeagueSchedule
import com.nextup.core.domain.schedule.ScheduleStatus
import com.nextup.core.port.repository.LeagueScheduleRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface LeagueScheduleRepository :
    JpaRepository<LeagueSchedule, Long>,
    LeagueScheduleRepositoryPort {
    override fun findByIdOrNull(id: Long): LeagueSchedule? = findById(id).orElse(null)

    override fun findByCompetitionId(competitionId: Long): List<LeagueSchedule>

    override fun findByCompetitionIdAndRound(
        competitionId: Long,
        round: Int
    ): List<LeagueSchedule>

    override fun findByCompetitionIdAndStatus(
        competitionId: Long,
        status: ScheduleStatus
    ): List<LeagueSchedule>

    @Query(
        "SELECT s FROM LeagueSchedule s WHERE s.scheduledDate BETWEEN :startDate AND :endDate ORDER BY s.scheduledDate",
    )
    override fun findByScheduledDateBetween(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<LeagueSchedule>

    override fun existsByCompetitionIdAndRoundAndMatchNumber(
        competitionId: Long,
        round: Int,
        matchNumber: Int,
    ): Boolean
}
