package com.nextup.common.exception

/**
 * 대회를 찾을 수 없을 때 발생하는 예외
 */
class CompetitionNotFoundException(
    competitionId: Long,
) : NotFoundException(
        "COMPETITION_NOT_FOUND",
        "Competition not found: $competitionId",
    )

/**
 * 대회 상태가 유효하지 않을 때 발생하는 예외
 */
class InvalidCompetitionStateException(
    message: String,
) : BusinessException(
        "INVALID_COMPETITION_STATE",
        message,
    )
