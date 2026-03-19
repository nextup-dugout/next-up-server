package com.nextup.core.port.repository

import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.PitchingDecision
import com.nextup.core.domain.game.PitchingRecord

/**
 * PitchingRecord Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface PitchingRecordRepositoryPort {
    fun save(pitchingRecord: PitchingRecord): PitchingRecord

    fun findAll(): List<PitchingRecord>

    fun findByIdOrNull(id: Long): PitchingRecord?

    fun delete(pitchingRecord: PitchingRecord)

    fun deleteById(id: Long)

    /**
     * GamePlayer로 투수 기록을 조회합니다.
     */
    fun findByGamePlayer(gamePlayer: GamePlayer): PitchingRecord?

    /**
     * GamePlayer ID로 투수 기록을 조회합니다.
     */
    fun findByGamePlayerId(gamePlayerId: Long): PitchingRecord?

    /**
     * 경기 ID로 모든 투수 기록을 조회합니다.
     */
    fun findAllByGameId(gameId: Long): List<PitchingRecord>

    /**
     * 선수 ID로 모든 투수 기록을 조회합니다.
     */
    fun findAllByPlayerId(playerId: Long): List<PitchingRecord>

    /**
     * 선수의 최근 N경기 투수 기록을 조회합니다.
     */
    fun findRecentByPlayerId(
        playerId: Long,
        limit: Int,
    ): List<PitchingRecord>

    /**
     * 팀의 특정 경기 투수 기록을 조회합니다.
     */
    fun findAllByTeamIdAndGameId(
        teamId: Long,
        gameId: Long,
    ): List<PitchingRecord>

    /**
     * 경기의 선발 투수 기록을 조회합니다.
     */
    fun findStartingPitchersByGameId(gameId: Long): List<PitchingRecord>

    /**
     * 경기의 구원 투수 기록을 조회합니다.
     */
    fun findReliefPitchersByGameId(gameId: Long): List<PitchingRecord>

    /**
     * 특정 결정(WIN/LOSS/SAVE/HOLD)을 받은 투수 기록을 조회합니다.
     */
    fun findAllByDecision(decision: PitchingDecision): List<PitchingRecord>

    /**
     * 경기의 승리 투수를 조회합니다.
     */
    fun findWinningPitcherByGameId(gameId: Long): PitchingRecord?

    /**
     * 경기의 패전 투수를 조회합니다.
     */
    fun findLosingPitcherByGameId(gameId: Long): PitchingRecord?

    /**
     * 경기의 세이브 투수를 조회합니다.
     */
    fun findSavePitcherByGameId(gameId: Long): PitchingRecord?

    /**
     * ERA 상위 N명을 조회합니다 (최소 이닝 조건).
     */
    fun findTopByEarnedRunAverage(
        minInningsPitchedOuts: Int,
        limit: Int,
    ): List<PitchingRecord>

    /**
     * 삼진 상위 N명을 조회합니다.
     */
    fun findTopByStrikeouts(limit: Int): List<PitchingRecord>

    /**
     * 승수 상위 N명을 조회합니다.
     */
    fun findTopByWins(limit: Int): List<PitchingRecord>

    /**
     * 세이브 상위 N명을 조회합니다.
     */
    fun findTopBySaves(limit: Int): List<PitchingRecord>

    /**
     * 선수의 특정 연도 투수 기록을 조회합니다.
     */
    fun findAllByPlayerIdAndYear(
        playerId: Long,
        year: Int,
    ): List<PitchingRecord>

    /**
     * 팀의 여러 경기 투수 기록을 한 번에 조회합니다. (N+1 방지용 배치 쿼리)
     */
    fun findAllByTeamIdAndGameIds(
        teamId: Long,
        gameIds: List<Long>,
    ): List<PitchingRecord>

    /**
     * 선수 ID로 모든 투수 기록을 조회합니다. (FETCH JOIN으로 N+1 방지)
     * gamePlayer, gameTeam, game을 함께 로딩하여 커리어 통계 계산에 사용합니다.
     */
    fun findAllByPlayerIdWithGameInfo(playerId: Long): List<PitchingRecord>

    /**
     * 선수 ID와 대회 ID로 투수 기록을 조회합니다.
     * 대회별 스탯 필터에 사용됩니다.
     */
    fun findAllByPlayerIdAndCompetitionId(
        playerId: Long,
        competitionId: Long,
    ): List<PitchingRecord>
}
