package com.nextup.common.exception

/**
 * 수비 기록을 찾을 수 없을 때 발생하는 예외
 */
class FieldingRecordNotFoundException(
    gamePlayerId: Long,
) : NotFoundException("FIELDING_RECORD_NOT_FOUND", "Fielding record not found for GamePlayer: $gamePlayerId")

/**
 * 시즌 수비 통계를 찾을 수 없을 때 발생하는 예외
 */
class SeasonFieldingStatsNotFoundException(
    playerId: Long,
    year: Int,
) : NotFoundException(
        "SEASON_FIELDING_STATS_NOT_FOUND",
        "Season fielding stats not found for Player: $playerId, Year: $year",
    )

/**
 * 통산 수비 통계를 찾을 수 없을 때 발생하는 예외
 */
class CareerFieldingStatsNotFoundException(
    playerId: Long,
) : NotFoundException(
        "CAREER_FIELDING_STATS_NOT_FOUND",
        "Career fielding stats not found for Player: $playerId",
    )
