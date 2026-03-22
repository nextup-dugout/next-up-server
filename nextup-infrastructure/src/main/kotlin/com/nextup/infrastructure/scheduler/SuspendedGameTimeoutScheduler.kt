package com.nextup.infrastructure.scheduler

import com.nextup.core.domain.game.GameStatus
import com.nextup.core.port.repository.GameRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * L-11: SUSPENDED 경기 자동 타임아웃 스케줄러
 *
 * 1시간마다 실행되어 SUSPENDED 상태가 48시간 이상 경과한 경기를 자동으로 CANCELLED 전환합니다.
 * Game 엔티티의 isSuspendedTimeout()과 cancelByTimeout() 메서드를 활용합니다.
 */
@Component
class SuspendedGameTimeoutScheduler(
    private val gameRepository: GameRepositoryPort,
) {
    private val logger = LoggerFactory.getLogger(SuspendedGameTimeoutScheduler::class.java)

    @Scheduled(fixedRate = CHECK_INTERVAL_MS)
    @Transactional
    fun cancelTimedOutSuspendedGames() {
        val suspendedGames = gameRepository.findByStatus(GameStatus.SUSPENDED)

        if (suspendedGames.isEmpty()) {
            logger.debug("SUSPENDED 상태 경기 없음")
            return
        }

        val now = Instant.now()
        var cancelledCount = 0

        suspendedGames.forEach { game ->
            if (game.isSuspendedTimeout(SUSPENDED_TIMEOUT_HOURS, now)) {
                logger.info(
                    "SUSPENDED 경기 타임아웃 자동 취소: gameId={}, updatedAt={}",
                    game.id,
                    game.updatedAt,
                )
                game.cancelByTimeout()
                gameRepository.save(game)
                cancelledCount++
            }
        }

        if (cancelledCount > 0) {
            logger.info("총 {}건의 SUSPENDED 경기가 타임아웃으로 자동 취소됨", cancelledCount)
        }
    }

    companion object {
        /** 타임아웃 기준 시간 (48시간) */
        const val SUSPENDED_TIMEOUT_HOURS = 48L

        /** 스케줄러 실행 주기 (1시간 = 3,600,000ms) */
        const val CHECK_INTERVAL_MS = 3_600_000L
    }
}
