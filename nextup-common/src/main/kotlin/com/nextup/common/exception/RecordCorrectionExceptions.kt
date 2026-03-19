package com.nextup.common.exception

/**
 * 기록 정정 항목을 찾을 수 없을 때 발생하는 예외
 */
class RecordCorrectionNotFoundException(
    id: Long,
) : NotFoundException(
        code = "RECORD_CORRECTION_NOT_FOUND",
        message = "기록 정정 내역을 찾을 수 없습니다: $id",
    )

/**
 * 타격 기록 ID로 찾을 수 없을 때 발생하는 예외 (정정 목적)
 */
class BattingRecordNotFoundByIdException(
    id: Long,
) : NotFoundException(
        code = "BATTING_RECORD_NOT_FOUND",
        message = "타격 기록을 찾을 수 없습니다: $id",
    )

/**
 * 투수 기록 ID로 찾을 수 없을 때 발생하는 예외 (정정 목적)
 */
class PitchingRecordNotFoundByIdException(
    id: Long,
) : NotFoundException(
        code = "PITCHING_RECORD_NOT_FOUND",
        message = "투수 기록을 찾을 수 없습니다: $id",
    )

/**
 * 수비 기록 ID로 찾을 수 없을 때 발생하는 예외 (정정 목적)
 */
class FieldingRecordNotFoundByIdException(
    id: Long,
) : NotFoundException(
        code = "FIELDING_RECORD_NOT_FOUND",
        message = "수비 기록을 찾을 수 없습니다: $id",
    )

/**
 * 유효하지 않은 기록 정정 필드일 때 발생하는 예외
 */
class InvalidCorrectionFieldException(
    fieldName: String,
    recordType: String,
) : InvalidInputException(
        code = "INVALID_CORRECTION_FIELD",
        message = "유효하지 않은 정정 필드입니다: $fieldName (recordType=$recordType)",
    )

/**
 * 기록 정정 값이 유효하지 않을 때 발생하는 예외
 */
class InvalidCorrectionValueException(
    fieldName: String,
    value: String,
) : InvalidInputException(
        code = "INVALID_CORRECTION_VALUE",
        message = "유효하지 않은 정정 값입니다: $fieldName=$value",
    )
