package com.nextup.core.service.stats

import com.nextup.core.service.stats.dto.BattingCategory
import com.nextup.core.service.stats.dto.BattingLeaderDto
import com.nextup.core.service.stats.dto.PitchingCategory
import com.nextup.core.service.stats.dto.PitchingLeaderDto

/**
 * 개인 타이틀 리더보드 서비스 인터페이스
 *
 * 대회별 카테고리 TOP N 리더보드를 조회합니다.
 */
interface IndividualRankingService {
    fun getBattingLeaders(
        competitionId: Long,
        category: BattingCategory,
        limit: Int = 10,
    ): List<BattingLeaderDto>

    fun getPitchingLeaders(
        competitionId: Long,
        category: PitchingCategory,
        limit: Int = 10,
    ): List<PitchingLeaderDto>
}
