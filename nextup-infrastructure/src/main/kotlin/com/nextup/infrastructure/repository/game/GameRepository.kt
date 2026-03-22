package com.nextup.infrastructure.repository.game

import com.nextup.core.common.PageCommand
import com.nextup.core.common.PageResult
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.port.repository.GameRepositoryPort
import jakarta.persistence.LockModeType
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

interface GameRepository :
    JpaRepository<Game, Long>,
    GameRepositoryPort {
    override fun findByIdOrNull(id: Long): Game? = findById(id).orElse(null)

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM Game g WHERE g.id = :id")
    fun findByIdWithPessimisticLock(
        @Param("id") id: Long,
    ): Game?

    override fun findByIdForUpdate(id: Long): Game? = findByIdWithPessimisticLock(id)

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

    @Query(
        "SELECT g FROM Game g " +
            "WHERE g.scorerId IS NOT NULL " +
            "AND g.lockedAt IS NOT NULL " +
            "AND g.lockedAt < :threshold",
    )
    override fun findLockedGamesBefore(
        @Param("threshold") threshold: LocalDateTime,
    ): List<Game>

    // ---- 페이징용 내부 쿼리 메서드 (Spring Data Pageable은 Infra 레이어에서만 사용) ----

    @Query(
        "SELECT g FROM Game g " +
            "WHERE (:competitionId IS NULL OR g.competition.id = :competitionId) " +
            "AND (:startOfDay IS NULL OR g.scheduledAt >= :startOfDay) " +
            "AND (:endOfDay IS NULL OR g.scheduledAt <= :endOfDay) " +
            "AND (:status IS NULL OR g.status = :status)",
    )
    fun findByFilters(
        @Param("competitionId") competitionId: Long?,
        @Param("startOfDay") startOfDay: LocalDateTime?,
        @Param("endOfDay") endOfDay: LocalDateTime?,
        @Param("status") status: GameStatus?,
        pageable: org.springframework.data.domain.Pageable,
    ): org.springframework.data.domain.Page<Game>

    @Query(
        "SELECT g FROM Game g " +
            "JOIN GameTeam gt ON gt.game.id = g.id " +
            "WHERE gt.team.id IN :teamIds " +
            "AND (:competitionId IS NULL OR g.competition.id = :competitionId) " +
            "AND (:startOfDay IS NULL OR g.scheduledAt >= :startOfDay) " +
            "AND (:endOfDay IS NULL OR g.scheduledAt <= :endOfDay) " +
            "AND (:status IS NULL OR g.status = :status)",
    )
    fun findByFiltersAndTeamIds(
        @Param("teamIds") teamIds: List<Long>,
        @Param("competitionId") competitionId: Long?,
        @Param("startOfDay") startOfDay: LocalDateTime?,
        @Param("endOfDay") endOfDay: LocalDateTime?,
        @Param("status") status: GameStatus?,
        pageable: org.springframework.data.domain.Pageable,
    ): org.springframework.data.domain.Page<Game>

    override fun findGames(
        date: LocalDate?,
        teamId: Long?,
        competitionId: Long?,
        status: GameStatus?,
        pageCommand: PageCommand,
    ): PageResult<Game> {
        val pageable =
            PageRequest.of(
                pageCommand.page,
                pageCommand.size,
                Sort.by(Sort.Direction.ASC, "scheduledAt"),
            )
        val startOfDay = date?.atStartOfDay()
        val endOfDay = date?.atTime(LocalTime.MAX)

        val page =
            if (teamId != null) {
                findByFiltersAndTeamIds(
                    teamIds = listOf(teamId),
                    competitionId = competitionId,
                    startOfDay = startOfDay,
                    endOfDay = endOfDay,
                    status = status,
                    pageable = pageable,
                )
            } else {
                findByFilters(
                    competitionId = competitionId,
                    startOfDay = startOfDay,
                    endOfDay = endOfDay,
                    status = status,
                    pageable = pageable,
                )
            }

        return PageResult(
            content = page.content,
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }
}
