package com.nextup.core.port.repository

import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.stats.SeasonPitchingStats

/**
 * SeasonPitchingStats Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface SeasonPitchingStatsRepositoryPort {
    fun save(seasonPitchingStats: SeasonPitchingStats): SeasonPitchingStats

    fun findAll(): List<SeasonPitchingStats>

    fun delete(seasonPitchingStats: SeasonPitchingStats)

    fun deleteById(id: Long)

    /**
     * 선수 ID와 연도로 시즌 투수 통계를 조회합니다.
     */
    fun findByPlayerIdAndYear(
        playerId: Long,
        year: Int,
    ): SeasonPitchingStats?

    /**
     * 선수 ID, 연도, 팀 ID로 시즌 투수 통계를 조회합니다 (이적 시 팀별 기록 분리).
     */
    fun findByPlayerIdAndYearAndTeamId(
        playerId: Long,
        year: Int,
        teamId: Long?,
    ): SeasonPitchingStats?

    /**
     * 선수 ID, 연도, 팀 ID, 대회 유형으로 시즌 투수 통계를 조회합니다.
     */
    fun findByPlayerIdAndYearAndTeamIdAndCompetitionType(
        playerId: Long,
        year: Int,
        teamId: Long?,
        competitionType: CompetitionType,
    ): SeasonPitchingStats?

    /**
     * 선수 ID와 연도로 모든 팀별 시즌 투수 통계를 조회합니다 (이적 시 팀별 기록).
     */
    fun findAllByPlayerIdAndYear(
        playerId: Long,
        year: Int,
    ): List<SeasonPitchingStats>

    /**
     * 선수 ID로 모든 시즌 투수 통계를 조회합니다.
     */
    fun findAllByPlayerId(playerId: Long): List<SeasonPitchingStats>

    /**
     * 특정 연도의 모든 시즌 투수 통계를 조회합니다.
     */
    fun findAllByYear(year: Int): List<SeasonPitchingStats>

    /**
     * ERA 상위 N명을 조회합니다 (최소 이닝 조건).
     * ERA는 낮을수록 좋으므로 ASC 정렬합니다.
     */
    fun findTopByEra(
        year: Int,
        minInningsPitchedOuts: Int,
        limit: Int,
    ): List<SeasonPitchingStats>

    /**
     * 승수 상위 N명을 조회합니다.
     */
    fun findTopByWins(
        year: Int,
        limit: Int,
    ): List<SeasonPitchingStats>

    /**
     * 삼진 상위 N명을 조회합니다.
     */
    fun findTopByStrikeouts(
        year: Int,
        limit: Int,
    ): List<SeasonPitchingStats>

    /**
     * 세이브 상위 N명을 조회합니다.
     */
    fun findTopBySaves(
        year: Int,
        limit: Int,
    ): List<SeasonPitchingStats>

    /**
     * WHIP 상위 N명을 조회합니다 (최소 이닝 조건).
     * WHIP는 낮을수록 좋으므로 ASC 정렬합니다.
     */
    fun findTopByWhip(
        year: Int,
        minInningsPitchedOuts: Int,
        limit: Int,
    ): List<SeasonPitchingStats>

    /**
     * 특정 경기에 등판하여 투구 기록이 있는 선수들의 시즌 투구 통계를 조회합니다.
     * 경기 취소 시 스탯 롤백에 사용됩니다.
     */
    fun findAllByGameId(gameId: Long): List<SeasonPitchingStats>
}
