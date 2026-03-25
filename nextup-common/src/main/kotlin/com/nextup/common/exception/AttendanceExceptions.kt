package com.nextup.common.exception

/**
 * 출석 투표를 찾을 수 없을 때 발생하는 예외
 */
class AttendancePollNotFoundException(
    pollId: Long,
) : NotFoundException("ATTENDANCE_POLL_NOT_FOUND", "출석 투표를 찾을 수 없습니다: $pollId")

/**
 * 출석 투표가 닫혀있을 때 발생하는 예외
 */
class AttendancePollClosedException(
    pollId: Long,
) : InvalidStateException("ATTENDANCE_POLL_CLOSED", "출석 투표가 마감되었습니다: $pollId")

/**
 * 이미 투표했을 때 발생하는 예외
 */
class AlreadyVotedException(
    pollId: Long,
    playerId: Long,
) : InvalidStateException("ALREADY_VOTED", "이미 투표했습니다 - pollId: $pollId, playerId: $playerId")
