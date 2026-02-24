package com.nextup.infrastructure.listener

import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.domain.event.PlateAppearanceRecordedEvent
import com.nextup.core.domain.event.PlateAppearanceUndoneEvent
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.SeasonBattingStatsRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 실시간 통계 갱신 이벤트 리스너
 *
 * 경기 중 타석 이벤트를 수신하여 시즌 타격 통계를 즉시 갱신합니다.
 * Infrastructure 계층에 위치하여 Core의 Port를 통해 데이터에 접근합니다.
 */
@Component
class StatsEventListener(
    private val seasonBattingStatsRepository: SeasonBattingStatsRepositoryPort,
    private val gameRepository: GameRepositoryPort,
) {
    private val logger = LoggerFactory.getLogger(StatsEventListener::class.java)

    /**
     * 타석 결과 기록 이벤트를 처리합니다.
     *
     * 해당 선수의 시즌 타격 통계를 찾아 실시간으로 갱신합니다.
     * 시즌 통계가 없는 경우 갱신을 건너뜁니다.
     */
    @EventListener
    @Transactional
    fun onPlateAppearanceRecorded(event: PlateAppearanceRecordedEvent) {
        val year = resolveYear(event.gameId)
        val stats =
            seasonBattingStatsRepository.findByPlayerIdAndYear(event.playerId, year)
                ?: run {
                    logger.debug(
                        "시즌 타격 통계 없음 - 갱신 건너뜀 (playerId={}, year={})",
                        event.playerId,
                        year,
                    )
                    return
                }

        stats.applyLiveUpdate(event.result)
        seasonBattingStatsRepository.save(stats)

        logger.debug(
            "실시간 타격 통계 갱신 완료 (playerId={}, year={}, result={})",
            event.playerId,
            year,
            event.result,
        )
    }

    /**
     * 타석 결과 취소 이벤트를 처리합니다.
     *
     * 해당 선수의 시즌 타격 통계를 찾아 이전 타석 결과를 역산합니다.
     * 시즌 통계가 없는 경우 갱신을 건너뜁니다.
     */
    @EventListener
    @Transactional
    fun onPlateAppearanceUndone(event: PlateAppearanceUndoneEvent) {
        val year = resolveYear(event.gameId)
        val stats =
            seasonBattingStatsRepository.findByPlayerIdAndYear(event.playerId, year)
                ?: run {
                    logger.debug(
                        "시즌 타격 통계 없음 - Undo 건너뜀 (playerId={}, year={})",
                        event.playerId,
                        year,
                    )
                    return
                }

        stats.revertLiveUpdate(event.result)
        seasonBattingStatsRepository.save(stats)

        logger.debug(
            "실시간 타격 통계 역산 완료 (playerId={}, year={}, result={})",
            event.playerId,
            year,
            event.result,
        )
    }

    private fun resolveYear(gameId: Long): Int {
        val game =
            gameRepository.findByIdOrNull(gameId)
                ?: throw GameNotFoundException(gameId)
        return game.scheduledAt.year
    }
}
