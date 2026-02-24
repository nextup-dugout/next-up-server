package com.nextup.core.port.repository

import com.nextup.core.domain.game.AttendanceStatus
import com.nextup.core.domain.game.GameParticipation

/**
 * GameParticipation Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface AttendanceVoteRepositoryPort {
    fun save(attendanceVote: GameParticipation): GameParticipation

    fun saveAll(attendanceVotes: List<GameParticipation>): List<GameParticipation>

    fun findByIdOrNull(id: Long): GameParticipation?

    fun findByGameId(gameId: Long): List<GameParticipation>

    fun findByGameIdAndMemberId(
        gameId: Long,
        memberId: Long,
    ): GameParticipation?

    fun findByGameIdAndStatus(
        gameId: Long,
        status: AttendanceStatus,
    ): List<GameParticipation>

    fun findNonVotersByGameId(gameId: Long): List<GameParticipation>

    fun countByGameId(gameId: Long): Long

    fun countByGameIdAndStatus(
        gameId: Long,
        status: AttendanceStatus,
    ): Long

    fun delete(attendanceVote: GameParticipation)

    fun deleteById(id: Long)
}
