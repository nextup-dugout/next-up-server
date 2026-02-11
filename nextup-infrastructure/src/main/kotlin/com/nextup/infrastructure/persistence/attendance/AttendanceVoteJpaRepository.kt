package com.nextup.infrastructure.persistence.attendance

import com.nextup.core.domain.attendance.AttendanceVote
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AttendanceVoteJpaRepository : JpaRepository<AttendanceVote, Long> {
    fun findByPollId(pollId: Long): List<AttendanceVote>

    fun findByPlayerId(playerId: Long): List<AttendanceVote>

    fun findByPollIdAndPlayerId(
        pollId: Long,
        playerId: Long,
    ): AttendanceVote?

    fun existsByPollIdAndPlayerId(
        pollId: Long,
        playerId: Long,
    ): Boolean
}
