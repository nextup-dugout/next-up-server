package com.nextup.infrastructure.service.attendance

import com.nextup.common.exception.AttendancePollClosedException
import com.nextup.common.exception.AttendancePollNotFoundException
import com.nextup.common.exception.ForbiddenException
import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.attendance.AbsenceReason
import com.nextup.core.domain.attendance.AttendancePoll
import com.nextup.core.domain.attendance.AttendanceVote
import com.nextup.core.domain.attendance.EventCategory
import com.nextup.core.domain.attendance.PollStatus
import com.nextup.core.domain.attendance.VoteType
import com.nextup.core.domain.event.AttendanceVoteCreatedEvent
import com.nextup.core.port.attendance.AttendancePollRepositoryPort
import com.nextup.core.port.attendance.AttendanceVoteRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.service.attendance.AttendanceService
import com.nextup.core.service.attendance.GameVoteSummary
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 출석 관리 서비스 구현
 *
 * 범용 출석 투표와 경기 출석 투표를 통합하여 관리합니다.
 */
@Service
@Transactional(readOnly = true)
class AttendanceServiceImpl(
    private val attendancePollRepository: AttendancePollRepositoryPort,
    private val attendanceVoteRepository: AttendanceVoteRepositoryPort,
    private val teamRepository: TeamRepositoryPort,
    private val playerRepository: PlayerRepositoryPort,
    private val teamMemberRepository: TeamMemberRepositoryPort,
    private val gameRepository: GameRepositoryPort,
    private val gameTeamRepository: GameTeamRepositoryPort,
    private val eventPublisher: ApplicationEventPublisher,
) : AttendanceService {
    private val logger = LoggerFactory.getLogger(AttendanceServiceImpl::class.java)

    @Transactional
    override fun createPoll(
        teamId: Long,
        title: String,
        eventDate: String,
        deadline: String,
        category: EventCategory,
        gameId: Long?,
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
                category = category,
                gameId = gameId,
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

    // === 경기 출석 투표 통합 메서드 ===

    @Transactional
    override fun createPollsForGame(gameId: Long): List<AttendancePoll> {
        val game =
            gameRepository.findByIdOrNull(gameId)
                ?: throw GameNotFoundException(gameId)

        val gameTeams = gameTeamRepository.findAllByGameId(gameId)
        if (gameTeams.size != 2) {
            logger.warn("경기 {}의 팀 수가 2가 아닙니다: {}", gameId, gameTeams.size)
            return emptyList()
        }

        val createdPolls = mutableListOf<AttendancePoll>()

        for (gameTeam in gameTeams) {
            val teamId = gameTeam.team.id

            // 이미 투표가 있으면 스킵
            if (attendancePollRepository.existsByGameIdAndTeamId(gameId, teamId)) {
                logger.debug("경기 {} 팀 {} 투표가 이미 존재합니다", gameId, teamId)
                continue
            }

            val poll =
                AttendancePoll.createForGame(
                    team = gameTeam.team,
                    gameId = gameId,
                    eventDate = game.scheduledAt,
                    deadline = game.scheduledAt.minusDays(1),
                )

            val savedPoll = attendancePollRepository.save(poll)
            createdPolls.add(savedPoll)

            logger.info("경기 {} 팀 {} 출석 투표 생성 (pollId: {})", gameId, teamId, savedPoll.id)
        }

        return createdPolls
    }

    override fun findGamePoll(
        gameId: Long,
        teamId: Long,
    ): AttendancePoll? = attendancePollRepository.findByGameIdAndTeamId(gameId, teamId)

    @Transactional
    override fun closeExpiredPolls(): Int {
        val now = LocalDateTime.now()
        val expiredPolls = attendancePollRepository.findOpenPollsWithDeadlineBefore(now)

        if (expiredPolls.isEmpty()) {
            return 0
        }

        var closedCount = 0
        for (poll in expiredPolls) {
            poll.close()
            attendancePollRepository.save(poll)
            closedCount++
        }

        logger.info("만료된 출석 투표 {} 건 마감 처리 완료", closedCount)
        return closedCount
    }

    @Transactional
    override fun voteForGame(
        gameId: Long,
        userId: Long,
        voteType: VoteType,
        absenceReason: AbsenceReason?,
        reasonDetail: String?,
    ): AttendanceVote {
        val (poll, member) = findGamePollAndMember(gameId, userId)

        if (!poll.canVote()) {
            throw AttendancePollClosedException(poll.id)
        }

        return submitVote(
            pollId = poll.id,
            playerId = member.player.id,
            voteType = voteType,
            absenceReason = absenceReason,
            reasonDetail = reasonDetail,
        )
    }

    override fun getGameVotes(
        gameId: Long,
        userId: Long,
    ): List<AttendanceVote> {
        val (poll, _) = findGamePollAndMember(gameId, userId)
        return attendanceVoteRepository.findByPollId(poll.id)
    }

    override fun getGameVoteSummary(
        gameId: Long,
        userId: Long,
    ): GameVoteSummary {
        val (poll, _) = findGamePollAndMember(gameId, userId)
        val votes = attendanceVoteRepository.findByPollId(poll.id)

        val attending = votes.count { it.isAttending() }
        val absent = votes.count { it.isAbsent() }
        val undecided = votes.count { it.voteType == VoteType.UNDECIDED }

        return GameVoteSummary(
            pollId = poll.id,
            gameId = gameId,
            totalVotes = votes.size,
            attending = attending,
            absent = absent,
            undecided = undecided,
        )
    }

    override fun getGameNonVoters(
        gameId: Long,
        userId: Long,
    ): List<AttendanceVote> {
        val (poll, _) = findGamePollAndMember(gameId, userId)
        return attendanceVoteRepository.findByPollId(poll.id)
            .filter { it.voteType == VoteType.UNDECIDED }
    }

    /**
     * 경기에 대한 사용자의 팀 투표와 팀 멤버를 조회합니다.
     */
    private fun findGamePollAndMember(
        gameId: Long,
        userId: Long,
    ): Pair<AttendancePoll, com.nextup.core.domain.team.TeamMember> {
        gameRepository.findByIdOrNull(gameId)
            ?: throw GameNotFoundException(gameId)

        val gameTeams = gameTeamRepository.findAllByGameId(gameId)

        // 사용자가 속한 팀 찾기
        val member =
            gameTeams
                .flatMap { gameTeam ->
                    teamMemberRepository.findByTeamId(gameTeam.team.id)
                }
                .firstOrNull { it.user.id == userId }
                ?: throw ForbiddenException(
                    "GAME_MEMBER_001",
                    "You are not a member of either team in this game",
                )

        val poll =
            attendancePollRepository.findByGameIdAndTeamId(gameId, member.team.id)
                ?: throw AttendancePollNotFoundException(0L)

        return Pair(poll, member)
    }
}
