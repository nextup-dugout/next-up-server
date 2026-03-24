package com.nextup.infrastructure.scheduler

import com.nextup.core.port.repository.GameRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 기록원 잠금 자동 만료 스케줄러
 *
 * 5분마다 실행되어 30분 이상 경과한 기록원 잠금을 자동으로 해제합니다.
 * lockForScorer() 후 앱 종료 시 발생하는 영구 잠금 문제를 방지합니다.
 */
@Component
class ScorerLockExpirationScheduler(
    private val gameRepository: GameRepositoryPort,
) {
    private val logger = LoggerFactory.getLogger(ScorerLockExpirationScheduler::class.java)

    @Scheduled(fixedRate = 300000)
    @Transactional
    fun expireLockedGames() {
        val threshold = LocalDateTime.now().minusMinutes(LOCK_TIMEOUT_MINUTES)
        val expiredGames = gameRepository.findLockedGamesBefore(threshold)

        if (expiredGames.isEmpty()) {
            logger.debug("만료 대상 기록원 잠금 없음")
            return
        }

        expiredGames.forEach { game ->
            logger.info(
                "기록원 잠금 자동 만료: gameId={}, scorerId={}, lockedAt={}",
                game.id,
                game.scorerLock.scorerId,
                game.scorerLock.lockedAt,
            )
            game.expireLock()
            gameRepository.save(game)
        }

        logger.info("총 {}건의 기록원 잠금이 자동 만료됨", expiredGames.size)
    }

    companion object {
        const val LOCK_TIMEOUT_MINUTES = 30L
    }
}
