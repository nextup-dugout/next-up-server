package com.nextup.core.service.game

import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameEvent
import com.nextup.core.service.game.dto.GameEndReason
import com.nextup.core.service.game.dto.PlateAppearanceRequest
import com.nextup.core.service.game.dto.SubstitutionRequest

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

    /**
     * 마지막 이벤트를 되돌립니다.
     *
     * @param gameId 경기 ID
     * @return 되돌려진 이벤트
     */
    fun undoLastEvent(gameId: Long): GameEvent

    /**
     * 경기를 몰수 처리합니다.
     *
     * 승리팀에 7점, 패배팀에 0점을 자동 반영하고 경기를 종료합니다.
     *
     * @param gameId 경기 ID
     * @param winnerTeamId 몰수승 팀 ID
     * @param reason 몰수 사유
     * @return 몰수 처리된 경기
     */
    fun forfeitGame(
        gameId: Long,
        winnerTeamId: Long,
        reason: String,
    ): Game

    /**
     * 선수를 교체합니다.
     *
     * 교체 이벤트를 기록하고 다음 규칙을 검증합니다:
     * - 퇴장한 선수의 재출전 방지
     * - DH 해제 조건 검증 (투수가 DH 타순으로 들어오는 경우만 허용)
     *
     * @param gameId 경기 ID
     * @param request 선수 교체 요청
     * @return 교체 이벤트
     */
    fun substitutePlayer(
        gameId: Long,
        request: SubstitutionRequest,
    ): GameEvent
}
