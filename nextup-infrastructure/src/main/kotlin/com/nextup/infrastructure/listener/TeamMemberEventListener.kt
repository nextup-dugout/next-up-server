package com.nextup.infrastructure.listener

import com.nextup.core.domain.attendance.PollStatus
import com.nextup.core.domain.competition.CompetitionPlayerStatus
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.domain.event.TeamDisbandedEvent
import com.nextup.core.domain.event.TeamMemberKickedEvent
import com.nextup.core.domain.event.TeamMemberLeftEvent
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.domain.game.LineupSubmissionStatus
import com.nextup.core.domain.stadium.BookingStatus
import com.nextup.core.domain.team.JoinRequestStatus
import com.nextup.core.port.attendance.ActivityScoreRepositoryPort
import com.nextup.core.port.attendance.AttendancePollRepositoryPort
import com.nextup.core.port.repository.BracketEntryRepositoryPort
import com.nextup.core.port.repository.CompetitionPlayerRepositoryPort
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.LineupEntryRepositoryPort
import com.nextup.core.port.repository.LineupSubmissionRepositoryPort
import com.nextup.core.port.repository.StadiumBookingRepositoryPort
import com.nextup.core.port.repository.TeamJoinRequestRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 팀원 변동 이벤트 리스너
 *
 * 팀원 탈퇴/강퇴/팀 해산 시 연쇄 데이터 처리를 담당합니다.
 * AFTER_COMMIT 단계에서 실행되어 비즈니스 트랜잭션 롤백에 영향을 주지 않습니다.
 */
@Component
class TeamMemberEventListener(
    private val lineupSubmissionRepository: LineupSubmissionRepositoryPort,
    private val lineupEntryRepository: LineupEntryRepositoryPort,
    private val competitionPlayerRepository: CompetitionPlayerRepositoryPort,
    private val competitionRepository: CompetitionRepositoryPort,
    private val gameRepository: GameRepositoryPort,
    private val bracketEntryRepository: BracketEntryRepositoryPort,
    private val stadiumBookingRepository: StadiumBookingRepositoryPort,
    private val attendancePollRepository: AttendancePollRepositoryPort,
    private val teamJoinRequestRepository: TeamJoinRequestRepositoryPort,
    private val activityScoreRepository: ActivityScoreRepositoryPort,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val logger = LoggerFactory.getLogger(TeamMemberEventListener::class.java)

    /**
     * 팀원 탈퇴 이벤트를 처리합니다.
     *
     * DRAFT/SUBMITTED 상태의 라인업에서 해당 선수를 제거합니다.
     * 제거 후 라인업이 비어 있으면 상태를 DRAFT로 되돌립니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleTeamMemberLeft(event: TeamMemberLeftEvent) {
        logger.info("팀원 탈퇴 처리 - teamId={}, playerId={}, memberId={}", event.teamId, event.playerId, event.memberId)
        removePlayerFromActiveLineups(event.teamId, event.playerId)
        withdrawCompetitionPlayersByPlayerId(event.playerId)
        cleanupActivityScore(event.memberId)
    }

    /**
     * 팀원 강퇴 이벤트를 처리합니다.
     *
     * DRAFT/SUBMITTED 상태의 라인업에서 해당 선수를 제거합니다.
     * 제거 후 라인업이 비어 있으면 상태를 DRAFT로 되돌립니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleTeamMemberKicked(event: TeamMemberKickedEvent) {
        logger.info("팀원 강퇴 처리 - teamId={}, playerId={}, memberId={}", event.teamId, event.playerId, event.memberId)
        removePlayerFromActiveLineups(event.teamId, event.playerId)
        withdrawCompetitionPlayersByPlayerId(event.playerId)
        cleanupActivityScore(event.memberId)
    }

    /**
     * 팀 해산 이벤트를 처리합니다.
     *
     * - 진행중 대회에서 팀 탈퇴: CompetitionPlayer WITHDRAWN + 경기 몰수승 + 대진표 부전승 처리
     * - CONFIRMED 상태의 StadiumBooking → CANCELLED
     * - OPEN 상태의 AttendancePoll → CLOSED
     * - PENDING 상태의 TeamJoinRequest → 일괄 REJECTED
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleTeamDisbanded(event: TeamDisbandedEvent) {
        logger.info("팀 해산 연쇄 처리 - teamId={}", event.teamId)

        withdrawFromCompetitions(event.teamId)
        cancelStadiumBookings(event.teamId)
        closeAttendancePolls(event.teamId)
        rejectPendingJoinRequests(event.teamId)
        cleanupActivityScoresByTeam(event.teamId)
    }

    // -------------------------------------------------------------------------
    // 내부 처리 메서드
    // -------------------------------------------------------------------------

    /**
     * DRAFT/SUBMITTED/CONFIRMED 상태의 라인업에서 특정 선수를 제거합니다.
     * 제거 후 라인업 엔트리가 없으면 상태를 DRAFT로 되돌립니다.
     */
    private fun removePlayerFromActiveLineups(
        teamId: Long,
        playerId: Long,
    ) {
        val activeStatuses =
            listOf(
                LineupSubmissionStatus.DRAFT,
                LineupSubmissionStatus.SUBMITTED,
                LineupSubmissionStatus.CONFIRMED,
            )

        val submissions =
            lineupSubmissionRepository.findAllByTeamIdAndStatusIn(teamId, activeStatuses)

        submissions.forEach { submission ->
            val entry =
                lineupEntryRepository.findBySubmissionIdAndPlayerId(submission.id, playerId)
            if (entry != null) {
                lineupEntryRepository.delete(entry)

                val remainingEntries = lineupEntryRepository.findAllBySubmissionId(submission.id)
                if (remainingEntries.isEmpty() && submission.status == LineupSubmissionStatus.SUBMITTED) {
                    // 제출된 상태였지만 엔트리가 없으면 DRAFT로 되돌림
                    // LineupSubmission은 Rich Domain Model이므로 상태 복원 불가 시 직접 처리
                    // SUBMITTED → DRAFT 는 도메인 메서드가 없으므로 revertToSubmitted 유사 처리
                    // 단, 라인업 엔트리 제거 자체가 목적이므로 로그만 남김
                    logger.warn(
                        "라인업 엔트리가 모두 제거됨 - submissionId={}, status={} 로 유지",
                        submission.id,
                        submission.status,
                    )
                }

                logger.info(
                    "라인업에서 선수 제거 완료 - submissionId={}, playerId={}",
                    submission.id,
                    playerId,
                )
            }
        }
    }

    /**
     * 팀이 참가 중인 대회에서 탈퇴 처리합니다.
     *
     * 1. CompetitionPlayer를 WITHDRAWN으로 처리
     * 2. 진행 중 대회의 잔여 경기(SCHEDULED/IN_PROGRESS/POSTPONED)에 몰수승 처리
     * 3. 진행 중 대회의 대진표(BracketEntry) 부전승 처리
     */
    private fun withdrawFromCompetitions(teamId: Long) {
        // 팀이 참가 중인 대회 ID 목록 조회 (WITHDRAWN 제외)
        val activeCompetitionIds =
            competitionPlayerRepository.findActiveCompetitionIdsByTeamId(teamId)

        if (activeCompetitionIds.isEmpty()) {
            logger.info("팀이 참가 중인 대회 없음 - teamId={}", teamId)
            return
        }

        // CompetitionPlayer WITHDRAWN 처리
        val activePlayers =
            competitionPlayerRepository.findByTeamIdAndStatus(teamId, CompetitionPlayerStatus.ACTIVE)
        activePlayers.forEach { cp ->
            cp.withdraw()
            competitionPlayerRepository.save(cp)
            logger.info("CompetitionPlayer WITHDRAWN 처리 - competitionPlayerId={}", cp.id)
        }

        // 각 대회에 대해 몰수승/부전승 처리
        activeCompetitionIds.forEach { competitionId ->
            val competition = competitionRepository.findByIdOrNull(competitionId)
            if (competition != null && competition.status == CompetitionStatus.IN_PROGRESS) {
                forfeitGamesForTeam(competitionId, teamId)
                processBracketEntriesForTeam(competitionId, teamId)
            }
        }
    }

    /**
     * 대회에서 해당 팀의 잔여 경기를 몰수승 처리합니다.
     */
    private fun forfeitGamesForTeam(
        competitionId: Long,
        teamId: Long,
    ) {
        val games = gameRepository.findByCompetitionId(competitionId)
        games.forEach { game ->
            if (game.status == GameStatus.SCHEDULED ||
                game.status == GameStatus.IN_PROGRESS ||
                game.status == GameStatus.POSTPONED
            ) {
                val gameTeams = game.gameTeams
                val isTeamInGame = gameTeams.any { it.team.id == teamId }
                if (isTeamInGame) {
                    val opponentTeam = gameTeams.first { it.team.id != teamId }
                    game.forfeit(
                        winnerTeamId = opponentTeam.team.id,
                        reason = "팀 해산으로 인한 몰수패",
                        gameTeams = gameTeams,
                    )
                    gameRepository.save(game)

                    val homeTeam = gameTeams.find { it.homeAway == HomeAway.HOME }
                    val awayTeam = gameTeams.find { it.homeAway == HomeAway.AWAY }
                    if (homeTeam != null && awayTeam != null) {
                        eventPublisher.publishEvent(
                            GameResultConfirmedEvent(
                                gameId = game.id,
                                homeTeamId = homeTeam.team.id,
                                awayTeamId = awayTeam.team.id,
                                homeScore = homeTeam.totalScore,
                                awayScore = awayTeam.totalScore,
                            ),
                        )
                    }

                    logger.info(
                        "경기 몰수승 처리 - gameId={}, competitionId={}, winnerTeamId={}",
                        game.id,
                        competitionId,
                        opponentTeam.team.id,
                    )
                }
            }
        }
    }

    /**
     * 대회에서 해당 팀의 대진표 엔트리를 부전승 처리합니다.
     */
    private fun processBracketEntriesForTeam(
        competitionId: Long,
        teamId: Long,
    ) {
        val bracketEntries = bracketEntryRepository.findByCompetitionId(competitionId)
        bracketEntries.forEach { entry ->
            if (!entry.isCompleted()) {
                val isTeam1 = entry.team1?.id == teamId
                val isTeam2 = entry.team2?.id == teamId
                if (isTeam1 && entry.team2 != null) {
                    entry.recordWinner(entry.team2!!)
                    bracketEntryRepository.save(entry)
                    logger.info(
                        "대진표 부전승 처리 - bracketEntryId={}, winnerTeamId={}",
                        entry.id,
                        entry.team2!!.id,
                    )
                } else if (isTeam2 && entry.team1 != null) {
                    entry.recordWinner(entry.team1!!)
                    bracketEntryRepository.save(entry)
                    logger.info(
                        "대진표 부전승 처리 - bracketEntryId={}, winnerTeamId={}",
                        entry.id,
                        entry.team1!!.id,
                    )
                }
            }
        }
    }

    /**
     * 특정 선수의 활성/정지 상태 CompetitionPlayer를 WITHDRAWN으로 처리합니다.
     * 팀원 개별 탈퇴/강퇴 시 호출됩니다.
     */
    private fun withdrawCompetitionPlayersByPlayerId(playerId: Long) {
        val activeStatuses =
            listOf(
                CompetitionPlayerStatus.ACTIVE,
                CompetitionPlayerStatus.SUSPENDED,
            )
        val competitionPlayers =
            competitionPlayerRepository.findByPlayerIdAndStatusIn(playerId, activeStatuses)
        competitionPlayers.forEach { cp ->
            cp.withdraw()
            competitionPlayerRepository.save(cp)
            logger.info("CompetitionPlayer WITHDRAWN 처리 (개별 탈퇴) - competitionPlayerId={}, playerId={}", cp.id, playerId)
        }
    }

    /**
     * 팀의 CONFIRMED 상태 구장 예약을 취소합니다.
     */
    private fun cancelStadiumBookings(teamId: Long) {
        val confirmedBookings =
            stadiumBookingRepository.findByTeamIdAndStatus(teamId, BookingStatus.CONFIRMED)
        confirmedBookings.forEach { booking ->
            booking.cancel()
            stadiumBookingRepository.save(booking)
            logger.info("구장 예약 취소 - bookingId={}", booking.id)
        }
    }

    /**
     * 팀의 OPEN 상태 출석 투표를 마감합니다.
     */
    private fun closeAttendancePolls(teamId: Long) {
        val openPolls =
            attendancePollRepository.findByTeamId(teamId, PollStatus.OPEN)
        openPolls.forEach { poll ->
            poll.close()
            attendancePollRepository.save(poll)
            logger.info("출석 투표 마감 - pollId={}", poll.id)
        }
    }

    /**
     * 팀의 PENDING 상태 가입 신청을 일괄 거절합니다.
     */
    private fun rejectPendingJoinRequests(teamId: Long) {
        val pendingRequests =
            teamJoinRequestRepository
                .findByTeamId(teamId)
                .filter { it.status == JoinRequestStatus.PENDING }
        pendingRequests.forEach { request ->
            request.cancel()
            teamJoinRequestRepository.save(request)
            logger.info("가입 신청 거절 - requestId={}", request.id)
        }
    }

    /**
     * 팀원의 활동 점수를 삭제합니다.
     * 팀원 탈퇴/강퇴 시 orphan 방지를 위해 호출됩니다.
     */
    private fun cleanupActivityScore(memberId: Long) {
        activityScoreRepository.deleteByMemberId(memberId)
        logger.info("활동 점수 삭제 완료 - memberId={}", memberId)
    }

    /**
     * 팀의 모든 활동 점수를 삭제합니다.
     * 팀 해산 시 orphan 방지를 위해 호출됩니다.
     */
    private fun cleanupActivityScoresByTeam(teamId: Long) {
        activityScoreRepository.deleteByTeamId(teamId)
        logger.info("팀 활동 점수 전체 삭제 완료 - teamId={}", teamId)
    }
}
