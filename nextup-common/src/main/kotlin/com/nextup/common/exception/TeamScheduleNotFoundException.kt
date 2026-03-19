package com.nextup.common.exception

/**
 * 팀 일정을 찾을 수 없을 때 발생하는 예외
 */
class TeamScheduleNotFoundException(
    id: Long,
) : NotFoundException(
        code = "TEAM_SCHEDULE_NOT_FOUND",
        message = "팀 일정을 찾을 수 없습니다: $id",
    )
