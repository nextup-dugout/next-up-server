package com.nextup.core.service.game

import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.service.game.dto.BoxScoreDto

/**
 * 박스스코어 서비스 인터페이스
 */
interface BoxScoreService {
    /**
     * 경기의 박스스코어를 조회합니다.
     */
    fun getBoxScore(gameId: Long): BoxScoreDto

    /**
     * 타석 결과에 따라 박스스코어를 갱신합니다.
     *
     * @param gameId 경기 ID
     * @param batter 타자
     * @param pitcher 투수
     * @param result 타석 결과
     * @param rbis 타점 수
     * @param runsScored 득점한 주자 ID 목록
     * @param inning 현재 이닝
     */
    fun updateOnPlateAppearance(
        gameId: Long,
        batter: GamePlayer,
        pitcher: GamePlayer,
        result: PlateAppearanceResult,
        rbis: Int,
        runsScored: List<Long>,
        inning: Int
    )
}
