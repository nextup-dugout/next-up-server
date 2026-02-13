package com.nextup.core.domain.schedule

import java.time.LocalDateTime

/**
 * 대진표 충돌 감지 도메인 서비스
 *
 * 대진표 생성/수정 시 발생할 수 있는 충돌을 감지합니다.
 * - 같은 팀이 동시간대에 여러 경기에 배정되는 충돌
 * - 같은 경기장에 동시간대에 여러 경기가 배정되는 충돌
 */
class ScheduleConflictDetector {
    /**
     * 팀 시간 충돌을 감지합니다.
     *
     * 같은 팀(홈 또는 원정)이 동일한 날짜/시간에 다른 경기에 배정되어 있는지 확인합니다.
     *
     * @param schedule 확인할 대진표
     * @param existingSchedules 같은 날짜의 기존 대진표 목록
     * @return 충돌 목록 (충돌 없으면 빈 리스트)
     */
    fun detectTeamConflicts(
        schedule: LeagueSchedule,
        existingSchedules: List<LeagueSchedule>,
    ): List<ScheduleConflict> {
        val conflicts = mutableListOf<ScheduleConflict>()

        // 시간이 지정되지 않은 경우 충돌 검사하지 않음
        val scheduleTime = schedule.scheduledTime ?: return emptyList()
        val scheduleDateTime = LocalDateTime.of(schedule.scheduledDate, scheduleTime)

        for (existing in existingSchedules) {
            // 자기 자신은 제외
            if (existing.id == schedule.id) continue

            // 시간이 지정되지 않은 대진표는 제외
            val existingTime = existing.scheduledTime ?: continue
            val existingDateTime = LocalDateTime.of(existing.scheduledDate, existingTime)

            // 같은 시간대인지 확인 (같은 시간이거나 1시간 이내)
            if (isTimeConflict(scheduleDateTime, existingDateTime)) {
                // 홈팀 충돌 확인
                if (schedule.homeTeam.id == existing.homeTeam.id || schedule.homeTeam.id == existing.awayTeam.id) {
                    conflicts.add(
                        ScheduleConflict(
                            type = ConflictType.TEAM_TIME_CONFLICT,
                            conflictingScheduleId = existing.id,
                            description =
                                "홈팀 '${schedule.homeTeam.name}'이(가) " +
                                    "${existing.scheduledDate} ${existing.scheduledTime}에 다른 경기(${existing.homeTeam.name} vs ${existing.awayTeam.name})에 배정되어 있습니다.",
                        ),
                    )
                }

                // 원정팀 충돌 확인
                if (schedule.awayTeam.id == existing.homeTeam.id || schedule.awayTeam.id == existing.awayTeam.id) {
                    conflicts.add(
                        ScheduleConflict(
                            type = ConflictType.TEAM_TIME_CONFLICT,
                            conflictingScheduleId = existing.id,
                            description =
                                "원정팀 '${schedule.awayTeam.name}'이(가) " +
                                    "${existing.scheduledDate} ${existing.scheduledTime}에 다른 경기(${existing.homeTeam.name} vs ${existing.awayTeam.name})에 배정되어 있습니다.",
                        ),
                    )
                }
            }
        }

        return conflicts
    }

    /**
     * 경기장 시간 충돌을 감지합니다.
     *
     * 같은 경기장에 동일한 날짜/시간에 다른 경기가 배정되어 있는지 확인합니다.
     *
     * @param schedule 확인할 대진표
     * @param existingSchedules 같은 날짜의 기존 대진표 목록
     * @return 충돌 목록 (충돌 없으면 빈 리스트)
     */
    fun detectVenueConflicts(
        schedule: LeagueSchedule,
        existingSchedules: List<LeagueSchedule>,
    ): List<ScheduleConflict> {
        val conflicts = mutableListOf<ScheduleConflict>()

        // 경기장이나 시간이 지정되지 않은 경우 충돌 검사하지 않음
        val venue = schedule.venue
        val scheduleTime = schedule.scheduledTime
        if (venue.isNullOrBlank() || scheduleTime == null) {
            return emptyList()
        }

        val scheduleDateTime = LocalDateTime.of(schedule.scheduledDate, scheduleTime)

        for (existing in existingSchedules) {
            // 자기 자신은 제외
            if (existing.id == schedule.id) continue

            // 경기장이나 시간이 지정되지 않은 대진표는 제외
            val existingVenue = existing.venue
            val existingTime = existing.scheduledTime
            if (existingVenue.isNullOrBlank() || existingTime == null) continue

            // 같은 경기장인지 확인 (대소문자 무시, 공백 제거)
            val isSameVenue =
                venue.trim().equals(existingVenue.trim(), ignoreCase = true)

            if (isSameVenue) {
                val existingDateTime = LocalDateTime.of(existing.scheduledDate, existingTime)

                // 같은 시간대인지 확인 (같은 시간이거나 1시간 이내)
                if (isTimeConflict(scheduleDateTime, existingDateTime)) {
                    conflicts.add(
                        ScheduleConflict(
                            type = ConflictType.VENUE_TIME_CONFLICT,
                            conflictingScheduleId = existing.id,
                            description =
                                "경기장 '$venue'에 " +
                                    "${existing.scheduledDate} ${existing.scheduledTime}에 다른 경기(${existing.homeTeam.name} vs ${existing.awayTeam.name})가 배정되어 있습니다.",
                        ),
                    )
                }
            }
        }

        return conflicts
    }

    /**
     * 모든 충돌을 감지합니다.
     *
     * 팀 시간 충돌과 경기장 시간 충돌을 모두 확인합니다.
     *
     * @param schedule 확인할 대진표
     * @param existingSchedules 같은 날짜의 기존 대진표 목록
     * @return 충돌 목록 (충돌 없으면 빈 리스트)
     */
    fun detectAllConflicts(
        schedule: LeagueSchedule,
        existingSchedules: List<LeagueSchedule>,
    ): List<ScheduleConflict> {
        val teamConflicts = detectTeamConflicts(schedule, existingSchedules)
        val venueConflicts = detectVenueConflicts(schedule, existingSchedules)
        return teamConflicts + venueConflicts
    }

    /**
     * 두 시간이 충돌하는지 확인합니다.
     *
     * 같은 시간이거나, 한 경기가 시작할 때 다른 경기가 진행 중인 경우 충돌로 간주합니다.
     * (경기 시간을 평균 3시간으로 가정)
     *
     * @param time1 첫 번째 시간
     * @param time2 두 번째 시간
     * @return 충돌 여부
     */
    private fun isTimeConflict(
        time1: LocalDateTime,
        time2: LocalDateTime,
    ): Boolean {
        val gamesDuration = 3L // 경기 시간 (시간 단위)

        // time1이 time2 경기 시간 범위 내에 있는지 확인
        val isTime1InTime2Range =
            !time1.isBefore(time2) && time1.isBefore(time2.plusHours(gamesDuration))

        // time2가 time1 경기 시간 범위 내에 있는지 확인
        val isTime2InTime1Range =
            !time2.isBefore(time1) && time2.isBefore(time1.plusHours(gamesDuration))

        return isTime1InTime2Range || isTime2InTime1Range
    }
}
