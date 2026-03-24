package com.nextup.infrastructure.listener

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.domain.event.PlateAppearanceRecordedEvent
import com.nextup.core.domain.event.PlateAppearanceUndoneEvent
import com.nextup.core.domain.stats.CareerBattingStats
import com.nextup.core.domain.stats.SeasonBattingStats
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.CareerBattingStatsRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.SeasonBattingStatsRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 타격 통계 이벤트 리스너
 *
 * 타석 이벤트를 수신하여 시즌 타격 통계를 실시간으로 갱신하고,
 * 경기 종료 이벤트를 수신하여 커리어 타격 통계를 집계합니다.
 *
 * Optimistic Locking(@Version) 기반 동시성 제어를 적용하여
 * Lost Update를 방지합니다. 충돌 시 최대 3회 재시도합니다.
 */
@Component
class BattingStatsEventListener(
    private val seasonBattingStatsRepository: SeasonBattingStatsRepositoryPort,
    private val careerBattingStatsRepository: CareerBattingStatsRepositoryPort,
    private val battingRecordRepository: BattingRecordRepositoryPort,
    private val gameRepository: GameRepositoryPort,
    private val playerRepository: PlayerRepositoryPort,
) {
    private val logger = LoggerFactory.getLogger(BattingStatsEventListener::class.java)

    /**
     * 타석 결과 기록 이벤트를 처리합니다.
     *
     * 해당 선수의 시즌 타격 통계를 실시간으로 갱신합니다.
     * 시즌 통계가 없는 경우 자동으로 생성합니다.
     * Optimistic Locking 충돌 시 최대 3회 재시도합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onPlateAppearanceRecorded(event: PlateAppearanceRecordedEvent) {
        val (year, competitionType) = resolveGameContext(event.gameId)

        retryOnOptimisticLock("onPlateAppearanceRecorded-batting") {
            val stats =
                findOrCreateSeasonBattingStats(event.playerId, year, event.batterTeamId, competitionType)

            stats.applyLiveUpdate(event.result)
            seasonBattingStatsRepository.save(stats)

            logger.debug(
                "실시간 타격 통계 갱신 완료 (playerId={}, year={}, teamId={}, competitionType={}, result={})",
                event.playerId,
                year,
                event.batterTeamId,
                competitionType,
                event.result,
            )
        }
    }

    /**
     * 타석 결과 취소 이벤트를 처리합니다.
     *
     * 해당 선수의 시즌 타격 통계를 역산합니다.
     * 시즌 통계가 없는 경우 자동으로 생성한 뒤 역산합니다.
     * Optimistic Locking 충돌 시 최대 3회 재시도합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onPlateAppearanceUndone(event: PlateAppearanceUndoneEvent) {
        val (year, competitionType) = resolveGameContext(event.gameId)

        retryOnOptimisticLock("onPlateAppearanceUndone-batting") {
            val stats =
                findOrCreateSeasonBattingStats(event.playerId, year, event.batterTeamId, competitionType)

            stats.revertLiveUpdate(event.result)
            seasonBattingStatsRepository.save(stats)

            logger.debug(
                "실시간 타격 통계 역산 완료 (playerId={}, year={}, teamId={}, competitionType={}, result={})",
                event.playerId,
                year,
                event.batterTeamId,
                competitionType,
                event.result,
            )
        }
    }

    /**
     * 경기 결과 확정 이벤트를 처리합니다 (타격 영역).
     *
     * 커리어 타격 통계를 집계합니다.
     * SeasonBattingStats는 PlateAppearanceRecordedEvent를 통해 실시간 반영되므로
     * 여기서는 처리하지 않습니다.
     *
     * 처리 후 SeasonBattingStats vs BattingRecord 합산값 교차 검증을 수행합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onGameResultConfirmed(event: GameResultConfirmedEvent) {
        val gameId = event.gameId
        val (year, competitionType) = resolveGameContext(gameId)

        val battingRecords = battingRecordRepository.findAllByGameId(gameId)

        if (battingRecords.isEmpty()) {
            logger.debug("타격 기록 없음 - 타격 스탯 집계 생략 (gameId={})", gameId)
            return
        }

        logger.info(
            "타격 스탯 집계 시작 (gameId={}, year={}, competitionType={}, records={})",
            gameId,
            year,
            competitionType,
            battingRecords.size,
        )

        for (battingRecord in battingRecords) {
            val player = battingRecord.gamePlayer.player
            val playerId = player.id
            val teamId = battingRecord.gamePlayer.gameTeam.team.id

            retryOnOptimisticLock("onGameResultConfirmed-batting(playerId=$playerId)") {
                val isFirstBattingSeason =
                    seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                        playerId,
                        year,
                        teamId,
                        competitionType,
                    ) == null

                val careerBattingStats =
                    careerBattingStatsRepository.findByPlayerId(playerId)
                        ?: CareerBattingStats.create(player = player)

                if (isFirstBattingSeason) {
                    careerBattingStats.addSeason()
                }
                careerBattingStats.addGameRecord(battingRecord)
                careerBattingStatsRepository.save(careerBattingStats)

                logger.debug(
                    "커리어 타격 통계 갱신 완료 (playerId={}, teamId={}, competitionType={}, isFirstSeason={})",
                    playerId,
                    teamId,
                    competitionType,
                    isFirstBattingSeason,
                )
            }
        }

        // L-7: SeasonBattingStats vs BattingRecord 합산값 교차 검증
        verifyBattingConsistency(battingRecords, year, gameId)

        logger.info("타격 스탯 집계 완료 (gameId={}, records={})", gameId, battingRecords.size)
    }

    /**
     * 선수의 팀별 시즌 타격 통계를 조회하거나, 없으면 자동 생성합니다.
     */
    private fun findOrCreateSeasonBattingStats(
        playerId: Long,
        year: Int,
        teamId: Long,
        competitionType: CompetitionType,
    ): SeasonBattingStats {
        return seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
            playerId,
            year,
            teamId,
            competitionType,
        )
            ?: run {
                val player =
                    playerRepository.findByIdOrNull(playerId)
                        ?: throw PlayerNotFoundException(playerId)
                logger.info(
                    "시즌 타격 통계 자동 생성 (playerId={}, year={}, teamId={}, competitionType={})",
                    playerId,
                    year,
                    teamId,
                    competitionType,
                )
                val newStats =
                    SeasonBattingStats.create(
                        player = player,
                        year = year,
                        teamId = teamId,
                        competitionType = competitionType,
                    )
                seasonBattingStatsRepository.save(newStats)
            }
    }

    /**
     * L-7: SeasonBattingStats와 BattingRecord 합산값의 교차 검증을 수행합니다.
     */
    private fun verifyBattingConsistency(
        battingRecords: List<com.nextup.core.domain.game.BattingRecord>,
        year: Int,
        gameId: Long,
    ) {
        val playerIds = battingRecords.map { it.gamePlayer.player.id }.distinct()

        for (playerId in playerIds) {
            val seasonStats =
                seasonBattingStatsRepository.findByPlayerIdAndYear(playerId, year) ?: continue

            val allSeasonRecords =
                battingRecordRepository.findAllByPlayerIdAndYear(playerId, year)

            val totalPlateAppearances = allSeasonRecords.sumOf { it.plateAppearances }
            val totalHits = allSeasonRecords.sumOf { it.hits }
            val totalAtBats = allSeasonRecords.sumOf { it.atBats }

            val mismatches =
                seasonStats.verifyConsistency(
                    totalPlateAppearances = totalPlateAppearances,
                    totalHits = totalHits,
                    totalAtBats = totalAtBats,
                )

            if (mismatches.isNotEmpty()) {
                logger.warn(
                    "L-7 타격 통계 정합성 불일치 발견 (gameId={}, playerId={}, year={}): {}",
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
        private val log = LoggerFactory.getLogger(BattingStatsEventListener::class.java)

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
