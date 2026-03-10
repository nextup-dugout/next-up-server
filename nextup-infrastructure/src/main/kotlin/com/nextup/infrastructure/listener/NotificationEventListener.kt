package com.nextup.infrastructure.listener

import com.nextup.core.domain.event.AttendanceVoteCreatedEvent
import com.nextup.core.domain.event.GameCancelledEvent
import com.nextup.core.domain.event.GamePostponedEvent
import com.nextup.core.domain.event.GameRescheduledEvent
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.domain.event.LineupConfirmedEvent
import com.nextup.core.domain.event.TeamJoinApprovedEvent
import com.nextup.core.domain.event.TeamJoinRejectedEvent
import com.nextup.core.domain.event.TeamMemberKickedEvent
import com.nextup.core.domain.event.TeamMemberLeftEvent
import com.nextup.core.domain.notification.NotificationType
import com.nextup.core.domain.team.TeamMemberRole
import com.nextup.core.domain.team.TeamMemberStatus
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import com.nextup.core.service.notification.NotificationService
import com.nextup.core.service.notification.dto.SendNotificationRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 알림 이벤트 리스너
 *
 * 도메인 이벤트를 수신하여 사용자 알림을 생성합니다.
 * AFTER_COMMIT 단계에서 실행되어 비즈니스 트랜잭션 롤백에 영향을 주지 않습니다.
 */
@Component
class NotificationEventListener(
    private val notificationService: NotificationService,
    private val teamMemberRepository: TeamMemberRepositoryPort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 팀 가입 승인 이벤트를 처리합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleTeamJoinApproved(event: TeamJoinApprovedEvent) {
        notificationService.sendNotification(
            SendNotificationRequest(
                userId = event.userId,
                type = NotificationType.TEAM_JOIN_APPROVED,
                title = "팀 가입 승인",
                body = "${event.teamName} 팀 가입이 승인되었습니다.",
            ),
        )
    }

    /**
     * 팀 가입 거절 이벤트를 처리합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleTeamJoinRejected(event: TeamJoinRejectedEvent) {
        notificationService.sendNotification(
            SendNotificationRequest(
                userId = event.userId,
                type = NotificationType.TEAM_JOIN_REJECTED,
                title = "팀 가입 거절",
                body = "${event.teamName} 팀 가입 신청이 거절되었습니다.",
            ),
        )
    }

    /**
     * 출석 투표 생성 이벤트를 처리합니다.
     *
     * 해당 팀의 모든 활성 멤버에게 알림을 발송합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleAttendanceVoteCreated(event: AttendanceVoteCreatedEvent) {
        val activeMembers =
            teamMemberRepository.findByTeamIdAndStatus(event.teamId, TeamMemberStatus.ACTIVE)
        val eventDateStr = event.eventDate.toLocalDate().toString()
        activeMembers.forEach { member ->
            notificationService.sendNotification(
                SendNotificationRequest(
                    userId = member.user.id,
                    type = NotificationType.ATTENDANCE_VOTE_CREATED,
                    title = "출석 투표 생성",
                    body = "$eventDateStr 이벤트 출석 투표가 생성되었습니다. 참석 여부를 입력해주세요.",
                ),
            )
        }
    }

    /**
     * 경기 결과 확정 이벤트를 처리합니다.
     *
     * 홈팀과 원정팀의 모든 활성 멤버에게 알림을 발송합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleGameResultConfirmed(event: GameResultConfirmedEvent) {
        val body = "경기 결과: ${event.homeScore} - ${event.awayScore}"
        val allTeamIds = listOf(event.homeTeamId, event.awayTeamId)
        allTeamIds.forEach { teamId ->
            val activeMembers =
                teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE)
            activeMembers.forEach { member ->
                notificationService.sendNotification(
                    SendNotificationRequest(
                        userId = member.user.id,
                        type = NotificationType.GAME_RESULT_CONFIRMED,
                        title = "경기 결과 확정",
                        body = body,
                    ),
                )
            }
        }
    }

    /**
     * 라인업 확정 이벤트를 처리합니다.
     *
     * 해당 팀의 모든 활성 멤버에게 알림을 발송합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleLineupConfirmed(event: LineupConfirmedEvent) {
        val activeMembers =
            teamMemberRepository.findByTeamIdAndStatus(event.teamId, TeamMemberStatus.ACTIVE)
        activeMembers.forEach { member ->
            notificationService.sendNotification(
                SendNotificationRequest(
                    userId = member.user.id,
                    type = NotificationType.LINEUP_CONFIRMED,
                    title = "라인업 확정",
                    body = "경기 라인업이 확정되었습니다.",
                ),
            )
        }
    }

    /**
     * 경기 취소 이벤트를 처리합니다.
     *
     * 홈팀과 원정팀의 모든 활성 멤버에게 알림을 발송합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleGameCancelled(event: GameCancelledEvent) {
        if (event.homeTeamId == 0L || event.awayTeamId == 0L) {
            log.warn("경기 취소 알림 스킵: 팀 정보 없음 - gameId={}", event.gameId)
            return
        }
        val allTeamIds = listOf(event.homeTeamId, event.awayTeamId)
        allTeamIds.forEach { teamId ->
            val activeMembers =
                teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE)
            activeMembers.forEach { member ->
                notificationService.sendNotification(
                    SendNotificationRequest(
                        userId = member.user.id,
                        type = NotificationType.GAME_CANCELLED,
                        title = "경기 취소",
                        body = "예정된 경기가 취소되었습니다.",
                    ),
                )
            }
        }
    }

    /**
     * 경기 연기 이벤트를 처리합니다.
     *
     * 홈팀과 원정팀의 모든 활성 멤버에게 알림을 발송합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleGamePostponed(event: GamePostponedEvent) {
        val newDateStr = event.newScheduledAt.toLocalDate().toString()
        val allTeamIds = listOf(event.homeTeamId, event.awayTeamId)
        allTeamIds.forEach { teamId ->
            val activeMembers =
                teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE)
            activeMembers.forEach { member ->
                notificationService.sendNotification(
                    SendNotificationRequest(
                        userId = member.user.id,
                        type = NotificationType.GAME_POSTPONED,
                        title = "경기 연기",
                        body = "경기가 $newDateStr 으로 연기되었습니다.",
                    ),
                )
            }
        }
    }

    /**
     * 경기 일정 변경 이벤트를 처리합니다.
     *
     * 홈팀과 원정팀의 모든 활성 멤버에게 알림을 발송합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleGameRescheduled(event: GameRescheduledEvent) {
        val newDateStr = event.newScheduledAt.toLocalDate().toString()
        val allTeamIds = listOf(event.homeTeamId, event.awayTeamId)
        allTeamIds.forEach { teamId ->
            val activeMembers =
                teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE)
            activeMembers.forEach { member ->
                notificationService.sendNotification(
                    SendNotificationRequest(
                        userId = member.user.id,
                        type = NotificationType.GAME_RESCHEDULED,
                        title = "경기 일정 변경",
                        body = "경기 일정이 $newDateStr 으로 변경되었습니다.",
                    ),
                )
            }
        }
    }

    /**
     * 팀원 탈퇴 이벤트를 처리합니다.
     *
     * 팀 관리자(OWNER, MANAGER)에게 알림을 발송합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleTeamMemberLeft(event: TeamMemberLeftEvent) {
        val admins =
            teamMemberRepository.findByTeamIdAndStatus(event.teamId, TeamMemberStatus.ACTIVE)
                .filter { it.role == TeamMemberRole.OWNER || it.role == TeamMemberRole.MANAGER }
        admins.forEach { admin ->
            notificationService.sendNotification(
                SendNotificationRequest(
                    userId = admin.user.id,
                    type = NotificationType.TEAM_MEMBER_LEFT,
                    title = "팀원 탈퇴",
                    body = "${event.teamName} 팀에서 팀원이 탈퇴했습니다.",
                ),
            )
        }
    }

    /**
     * 팀원 강퇴 이벤트를 처리합니다.
     *
     * 강퇴된 멤버에게 알림을 발송합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleTeamMemberKicked(event: TeamMemberKickedEvent) {
        notificationService.sendNotification(
            SendNotificationRequest(
                userId = event.userId,
                type = NotificationType.TEAM_MEMBER_KICKED,
                title = "팀 강퇴",
                body = "${event.teamName} 팀에서 강퇴되었습니다.",
            ),
        )
    }
}
