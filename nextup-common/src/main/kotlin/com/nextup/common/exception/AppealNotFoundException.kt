package com.nextup.common.exception

class AppealNotFoundException(
    id: Long,
) : NotFoundException(
        code = "APPEAL_NOT_FOUND",
        message = "이의 제기를 찾을 수 없습니다: $id",
    )
