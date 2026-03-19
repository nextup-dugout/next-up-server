package com.nextup.core.port.repository

import com.nextup.core.domain.stats.SeasonBattingStats

/**
 * SeasonBattingStats Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface SeasonBattingStatsRepositoryPort {
    fun save(seasonBattingStats: SeasonBattingStats): SeasonBattingStats

    fun findAll(): List<SeasonBattingStats>

    fun delete(seasonBattingStats: SeasonBattingStats)

    fun deleteById(id: Long)

    /**
     * 선수 ID와 연도로 시즌 타격 통계를 조회합니다.
     */
    fun findByPlayerIdAndYear(
        playerId: Long,
        year: Int,
    ): SeasonBattingStats?

    /**
     * 선수 ID, 연도, 팀 ID로 시즌 타격 통계를 조회합니다 (이적 시 팀별 기록 분리).
     */
    fun findByPlayerIdAndYearAndTeamId(
        playerId: Long,
        year: Int,
        teamId: Long?,
    ): SeasonBattingStats?

    /**
     * 선수 ID로 모든 시즌 타격 통계를 조회합니다.
     */
    fun findAllByPlayerId(playerId: Long): List<SeasonBattingStats>

    /**
     * 특정 연도의 모든 시즌 타격 통계를 조회합니다.
     */
    fun findAllByYear(year: Int): List<SeasonBattingStats>

    /**
     * 타율 상위 N명을 조회합니다 (최소 타수 조건).
     */
    fun findTopByBattingAverage(
        year: Int,
        minAtBats: Int,
        limit: Int,
    ): List<SeasonBattingStats>

    /**
     * 홈런 상위 N명을 조회합니다.
     */
    fun findTopByHomeRuns(
        year: Int,
        limit: Int,
    ): List<SeasonBattingStats>

    /**
     * 타점 상위 N명을 조회합니다.
     */
    fun findTopByRunsBattedIn(
        year: Int,
        limit: Int,
    ): List<SeasonBattingStats>

    /**
     * OPS 상위 N명을 조회합니다 (최소 타석 조건).
     */
    fun findTopByOps(
        year: Int,
        minPlateAppearances: Int,
        limit: Int,
    ): List<SeasonBattingStats>

    fun findTopByHits(
        year: Int,
        limit: Int,
    ): List<SeasonBattingStats>

    fun findTopByStolenBases(
        year: Int,
        limit: Int,
    ): List<SeasonBattingStats>

    fun findTopByOnBasePercentage(
        year: Int,
        minPlateAppearances: Int,
        limit: Int,
    ): List<SeasonBattingStats>

    fun findTopBySlugging(
        year: Int,
        minPlateAppearances: Int,
        limit: Int,
    ): List<SeasonBattingStats>

    /**
     * 특정 경기에 출전하여 타격 기록이 있는 선수들의 시즌 타격 통계를 조회합니다.
     * 경기 취소 시 스탯 롤백에 사용됩니다.
     */
    fun findAllByGameId(gameId: Long): List<SeasonBattingStats>
}
