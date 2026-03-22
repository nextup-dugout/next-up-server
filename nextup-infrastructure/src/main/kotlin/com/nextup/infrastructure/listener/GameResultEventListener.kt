package com.nextup.infrastructure.listener

import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.event.CompetitionCompletedEvent
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.infrastructure.config.CacheConfig
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 경기 결과 확정 이벤트 리스너
 *
 * GameResultConfirmedEvent 수신 시:
 * 1. 해당 대회의 순위 캐시를 무효화합니다.
 * 2. 대회의 모든 경기가 완료되었으면 Competition.complete()를 자동 호출합니다.
 */
@Component
class GameResultEventListener(
    private val gameRepository: GameRepositoryPort,
    private val competitionRepository: CompetitionRepositoryPort,
    private val cacheManager: CacheManager,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val logger = LoggerFactory.getLogger(GameResultEventListener::class.java)

    /**
     * 경기 결과 확정 이벤트를 처리합니다.
     *
     * BEFORE_COMMIT 단계에서 실행하여 대회 자동 완료를 동일 트랜잭션 내에서 처리합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun onGameResultConfirmed(event: GameResultConfirmedEvent) {
        val game =
            gameRepository.findByIdOrNull(event.gameId)
                ?: throw GameNotFoundException(event.gameId)

        val competition = game.competition
        val competitionId = competition.id

        logger.debug(
            "경기 결과 확정 이벤트 수신 (gameId={}, competitionId={})",
            event.gameId,
            competitionId,
        )

        // 대회 자동 완료 처리
        if (competition.status == CompetitionStatus.IN_PROGRESS) {
            val totalGames = gameRepository.countByCompetitionId(competitionId)
            val completedOrCancelledGames =
                gameRepository.countCompletedOrCancelledByCompetitionId(competitionId)

            logger.debug(
                "대회 경기 현황 (competitionId={}, total={}, completedOrCancelled={})",
                competitionId,
                totalGames,
                completedOrCancelledGames,
            )

            if (totalGames > 0 && totalGames == completedOrCancelledGames) {
                competition.complete()
                competitionRepository.save(competition)
                logger.info(
                    "대회 자동 완료 처리 (competitionId={}, name={})",
                    competitionId,
                    competition.name,
                )
                eventPublisher.publishEvent(
                    CompetitionCompletedEvent(
                        competitionId = competitionId,
                        competitionName = competition.name,
                        leagueId = competition.league.id,
                    ),
                )
            }
        }
    }

    /**
     * 경기 결과 확정 후 순위 캐시를 무효화합니다.
     *
     * AFTER_COMMIT 단계에서 실행하여 DB 커밋 이후 캐시를 제거합니다.
     * competitionId 기준으로 캐시 키를 직접 제거합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun evictStandingsCache(event: GameResultConfirmedEvent) {
        val game =
            gameRepository.findByIdOrNull(event.gameId)
                ?: run {
                    logger.warn("캐시 무효화 중 경기를 찾을 수 없음 (gameId={})", event.gameId)
                    return
                }

        val competitionId = game.competition.id
        evictStatsCaches(competitionId)

        logger.debug(
            "캐시 무효화 완료 (competitionId={}, gameId={})",
            competitionId,
            event.gameId,
        )
    }

    /**
     * 순위, 리더보드, 팀 통계 캐시를 일괄 무효화합니다.
     */
    private fun evictStatsCaches(competitionId: Long) {
        cacheManager.getCache(CacheConfig.STANDINGS_CACHE)?.evict(competitionId)
        cacheManager.getCache(CacheConfig.LEADERBOARD_CACHE)?.clear()
        cacheManager.getCache(CacheConfig.TEAM_STATS_CACHE)?.clear()
    }
}
