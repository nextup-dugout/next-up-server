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

    /**
     * 대회 ID로 모든 GameTeam을 조회합니다. (결과 확정된 경기만)
     */
    fun findAllByCompetitionIdWithDecidedResult(competitionId: Long): List<GameTeam>

    /**
     * 대회 ID로 모든 GameTeam을 조회합니다. (전체 - 남은 경기 계산용)
     */
    fun findAllByCompetitionId(competitionId: Long): List<GameTeam>

    /**
     * 두 팀 간의 완료된 경기 목록을 조회합니다. (Game과 상대 GameTeam 포함)
     *
     * @param teamId 조회 대상 팀 ID
     * @param opponentId 상대 팀 ID
     * @param competitionId 대회 ID 필터 (선택사항)
     * @return 완료된 경기의 GameTeam 목록 (teamId에 해당하는 GameTeam만 반환)
     */
    fun findCompletedGamesBetweenTeams(
        teamId: Long,
        opponentId: Long,
        competitionId: Long? = null,
    ): List<GameTeam>
}
