package com.nextup.common.exception

/**
 * 동일 대회에서 선수가 이미 다른 팀으로 등록되어 있을 때 발생하는 예외
 *
 * 부정선수 방지: 한 선수가 같은 대회에서 두 팀에 등록할 수 없음
 */
class CrossRegistrationException(
    competitionId: Long,
    playerId: Long,
    existingTeamId: Long,
) : InvalidInputException(
        "CROSS_REGISTRATION",
        "선수(ID: $playerId)가 이미 대회(ID: $competitionId)에 " +
            "다른 팀(ID: $existingTeamId)으로 등록되어 있습니다.",
    )
