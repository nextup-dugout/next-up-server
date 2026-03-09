package com.nextup.infrastructure.scheduler

import com.nextup.core.port.repository.MatchRequestRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 매치 요청 만료 자동 처리 스케줄러
 *
 * 매일 새벽 3시에 실행되어, 30일이 경과한 OPEN 상태의 매치 요청을
 * 자동으로 EXPIRED 상태로 전환합니다.
 */
@Component
class MatchRequestExpirationScheduler(
    private val matchRequestRepository: MatchRequestRepositoryPort,
) {
    private val logger = LoggerFactory.getLogger(MatchRequestExpirationScheduler::class.java)

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    fun expireOldMatchRequests() {
        val openRequests = matchRequestRepository.findAllOpen()
        val expiredRequests = openRequests.filter { it.isExpired() }

        if (expiredRequests.isEmpty()) {
            logger.debug("만료 대상 매치 요청 없음")
            return
        }

        expiredRequests.forEach { request ->
            request.expire()
            matchRequestRepository.save(request)
        }

        logger.info("매치 요청 만료 처리 완료: {}건", expiredRequests.size)
    }
}
