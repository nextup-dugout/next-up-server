package com.nextup.infrastructure.repository

import com.nextup.core.domain.game.AttendanceStatus
import com.nextup.core.domain.game.GameParticipation
import com.nextup.core.port.repository.AttendanceVoteRepositoryPort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

/**
 * GameAttendanceVote Repository Adapter
 * Hexagonal Architecture Outbound Adapter - game 도메인 AttendanceVoteRepositoryPort 구현
 */
@Repository
class GameAttendanceVoteRepositoryAdapter(
    private val jpaRepository: AttendanceVoteRepository,
) : AttendanceVoteRepositoryPort {
    override fun save(attendanceVote: GameParticipation): GameParticipation = jpaRepository.save(attendanceVote)

    override fun saveAll(attendanceVotes: List<GameParticipation>): List<GameParticipation> =
        jpaRepository.saveAll(attendanceVotes)

    override fun findByIdOrNull(id: Long): GameParticipation? = jpaRepository.findByIdOrNull(id)

    override fun findByGameId(gameId: Long): List<GameParticipation> = jpaRepository.findByGameId(gameId)

    override fun findByGameIdAndMemberId(
        gameId: Long,
        memberId: Long,
    ): GameParticipation? = jpaRepository.findByGameIdAndMemberId(gameId, memberId)

    override fun findByGameIdAndStatus(
        gameId: Long,
        status: AttendanceStatus,
    ): List<GameParticipation> = jpaRepository.findByGameIdAndStatus(gameId, status)

    override fun findNonVotersByGameId(gameId: Long): List<GameParticipation> =
        jpaRepository.findNonVotersByGameId(gameId)

    override fun countByGameId(gameId: Long): Long = jpaRepository.countByGameId(gameId)

    override fun countByGameIdAndStatus(
        gameId: Long,
        status: AttendanceStatus,
    ): Long = jpaRepository.countByGameIdAndStatus(gameId, status)

    override fun delete(attendanceVote: GameParticipation) {
        jpaRepository.delete(attendanceVote)
    }

    override fun deleteById(id: Long) {
        jpaRepository.deleteById(id)
    }
}
