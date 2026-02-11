package com.nextup.common.exception

/**
 * 시즌 타격 통계를 찾을 수 없을 때 발생하는 예외
 */
class SeasonBattingStatsNotFoundException(
    playerId: Long,
    year: Int,
) : NotFoundException(
        "SEASON_BATTING_STATS_NOT_FOUND",
        "Season batting stats not found for Player: $playerId, Year: $year",
    )

/**
 * 시즌 투수 통계를 찾을 수 없을 때 발생하는 예외
 */
class SeasonPitchingStatsNotFoundException(
    playerId: Long,
    year: Int,
) : NotFoundException(
        "SEASON_PITCHING_STATS_NOT_FOUND",
        "Season pitching stats not found for Player: $playerId, Year: $year",
    )

/**
 * 통산 타격 통계를 찾을 수 없을 때 발생하는 예외
 */
class CareerBattingStatsNotFoundException(
    playerId: Long,
) : NotFoundException(
        "CAREER_BATTING_STATS_NOT_FOUND",
        "Career batting stats not found for Player: $playerId",
    )

/**
 * 통산 투수 통계를 찾을 수 없을 때 발생하는 예외
 */
class CareerPitchingStatsNotFoundException(
    playerId: Long,
) : NotFoundException(
        "CAREER_PITCHING_STATS_NOT_FOUND",
        "Career pitching stats not found for Player: $playerId",
    )

/**
 * 선수를 찾을 수 없을 때 발생하는 예외
 */
class PlayerNotFoundException(
    playerId: Long,
) : NotFoundException(
        "PLAYER_NOT_FOUND",
        "Player not found: $playerId",
    )

/**
 * 통계 유효성 검증 실패 시 발생하는 예외
 */
class StatsValidationException(
    message: String,
) : InvalidStateException("STATS_VALIDATION_ERROR", message)
