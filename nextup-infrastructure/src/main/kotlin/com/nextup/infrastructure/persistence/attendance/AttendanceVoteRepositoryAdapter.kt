package com.nextup.infrastructure.persistence.attendance

import com.nextup.core.domain.attendance.AttendancePoll
import com.nextup.core.domain.attendance.AttendanceVote
import com.nextup.core.domain.player.Player
import com.nextup.core.port.attendance.AttendanceVoteRepositoryPort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class AttendanceVoteRepositoryAdapter(
    private val jpaRepository: AttendanceVoteJpaRepository,
) : AttendanceVoteRepositoryPort {
    override fun save(attendanceVote: AttendanceVote): AttendanceVote = jpaRepository.save(attendanceVote)

    override fun findById(id: Long): AttendanceVote? = jpaRepository.findByIdOrNull(id)

    override fun findByPollAndPlayer(
        poll: AttendancePoll,
        player: Player,
    ): AttendanceVote? = jpaRepository.findByPollIdAndPlayerId(poll.id, player.id)

    override fun findByPollIdAndPlayerId(
        pollId: Long,
        playerId: Long,
    ): AttendanceVote? = jpaRepository.findByPollIdAndPlayerId(pollId, playerId)

    override fun findByPoll(poll: AttendancePoll): List<AttendanceVote> = jpaRepository.findByPollId(poll.id)

    override fun findByPollId(pollId: Long): List<AttendanceVote> = jpaRepository.findByPollId(pollId)

    override fun findByPlayer(player: Player): List<AttendanceVote> = jpaRepository.findByPlayerId(player.id)

    override fun delete(attendanceVote: AttendanceVote) {
        jpaRepository.delete(attendanceVote)
    }

    override fun existsByPollAndPlayer(
        poll: AttendancePoll,
        player: Player,
    ): Boolean = jpaRepository.existsByPollIdAndPlayerId(poll.id, player.id)
}
