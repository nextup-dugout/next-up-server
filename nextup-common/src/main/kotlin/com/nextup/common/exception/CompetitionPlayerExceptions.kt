package com.nextup.common.exception

/**
 * 대회 등록 선수를 찾을 수 없을 때 발생하는 예외
 */
class CompetitionPlayerNotFoundException(
    id: Long,
) : NotFoundException(
        "COMPETITION_PLAYER_NOT_FOUND",
        "Competition player not found: $id",
    )

/**
 * 라인업에 리그 미등록 선수가 포함되었을 때 발생하는 예외
 */
class UnregisteredPlayerInLineupException(
    playerIds: Set<Long>,
) : LineupValidationException(
        "UNREGISTERED_PLAYER",
        "리그에 등록되지 않은 선수가 포함되어 있습니다: $playerIds",
    )
