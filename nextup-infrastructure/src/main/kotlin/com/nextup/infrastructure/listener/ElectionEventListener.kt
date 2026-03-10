package com.nextup.infrastructure.listener

import com.nextup.core.domain.election.ElectionType
import com.nextup.core.domain.event.ElectionCompletedEvent
import com.nextup.core.domain.team.TeamMemberRole
import com.nextup.core.port.repository.CandidateRepositoryPort
import com.nextup.core.port.repository.ElectionVoteRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 선거 완료 이벤트 리스너
 *
 * OWNER_ELECTION 완료 시 당선자에게 OWNER 권한을 이양합니다.
 * Election Aggregate와 TeamMember Aggregate의 변경을 분리합니다.
 */
@Component
class ElectionEventListener(
    private val electionVoteRepository: ElectionVoteRepositoryPort,
    private val candidateRepository: CandidateRepositoryPort,
    private val teamMemberRepository: TeamMemberRepositoryPort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 선거 완료 이벤트를 수신하여 OWNER 권한을 이양합니다.
     * 동률이거나 투표가 없으면 이양하지 않습니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onElectionCompleted(event: ElectionCompletedEvent) {
        if (event.electionType != ElectionType.OWNER_ELECTION) {
            log.debug("선거 완료 이벤트 무시 (OWNER_ELECTION이 아님): electionId={}", event.electionId)
            return
        }

        log.info("OWNER 선거 완료 이벤트 수신: electionId={}, teamId={}", event.electionId, event.teamId)
        transferOwnershipToWinner(event.electionId, event.teamId)
    }

    private fun transferOwnershipToWinner(
        electionId: Long,
        teamId: Long,
    ) {
        val voteCounts =
            electionVoteRepository.countByElectionIdGroupByCandidateId(electionId)

        // 투표가 없으면 이양하지 않음
        if (voteCounts.isEmpty()) {
            log.info("투표가 없어 OWNER 이양 생략: electionId={}", electionId)
            return
        }

        // 최다 득표수 확인
        val maxVotes = voteCounts.values.max()
        val topCandidates = voteCounts.filter { it.value == maxVotes }

        // 동률이면 이양하지 않음
        if (topCandidates.size > 1) {
            log.info("동률로 OWNER 이양 생략: electionId={}, 후보수={}", electionId, topCandidates.size)
            return
        }

        val winnerCandidateId = topCandidates.keys.first()
        val winnerCandidate = candidateRepository.findById(winnerCandidateId) ?: return

        val winner = teamMemberRepository.findByIdOrNull(winnerCandidate.memberId) ?: return

        // 당선자가 이미 OWNER이면 이양 불필요
        if (winner.role == TeamMemberRole.OWNER) {
            log.info("당선자가 이미 OWNER: electionId={}, memberId={}", electionId, winnerCandidate.memberId)
            return
        }

        // 기존 OWNER를 MEMBER로 강등
        val teamMembers = teamMemberRepository.findByTeamId(teamId)
        val currentOwner = teamMembers.firstOrNull { it.role == TeamMemberRole.OWNER }

        if (currentOwner != null) {
            currentOwner.role = TeamMemberRole.MEMBER
            teamMemberRepository.save(currentOwner)
            log.info("기존 OWNER 강등: memberId={}", currentOwner.id)
        }

        // 당선자를 OWNER로 승격
        winner.role = TeamMemberRole.OWNER
        teamMemberRepository.save(winner)
        log.info("당선자 OWNER 승격: electionId={}, memberId={}", electionId, winnerCandidate.memberId)
    }
}
