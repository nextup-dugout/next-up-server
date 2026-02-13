package com.nextup.common.exception

class MatchRequestNotFoundException(
    id: Long,
) : NotFoundException(
        code = "MATCH_REQUEST_NOT_FOUND",
        message = "매칭 요청을 찾을 수 없습니다: $id",
    )
