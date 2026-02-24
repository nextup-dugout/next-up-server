package com.nextup.core.port.repository

import com.nextup.core.domain.competition.CompetitionPlayer
import com.nextup.core.domain.competition.CompetitionPlayerStatus

/**
 * 대회 등록 선수 저장소 포트
 */
interface CompetitionPlayerRepositoryPort {
    fun save(competitionPlayer: CompetitionPlayer): CompetitionPlayer

    fun saveAll(players: List<CompetitionPlayer>): List<CompetitionPlayer>

    fun findByIdOrNull(id: Long): CompetitionPlayer?

    fun findByCompetitionId(competitionId: Long): List<CompetitionPlayer>

    fun findByCompetitionIdAndTeamId(
        competitionId: Long,
        teamId: Long,
    ): List<CompetitionPlayer>

    fun findByCompetitionIdAndStatus(
        competitionId: Long,
        status: CompetitionPlayerStatus,
    ): List<CompetitionPlayer>

    /**
     * 대회에 등록된 선수 ID 목록을 조회합니다. (부정선수 체크용)
     */
    fun findPlayerIdsByCompetitionId(competitionId: Long): Set<Long>

    /**
     * 대회에 등록된 활성 선수 ID 목록을 조회합니다. (라인업 검증용)
     */
    fun findEligiblePlayerIdsByCompetitionId(competitionId: Long): Set<Long>

    fun findByCompetitionIdAndPlayerId(
        competitionId: Long,
        playerId: Long,
    ): CompetitionPlayer?

    fun existsByCompetitionIdAndPlayerId(
        competitionId: Long,
        playerId: Long,
    ): Boolean

    fun deleteById(id: Long)
}
