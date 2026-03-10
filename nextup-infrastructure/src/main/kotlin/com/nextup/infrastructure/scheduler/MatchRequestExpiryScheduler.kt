package com.nextup.infrastructure.scheduler

import com.nextup.core.domain.match.MatchResponseStatus
import com.nextup.core.port.repository.MatchRequestRepositoryPort
import com.nextup.core.port.repository.MatchResponseRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 매칭 요청 만료 자동 처리 스케줄러
 *
 * 매일 새벽 3시에 실행되어 30일이 경과한 OPEN 상태의
 * 매칭 요청을 EXPIRED로 전환하고, 관련 PENDING 응답을 REJECTED로 처리합니다.
 */
@Component
class MatchRequestExpiryScheduler(
    private val matchRequestRepository: MatchRequestRepositoryPort,
    private val matchResponseRepository: MatchResponseRepositoryPort,
) {
    private val logger = LoggerFactory.getLogger(MatchRequestExpiryScheduler::class.java)

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    fun expireMatchRequests() {
        val openRequests = matchRequestRepository.findAllOpen()
        val expiredRequests = openRequests.filter { it.isExpired() }

        if (expiredRequests.isEmpty()) {
            logger.debug("만료 대상 매칭 요청 없음")
            return
        }

        logger.info("매칭 요청 만료 처리 시작 (대상: {}건)", expiredRequests.size)

        var expiredCount = 0
        var rejectedResponseCount = 0

        for (request in expiredRequests) {
            request.expire()
            matchRequestRepository.save(request)
            expiredCount++

            val pendingResponses =
                matchResponseRepository.findByMatchRequestId(request.id)
                    .filter { it.status == MatchResponseStatus.PENDING }

            for (response in pendingResponses) {
                response.reject()
                matchResponseRepository.save(response)
                rejectedResponseCount++
            }
        }

        logger.info(
            "매칭 요청 만료 처리 완료 (만료 요청: {}건, 거절 응답: {}건)",
            expiredCount,
            rejectedResponseCount,
        )
    }
}
