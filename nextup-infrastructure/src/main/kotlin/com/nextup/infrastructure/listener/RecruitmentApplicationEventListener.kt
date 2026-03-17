package com.nextup.infrastructure.listener

import com.nextup.core.domain.event.RecruitmentApplicationAcceptedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 모집 지원 이벤트 리스너
 *
 * 지원 수락 시 팀 자동 합류 처리를 담당합니다.
 */
@Component
class RecruitmentApplicationEventListener {
    private val logger = LoggerFactory.getLogger(RecruitmentApplicationEventListener::class.java)

    /**
     * 모집 지원 수락 이벤트를 처리합니다.
     *
     * 지원 수락 시 해당 지원자를 팀에 자동 합류시킵니다.
     * 실제 팀 멤버 추가는 TeamMembershipService를 통해 처리됩니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleApplicationAccepted(event: RecruitmentApplicationAcceptedEvent) {
        logger.info(
            "모집 지원 수락 처리 - applicationId={}, teamId={}, applicantId={}, teamName={}",
            event.applicationId,
            event.teamId,
            event.applicantId,
            event.teamName,
        )
        // TODO: TeamMembershipService를 통한 팀 자동 합류 처리
        // 팀 합류에는 등번호 배정 등 추가 정보가 필요하므로,
        // 이 이벤트는 알림 발송 등의 용도로 사용하고
        // 실제 팀 합류는 별도 프로세스로 처리합니다.
    }
}
