package com.nextup.infrastructure.repository.discipline

import com.nextup.core.domain.discipline.Discipline
import com.nextup.core.domain.discipline.DisciplineStatus
import com.nextup.core.port.repository.DisciplineRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface DisciplineRepository :
    JpaRepository<Discipline, Long>,
    DisciplineRepositoryPort {
    override fun findByPlayerId(playerId: Long): List<Discipline>

    override fun findByPlayerIdAndCompetitionId(
        playerId: Long,
        competitionId: Long,
    ): List<Discipline>

    override fun findByCompetitionId(competitionId: Long): List<Discipline>

    override fun findByStatus(status: DisciplineStatus): List<Discipline>

    @Query(
        """
        SELECT d FROM Discipline d
        WHERE d.player.id = :playerId
        AND d.status = 'ACTIVE'
        ORDER BY d.issuedAt DESC
        """
    )
    override fun findActiveByPlayerId(playerId: Long): List<Discipline>

    @Query(
        """
        SELECT d FROM Discipline d
        WHERE d.player.id = :playerId
        AND d.competition.id = :competitionId
        AND d.status = 'ACTIVE'
        ORDER BY d.issuedAt DESC
        """
    )
    override fun findActiveByPlayerIdAndCompetitionId(
        playerId: Long,
        competitionId: Long,
    ): List<Discipline>
}
