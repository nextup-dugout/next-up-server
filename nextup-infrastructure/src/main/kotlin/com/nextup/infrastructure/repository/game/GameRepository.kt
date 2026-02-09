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

    override fun findAllByIds(ids: List<Long>): List<Game> = findAllById(ids)

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
}
