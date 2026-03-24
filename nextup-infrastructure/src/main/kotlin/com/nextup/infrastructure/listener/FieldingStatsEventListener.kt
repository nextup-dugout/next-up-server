package com.nextup.infrastructure.listener

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.event.FieldingRecordUpdatedEvent
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.domain.stats.CareerFieldingStats
import com.nextup.core.domain.stats.SeasonFieldingStats
import com.nextup.core.port.repository.CareerFieldingStatsRepositoryPort
import com.nextup.core.port.repository.FieldingRecordRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.SeasonFieldingStatsRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 수비 통계 이벤트 리스너
 *
 * 수비 기록 이벤트를 수신하여 시즌 수비 통계를 실시간으로 갱신하고,
 * 경기 종료 이벤트를 수신하여 시즌/커리어 수비 통계를 집계합니다.
 *
 * Optimistic Locking(@Version) 기반 동시성 제어를 적용하여
 * Lost Update를 방지합니다. 충돌 시 최대 3회 재시도합니다.
 */
@Component
class FieldingStatsEventListener(
    private val seasonFieldingStatsRepository: SeasonFieldingStatsRepositoryPort,
    private val careerFieldingStatsRepository: CareerFieldingStatsRepositoryPort,
    private val fieldingRecordRepository: FieldingRecordRepositoryPort,
    private val gameRepository: GameRepositoryPort,
    private val playerRepository: PlayerRepositoryPort,
) {
    private val logger = LoggerFactory.getLogger(FieldingStatsEventListener::class.java)

    /**
     * 수비 기록 갱신 이벤트를 처리합니다.
     *
     * 경기 중 수비 기록(자살, 보살, 실책 등)이 발생하면
     * 해당 수비수의 시즌 수비 통계를 실시간으로 갱신합니다.
     * 시즌 통계가 없는 경우 자동으로 생성합니다.
     * Optimistic Locking 충돌 시 최대 3회 재시도합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onFieldingRecordUpdated(event: FieldingRecordUpdatedEvent) {
        val (year, competitionType) = resolveGameContext(event.gameId)

        retryOnOptimisticLock("onFieldingRecordUpdated") {
            val stats = findOrCreateSeasonFieldingStats(event.playerId, year, competitionType)

            if (event.isRevert) {
                stats.revertLiveFieldingUpdate(event.type)
            } else {
                stats.applyLiveFieldingUpdate(event.type)
            }
            seasonFieldingStatsRepository.save(stats)

            logger.debug(
                "실시간 수비 통계 {} 완료 (playerId={}, year={}, competitionType={}, type={})",
                if (event.isRevert) "역산" else "갱신",
                event.playerId,
                year,
                competitionType,
                event.type,
            )
        }
    }

    /**
     * 경기 결과 확정 이벤트를 처리합니다 (수비 영역).
     *
     * 경기 종료 시점에 시즌/커리어 수비 통계를 집계합니다.
     * 한 선수가 여러 포지션을 소화하면 FieldingRecord가 포지션별로 여러 개 존재.
     * gamesPlayed는 선수당 1회만 증가시키고, 수비 기록은 모든 포지션별 기록을 합산.
     *
     * 처리 후 SeasonFieldingStats vs FieldingRecord 합산값 교차 검증을 수행합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onGameResultConfirmed(event: GameResultConfirmedEvent) {
        val gameId = event.gameId
        val (year, competitionType) = resolveGameContext(gameId)

        val fieldingRecords = fieldingRecordRepository.findAllByGameId(gameId)

        if (fieldingRecords.isEmpty()) {
            logger.debug("수비 기록 없음 - 수비 스탯 집계 생략 (gameId={})", gameId)
            return
        }

        logger.info("수비 스탯 집계 시작 (gameId={}, year={}, competitionType={}, records={})", gameId, year, competitionType, fieldingRecords.size)

        val fieldingRecordsByPlayer = fieldingRecords.groupBy { it.gamePlayer.player.id }

        for ((playerId, playerFieldingRecords) in fieldingRecordsByPlayer) {
            val player = playerFieldingRecords.first().gamePlayer.player

            retryOnOptimisticLock("onGameResultConfirmed-fielding(playerId=$playerId)") {
                val existingSeasonFielding =
                    seasonFieldingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                        playerId,
                        year,
                        null,
                        competitionType,
                    )
                val isFirstFieldingSeason = existingSeasonFielding == null

                val seasonFieldingStats =
                    existingSeasonFielding
                        ?: SeasonFieldingStats.create(
                            player = player,
                            year = year,
                            competitionType = competitionType,
                        )
                seasonFieldingStats.addGameRecords(playerFieldingRecords)
                seasonFieldingStatsRepository.save(seasonFieldingStats)

                val careerFieldingStats =
                    careerFieldingStatsRepository.findByPlayerId(playerId)
                        ?: CareerFieldingStats.create(player = player)

                if (isFirstFieldingSeason) {
                    careerFieldingStats.addSeason()
                }
                careerFieldingStats.addGameRecords(playerFieldingRecords)
                careerFieldingStatsRepository.save(careerFieldingStats)

                logger.debug(
                    "수비 통계 갱신 완료 (playerId={}, year={}, competitionType={}, isFirstSeason={}, positionRecords={})",
                    playerId,
                    year,
                    competitionType,
                    isFirstFieldingSeason,
                    playerFieldingRecords.size,
                )
            }
        }

        // L-7: SeasonFieldingStats vs FieldingRecord 합산값 교차 검증
        verifyFieldingConsistency(fieldingRecords, year, gameId)

        logger.info("수비 스탯 집계 완료 (gameId={}, records={})", gameId, fieldingRecords.size)
    }

    /**
     * 선수의 시즌 수비 통계를 조회하거나, 없으면 자동 생성합니다.
     */
    private fun findOrCreateSeasonFieldingStats(
        playerId: Long,
        year: Int,
        competitionType: CompetitionType,
    ): SeasonFieldingStats {
        return seasonFieldingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
            playerId,
            year,
            null,
            competitionType,
        )
            ?: run {
                val player =
                    playerRepository.findByIdOrNull(playerId)
                        ?: throw PlayerNotFoundException(playerId)
                logger.info(
                    "시즌 수비 통계 자동 생성 (playerId={}, year={}, competitionType={})",
                    playerId,
                    year,
                    competitionType,
                )
                val newStats =
                    SeasonFieldingStats.create(
                        player = player,
                        year = year,
                        competitionType = competitionType,
                    )
                seasonFieldingStatsRepository.save(newStats)
            }
    }

    /**
     * L-7: SeasonFieldingStats와 FieldingRecord 합산값의 교차 검증을 수행합니다.
     */
    private fun verifyFieldingConsistency(
        fieldingRecords: List<com.nextup.core.domain.game.FieldingRecord>,
        year: Int,
        gameId: Long,
    ) {
        val playerIds = fieldingRecords.map { it.gamePlayer.player.id }.distinct()

        for (playerId in playerIds) {
            val seasonStats =
                seasonFieldingStatsRepository.findByPlayerIdAndYear(playerId, year) ?: continue

            val allSeasonRecords =
                fieldingRecordRepository.findAllByPlayerIdAndYear(playerId, year)

            val totalPutOuts = allSeasonRecords.sumOf { it.putOuts }
            val totalAssists = allSeasonRecords.sumOf { it.assists }
            val totalErrors = allSeasonRecords.sumOf { it.errors }

            val mismatches =
                seasonStats.verifyConsistency(
                    totalPutOuts = totalPutOuts,
                    totalAssists = totalAssists,
                    totalErrors = totalErrors,
                )

            if (mismatches.isNotEmpty()) {
                logger.warn(
                    "L-7 수비 통계 정합성 불일치 발견 (gameId={}, playerId={}, year={}): {}",
                    gameId,
                    playerId,
                    year,
                    mismatches.joinToString("; "),
                )
            }
        }
    }

    private fun resolveGameContext(gameId: Long): Pair<Int, CompetitionType> {
        val game =
            gameRepository.findByIdOrNull(gameId)
                ?: throw GameNotFoundException(gameId)
        return Pair(game.scheduledAt.year, game.competition.type)
    }

    companion object {
        private const val MAX_RETRIES = 3
        private val log = LoggerFactory.getLogger(FieldingStatsEventListener::class.java)

        internal fun retryOnOptimisticLock(
            operationName: String,
            action: () -> Unit,
        ) {
            var lastException: ObjectOptimisticLockingFailureException? = null
            for (attempt in 1..MAX_RETRIES) {
                try {
                    action()
                    return
                } catch (ex: ObjectOptimisticLockingFailureException) {
                    lastException = ex
                    log.warn(
                        "Optimistic Locking 충돌 발생 - 재시도 {}/{} (operation={})",
                        attempt,
                        MAX_RETRIES,
                        operationName,
                    )
                }
            }
            log.error(
                "Optimistic Locking 재시도 초과 (operation={}, maxRetries={})",
                operationName,
                MAX_RETRIES,
            )
            throw lastException!!
        }
    }
}
