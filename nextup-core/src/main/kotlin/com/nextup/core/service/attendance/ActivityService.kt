package com.nextup.core.service.attendance

import java.math.BigDecimal

/**
 * 활동 점수 서비스 인터페이스 (자동 집계)
 *
 * 기존 ActivityScore 테이블 기반 수동 PUT을 삭제하고,
 * game_players 기반 경기참여율 자동 집계로 전환합니다.
 */
interface ActivityService {
    /**
     * 팀원의 경기참여율을 자동 집계합니다.
     *
     * 계산식: (해당 멤버의 GamePlayer 수 / 팀 전체 경기 수) * 100
     *
     * @param teamId 팀 ID
     * @param playerId 선수 ID
     * @return 경기참여율 (0~100)
     */
    fun getGameParticipationRate(
        teamId: Long,
        playerId: Long,
    ): BigDecimal

    /**
     * 팀의 모든 멤버별 경기참여율을 자동 집계합니다.
     *
     * @param teamId 팀 ID
     * @return 멤버별 경기참여율 목록
     */
    fun listGameParticipationRates(teamId: Long): List<PlayerParticipationRate>
}

/**
 * 선수별 경기참여율 DTO
 */
data class PlayerParticipationRate(
    val playerId: Long,
    val playerName: String,
    val gamesPlayed: Int,
    val totalTeamGames: Int,
    val participationRate: BigDecimal,
)
