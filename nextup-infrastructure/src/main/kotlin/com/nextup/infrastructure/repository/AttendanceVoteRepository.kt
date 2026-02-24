package com.nextup.infrastructure.repository

import com.nextup.core.domain.game.AttendanceStatus
import com.nextup.core.domain.game.GameParticipation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

/**
 * GameParticipation JPA Repository
 * JPA 전용 쿼리 메서드 정의
 */
interface AttendanceVoteRepository : JpaRepository<GameParticipation, Long> {
    @Query("SELECT av FROM GameParticipation av WHERE av.game.id = :gameId")
    fun findByGameId(gameId: Long): List<GameParticipation>

    @Query("SELECT av FROM GameParticipation av WHERE av.game.id = :gameId AND av.member.id = :memberId")
    fun findByGameIdAndMemberId(
        gameId: Long,
        memberId: Long,
    ): GameParticipation?

    @Query("SELECT av FROM GameParticipation av WHERE av.game.id = :gameId AND av.status = :status")
    fun findByGameIdAndStatus(
        gameId: Long,
        status: AttendanceStatus,
    ): List<GameParticipation>

    @Query("SELECT av FROM GameParticipation av WHERE av.game.id = :gameId AND av.respondedAt IS NULL")
    fun findNonVotersByGameId(gameId: Long): List<GameParticipation>

    @Query("SELECT COUNT(av) FROM GameParticipation av WHERE av.game.id = :gameId")
    fun countByGameId(gameId: Long): Long

    @Query("SELECT COUNT(av) FROM GameParticipation av WHERE av.game.id = :gameId AND av.status = :status")
    fun countByGameIdAndStatus(
        gameId: Long,
        status: AttendanceStatus,
    ): Long
}
