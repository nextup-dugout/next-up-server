package com.nextup.core.service.stats

import com.nextup.core.service.stats.dto.FormType
import com.nextup.core.service.stats.dto.RecentFormDto

/**
 * 최근 N경기 폼 분석 서비스
 */
interface RecentFormService {
    /**
     * 선수의 최근 N경기 폼을 분석합니다.
     *
     * @param playerId 선수 ID
     * @param games 조회할 경기 수 (최대 20)
     * @param type 타격/투수 기록 타입
     * @return 최근 폼 분석 결과
     */
    fun getRecentForm(
        playerId: Long,
        games: Int,
        type: FormType,
    ): RecentFormDto
}
