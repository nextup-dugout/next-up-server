package com.nextup.core.port.repository

import com.nextup.core.domain.stats.SeasonFieldingStats

/**
 * SeasonFieldingStats Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface SeasonFieldingStatsRepositoryPort {
    fun save(stats: SeasonFieldingStats): SeasonFieldingStats

    /**
     * 선수 ID와 연도로 시즌 수비 통계를 조회합니다.
     */
    fun findByPlayerIdAndYear(
        playerId: Long,
        year: Int,
    ): SeasonFieldingStats?

    /**
     * 선수 ID로 모든 시즌 수비 통계를 조회합니다.
     */
    fun findAllByPlayerId(playerId: Long): List<SeasonFieldingStats>

    /**
     * 특정 연도의 모든 시즌 수비 통계를 조회합니다.
     */
    fun findAllByYear(year: Int): List<SeasonFieldingStats>

    /**
     * 경기 ID로 해당 경기에 참여한 선수들의 시즌 수비 통계를 조회합니다.
     * 경기 취소 시 롤백용으로 사용합니다.
     */
    fun findAllByGameId(gameId: Long): List<SeasonFieldingStats>
}
