package com.nextup.core.port.repository

import com.nextup.core.domain.game.GameTeam

/**
 * GameTeam Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface GameTeamRepositoryPort {
    fun save(gameTeam: GameTeam): GameTeam

    fun findByIdOrNull(id: Long): GameTeam?

    fun findAll(): List<GameTeam>

    fun delete(gameTeam: GameTeam)

    fun deleteById(id: Long)

    /**
     * 팀 ID로 모든 GameTeam을 조회합니다.
     */
    fun findAllByTeamId(teamId: Long): List<GameTeam>

    /**
     * 팀 ID와 연도로 GameTeam을 조회합니다.
     */
    fun findAllByTeamIdAndYear(
        teamId: Long,
        year: Int,
    ): List<GameTeam>

    /**
     * 팀 ID와 대회 ID로 GameTeam을 조회합니다.
     */
    fun findAllByTeamIdAndCompetitionId(
        teamId: Long,
        competitionId: Long,
    ): List<GameTeam>

    /**
     * 경기 ID로 GameTeam을 조회합니다.
     */
    fun findAllByGameId(gameId: Long): List<GameTeam>

    /**
     * 여러 경기 ID로 GameTeam을 한 번에 조회합니다. (N+1 방지용 배치 쿼리)
     */
    fun findAllByGameIds(gameIds: List<Long>): List<GameTeam>
}
