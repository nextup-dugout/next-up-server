package com.nextup.core.port.repository

import com.nextup.core.domain.game.AttendanceStatus
import com.nextup.core.domain.game.AttendanceVote

/**
 * AttendanceVote Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface AttendanceVoteRepositoryPort {
    fun save(attendanceVote: AttendanceVote): AttendanceVote

    fun saveAll(attendanceVotes: List<AttendanceVote>): List<AttendanceVote>

    fun findByIdOrNull(id: Long): AttendanceVote?

    fun findByGameId(gameId: Long): List<AttendanceVote>

    fun findByGameIdAndMemberId(
        gameId: Long,
        memberId: Long,
    ): AttendanceVote?

    fun findByGameIdAndStatus(
        gameId: Long,
        status: AttendanceStatus,
    ): List<AttendanceVote>

    fun findNonVotersByGameId(gameId: Long): List<AttendanceVote>

    fun countByGameId(gameId: Long): Long

    fun countByGameIdAndStatus(
        gameId: Long,
        status: AttendanceStatus,
    ): Long

    fun delete(attendanceVote: AttendanceVote)

    fun deleteById(id: Long)
}
