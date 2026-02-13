package com.nextup.infrastructure.repository

import com.nextup.core.domain.game.AttendanceStatus
import com.nextup.core.domain.game.AttendanceVote
import com.nextup.core.port.repository.AttendanceVoteRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

/**
 * AttendanceVote Repository
 * JpaRepository 상속 + Port 인터페이스 구현
 */
interface AttendanceVoteRepository :
    JpaRepository<AttendanceVote, Long>,
    AttendanceVoteRepositoryPort {
    override fun findByIdOrNull(id: Long): AttendanceVote? = findById(id).orElse(null)

    @Query("SELECT av FROM AttendanceVote av WHERE av.game.id = :gameId")
    override fun findByGameId(gameId: Long): List<AttendanceVote>

    @Query("SELECT av FROM AttendanceVote av WHERE av.game.id = :gameId AND av.member.id = :memberId")
    override fun findByGameIdAndMemberId(
        gameId: Long,
        memberId: Long,
    ): AttendanceVote?

    @Query("SELECT av FROM AttendanceVote av WHERE av.game.id = :gameId AND av.status = :status")
    override fun findByGameIdAndStatus(
        gameId: Long,
        status: AttendanceStatus,
    ): List<AttendanceVote>

    @Query("SELECT av FROM AttendanceVote av WHERE av.game.id = :gameId AND av.respondedAt IS NULL")
    override fun findNonVotersByGameId(gameId: Long): List<AttendanceVote>

    @Query("SELECT COUNT(av) FROM AttendanceVote av WHERE av.game.id = :gameId")
    override fun countByGameId(gameId: Long): Long

    @Query("SELECT COUNT(av) FROM AttendanceVote av WHERE av.game.id = :gameId AND av.status = :status")
    override fun countByGameIdAndStatus(
        gameId: Long,
        status: AttendanceStatus,
    ): Long
}
