package com.nextup.core.service.team

import com.nextup.core.service.team.dto.TeamMatchupDto
import com.nextup.core.service.team.dto.TeamMatchupGameDto

/**
 * 팀 간 상대 전적 서비스 인터페이스
 *
 * 두 팀 간의 대결 전적 및 경기 기록을 제공합니다.
 */
interface TeamMatchupService {
    /**
     * 두 팀 간의 상대 전적을 조회합니다.
     *
     * @param teamId 조회 대상 팀 ID
     * @param opponentId 상대 팀 ID
     * @param competitionId 대회 ID 필터 (선택사항)
     * @return 팀 간 상대 전적 정보
     */
    fun getTeamMatchup(
        teamId: Long,
        opponentId: Long,
        competitionId: Long? = null,
    ): TeamMatchupDto

    /**
     * 두 팀 간의 최근 경기 목록을 조회합니다.
     *
     * @param teamId 조회 대상 팀 ID
     * @param opponentId 상대 팀 ID
     * @param limit 조회할 경기 수 (기본값: 10)
     * @return 최근 교전 경기 목록
     */
    fun getRecentGames(
        teamId: Long,
        opponentId: Long,
        limit: Int = 10,
    ): List<TeamMatchupGameDto>
}
