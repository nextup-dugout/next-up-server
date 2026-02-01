package com.nextup.common.exception

/**
 * 리그를 찾을 수 없을 때 발생하는 예외
 */
class LeagueNotFoundException(leagueId: Long) :
    NotFoundException(
        "LEAGUE_NOT_FOUND",
        "League not found: $leagueId"
    )

/**
 * 리그 이름이 해당 협회 내에서 중복될 때 발생하는 예외
 */
class LeagueNameDuplicateException(associationId: Long, name: String) :
    BusinessException(
        "LEAGUE_NAME_DUPLICATE",
        "League name already exists in association $associationId: $name"
    )
