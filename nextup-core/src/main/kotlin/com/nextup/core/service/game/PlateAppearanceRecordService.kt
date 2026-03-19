package com.nextup.core.service.game

import com.nextup.core.service.game.dto.PlateAppearanceRecordResult
import com.nextup.core.service.game.dto.PlateAppearanceRequest

/**
 * 타석 결과 기록 서비스 인터페이스
 *
 * 타석 기록 입력과 관련 경고(투구 수, 타순 위반)를 처리합니다.
 */
interface PlateAppearanceRecordService {
    fun recordPlateAppearance(
        gameId: Long,
        request: PlateAppearanceRequest,
        scorerId: Long,
    ): PlateAppearanceRecordResult
}
