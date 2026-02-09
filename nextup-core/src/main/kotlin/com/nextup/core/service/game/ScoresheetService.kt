package com.nextup.core.service.game

import com.nextup.core.service.game.dto.ScoresheetDto

/**
 * 공식 기록지 서비스 인터페이스
 */
interface ScoresheetService {
    /**
     * 경기의 공식 기록지 데이터를 조회합니다.
     *
     * @param gameId 경기 ID
     * @return 공식 기록지 데이터
     */
    fun getScoresheet(gameId: Long): ScoresheetDto
}
