package com.nextup.common.exception

class MatchResponseNotFoundException(
    id: Long,
) : NotFoundException(
        code = "MATCH_RESPONSE_NOT_FOUND",
        message = "매칭 응답을 찾을 수 없습니다: $id",
    )
