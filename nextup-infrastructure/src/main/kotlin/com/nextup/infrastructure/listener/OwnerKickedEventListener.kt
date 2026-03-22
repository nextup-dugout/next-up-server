package com.nextup.infrastructure.listener

import com.nextup.core.domain.election.Election
import com.nextup.core.domain.election.ElectionType
import com.nextup.core.domain.event.OwnerKickedEvent
import com.nextup.core.domain.team.TeamMemberRole
import com.nextup.core.port.repository.ElectionRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * L-10: OWNER 강퇴 이벤트 리스너
 *
 * OWNER가 강제 강퇴되었을 때 자동으로 긴급 선거(EMERGENCY)를 생성합니다.
 * 팀에 MANAGER가 있으면 해당 MANAGER가 임시 구단주로 지정됩니다.
 */
@Component
class OwnerKickedEventListener(
    private val electionRepository: ElectionRepositoryPort,
    private val teamMemberRepository: TeamMemberRepositoryPort,
) {
    private val logger = LoggerFactory.getLogger(OwnerKickedEventListener::class.java)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleOwnerKicked(event: OwnerKickedEvent) {
        logger.info(
            "OWNER 강퇴 이벤트 수신 - teamId={}, kickedMemberId={}",
            event.teamId,
            event.kickedMemberId,
        )

        // 팀에 다른 OWNER가 이미 있는지 확인 (이양 후 강퇴된 경우)
        val currentOwnerCount = teamMemberRepository.countOwnersByTeamId(event.teamId)
        if (currentOwnerCount > 0) {
            logger.info(
                "팀에 이미 OWNER 존재, 선거 생성 생략 - teamId={}, ownerCount={}",
                event.teamId,
                currentOwnerCount,
            )
            return
        }

        // 긴급 선거 생성
        val now = Instant.now()
        val electionEnd = now.plus(ELECTION_DURATION_DAYS, ChronoUnit.DAYS)

        // MANAGER를 트리거 멤버로 지정 (없으면 시스템 발동)
        val managers =
            teamMemberRepository.findByTeamId(event.teamId)
                .filter { it.role == TeamMemberRole.MANAGER && it.isActive }
        val triggerMemberId = managers.firstOrNull()?.id ?: 0L

        val election =
            if (triggerMemberId > 0L) {
                Election.createEmergency(
                    teamId = event.teamId,
                    triggeredByMemberId = triggerMemberId,
                    title = "OWNER 강퇴에 따른 긴급 구단주 선거",
                    description = "기존 구단주가 강퇴되어 긴급 구단주 선거가 자동 개시되었습니다.",
                    startAt = now,
                    endAt = electionEnd,
                )
            } else {
                Election.create(
                    teamId = event.teamId,
                    title = "OWNER 강퇴에 따른 구단주 선거",
                    description = "기존 구단주가 강퇴되어 구단주 선거가 자동 개시되었습니다.",
                    electionType = ElectionType.OWNER_ELECTION,
                    startAt = now,
                    endAt = electionEnd,
                )
            }

        val savedElection = electionRepository.save(election)
        logger.info(
            "자동 선거 생성 완료 - teamId={}, electionId={}, type={}",
            event.teamId,
            savedElection.id,
            savedElection.electionType,
        )
    }

    companion object {
        /** 긴급 선거 기간 (일) */
        const val ELECTION_DURATION_DAYS = 7L
    }
}
