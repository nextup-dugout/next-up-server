package com.nextup.infrastructure.repository.game

import com.nextup.core.domain.game.Game
import com.nextup.core.port.repository.GameRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface GameRepository :
    JpaRepository<Game, Long>,
    GameRepositoryPort {
    override fun findByIdOrNull(id: Long): Game? = findById(id).orElse(null)

    @Query("SELECT g FROM Game g WHERE g.id IN :ids")
    override fun findAllByIds(
        @Param("ids") ids: List<Long>,
    ): List<Game>

    @Query("SELECT g FROM Game g WHERE g.competition.id = :competitionId ORDER BY g.scheduledAt ASC")
    override fun findByCompetitionId(
        @Param("competitionId") competitionId: Long,
    ): List<Game>

    @Query("SELECT g FROM Game g WHERE g.scheduledAt BETWEEN :start AND :end ORDER BY g.scheduledAt ASC")
    override fun findByScheduledAtBetween(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
    ): List<Game>

    @Query(
        "SELECT g FROM Game g " +
            "LEFT JOIN FETCH g.competition " +
            "WHERE g.id = :id",
    )
    override fun findByIdWithTeams(
        @Param("id") id: Long,
    ): Game?

    @Query("SELECT COUNT(g) FROM Game g WHERE g.competition.id = :competitionId")
    override fun countByCompetitionId(
        @Param("competitionId") competitionId: Long,
    ): Long

    @Query(
        "SELECT COUNT(g) FROM Game g " +
            "WHERE g.competition.id = :competitionId " +
            "AND g.status IN ('FINISHED', 'CALLED', 'FORFEITED', 'CANCELLED')",
    )
    override fun countCompletedOrCancelledByCompetitionId(
        @Param("competitionId") competitionId: Long,
    ): Long

    @Query(
        "SELECT DISTINCT FUNCTION('DAY', g.scheduledAt) FROM Game g " +
            "JOIN GameTeam gt ON gt.game.id = g.id " +
            "WHERE FUNCTION('YEAR', g.scheduledAt) = :year " +
            "AND FUNCTION('MONTH', g.scheduledAt) = :month " +
            "AND (:teamId IS NULL OR gt.team.id = :teamId) " +
            "ORDER BY FUNCTION('DAY', g.scheduledAt) ASC",
    )
    fun findGameDaysByYearAndMonth(
        @Param("year") year: Int,
        @Param("month") month: Int,
        @Param("teamId") teamId: Long?,
    ): List<Int>

    override fun findGameDaysInMonth(
        year: Int,
        month: Int,
        teamId: Long?,
    ): List<Int> = findGameDaysByYearAndMonth(year, month, teamId)
}
