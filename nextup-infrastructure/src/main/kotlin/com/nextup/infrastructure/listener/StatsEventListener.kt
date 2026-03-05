package com.nextup.infrastructure.listener

import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.domain.event.PlateAppearanceRecordedEvent
import com.nextup.core.domain.event.PlateAppearanceUndoneEvent
import com.nextup.core.domain.stats.CareerBattingStats
import com.nextup.core.domain.stats.CareerFieldingStats
import com.nextup.core.domain.stats.CareerPitchingStats
import com.nextup.core.domain.stats.SeasonFieldingStats
import com.nextup.core.domain.stats.SeasonPitchingStats
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.CareerBattingStatsRepositoryPort
import com.nextup.core.port.repository.CareerFieldingStatsRepositoryPort
import com.nextup.core.port.repository.CareerPitchingStatsRepositoryPort
import com.nextup.core.port.repository.FieldingRecordRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.port.repository.SeasonBattingStatsRepositoryPort
import com.nextup.core.port.repository.SeasonFieldingStatsRepositoryPort
import com.nextup.core.port.repository.SeasonPitchingStatsRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 실시간 통계 갱신 이벤트 리스너
 *
 * 경기 중 타석 이벤트를 수신하여 시즌 타격 통계를 즉시 갱신합니다.
 * 경기 종료 이벤트를 수신하여 투수 스탯 및 커리어 스탯을 집계합니다.
 * Infrastructure 계층에 위치하여 Core의 Port를 통해 데이터에 접근합니다.
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
) {
    private val logger = LoggerFactory.getLogger(StatsEventListener::class.java)

    /**
     * 타석 결과 기록 이벤트를 처리합니다.
     *
     * 해당 선수의 시즌 타격 통계를 찾아 실시간으로 갱신합니다.
     * 시즌 통계가 없는 경우 갱신을 건너뜁니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
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

            val existingSeasonPitching =
                seasonPitchingStatsRepository.findByPlayerIdAndYear(playerId, year)
            val isFirstPitchingSeason = existingSeasonPitching == null

            // SeasonPitchingStats 갱신
            val seasonPitchingStats = existingSeasonPitching ?: SeasonPitchingStats.create(player = player, year = year)
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

        // 커리어 타격 스탯 집계: CareerBattingStats
        // SeasonBattingStats는 PlateAppearanceRecordedEvent로 실시간 갱신되므로
        // 해당 시즌 통계 존재 여부로 첫 시즌인지 판단합니다.
        for (battingRecord in battingRecords) {
            val player = battingRecord.gamePlayer.player
            val playerId = player.id

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

        // 수비 스탯 집계: SeasonFieldingStats + CareerFieldingStats
        val fieldingRecords = fieldingRecordRepository.findAllByGameId(gameId)
        for (fieldingRecord in fieldingRecords) {
            val player = fieldingRecord.gamePlayer.player
            val playerId = player.id

            val existingSeasonFielding =
                seasonFieldingStatsRepository.findByPlayerIdAndYear(playerId, year)
            val isFirstFieldingSeason = existingSeasonFielding == null

            // SeasonFieldingStats 갱신
            val seasonFieldingStats =
                existingSeasonFielding ?: SeasonFieldingStats.create(player = player, year = year)
            seasonFieldingStats.addGameRecord(fieldingRecord)
            seasonFieldingStatsRepository.save(seasonFieldingStats)

            // CareerFieldingStats 갱신
            val careerFieldingStats =
                careerFieldingStatsRepository.findByPlayerId(playerId)
                    ?: CareerFieldingStats.create(player = player)

            if (isFirstFieldingSeason) {
                careerFieldingStats.addSeason()
            }
            careerFieldingStats.addGameRecord(fieldingRecord)
            careerFieldingStatsRepository.save(careerFieldingStats)

            logger.debug(
                "수비 통계 갱신 완료 (playerId={}, year={}, isFirstSeason={})",
                playerId,
                year,
                isFirstFieldingSeason,
            )
        }

        logger.info(
            "경기 결과 확정 스탯 집계 완료 (gameId={}, battingRecords={}, pitchingRecords={}, fieldingRecords={})",
            gameId,
            battingRecords.size,
            pitchingRecords.size,
            fieldingRecords.size,
        )
    }

    private fun resolveYear(gameId: Long): Int {
        val game =
            gameRepository.findByIdOrNull(gameId)
                ?: throw GameNotFoundException(gameId)
        return game.scheduledAt.year
    }
}
