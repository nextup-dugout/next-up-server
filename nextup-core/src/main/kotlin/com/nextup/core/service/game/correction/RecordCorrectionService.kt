package com.nextup.core.service.game.correction

import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.PitchingRecord

/**
 * 기록 정정 서비스 인터페이스 (Port)
 *
 * 관리자가 타격/투수 기록을 정정하는 유스케이스를 정의합니다.
 * 경기 종료 후에도 관리자 권한으로 정정 가능합니다.
 */
interface RecordCorrectionService {
    /**
     * 타격 기록을 정정합니다.
     *
     * @param gameId 경기 ID
     * @param recordId 타격 기록 ID
     * @param request 정정 요청 정보
     * @return 정정된 타격 기록
     */
    fun correctBattingRecord(
        gameId: Long,
        recordId: Long,
        request: BattingCorrectionRequest,
    ): BattingRecord

    /**
     * 투수 기록을 정정합니다.
     *
     * @param gameId 경기 ID
     * @param recordId 투수 기록 ID
     * @param request 정정 요청 정보
     * @return 정정된 투수 기록
     */
    fun correctPitchingRecord(
        gameId: Long,
        recordId: Long,
        request: PitchingCorrectionRequest,
    ): PitchingRecord

    /**
     * 경기의 기록 정정 이력을 조회합니다.
     *
     * @param gameId 경기 ID
     * @return 정정 이력 목록
     */
    fun getCorrectionHistory(gameId: Long): List<RecordCorrectionDto>
}
