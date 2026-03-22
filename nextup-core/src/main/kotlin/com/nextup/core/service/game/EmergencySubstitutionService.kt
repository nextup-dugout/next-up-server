package com.nextup.core.service.game

import com.nextup.core.domain.game.GameEvent
import com.nextup.core.service.game.dto.EjectAndSubstituteRequest
import com.nextup.core.service.game.dto.EjectionRequest

/**
 * 긴급 교체 서비스 인터페이스 (Port)
 *
 * 부상 퇴장 및 긴급 교체를 처리합니다.
 * - 퇴장만 처리 (교체 선수 없는 경우)
 * - 퇴장 + 교체를 원자적으로 처리
 * - 주자 상태 선수 퇴장 시 베이스 계승
 * - 투수 부상 퇴장 시 currentPitcherId 갱신
 */
interface EmergencySubstitutionService {
    /**
     * 선수를 퇴장 처리합니다 (교체 없음).
     *
     * 교체 선수가 없는 경우 (벤치 소진 등) 퇴장만 처리합니다.
     * 해당 타순은 이후 자동 아웃으로 처리됩니다.
     *
     * @param gameId 경기 ID
     * @param request 퇴장 요청
     * @param scorerId 기록원 ID
     * @return 퇴장 이벤트
     */
    fun ejectPlayer(
        gameId: Long,
        request: EjectionRequest,
        scorerId: Long,
    ): GameEvent

    /**
     * 선수를 퇴장시키고 교체 선수를 투입합니다.
     *
     * 퇴장과 교체를 원자적으로 처리합니다.
     * 주자 상태인 선수가 퇴장하면 교체 선수가 해당 베이스를 계승합니다.
     * 투수가 부상 퇴장하면 교체 투수로 currentPitcherId를 갱신합니다.
     *
     * @param gameId 경기 ID
     * @param request 퇴장 + 교체 요청
     * @param scorerId 기록원 ID
     * @return 긴급 교체 이벤트
     */
    fun ejectAndSubstitute(
        gameId: Long,
        request: EjectAndSubstituteRequest,
        scorerId: Long,
    ): GameEvent
}
