package com.nextup.common.exception

class MercenaryRequestNotFoundException(
    id: Long,
) : NotFoundException(
        code = "MERCENARY_REQUEST_NOT_FOUND",
        message = "용병 요청을 찾을 수 없습니다: $id",
    )

class MercenaryApplicationNotFoundException(
    id: Long,
) : NotFoundException(
        code = "MERCENARY_APPLICATION_NOT_FOUND",
        message = "용병 지원을 찾을 수 없습니다: $id",
    )

class MercenaryAlreadyAppliedException(
    requestId: Long,
    playerId: Long,
) : InvalidInputException(
        code = "MERCENARY_ALREADY_APPLIED",
        message = "이미 해당 용병 요청에 지원했습니다: requestId=$requestId, playerId=$playerId",
    )

class MercenaryRequestClosedException(
    id: Long,
) : InvalidStateException(
        code = "MERCENARY_REQUEST_CLOSED",
        message = "마감된 용병 요청입니다: $id",
    )

class MercenaryMaxCountReachedException(
    requestId: Long,
) : InvalidStateException(
        code = "MERCENARY_MAX_COUNT_REACHED",
        message = "최대 모집 인원에 도달했습니다: requestId=$requestId",
    )
