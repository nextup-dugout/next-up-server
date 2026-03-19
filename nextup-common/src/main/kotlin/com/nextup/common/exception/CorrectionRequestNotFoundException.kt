package com.nextup.common.exception

/**
 * 기록 정정 요청을 찾을 수 없을 때 발생하는 예외
 */
class CorrectionRequestNotFoundException(
    id: Long,
) : NotFoundException(
        code = "CORRECTION_REQUEST_NOT_FOUND",
        message = "기록 정정 요청을 찾을 수 없습니다: $id",
    )
