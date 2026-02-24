package com.nextup.common.exception

/**
 * 라인업 검증 예외 기본 클래스
 */
open class LineupValidationException(
    code: String,
    message: String,
) : InvalidInputException(code, message)

/**
 * 라인업에 동일 선수가 중복 등록되었을 때 발생하는 예외
 */
class DuplicatePlayerInLineupException(
    playerIds: Set<Long>,
) : LineupValidationException(
        "DUPLICATE_PLAYER_IN_LINEUP",
        "라인업에 중복된 선수가 있습니다. 중복 선수 ID: $playerIds",
    )

/**
 * 라인업에 포수가 없을 때 발생하는 예외
 */
class NoCatcherInLineupException :
    LineupValidationException(
        "NO_CATCHER_IN_LINEUP",
        "라인업에 포수(C)가 최소 1명 필요합니다.",
    )

/**
 * DH 규칙 위반 시 발생하는 예외
 */
class InvalidDhRuleException(
    message: String,
) : LineupValidationException(
        "INVALID_DH_RULE",
        message,
    )

/**
 * 참석하지 않는 선수가 라인업에 포함되었을 때 발생하는 예외
 */
class NonAttendingPlayerInLineupException(
    playerIds: Set<Long>,
) : LineupValidationException(
        "NON_ATTENDING_PLAYER_IN_LINEUP",
        "라인업에 참석(ATTENDING)이 아닌 선수가 포함되어 있습니다. 선수 ID: $playerIds",
    )

/**
 * 라인업이 아직 교환되지 않아 상대팀이 조회할 수 없을 때 발생하는 예외
 */
class LineupNotExchangedException(
    gameId: Long,
) : ForbiddenException(
        "LINEUP_NOT_EXCHANGED",
        "아직 양 팀 라인업 교환이 완료되지 않았습니다. 경기 ID: $gameId",
    )

/**
 * 대진표를 찾을 수 없을 때 발생하는 예외
 */
class ScheduleNotFoundException(
    scheduleId: Long,
) : NotFoundException(
        "SCHEDULE_NOT_FOUND",
        "Schedule not found: $scheduleId",
    )

/**
 * 대진표 상태가 유효하지 않을 때 발생하는 예외
 */
class InvalidScheduleStateException(
    message: String,
) : InvalidStateException(
        "INVALID_SCHEDULE_STATE",
        message,
    )
