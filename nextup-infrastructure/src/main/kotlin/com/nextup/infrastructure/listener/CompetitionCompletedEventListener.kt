package com.nextup.infrastructure.listener

import com.nextup.core.domain.event.CompetitionCompletedEvent
import com.nextup.core.domain.notification.NotificationType
import com.nextup.core.domain.team.TeamMemberStatus
import com.nextup.core.port.repository.GameTeamRepositoryPort
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
 * 대회 완료 이벤트 리스너
 *
 * CompetitionCompletedEvent 수신 시 대회에 참가한 모든 팀의 활성 멤버에게
 * 대회 완료 알림을 발송합니다.
 * AFTER_COMMIT 단계에서 실행되어 비즈니스 트랜잭션 롤백에 영향을 주지 않습니다.
 */
@Component
class CompetitionCompletedEventListener(
    private val notificationService: NotificationService,
    private val teamMemberRepository: TeamMemberRepositoryPort,
    private val gameTeamRepository: GameTeamRepositoryPort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 대회 완료 이벤트를 처리합니다.
     *
     * 대회에 참가한 모든 팀의 활성 멤버에게 배치로 알림을 발송합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleCompetitionCompleted(event: CompetitionCompletedEvent) {
        log.info(
            "대회 완료 이벤트 수신 (competitionId={}, competitionName={})",
            event.competitionId,
            event.competitionName,
        )

        val gameTeams = gameTeamRepository.findAllByCompetitionId(event.competitionId)
        val distinctTeamIds =
            gameTeams.map { it.team.id }.distinct()

        if (distinctTeamIds.isEmpty()) {
            log.debug(
                "대회에 참가한 팀이 없어 알림 발송 스킵 (competitionId={})",
                event.competitionId,
            )
            return
        }

        val requests =
            distinctTeamIds.flatMap { teamId ->
                val activeMembers =
                    teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE)
                activeMembers.map { member ->
                    SendNotificationRequest(
                        userId = member.user.id,
                        type = NotificationType.COMPETITION_COMPLETED,
                        title = "대회 완료",
                        body = "'${event.competitionName}' 대회가 완료되었습니다.",
                    )
                }
            }

        if (requests.isNotEmpty()) {
            notificationService.sendBatchNotifications(requests)
            log.info(
                "대회 완료 알림 발송 완료 (competitionId={}, teamCount={}, notificationCount={})",
                event.competitionId,
                distinctTeamIds.size,
                requests.size,
            )
        }
    }
}
