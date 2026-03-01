package com.nextup.core.service.game

import com.nextup.core.service.game.dto.GameAggregateDto

/**
 * 경기 상세 통합 조회 서비스 인터페이스
 *
 * 프론트엔드 경기 상세 화면에 필요한 모든 데이터를 한 번의 호출로 제공합니다.
 */
interface GameAggregateService {
    /**
     * 경기 상세 통합 데이터를 조회합니다.
     *
     * @param gameId 경기 ID
     * @return 경기 상세 통합 데이터
     */
    fun getGameAggregate(gameId: Long): GameAggregateDto
}
