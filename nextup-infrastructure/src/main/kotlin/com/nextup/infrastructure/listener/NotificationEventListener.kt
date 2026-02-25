package com.nextup.infrastructure.listener

import com.nextup.core.domain.event.AttendanceVoteCreatedEvent
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.domain.event.LineupConfirmedEvent
import com.nextup.core.domain.event.TeamJoinApprovedEvent
import com.nextup.core.domain.event.TeamJoinRejectedEvent
import com.nextup.core.domain.notification.NotificationType
import com.nextup.core.domain.team.TeamMemberStatus
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import com.nextup.core.service.notification.NotificationService
import com.nextup.core.service.notification.dto.SendNotificationRequest
import org.springframework.stereotype.Component
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
}
