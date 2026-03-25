package com.nextup.infrastructure.scheduler

import com.nextup.core.domain.game.GameStatus
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.service.attendance.AttendanceService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 출석 투표 자동 생성/마감 스케줄러
 *
 * 1. 매일 새벽 2시: 1주일 이내 예정된 경기에 대해 출석 투표 자동 생성
 * 2. 매일 새벽 2시 30분: 마감 기한이 지난 OPEN 투표를 자동 마감
 */
@Component
class AttendancePollScheduler(
    private val attendanceService: AttendanceService,
    private val gameRepository: GameRepositoryPort,
) {
    private val logger = LoggerFactory.getLogger(AttendancePollScheduler::class.java)

    /**
     * 1주일 이내 예정된 경기에 대해 출석 투표를 자동 생성합니다.
     * 매일 새벽 2시에 실행됩니다.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    fun createPollsForUpcomingGames() {
        val now = LocalDateTime.now()
        val oneWeekLater = now.plusWeeks(1)

        logger.info("출석 투표 자동 생성 시작 (대상: {} ~ {})", now, oneWeekLater)

        val scheduledGames =
            gameRepository.findByScheduledAtBetween(now, oneWeekLater)
                .filter { it.status == GameStatus.SCHEDULED }

        if (scheduledGames.isEmpty()) {
            logger.debug("1주일 이내 예정된 경기 없음")
            return
        }

        var createdCount = 0
        for (game in scheduledGames) {
            try {
                val polls = attendanceService.createPollsForGame(game.id)
                createdCount += polls.size
            } catch (e: Exception) {
                logger.error("경기 {} 출석 투표 생성 실패: {}", game.id, e.message)
            }
        }

        logger.info("출석 투표 자동 생성 완료 (생성: {}건)", createdCount)
    }

    /**
     * 마감 기한이 지난 OPEN 투표를 자동 마감합니다.
     * 매일 새벽 2시 30분에 실행됩니다.
     */
    @Scheduled(cron = "0 30 2 * * *")
    @Transactional
    fun closeExpiredPolls() {
        logger.info("만료된 출석 투표 자동 마감 시작")

        val closedCount = attendanceService.closeExpiredPolls()

        logger.info("만료된 출석 투표 자동 마감 완료 (마감: {}건)", closedCount)
    }
}
