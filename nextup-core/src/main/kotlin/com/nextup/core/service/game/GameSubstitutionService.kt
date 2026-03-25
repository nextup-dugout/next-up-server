package com.nextup.core.service.game

import com.nextup.core.domain.game.GameEvent
import com.nextup.core.domain.game.PlayerShortageResult
import com.nextup.core.service.game.dto.SubstitutionRequest

/**
 * 선수 교체 서비스 인터페이스
 *
 * 선수 교체, DH 해제 규칙 검증, 인원 부족 감지를 담당합니다.
 */
interface GameSubstitutionService {
    fun substitutePlayer(
        gameId: Long,
        request: SubstitutionRequest,
        scorerId: Long,
    ): GameEvent

    /**
     * 교체 없이 선수를 퇴장시킵니다 (부상/퇴장 등).
     *
     * 교체 선수가 없는 상황에서 선수가 경기에서 빠져야 할 때 사용합니다.
     * 퇴장 후 인원 부족 여부를 감지하여 [PlayerShortageResult]를 반환합니다.
     * 인원 부족이 감지되면 기록원이 몰수패 여부를 판단해야 합니다.
     *
     * @param gameId 경기 ID
     * @param gamePlayerId 퇴장할 선수의 GamePlayer ID
     * @param inning 퇴장 이닝
     * @param scorerId 기록원 ID
     * @return 인원 부족 감지 결과
     */
    fun removePlayerWithoutSubstitution(
        gameId: Long,
        gamePlayerId: Long,
        inning: Int,
        scorerId: Long,
    ): PlayerShortageResult
}
