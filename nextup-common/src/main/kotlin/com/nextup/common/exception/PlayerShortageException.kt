package com.nextup.common.exception

/**
 * 경기 중 인원 부족이 감지되었을 때 발생하는 예외
 *
 * 선수 퇴장 후 남은 활동 선수가 최소 인원 미만일 때 발생합니다.
 * 기록원이 이 상황을 인지하고 몰수패 또는 경기 속행을 결정해야 합니다.
 */
class PlayerShortageException(
    gameId: Long,
    teamId: Long,
    activeCount: Int,
    minimumRequired: Int,
) : BusinessException(
        "PLAYER_SHORTAGE",
        "경기(ID: $gameId) 팀(ID: $teamId)의 활동 선수가 $activeCount 명으로 " +
            "최소 인원($minimumRequired 명) 미만입니다. 몰수패 처리가 필요할 수 있습니다.",
    )
