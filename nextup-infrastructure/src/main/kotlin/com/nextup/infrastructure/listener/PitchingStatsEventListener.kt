package com.nextup.infrastructure.listener

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.domain.event.PlateAppearanceRecordedEvent
import com.nextup.core.domain.event.PlateAppearanceUndoneEvent
import com.nextup.core.domain.stats.CareerPitchingStats
import com.nextup.core.domain.stats.SeasonPitchingStats
import com.nextup.core.port.repository.CareerPitchingStatsRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.SeasonPitchingStatsRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 투수 통계 이벤트 리스너
 *
 * 타석 이벤트를 수신하여 시즌 투수 통계를 실시간으로 갱신하고,
 * 경기 종료 이벤트를 수신하여 시즌 투수 통계의 경기 요약 필드 및
 * 커리어 투수 통계를 집계합니다.
 *
 * 투수 통계 중복 방지 전략:
 * - 경기 중: applyLiveUpdate()로 타석 단위 필드(피안타, 삼진, 볼넷, 사구, 피홈런, 대면타자) 실시간 갱신
 * - 경기 종료 시: addGameRecordForEndOfGame()으로 경기 요약 필드만 추가 (실시간 갱신 필드 제외)
 *
 * Optimistic Locking(@Version) 기반 동시성 제어를 적용하여
 * Lost Update를 방지합니다. 충돌 시 최대 3회 재시도합니다.
 */
@Component
class PitchingStatsEventListener(
    private val seasonPitchingStatsRepository: SeasonPitchingStatsRepositoryPort,
    private val careerPitchingStatsRepository: CareerPitchingStatsRepositoryPort,
    private val pitchingRecordRepository: PitchingRecordRepositoryPort,
    private val gameRepository: GameRepositoryPort,
    private val playerRepository: PlayerRepositoryPort,
) {
    private val logger = LoggerFactory.getLogger(PitchingStatsEventListener::class.java)

    /**
     * 타석 결과 기록 이벤트를 처리합니다 (투수 영역).
     *
     * 해당 투수의 시즌 투수 통계를 실시간으로 갱신합니다.
     * 시즌 통계가 없는 경우 자동으로 생성합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onPlateAppearanceRecorded(event: PlateAppearanceRecordedEvent) {
        val (year, competitionType) = resolveGameContext(event.gameId)

        val pitchingStats =
            findOrCreateSeasonPitchingStats(event.pitcherId, year, event.pitcherTeamId, competitionType)

        pitchingStats.applyLiveUpdate(event.result)
        seasonPitchingStatsRepository.save(pitchingStats)

        logger.debug(
            "실시간 투수 통계 갱신 완료 (pitcherId={}, year={}, teamId={}, competitionType={}, result={})",
            event.pitcherId,
            year,
            event.pitcherTeamId,
            competitionType,
            event.result,
        )
    }

    /**
     * 타석 결과 취소 이벤트를 처리합니다 (투수 영역).
     *
     * 해당 투수의 시즌 투수 통계를 역산합니다.
     * 시즌 통계가 없는 경우 자동으로 생성한 뒤 역산합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onPlateAppearanceUndone(event: PlateAppearanceUndoneEvent) {
        val (year, competitionType) = resolveGameContext(event.gameId)

        val pitchingStats =
            findOrCreateSeasonPitchingStats(event.pitcherId, year, event.pitcherTeamId, competitionType)

        pitchingStats.revertLiveUpdate(event.result)
        seasonPitchingStatsRepository.save(pitchingStats)

        logger.debug(
            "실시간 투수 통계 역산 완료 (pitcherId={}, year={}, teamId={}, competitionType={}, result={})",
            event.pitcherId,
            year,
            event.pitcherTeamId,
            competitionType,
            event.result,
        )
    }

    /**
     * 경기 결과 확정 이벤트를 처리합니다 (투수 영역).
     *
     * 경기 종료 시점에 다음 통계를 집계합니다:
     * - SeasonPitchingStats: 경기 요약 필드만 추가 (이닝, 실점, 자책점, 결정 등)
     *   실시간 갱신 필드(피안타, 삼진, 볼넷 등)는 applyLiveUpdate로 이미 반영되었으므로 제외
     * - CareerPitchingStats: 커리어 투수 기록 누적
     *
     * isFirstSeasonRecord 확인 → SeasonPitchingStats 갱신 → CareerPitchingStats 갱신
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onGameResultConfirmed(event: GameResultConfirmedEvent) {
        val gameId = event.gameId
        val (year, competitionType) = resolveGameContext(gameId)

        val pitchingRecords = pitchingRecordRepository.findAllByGameId(gameId)

        if (pitchingRecords.isEmpty()) {
            logger.debug("투수 기록 없음 - 투수 스탯 집계 생략 (gameId={})", gameId)
            return
        }

        logger.info(
            "투수 스탯 집계 시작 (gameId={}, year={}, competitionType={}, records={})",
            gameId,
            year,
            competitionType,
            pitchingRecords.size,
        )

        for (pitchingRecord in pitchingRecords) {
            val player = pitchingRecord.gamePlayer.player
            val playerId = player.id
            val teamId = pitchingRecord.gamePlayer.gameTeam.team.id

            retryOnOptimisticLock("onGameResultConfirmed-pitching(playerId=$playerId)") {
                val existingSeasonPitching =
                    seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                        playerId,
                        year,
                        teamId,
                        competitionType,
                    )
                val isFirstPitchingSeason = existingSeasonPitching == null

                val seasonPitchingStats =
                    existingSeasonPitching
                        ?: SeasonPitchingStats.create(
                            player = player,
                            year = year,
                            teamId = teamId,
                            competitionType = competitionType,
                        )
                if (existingSeasonPitching != null) {
                    seasonPitchingStats.addGameRecordForEndOfGame(pitchingRecord)
                } else {
                    seasonPitchingStats.addGameRecord(pitchingRecord)
                }
                seasonPitchingStatsRepository.save(seasonPitchingStats)

                val careerPitchingStats =
                    careerPitchingStatsRepository.findByPlayerId(playerId)
                        ?: CareerPitchingStats.create(player = player)

                if (isFirstPitchingSeason) {
                    careerPitchingStats.addSeason()
                }
                careerPitchingStats.addGameRecord(pitchingRecord)
                careerPitchingStatsRepository.save(careerPitchingStats)

                logger.debug(
                    "투수 통계 갱신 완료 (playerId={}, year={}, teamId={}, competitionType={}, isFirstSeason={})",
                    playerId,
                    year,
                    teamId,
                    competitionType,
                    isFirstPitchingSeason,
                )
            }
        }

        logger.info("투수 스탯 집계 완료 (gameId={}, records={})", gameId, pitchingRecords.size)
    }

    /**
     * 선수의 팀별 시즌 투수 통계를 조회하거나, 없으면 자동 생성합니다.
     */
    private fun findOrCreateSeasonPitchingStats(
        pitcherId: Long,
        year: Int,
        teamId: Long,
        competitionType: CompetitionType,
    ): SeasonPitchingStats {
        return seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
            pitcherId,
            year,
            teamId,
            competitionType,
        )
            ?: run {
                val player =
                    playerRepository.findByIdOrNull(pitcherId)
                        ?: throw PlayerNotFoundException(pitcherId)
                logger.info(
                    "시즌 투수 통계 자동 생성 (pitcherId={}, year={}, teamId={}, competitionType={})",
                    pitcherId,
                    year,
                    teamId,
                    competitionType,
                )
                val newStats =
                    SeasonPitchingStats.create(
                        player = player,
                        year = year,
                        teamId = teamId,
                        competitionType = competitionType,
                    )
                seasonPitchingStatsRepository.save(newStats)
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
        private val log = LoggerFactory.getLogger(PitchingStatsEventListener::class.java)

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
