package com.nextup.infrastructure.service.attendance

import com.nextup.common.exception.AttendancePollClosedException
import com.nextup.common.exception.AttendancePollNotFoundException
import com.nextup.common.exception.ForbiddenException
import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.attendance.AbsenceReason
import com.nextup.core.domain.attendance.AttendancePoll
import com.nextup.core.domain.attendance.AttendanceVote
import com.nextup.core.domain.attendance.PollStatus
import com.nextup.core.domain.attendance.VoteType
import com.nextup.core.domain.event.AttendanceVoteCreatedEvent
import com.nextup.core.port.attendance.AttendancePollRepositoryPort
import com.nextup.core.port.attendance.AttendanceVoteRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.service.attendance.AttendanceService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 출석 관리 서비스 구현
 */
@Service
@Transactional(readOnly = true)
class AttendanceServiceImpl(
    private val attendancePollRepository: AttendancePollRepositoryPort,
    private val attendanceVoteRepository: AttendanceVoteRepositoryPort,
    private val teamRepository: TeamRepositoryPort,
    private val teamMemberRepository: TeamMemberRepositoryPort,
    private val playerRepository: PlayerRepositoryPort,
    private val eventPublisher: ApplicationEventPublisher,
) : AttendanceService {
    @Transactional
    override fun createPoll(
        teamId: Long,
        title: String,
        eventDate: String,
        deadline: String,
    ): AttendancePoll {
        val team =
            teamRepository.findByIdOrNull(teamId)
                ?: throw TeamNotFoundException(teamId)

        val eventDateTime = LocalDateTime.parse(eventDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val deadlineDateTime = LocalDateTime.parse(deadline, DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        val poll =
            AttendancePoll.create(
                team = team,
                title = title,
                eventDate = eventDateTime,
                deadline = deadlineDateTime,
            )

        val savedPoll = attendancePollRepository.save(poll)

        eventPublisher.publishEvent(
            AttendanceVoteCreatedEvent(
                teamId = teamId,
                pollId = savedPoll.id,
                eventDate = eventDateTime,
            ),
        )

        return savedPoll
    }

    @Transactional
    override fun submitVote(
        pollId: Long,
        playerId: Long,
        voteType: VoteType,
        absenceReason: AbsenceReason?,
        reasonDetail: String?,
    ): AttendanceVote {
        val poll =
            attendancePollRepository.findById(pollId)
                ?: throw AttendancePollNotFoundException(pollId)

        if (!poll.canVote()) {
            throw AttendancePollClosedException(pollId)
        }

        val player =
            playerRepository.findByIdOrNull(playerId)
                ?: throw PlayerNotFoundException(playerId)

        // Check if already voted
        val existingVote = attendanceVoteRepository.findByPollIdAndPlayerId(pollId, playerId)
        if (existingVote != null) {
            // Update existing vote
            existingVote.changeVote(voteType, absenceReason, reasonDetail)
            return attendanceVoteRepository.save(existingVote)
        }

        val vote =
            AttendanceVote.create(
                poll = poll,
                player = player,
                voteType = voteType,
                absenceReason = absenceReason,
                reasonDetail = reasonDetail,
            )

        return attendanceVoteRepository.save(vote)
    }

    @Transactional
    override fun submitVoteByUserId(
        pollId: Long,
        teamId: Long,
        userId: Long,
        voteType: VoteType,
        absenceReason: AbsenceReason?,
        reasonDetail: String?,
    ): AttendanceVote {
        val member =
            teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                ?: throw ForbiddenException(
                    "ATTENDANCE_VOTE_ACCESS_DENIED",
                    "해당 팀의 멤버가 아닙니다. teamId=$teamId",
                )

        return submitVote(
            pollId = pollId,
            playerId = member.player.id,
            voteType = voteType,
            absenceReason = absenceReason,
            reasonDetail = reasonDetail,
        )
    }

    override fun getPoll(pollId: Long): AttendancePoll =
        attendancePollRepository.findById(pollId)
            ?: throw AttendancePollNotFoundException(pollId)

    override fun listPolls(
        teamId: Long,
        status: PollStatus?,
    ): List<AttendancePoll> {
        if (!teamRepository.existsById(teamId)) {
            throw TeamNotFoundException(teamId)
        }

        return attendancePollRepository.findByTeamId(teamId, status)
    }

    @Transactional
    override fun closePoll(pollId: Long): AttendancePoll {
        val poll =
            attendancePollRepository.findById(pollId)
                ?: throw AttendancePollNotFoundException(pollId)

        poll.close()
        return attendancePollRepository.save(poll)
    }

    override fun listVotes(pollId: Long): List<AttendanceVote> {
        if (!attendancePollRepository.existsById(pollId)) {
            throw AttendancePollNotFoundException(pollId)
        }

        return attendanceVoteRepository.findByPollId(pollId)
    }
}
