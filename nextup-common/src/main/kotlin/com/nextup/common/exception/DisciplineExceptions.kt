package com.nextup.common.exception

/**
 * 징계를 찾을 수 없을 때 발생하는 예외
 */
class DisciplineNotFoundException(
    disciplineId: Long,
) : NotFoundException(
        "DISCIPLINE_NOT_FOUND",
        "Discipline not found: $disciplineId",
    )

/**
 * 징계 상태가 유효하지 않을 때 발생하는 예외
 */
class InvalidDisciplineStateException(
    message: String,
) : InvalidStateException(
        "INVALID_DISCIPLINE_STATE",
        message,
    )

/**
 * 선수가 출장 불가 상태일 때 발생하는 예외
 */
class PlayerIneligibleException(
    playerId: Long,
    reason: String,
) : BusinessException(
        "PLAYER_INELIGIBLE",
        "Player $playerId is ineligible to play: $reason",
    )
