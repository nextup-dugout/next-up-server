package com.nextup.infrastructure.listener

import com.nextup.core.domain.event.ElectionTiedEvent
import com.nextup.core.port.repository.ElectionVoteRepositoryPort
import com.nextup.core.service.election.ElectionService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 선거 동률 이벤트 리스너
 *
 * ElectionTiedEvent를 수신하여 재선거(결선투표)를 자동 생성합니다.
 * 동률 후보들만 대상으로 재선거를 생성하고, 최대 재선거 횟수를 초과하면 로그만 남깁니다.
 */
@Component
class ElectionTiedEventListener(
    private val electionService: ElectionService,
    private val electionVoteRepository: ElectionVoteRepositoryPort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 선거 동률 이벤트를 수신하여 재선거를 자동 생성합니다.
     *
     * 1. 동률 후보 ID 목록을 조회합니다.
     * 2. ElectionService를 통해 재선거를 생성합니다.
     * 3. 최대 재선거 횟수 초과 시 로그를 남기고 종료합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onElectionTied(event: ElectionTiedEvent) {
        log.info(
            "선거 동률 이벤트 수신: electionId={}, teamId={}, 동률 후보수={}, 득표수={}",
            event.electionId,
            event.teamId,
            event.tiedCandidateCount,
            event.tiedVoteCount,
        )

        try {
            val tiedCandidateIds = findTiedCandidateIds(event.electionId)
            if (tiedCandidateIds.isEmpty()) {
                log.warn(
                    "동률 후보를 찾을 수 없습니다: electionId={}",
                    event.electionId,
                )
                return
            }

            val runoffElection =
                electionService.createRunoffElection(
                    parentElectionId = event.electionId,
                    tiedCandidateIds = tiedCandidateIds,
                )

            log.info(
                "재선거 자동 생성 완료: runoffElectionId={}, parentElectionId={}, 후보수={}",
                runoffElection.id,
                event.electionId,
                tiedCandidateIds.size,
            )
        } catch (e: IllegalArgumentException) {
            log.warn(
                "재선거 생성 실패 (최대 횟수 초과): electionId={}, message={}",
                event.electionId,
                e.message,
            )
        } catch (e: Exception) {
            log.error(
                "재선거 생성 중 예외 발생: electionId={}",
                event.electionId,
                e,
            )
        }
    }

    /**
     * 동률 후보자 ID 목록을 조회합니다.
     */
    private fun findTiedCandidateIds(electionId: Long): List<Long> {
        val voteCounts =
            electionVoteRepository.countByElectionIdGroupByCandidateId(electionId)
        if (voteCounts.isEmpty()) return emptyList()

        val maxVotes = voteCounts.values.max()
        return voteCounts
            .filter { it.value == maxVotes }
            .keys
            .toList()
    }
}
