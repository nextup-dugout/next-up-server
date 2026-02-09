package com.nextup.common.exception

/**
 * 투구 이벤트를 찾을 수 없을 때 발생하는 예외
 */
class PitchEventNotFoundException(
    id: Long,
) : NotFoundException("PITCH_EVENT_NOT_FOUND", "PitchEvent not found: $id")
