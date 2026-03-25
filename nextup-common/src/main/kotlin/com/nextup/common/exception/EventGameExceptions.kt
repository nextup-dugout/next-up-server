package com.nextup.common.exception

class EventGameNotFoundException(
    id: Long,
) : NotFoundException(
        code = "EVENT_GAME_NOT_FOUND",
        message = "이벤트 게임을 찾을 수 없습니다: $id",
    )

class EventGameParticipantNotFoundException(
    id: Long,
) : NotFoundException(
        code = "EVENT_GAME_PARTICIPANT_NOT_FOUND",
        message = "이벤트 게임 참가자를 찾을 수 없습니다: $id",
    )
