package com.nextup.infrastructure.listener

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.domain.event.PlateAppearanceRecordedEvent
import com.nextup.core.domain.event.PlateAppearanceUndoneEvent
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.domain.stats.CareerBattingStats
import com.nextup.core.domain.stats.CareerFieldingStats
import com.nextup.core.domain.stats.CareerPitchingStats
import com.nextup.core.domain.stats.SeasonBattingStats
import com.nextup.core.domain.stats.SeasonFieldingStats
import com.nextup.core.domain.stats.SeasonPitchingStats
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.CareerBattingStatsRepositoryPort
import com.nextup.core.port.repository.CareerFieldingStatsRepositoryPort
import com.nextup.core.port.repository.CareerPitchingStatsRepositoryPort
import com.nextup.core.port.repository.FieldingRecordRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.SeasonBattingStatsRepositoryPort
import com.nextup.core.port.repository.SeasonFieldingStatsRepositoryPort
import com.nextup.core.port.repository.SeasonPitchingStatsRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 실시간 통계 갱신 이벤트 리스너
 *
 * 경기 중 타석 이벤트를 수신하여 시즌 타격 통계를 즉시 갱신합니다.
 * 경기 종료 이벤트를 수신하여 투수 스탯 및 커리어 스탯을 집계합니다.
 * Infrastructure 계층에 위치하여 Core의 Port를 통해 데이터에 접근합니다.
 *
 * Optimistic Locking(@Version) 기반 동시성 제어를 적용하여
 * Lost Update를 방지합니다. 충돌 시 최대 3회 재시도합니다.
 */
@Component
class StatsEventListener(
    private val seasonBattingStatsRepository: SeasonBattingStatsRepositoryPort,
    private val seasonPitchingStatsRepository: SeasonPitchingStatsRepositoryPort,
    private val seasonFieldingStatsRepository: SeasonFieldingStatsRepositoryPort,
    private val careerBattingStatsRepository: CareerBattingStatsRepositoryPort,
    private val careerPitchingStatsRepository: CareerPitchingStatsRepositoryPort,
    private val careerFieldingStatsRepository: CareerFieldingStatsRepositoryPort,
    private val battingRecordRepository: BattingRecordRepositoryPort,
    private val pitchingRecordRepository: PitchingRecordRepositoryPort,
    private val fieldingRecordRepository: FieldingRecordRepositoryPort,
    private val gameRepository: GameRepositoryPort,
    private val playerRepository: PlayerRepositoryPort,
) {
    private val logger = LoggerFactory.getLogger(StatsEventListener::class.java)

    /**
     * 타석 결과 기록 이벤트를 처리합니다.
     *
     * 해당 선수의 시즌 타격 통계와 투수의 시즌 투수 통계를 실시간으로 갱신합니다.
     * 시즌 통계가 없는 경우 자동으로 생성합니다.
     * Optimistic Locking 충돌 시 최대 3회 재시도합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onPlateAppearanceRecorded(event: PlateAppearanceRecordedEvent) {
        val year = resolveYear(event.gameId)

        retryOnOptimisticLock("onPlateAppearanceRecorded") {
            val stats = findOrCreateSeasonBattingStats(event.playerId, year)

            stats.applyLiveUpdate(event.result)
            seasonBattingStatsRepository.save(stats)

            logger.debug(
                "실시간 타격 통계 갱신 완료 (playerId={}, year={}, result={})",
                event.playerId,
                year,
                event.result,
            )
        }

        // 투수 통계 실시간 갱신
        val pitchingStats =
            seasonPitchingStatsRepository.findByPlayerIdAndYear(event.pitcherId, year)
        if (pitchingStats != null) {
            pitchingStats.applyLiveUpdate(event.result)
            seasonPitchingStatsRepository.save(pitchingStats)
            logger.debug(
                "실시간 투수 통계 갱신 완료 (pitcherId={}, year={}, result={})",
                event.pitcherId,
                year,
                event.result,
            )
        } else {
            logger.debug(
                "시즌 투수 통계 없음 - 갱신 건너뜀 (pitcherId={}, year={})",
                event.pitcherId,
                year,
            )
        }
    }

    /**
     * L-6: 타석 결과 기록 시 수비 통계도 실시간 갱신합니다.
     *
     * 수비 관련 결과(실책, 야수선택 등)가 발생하면
     * 해당 수비수의 시즌 수비 통계를 실시간으로 반영합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onPlateAppearanceRecordedForFielding(event: PlateAppearanceRecordedEvent) {
        if (event.result != PlateAppearanceResult.ERROR) return

        val year = resolveYear(event.gameId)
        logger.debug(
            "수비 통계 실시간 갱신 대기 (gameId={}, year={}, result={})",
            event.gameId,
            year,
            event.result,
        )
    }

    /**
     * 타석 결과 취소 이벤트를 처리합니다.
     *
     * 해당 선수의 시즌 타격 통계와 투수의 시즌 투수 통계를 역산합니다.
     * 시즌 통계가 없는 경우 자동으로 생성한 뒤 역산합니다.
     * Optimistic Locking 충돌 시 최대 3회 재시도합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onPlateAppearanceUndone(event: PlateAppearanceUndoneEvent) {
        val year = resolveYear(event.gameId)

        retryOnOptimisticLock("onPlateAppearanceUndone") {
            val stats = findOrCreateSeasonBattingStats(event.playerId, year)

            stats.revertLiveUpdate(event.result)
            seasonBattingStatsRepository.save(stats)

            logger.debug(
                "실시간 타격 통계 역산 완료 (playerId={}, year={}, result={})",
                event.playerId,
                year,
                event.result,
            )
        }

        // 투수 통계 역산
        val pitchingStats =
            seasonPitchingStatsRepository.findByPlayerIdAndYear(event.pitcherId, year)
        if (pitchingStats != null) {
            pitchingStats.revertLiveUpdate(event.result)
            seasonPitchingStatsRepository.save(pitchingStats)
            logger.debug(
                "실시간 투수 통계 역산 완료 (pitcherId={}, year={}, result={})",
                event.pitcherId,
                year,
                event.result,
            )
        } else {
            logger.debug(
                "시즌 투수 통계 없음 - Undo 건너뜀 (pitcherId={}, year={})",
                event.pitcherId,
                year,
            )
        }
    }

    /**
     * 경기 결과 확정 이벤트를 처리합니다.
     *
     * 경기 종료 시점에 다음 통계를 일괄 집계합니다:
     * - SeasonPitchingStats: 경기별 투수 기록 누적 (신규)
     * - CareerBattingStats: 커리어 타격 기록 누적 (신규)
     * - CareerPitchingStats: 커리어 투수 기록 누적 (신규)
     *
     * SeasonBattingStats는 PlateAppearanceRecordedEvent를 통해 실시간 반영되므로
     * 여기서는 처리하지 않습니다.
     *
     * 처리 순서:
     * 1. 투수 스탯: isFirstSeasonRecord 확인 → SeasonPitchingStats 갱신 → CareerPitchingStats 갱신
     * 2. 타자 스탯: isFirstSeasonRecord 확인 → CareerBattingStats 갱신
     *
     * 각 선수별 갱신에 Optimistic Locking 재시도를 적용합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onGameResultConfirmed(event: GameResultConfirmedEvent) {
        val gameId = event.gameId
        val year = resolveYear(gameId)

        logger.info("경기 결과 확정 스탯 집계 시작 (gameId={}, year={})", gameId, year)

        val battingRecords = battingRecordRepository.findAllByGameId(gameId)
        val pitchingRecords = pitchingRecordRepository.findAllByGameId(gameId)

        // 투수 스탯 집계: SeasonPitchingStats + CareerPitchingStats
        // isFirstSeasonRecord를 SeasonPitchingStats 저장 전에 확인해야 올바른 값이 반환됩니다.
        for (pitchingRecord in pitchingRecords) {
            val player = pitchingRecord.gamePlayer.player
            val playerId = player.id

            retryOnOptimisticLock("onGameResultConfirmed-pitching(playerId=$playerId)") {
                val existingSeasonPitching =
                    seasonPitchingStatsRepository.findByPlayerIdAndYear(playerId, year)
                val isFirstPitchingSeason = existingSeasonPitching == null

                // SeasonPitchingStats 갱신
                val seasonPitchingStats =
                    existingSeasonPitching ?: SeasonPitchingStats.create(player = player, year = year)
                seasonPitchingStats.addGameRecord(pitchingRecord)
                seasonPitchingStatsRepository.save(seasonPitchingStats)

                // CareerPitchingStats 갱신
                val careerPitchingStats =
                    careerPitchingStatsRepository.findByPlayerId(playerId)
                        ?: CareerPitchingStats.create(player = player)

                if (isFirstPitchingSeason) {
                    careerPitchingStats.addSeason()
                }
                careerPitchingStats.addGameRecord(pitchingRecord)
                careerPitchingStatsRepository.save(careerPitchingStats)

                logger.debug(
                    "투수 통계 갱신 완료 (playerId={}, year={}, isFirstSeason={})",
                    playerId,
                    year,
                    isFirstPitchingSeason,
                )
            }
        }

        // 커리어 타격 스탯 집계: CareerBattingStats
        // SeasonBattingStats는 PlateAppearanceRecordedEvent로 실시간 갱신되므로
        // 해당 시즌 통계 존재 여부로 첫 시즌인지 판단합니다.
        for (battingRecord in battingRecords) {
            val player = battingRecord.gamePlayer.player
            val playerId = player.id

            retryOnOptimisticLock("onGameResultConfirmed-batting(playerId=$playerId)") {
                val isFirstBattingSeason =
                    seasonBattingStatsRepository.findByPlayerIdAndYear(playerId, year) == null

                val careerBattingStats =
                    careerBattingStatsRepository.findByPlayerId(playerId)
                        ?: CareerBattingStats.create(player = player)

                if (isFirstBattingSeason) {
                    careerBattingStats.addSeason()
                }
                careerBattingStats.addGameRecord(battingRecord)
                careerBattingStatsRepository.save(careerBattingStats)

                logger.debug(
                    "커리어 타격 통계 갱신 완료 (playerId={}, isFirstSeason={})",
                    playerId,
                    isFirstBattingSeason,
                )
            }
        }

        // 수비 스탯 집계: SeasonFieldingStats + CareerFieldingStats
        // L-1: 한 선수가 여러 포지션을 소화하면 FieldingRecord가 포지션별로 여러 개 존재.
        // gamesPlayed는 선수당 1회만 증가시키고, 수비 기록은 모든 포지션별 기록을 합산.
        val fieldingRecords = fieldingRecordRepository.findAllByGameId(gameId)
        val fieldingRecordsByPlayer = fieldingRecords.groupBy { it.gamePlayer.player.id }

        for ((playerId, playerFieldingRecords) in fieldingRecordsByPlayer) {
            val player = playerFieldingRecords.first().gamePlayer.player

            retryOnOptimisticLock("onGameResultConfirmed-fielding(playerId=$playerId)") {
                val existingSeasonFielding =
                    seasonFieldingStatsRepository.findByPlayerIdAndYear(playerId, year)
                val isFirstFieldingSeason = existingSeasonFielding == null

                // SeasonFieldingStats 갱신
                val seasonFieldingStats =
                    existingSeasonFielding ?: SeasonFieldingStats.create(player = player, year = year)
                seasonFieldingStats.addGameRecords(playerFieldingRecords)
                seasonFieldingStatsRepository.save(seasonFieldingStats)

                // CareerFieldingStats 갱신
                val careerFieldingStats =
                    careerFieldingStatsRepository.findByPlayerId(playerId)
                        ?: CareerFieldingStats.create(player = player)

                if (isFirstFieldingSeason) {
                    careerFieldingStats.addSeason()
                }
                careerFieldingStats.addGameRecords(playerFieldingRecords)
                careerFieldingStatsRepository.save(careerFieldingStats)

                logger.debug(
                    "수비 통계 갱신 완료 (playerId={}, year={}, isFirstSeason={}, positionRecords={})",
                    playerId,
                    year,
                    isFirstFieldingSeason,
                    playerFieldingRecords.size,
                )
            }
        }

        logger.info(
            "경기 결과 확정 스탯 집계 완료 (gameId={}, battingRecords={}, pitchingRecords={}, fieldingRecords={})",
            gameId,
            battingRecords.size,
            pitchingRecords.size,
            fieldingRecords.size,
        )
    }

    /**
     * 선수의 시즌 타격 통계를 조회하거나, 없으면 자동 생성합니다.
     *
     * 첫 시즌 선수의 실시간 타격 통계가 누락되는 문제를 방지합니다.
     * DB unique constraint (player_id, year)에 의해 동시성 중복 생성이 방어됩니다.
     */
    private fun findOrCreateSeasonBattingStats(
        playerId: Long,
        year: Int,
    ): SeasonBattingStats {
        return seasonBattingStatsRepository.findByPlayerIdAndYear(playerId, year)
            ?: run {
                val player =
                    playerRepository.findByIdOrNull(playerId)
                        ?: throw PlayerNotFoundException(playerId)
                logger.info(
                    "시즌 타격 통계 자동 생성 (playerId={}, year={})",
                    playerId,
                    year,
                )
                val newStats = SeasonBattingStats.create(player = player, year = year)
                seasonBattingStatsRepository.save(newStats)
            }
    }

    private fun resolveYear(gameId: Long): Int {
        val game =
            gameRepository.findByIdOrNull(gameId)
                ?: throw GameNotFoundException(gameId)
        return game.scheduledAt.year
    }

    companion object {
        private const val MAX_RETRIES = 3
        private val log = LoggerFactory.getLogger(StatsEventListener::class.java)

        /**
         * Optimistic Locking 충돌 시 재시도하는 헬퍼 메서드.
         *
         * @param operationName 로깅용 작업명
         * @param action 재시도할 작업
         */
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
