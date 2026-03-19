package com.nextup.core.service.game

import com.nextup.core.domain.game.GameEvent
import com.nextup.core.service.game.dto.SubstitutionRequest

/**
 * 선수 교체 서비스 인터페이스
 *
 * 선수 교체, DH 해제 규칙 검증을 담당합니다.
 */
interface GameSubstitutionService {
    fun substitutePlayer(
        gameId: Long,
        request: SubstitutionRequest,
        scorerId: Long,
    ): GameEvent
}
