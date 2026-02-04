package com.nextup.core.service.standings

import com.nextup.core.service.standings.dto.StandingsDto

/**
 * 순위표 서비스
 */
interface StandingsService {
    /**
     * 대회 순위표를 조회합니다.
     *
     * @param competitionId 대회 ID
     * @return 순위표 정보
     */
    fun getStandings(competitionId: Long): StandingsDto
}
