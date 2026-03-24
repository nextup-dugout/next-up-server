package com.nextup.common.exception

/**
 * 선수 소속 이력을 찾을 수 없을 때 발생하는 예외
 */
class PlayerTeamHistoryNotFoundException(
    id: Long,
) : NotFoundException(
        "PLAYER_TEAM_HISTORY_NOT_FOUND",
        "Player team history not found: $id",
    )

/**
 * 선수가 해당 팀에 소속되어 있지 않을 때 발생하는 예외
 */
class PlayerNotInTeamException(
    playerId: Long,
    teamId: Long,
) : NotFoundException(
        "PLAYER_NOT_IN_TEAM",
        "Player $playerId is not affiliated with team $teamId",
    )

/**
 * 선수가 이미 해당 리그에 소속되어 있을 때 발생하는 예외
 */
class PlayerAlreadyInLeagueException(
    playerId: Long,
    leagueId: Long,
) : BusinessException(
        "PLAYER_ALREADY_IN_LEAGUE",
        "Player $playerId is already active in league $leagueId",
    )

/**
 * 유효하지 않은 선수 소속 상태일 때 발생하는 예외
 */
class InvalidPlayerTeamStatusException(
    message: String,
) : InvalidStateException(
        "INVALID_PLAYER_TEAM_STATUS",
        message,
    )
