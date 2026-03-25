package com.nextup.common.exception

/**
 * 선수 제재를 찾을 수 없을 때 발생하는 예외
 */
class PlayerBanNotFoundException(
    banId: Long,
) : NotFoundException(
        "PLAYER_BAN_NOT_FOUND",
        "Player ban not found: $banId",
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
