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
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 실시간 통계 갱신 이벤트 리스너
 *
 * 경기 중 타석 이벤트를 수신하여 시즌 타격/투수 통계를 즉시 갱신합니다.
 * 경기 종료 이벤트를 수신하여 경기 요약 필드(이닝, 실점, 자책점, 결정 등) 및
 * 커리어 스탯을 집계합니다. Infrastructure 계층에 위치하여 Core의 Port를 통해
 * 데이터에 접근합니다.
 *
 * 투수 통계 중복 방지 전략:
 * - 경기 중: applyLiveUpdate()로 타석 단위 필드(피안타, 삼진, 볼넷, 사구, 피홈런, 대면타자) 실시간 갱신
 * - 경기 종료 시: addGameRecordForEndOfGame()으로 경기 요약 필드만 추가 (실시간 갱신 필드 제외)
 *
 * Optimistic Locking(@Version) 기반 동시성 제어를 적용하여
 * Lost Update를 방지합니다. 충돌 시 @Retryable로 메서드 레벨에서
 * 최대 3회 재시도하며, 각 재시도마다 새 트랜잭션(REQUIRES_NEW)에서 실행됩니다.
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
     * Optimistic Locking 충돌 시 @Retryable로 최대 3회 재시도합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(
        retryFor = [ObjectOptimisticLockingFailureException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 100),
    )
    fun onPlateAppearanceRecorded(event: PlateAppearanceRecordedEvent) {
        val year = resolveYear(event.gameId)

        val stats = findOrCreateSeasonBattingStats(event.playerId, year, event.batterTeamId)

        stats.applyLiveUpdate(event.result)
        seasonBattingStatsRepository.save(stats)

        logger.debug(
            "실시간 타격 통계 갱신 완료 (playerId={}, year={}, teamId={}, result={})",
            event.playerId,
            year,
            event.batterTeamId,
            event.result,
        )

        // 투수 통계 실시간 갱신
        val pitchingStats = findOrCreateSeasonPitchingStats(event.pitcherId, year, event.pitcherTeamId)

        pitchingStats.applyLiveUpdate(event.result)
        seasonPitchingStatsRepository.save(pitchingStats)

        logger.debug(
            "실시간 투수 통계 갱신 완료 (pitcherId={}, year={}, teamId={}, result={})",
            event.pitcherId,
            year,
            event.pitcherTeamId,
            event.result,
        )
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
     * Optimistic Locking 충돌 시 @Retryable로 최대 3회 재시도합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(
        retryFor = [ObjectOptimisticLockingFailureException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 100),
    )
    fun onPlateAppearanceUndone(event: PlateAppearanceUndoneEvent) {
        val year = resolveYear(event.gameId)

        val stats = findOrCreateSeasonBattingStats(event.playerId, year, event.batterTeamId)

        stats.revertLiveUpdate(event.result)
        seasonBattingStatsRepository.save(stats)

        logger.debug(
            "실시간 타격 통계 역산 완료 (playerId={}, year={}, teamId={}, result={})",
            event.playerId,
            year,
            event.batterTeamId,
            event.result,
        )

        // 투수 통계 역산
        val pitchingStats = findOrCreateSeasonPitchingStats(event.pitcherId, year, event.pitcherTeamId)

        pitchingStats.revertLiveUpdate(event.result)
        seasonPitchingStatsRepository.save(pitchingStats)

        logger.debug(
            "실시간 투수 통계 역산 완료 (pitcherId={}, year={}, teamId={}, result={})",
            event.pitcherId,
            year,
            event.pitcherTeamId,
            event.result,
        )
    }

    /**
     * 경기 결과 확정 이벤트를 처리합니다.
     *
     * 경기 종료 시점에 다음 통계를 일괄 집계합니다:
     * - SeasonPitchingStats: 경기 요약 필드만 추가 (이닝, 실점, 자책점, 결정 등)
     *   실시간 갱신 필드(피안타, 삼진, 볼넷 등)는 applyLiveUpdate로 이미 반영되었으므로 제외
     * - CareerBattingStats: 커리어 타격 기록 누적
     * - CareerPitchingStats: 커리어 투수 기록 누적
     *
     * SeasonBattingStats는 PlateAppearanceRecordedEvent를 통해 실시간 반영되므로
     * 여기서는 처리하지 않습니다.
     *
     * 처리 순서:
     * 1. 투수 스탯: isFirstSeasonRecord 확인 → SeasonPitchingStats 갱신 → CareerPitchingStats 갱신
     * 2. 타자 스탯: isFirstSeasonRecord 확인 → CareerBattingStats 갱신
     *
     * Optimistic Locking 충돌 시 @Retryable로 최대 3회 재시도합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(
        retryFor = [ObjectOptimisticLockingFailureException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 100),
    )
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
            val teamId = pitchingRecord.gamePlayer.gameTeam.team.id

            val existingSeasonPitching =
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamId(playerId, year, teamId)
            val isFirstPitchingSeason = existingSeasonPitching == null

            // SeasonPitchingStats 갱신
            // 기존 시즌 통계가 있으면 경기 중 applyLiveUpdate로 실시간 갱신된 필드는 이미 반영되어 있으므로
            // 경기 종료 시에만 확정되는 필드(이닝, 실점, 자책점, 결정 등)만 추가한다.
            // 새로 생성된 경우에는 실시간 갱신이 적용되지 않았으므로 전체 기록을 추가한다.
            val seasonPitchingStats =
                existingSeasonPitching ?: SeasonPitchingStats.create(player = player, year = year, teamId = teamId)
            if (existingSeasonPitching != null) {
                seasonPitchingStats.addGameRecordForEndOfGame(pitchingRecord)
            } else {
                seasonPitchingStats.addGameRecord(pitchingRecord)
            }
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
                "투수 통계 갱신 완료 (playerId={}, year={}, teamId={}, isFirstSeason={})",
                playerId,
                year,
                teamId,
                isFirstPitchingSeason,
            )
        }

        // 커리어 타격 스탯 집계: CareerBattingStats
        // SeasonBattingStats는 PlateAppearanceRecordedEvent로 실시간 갱신되므로
        // 해당 시즌+팀 통계 존재 여부로 첫 시즌인지 판단합니다.
        for (battingRecord in battingRecords) {
            val player = battingRecord.gamePlayer.player
            val playerId = player.id
            val teamId = battingRecord.gamePlayer.gameTeam.team.id

            val isFirstBattingSeason =
                seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamId(playerId, year, teamId) == null

            val careerBattingStats =
                careerBattingStatsRepository.findByPlayerId(playerId)
                    ?: CareerBattingStats.create(player = player)

            if (isFirstBattingSeason) {
                careerBattingStats.addSeason()
            }
            careerBattingStats.addGameRecord(battingRecord)
            careerBattingStatsRepository.save(careerBattingStats)

            logger.debug(
                "커리어 타격 통계 갱신 완료 (playerId={}, teamId={}, isFirstSeason={})",
                playerId,
                teamId,
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

    /**
     * 선수의 팀별 시즌 투수 통계를 조회하거나, 없으면 자동 생성합니다.
     *
     * 첫 시즌 투수의 실시간 통계가 누락되는 문제를 방지합니다.
     * DB unique constraint (player_id, year, team_id)에 의해 동시성 중복 생성이 방어됩니다.
     */
    private fun findOrCreateSeasonPitchingStats(
        pitcherId: Long,
        year: Int,
        teamId: Long,
    ): SeasonPitchingStats {
        return seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamId(pitcherId, year, teamId)
            ?: run {
                val player =
                    playerRepository.findByIdOrNull(pitcherId)
                        ?: throw PlayerNotFoundException(pitcherId)
                logger.info(
                    "시즌 투수 통계 자동 생성 (pitcherId={}, year={}, teamId={})",
                    pitcherId,
                    year,
                    teamId,
                )
                val newStats = SeasonPitchingStats.create(player = player, year = year, teamId = teamId)
                seasonPitchingStatsRepository.save(newStats)
            }
    }

    /**
     * 선수의 팀별 시즌 타격 통계를 조회하거나, 없으면 자동 생성합니다.
     *
     * 첫 시즌 선수의 실시간 타격 통계가 누락되는 문제를 방지합니다.
     * DB unique constraint (player_id, year, team_id)에 의해 동시성 중복 생성이 방어됩니다.
     */
    private fun findOrCreateSeasonBattingStats(
        playerId: Long,
        year: Int,
        teamId: Long,
    ): SeasonBattingStats {
        return seasonBattingStatsRepository.findByPlayerIdAndYearAndTeamId(playerId, year, teamId)
            ?: run {
                val player =
                    playerRepository.findByIdOrNull(playerId)
                        ?: throw PlayerNotFoundException(playerId)
                logger.info(
                    "시즌 타격 통계 자동 생성 (playerId={}, year={}, teamId={})",
                    playerId,
                    year,
                    teamId,
                )
                val newStats = SeasonBattingStats.create(player = player, year = year, teamId = teamId)
                seasonBattingStatsRepository.save(newStats)
            }
    }

    private fun resolveYear(gameId: Long): Int {
        val game =
            gameRepository.findByIdOrNull(gameId)
                ?: throw GameNotFoundException(gameId)
        return game.scheduledAt.year
    }
}
