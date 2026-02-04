package com.nextup.core.service.game

import com.nextup.core.service.game.dto.GameTimelineDto

/**
 * 경기 타임라인 서비스 인터페이스
 *
 * 경기의 이벤트 타임라인을 조회합니다.
 */
interface GameTimelineService {
    /**
     * 경기의 타임라인을 조회합니다.
     *
     * @param gameId 경기 ID
     * @param fromInning 시작 이닝 (nullable, 지정하지 않으면 1이닝부터)
     * @param toInning 종료 이닝 (nullable, 지정하지 않으면 마지막 이닝까지)
     * @return 경기 타임라인 DTO
     */
    fun getTimeline(
        gameId: Long,
        fromInning: Int?,
        toInning: Int?,
    ): GameTimelineDto
}
