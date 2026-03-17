package com.nextup.core.service.game.correction

import com.nextup.core.domain.game.CorrectionType

/**
 * 타격 기록 정정 요청 DTO (Service 계층용)
 */
data class BattingCorrectionRequest(
    val adminUserId: Long,
    val fieldName: String,
    val newValue: String,
    val reason: String,
)

/**
 * 투수 기록 정정 요청 DTO (Service 계층용)
 */
data class PitchingCorrectionRequest(
    val adminUserId: Long,
    val fieldName: String,
    val newValue: String,
    val reason: String,
)

/**
 * 수비 기록 정정 요청 DTO (Service 계층용)
 */
data class FieldingCorrectionRequest(
    val adminUserId: Long,
    val fieldName: String,
    val newValue: String,
    val reason: String,
)

/**
 * 기록 정정 이력 조회 DTO
 */
data class RecordCorrectionDto(
    val id: Long,
    val gameId: Long,
    val adminUserId: Long,
    val correctionType: CorrectionType,
    val targetRecordId: Long,
    val fieldName: String,
    val oldValue: String,
    val newValue: String,
    val reason: String,
)
