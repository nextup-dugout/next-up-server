package com.nextup.core.port.repository

import com.nextup.core.domain.game.CorrectionType
import com.nextup.core.domain.game.RecordCorrection

/**
 * RecordCorrection Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface RecordCorrectionRepositoryPort {
    fun save(recordCorrection: RecordCorrection): RecordCorrection

    /**
     * 경기 ID로 정정 이력을 조회합니다.
     */
    fun findAllByGameId(gameId: Long): List<RecordCorrection>

    /**
     * 경기 ID와 정정 유형으로 정정 이력을 조회합니다.
     */
    fun findAllByGameIdAndCorrectionType(
        gameId: Long,
        correctionType: CorrectionType,
    ): List<RecordCorrection>

    /**
     * 특정 기록의 정정 이력을 조회합니다.
     */
    fun findAllByTargetRecordId(
        correctionType: CorrectionType,
        targetRecordId: Long,
    ): List<RecordCorrection>

    /**
     * ID로 정정 이력을 조회합니다.
     */
    fun findByIdOrNull(id: Long): RecordCorrection?
}
