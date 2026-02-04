package com.nextup.core.service.stats

import com.nextup.core.service.stats.dto.TeamStatsDto

/**
 * 팀 통계 서비스
 *
 * 팀의 전체 통계(성적, 타격, 투수)를 제공합니다.
 */
interface TeamStatsService {
    /**
     * 팀 통계를 조회합니다.
     *
     * @param teamId 팀 ID
     * @param year 연도 (nullable, 미지정 시 전체 기간)
     * @param competitionId 대회 ID (nullable, 추후 구현 예정)
     * @return 팀 통계
     */
    fun getTeamStats(
        teamId: Long,
        year: Int?,
        competitionId: Long?,
    ): TeamStatsDto
}
