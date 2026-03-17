package com.nextup.core.port.repository

import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.GamePlayer

/**
 * BattingRecord Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface BattingRecordRepositoryPort {
    fun save(battingRecord: BattingRecord): BattingRecord

    fun findAll(): List<BattingRecord>

    fun findByIdOrNull(id: Long): BattingRecord?

    fun delete(battingRecord: BattingRecord)

    fun deleteById(id: Long)

    /**
     * GamePlayer로 타격 기록을 조회합니다.
     */
    fun findByGamePlayer(gamePlayer: GamePlayer): BattingRecord?

    /**
     * GamePlayer ID로 타격 기록을 조회합니다.
     */
    fun findByGamePlayerId(gamePlayerId: Long): BattingRecord?

    /**
     * 경기 ID로 모든 타격 기록을 조회합니다.
     */
    fun findAllByGameId(gameId: Long): List<BattingRecord>

    /**
     * 선수 ID로 모든 타격 기록을 조회합니다.
     */
    fun findAllByPlayerId(playerId: Long): List<BattingRecord>

    /**
     * 선수의 최근 N경기 타격 기록을 조회합니다.
     */
    fun findRecentByPlayerId(
        playerId: Long,
        limit: Int,
    ): List<BattingRecord>

    /**
     * 팀의 특정 경기 타격 기록을 조회합니다.
     */
    fun findAllByTeamIdAndGameId(
        teamId: Long,
        gameId: Long,
    ): List<BattingRecord>

    /**
     * 경기에서 최소 타석 수 이상인 타격 기록을 조회합니다.
     */
    fun findByGameIdAndMinPlateAppearances(
        gameId: Long,
        minPlateAppearances: Int,
    ): List<BattingRecord>

    /**
     * 타율 상위 N명을 조회합니다 (최소 타수 조건).
     */
    fun findTopByBattingAverage(
        minAtBats: Int,
        limit: Int,
    ): List<BattingRecord>

    /**
     * 홈런 상위 N명을 조회합니다.
     */
    fun findTopByHomeRuns(limit: Int): List<BattingRecord>

    /**
     * 타점 상위 N명을 조회합니다.
     */
    fun findTopByRunsBattedIn(limit: Int): List<BattingRecord>

    /**
     * 선수의 특정 연도 타격 기록을 조회합니다.
     */
    fun findAllByPlayerIdAndYear(
        playerId: Long,
        year: Int,
    ): List<BattingRecord>

    /**
     * 팀의 여러 경기 타격 기록을 한 번에 조회합니다. (N+1 방지용 배치 쿼리)
     */
    fun findAllByTeamIdAndGameIds(
        teamId: Long,
        gameIds: List<Long>,
    ): List<BattingRecord>

    /**
     * 선수 ID로 모든 타격 기록을 조회합니다. (FETCH JOIN으로 N+1 방지)
     * gamePlayer, gameTeam, game을 함께 로딩하여 커리어 통계 계산에 사용합니다.
     */
    fun findAllByPlayerIdWithGameInfo(playerId: Long): List<BattingRecord>

    /**
     * 선수 ID와 대회 ID로 타격 기록을 조회합니다.
     * 대회별 스탯 필터에 사용됩니다.
     */
    fun findAllByPlayerIdAndCompetitionId(
        playerId: Long,
        competitionId: Long,
    ): List<BattingRecord>
}
