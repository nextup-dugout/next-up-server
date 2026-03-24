package com.nextup.core.port.repository

import com.nextup.core.domain.player.PlayerTeamHistory
import java.time.LocalDate

/**
 * PlayerTeamHistory Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface PlayerTeamHistoryRepositoryPort {
    fun save(playerTeamHistory: PlayerTeamHistory): PlayerTeamHistory

    fun findAll(): List<PlayerTeamHistory>

    fun findByIdOrNull(id: Long): PlayerTeamHistory?

    fun delete(playerTeamHistory: PlayerTeamHistory)

    fun deleteById(id: Long)

    fun findByPlayerId(playerId: Long): List<PlayerTeamHistory>

    fun findByTeamId(teamId: Long): List<PlayerTeamHistory>

    fun findCurrentByPlayerId(playerId: Long): PlayerTeamHistory?

    fun findCurrentByTeamId(teamId: Long): List<PlayerTeamHistory>

    fun findByPlayerIdWithDetails(playerId: Long): List<PlayerTeamHistory>

    fun findByTeamIdAtDate(
        teamId: Long,
        date: LocalDate,
    ): List<PlayerTeamHistory>

    // ===== 신규 메서드 (Issue #37) =====

    /**
     * 선수의 활성(ACTIVE 상태) 소속 이력을 조회합니다.
     *
     * @param playerId 선수 ID
     * @return 활성 상태의 소속 이력 목록
     */
    fun findActiveByPlayerId(playerId: Long): List<PlayerTeamHistory>

    /**
     * 선수의 특정 리그 내 활성 소속 이력을 조회합니다.
     *
     * @param playerId 선수 ID
     * @param leagueId 리그 ID
     * @return 해당 리그 내 활성 소속 이력 (없으면 null)
     */
    fun findActiveByPlayerIdAndLeagueId(
        playerId: Long,
        leagueId: Long,
    ): PlayerTeamHistory?

    /**
     * 선수가 특정 리그에 활성 소속되어 있는지 확인합니다.
     *
     * @param playerId 선수 ID
     * @param leagueId 리그 ID
     * @return 활성 소속 여부
     */
    fun existsActiveByPlayerIdAndLeagueId(
        playerId: Long,
        leagueId: Long,
    ): Boolean

    /**
     * 팀의 활성 선수 목록을 조회합니다.
     *
     * @param teamId 팀 ID
     * @return 활성 상태의 선수 소속 이력 목록
     */
    fun findActiveByTeamId(teamId: Long): List<PlayerTeamHistory>
}
