package com.nextup.core.port.attendance

import com.nextup.core.domain.attendance.AttendancePoll
import com.nextup.core.domain.attendance.AttendanceVote
import com.nextup.core.domain.player.Player

/**
 * 출석 투표 응답 Repository Port
 *
 * Hexagonal Architecture의 Port 역할을 수행합니다.
 * Infrastructure 계층에서 이 인터페이스를 구현합니다.
 */
interface AttendanceVoteRepositoryPort {
    /**
     * 출석 투표 응답을 저장합니다.
     */
    fun save(attendanceVote: AttendanceVote): AttendanceVote

    /**
     * ID로 출석 투표 응답을 조회합니다.
     */
    fun findById(id: Long): AttendanceVote?

    /**
     * 투표와 선수로 응답을 조회합니다.
     */
    fun findByPollAndPlayer(
        poll: AttendancePoll,
        player: Player,
    ): AttendanceVote?

    /**
     * 투표 ID와 선수 ID로 응답을 조회합니다.
     */
    fun findByPollIdAndPlayerId(
        pollId: Long,
        playerId: Long,
    ): AttendanceVote?

    /**
     * 투표의 모든 응답을 조회합니다.
     */
    fun findByPoll(poll: AttendancePoll): List<AttendanceVote>

    /**
     * 투표 ID로 모든 응답을 조회합니다.
     */
    fun findByPollId(pollId: Long): List<AttendanceVote>

    /**
     * 선수의 모든 투표 응답을 조회합니다.
     */
    fun findByPlayer(player: Player): List<AttendanceVote>

    /**
     * 출석 투표 응답을 삭제합니다.
     */
    fun delete(attendanceVote: AttendanceVote)

    /**
     * 투표와 선수 조합이 존재하는지 확인합니다.
     */
    fun existsByPollAndPlayer(
        poll: AttendancePoll,
        player: Player,
    ): Boolean
}
