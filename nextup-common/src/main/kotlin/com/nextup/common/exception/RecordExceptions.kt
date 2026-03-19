package com.nextup.common.exception

/**
 * 타격 기록을 찾을 수 없을 때 발생하는 예외
 */
class BattingRecordNotFoundException(
    gamePlayerId: Long,
) : NotFoundException("BATTING_RECORD_NOT_FOUND", "Batting record not found for GamePlayer: $gamePlayerId")

/**
 * 투수 기록을 찾을 수 없을 때 발생하는 예외
 */
class PitchingRecordNotFoundException(
    gamePlayerId: Long,
) : NotFoundException("PITCHING_RECORD_NOT_FOUND", "Pitching record not found for GamePlayer: $gamePlayerId")

/**
 * 경기 출전 선수를 찾을 수 없을 때 발생하는 예외
 */
class GamePlayerNotFoundException(
    id: Long,
) : NotFoundException("GAME_PLAYER_NOT_FOUND", "GamePlayer not found: $id")

/**
 * 경기 출전 선수를 게임과 선수 ID로 찾을 수 없을 때 발생하는 예외
 */
class GamePlayerNotFoundByGameAndPlayerException(
    gameId: Long,
    playerId: Long,
) : NotFoundException(
        "GAME_PLAYER_NOT_FOUND",
        "GamePlayer not found for Game: $gameId and Player: $playerId",
    )

/**
 * 이미 기록이 존재할 때 발생하는 예외
 */
class RecordAlreadyExistsException(
    gamePlayerId: Long,
    recordType: String,
) : InvalidStateException(
        "RECORD_ALREADY_EXISTS",
        "$recordType record already exists for GamePlayer: $gamePlayerId",
    )

/**
 * 경기를 찾을 수 없을 때 발생하는 예외
 */
class GameNotFoundException(
    id: Long,
) : NotFoundException("GAME_NOT_FOUND", "Game not found: $id")

/**
 * 잘못된 경기 상태일 때 발생하는 예외
 */
class InvalidGameStateException(
    message: String,
) : InvalidStateException("INVALID_GAME_STATE", message)

/**
 * Undo가 불가능한 상태일 때 발생하는 예외
 */
class UndoNotAvailableException(
    reason: String,
) : InvalidStateException("UNDO_NOT_AVAILABLE", reason)

/**
 * 되돌릴 이벤트가 없을 때 발생하는 예외
 */
class NoEventToUndoException :
    NotFoundException(
        "NO_EVENT_TO_UNDO",
        "되돌릴 이벤트가 없습니다",
    )

/**
 * 다른 기록원이 이미 경기를 잠금한 상태에서 기록을 시도할 때 발생하는 예외
 */
class GameAlreadyLockedException(
    gameId: Long,
    lockedByScorerId: Long,
) : InvalidStateException(
        "GAME_ALREADY_LOCKED",
        "Game $gameId is already locked by scorer $lockedByScorerId",
    )

/**
 * 잠금하지 않은 기록원이 경기를 해제하려 할 때 발생하는 예외
 */
class GameNotLockedByCurrentScorerException(
    gameId: Long,
    scorerId: Long,
) : InvalidStateException(
        "GAME_NOT_LOCKED_BY_SCORER",
        "Game $gameId is not locked by scorer $scorerId",
    )

/**
 * 경기가 잠금되지 않은 상태에서 기록을 시도할 때 발생하는 예외
 */
class GameNotLockedForRecordingException(
    gameId: Long,
) : InvalidStateException(
        "GAME_NOT_LOCKED",
        "Game $gameId is not locked by any scorer. Lock the game before recording.",
    )

/**
 * 잠금한 기록원이 아닌 다른 사용자가 기록을 시도할 때 발생하는 예외
 */
class ScorerMismatchException(
    gameId: Long,
    requestScorerId: Long,
    lockedScorerId: Long,
) : ForbiddenException(
        "SCORER_MISMATCH",
        "Game $gameId is locked by scorer $lockedScorerId, but scorer $requestScorerId attempted to record.",
    )
