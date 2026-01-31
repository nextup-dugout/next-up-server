package com.nextup.api.dto.game

import jakarta.validation.constraints.Min

/**
 * 투구 이벤트 기록 요청 DTO
 *
 * Entity의 개별 메서드 (recordOut(), recordHit() 등)와 대응하는 Sealed Class 패턴
 */
sealed class RecordPitchingEventRequest {
    /**
     * 아웃 기록
     */
    data class RecordOut(
        val isStrikeout: Boolean = false
    ) : RecordPitchingEventRequest()

    /**
     * 피안타 기록
     */
    data class RecordHit(
        val isHomeRun: Boolean = false,
        @field:Min(0, message = "실점은 0 이상이어야 합니다.")
        val runsScored: Int = 0,
        @field:Min(0, message = "자책점은 0 이상이어야 합니다.")
        val earnedRuns: Int = 0
    ) : RecordPitchingEventRequest()

    /**
     * 볼넷 기록
     */
    data class RecordWalk(
        val dummy: Boolean = true
    ) : RecordPitchingEventRequest()

    /**
     * 사구 기록
     */
    data class RecordHitByPitch(
        val dummy: Boolean = true
    ) : RecordPitchingEventRequest()

    /**
     * 투구 수 기록
     */
    data class RecordPitchCount(
        @field:Min(1, message = "총 투구 수는 1 이상이어야 합니다.")
        val totalPitches: Int,
        @field:Min(0, message = "스트라이크 수는 0 이상이어야 합니다.")
        val strikes: Int
    ) : RecordPitchingEventRequest()
}
