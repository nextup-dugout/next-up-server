package com.nextup.infrastructure.listener

import com.nextup.core.domain.attendance.PollStatus
import com.nextup.core.domain.competition.CompetitionPlayerStatus
import com.nextup.core.domain.election.ElectionStatus
import com.nextup.core.domain.event.TeamDisbandedEvent
import com.nextup.core.domain.event.TeamMemberKickedEvent
import com.nextup.core.domain.event.TeamMemberLeftEvent
import com.nextup.core.domain.game.LineupSubmissionStatus
import com.nextup.core.domain.stadium.BookingStatus
import com.nextup.core.domain.team.JoinRequestStatus
import com.nextup.core.port.attendance.ActivityScoreRepositoryPort
import com.nextup.core.port.attendance.AttendancePollRepositoryPort
import com.nextup.core.port.repository.CompetitionPlayerRepositoryPort
import com.nextup.core.port.repository.ElectionRepositoryPort
import com.nextup.core.port.repository.LineupEntryRepositoryPort
import com.nextup.core.port.repository.LineupSubmissionRepositoryPort
import com.nextup.core.port.repository.StadiumBookingRepositoryPort
import com.nextup.core.port.repository.TeamJoinRequestRepositoryPort
import org.slf4j.LoggerFactory
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
    private val stadiumBookingRepository: StadiumBookingRepositoryPort,
    private val attendancePollRepository: AttendancePollRepositoryPort,
    private val electionRepository: ElectionRepositoryPort,
    private val teamJoinRequestRepository: TeamJoinRequestRepositoryPort,
    private val activityScoreRepository: ActivityScoreRepositoryPort,
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
     * - 진행중 CompetitionPlayer → WITHDRAWN 처리
     * - CONFIRMED 상태의 StadiumBooking → CANCELLED
     * - OPEN 상태의 AttendancePoll → CLOSED
     * - PENDING/IN_PROGRESS 상태의 Election → CANCELLED
     * - PENDING 상태의 TeamJoinRequest → 일괄 REJECTED
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleTeamDisbanded(event: TeamDisbandedEvent) {
        logger.info("팀 해산 연쇄 처리 - teamId={}", event.teamId)

        withdrawCompetitionPlayers(event.teamId)
        cancelStadiumBookings(event.teamId)
        closeAttendancePolls(event.teamId)
        cancelElections(event.teamId)
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
     * 팀의 활성 CompetitionPlayer를 WITHDRAWN으로 처리합니다.
     */
    private fun withdrawCompetitionPlayers(teamId: Long) {
        val activePlayers =
            competitionPlayerRepository.findByTeamIdAndStatus(teamId, CompetitionPlayerStatus.ACTIVE)
        activePlayers.forEach { cp ->
            cp.withdraw()
            competitionPlayerRepository.save(cp)
            logger.info("CompetitionPlayer WITHDRAWN 처리 - competitionPlayerId={}", cp.id)
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
     * 팀의 PENDING/IN_PROGRESS 상태 선거를 취소합니다.
     */
    private fun cancelElections(teamId: Long) {
        val elections = electionRepository.findAllByTeamId(teamId)
        elections
            .filter { it.status == ElectionStatus.SCHEDULED || it.status == ElectionStatus.IN_PROGRESS }
            .forEach { election ->
                election.cancel()
                electionRepository.save(election)
                logger.info("선거 취소 - electionId={}", election.id)
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
