package com.nextup.core.port.repository

import com.nextup.core.domain.discipline.Discipline
import com.nextup.core.domain.discipline.DisciplineStatus

/**
 * Discipline Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface DisciplineRepositoryPort {
    fun save(discipline: Discipline): Discipline

    fun findAll(): List<Discipline>

    fun findByIdOrNull(id: Long): Discipline?

    fun delete(discipline: Discipline)

    fun deleteById(id: Long)

    fun existsById(id: Long): Boolean

    fun findByPlayerId(playerId: Long): List<Discipline>

    fun findByPlayerIdAndCompetitionId(
        playerId: Long,
        competitionId: Long,
    ): List<Discipline>

    fun findByCompetitionId(competitionId: Long): List<Discipline>

    fun findByStatus(status: DisciplineStatus): List<Discipline>

    fun findActiveByPlayerId(playerId: Long): List<Discipline>

    fun findActiveByPlayerIdAndCompetitionId(
        playerId: Long,
        competitionId: Long,
    ): List<Discipline>
}
