package com.nextup.core.service.game

import com.nextup.core.domain.game.Game
import com.nextup.core.service.game.dto.GameEndReason
import com.nextup.core.service.game.dto.PlateAppearanceRequest

/**
 * 기록원 전용 경기 기록 서비스 인터페이스
 *
 * 실시간 경기 기록 입력을 위한 서비스입니다.
 */
interface GameScorerService {
    /**
     * 경기를 시작합니다.
     *
     * @param gameId 경기 ID
     * @return 시작된 경기
     */
    fun startGame(gameId: Long): Game

    /**
     * 타석 결과를 기록합니다.
     *
     * @param gameId 경기 ID
     * @param request 타석 결과 요청
     * @return 업데이트된 경기
     */
    fun recordPlateAppearance(
        gameId: Long,
        request: PlateAppearanceRequest,
    ): Game

    /**
     * 반 이닝을 진행합니다 (공수 교대).
     *
     * @param gameId 경기 ID
     * @return 다음 이닝으로 진행된 경기
     */
    fun advanceHalfInning(gameId: Long): Game

    /**
     * 경기를 종료합니다.
     *
     * @param gameId 경기 ID
     * @param reason 종료 사유
     * @return 종료된 경기
     */
    fun endGame(
        gameId: Long,
        reason: GameEndReason,
    ): Game
}
