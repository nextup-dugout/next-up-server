package com.nextup.common.exception

/**
 * 팀을 찾을 수 없을 때 발생하는 예외
 */
class TeamNotFoundException(
    teamId: Long,
) : NotFoundException(
        "TEAM_NOT_FOUND",
        "Team not found: $teamId",
    )

/**
 * 팀이 이미 존재할 때 발생하는 예외
 */
class TeamAlreadyExistsException(
    teamName: String,
) : BusinessException(
        "TEAM_ALREADY_EXISTS",
        "Team already exists: $teamName",
    )

/**
 * 유효하지 않은 팀 상태일 때 발생하는 예외
 */
class InvalidTeamStateException(
    message: String,
) : InvalidStateException(
        "INVALID_TEAM_STATE",
        message,
    )
