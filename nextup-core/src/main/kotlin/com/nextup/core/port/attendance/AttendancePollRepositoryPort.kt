package com.nextup.core.port.attendance

import com.nextup.core.domain.attendance.AttendancePoll
import com.nextup.core.domain.attendance.PollStatus
import com.nextup.core.domain.team.Team

/**
 * 출석 투표 Repository Port
 *
 * Hexagonal Architecture의 Port 역할을 수행합니다.
 * Infrastructure 계층에서 이 인터페이스를 구현합니다.
 */
interface AttendancePollRepositoryPort {
    /**
     * 출석 투표를 저장합니다.
     */
    fun save(attendancePoll: AttendancePoll): AttendancePoll

    /**
     * ID로 출석 투표를 조회합니다.
     */
    fun findById(id: Long): AttendancePoll?

    /**
     * 팀의 출석 투표 목록을 조회합니다.
     */
    fun findByTeam(
        team: Team,
        status: PollStatus? = null,
    ): List<AttendancePoll>

    /**
     * 팀 ID로 출석 투표 목록을 조회합니다.
     */
    fun findByTeamId(
        teamId: Long,
        status: PollStatus? = null,
    ): List<AttendancePoll>

    /**
     * 출석 투표를 삭제합니다.
     */
    fun delete(attendancePoll: AttendancePoll)

    /**
     * ID로 존재 여부를 확인합니다.
     */
    fun existsById(id: Long): Boolean
}
