package com.nextup.infrastructure.listener

import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.domain.event.RecordCorrectedEvent
import com.nextup.core.domain.game.CorrectionType
import com.nextup.core.port.repository.CareerBattingStatsRepositoryPort
import com.nextup.core.port.repository.CareerFieldingStatsRepositoryPort
import com.nextup.core.port.repository.CareerPitchingStatsRepositoryPort
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.SeasonBattingStatsRepositoryPort
import com.nextup.core.port.repository.SeasonFieldingStatsRepositoryPort
import com.nextup.core.port.repository.SeasonPitchingStatsRepositoryPort
import com.nextup.infrastructure.config.CacheConfig
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 기록 정정 이벤트 리스너
 *
 * RecordCorrectedEvent 수신 시 해당 선수의 시즌/커리어 스탯에
 * 정정 델타(newValue - oldValue)를 반영합니다.
 *
 * Optimistic Locking(@Version) 기반 동시성 제어를 적용하여
 * Lost Update를 방지합니다. 충돌 시 최대 3회 재시도합니다.
 */
@Component
class RecordCorrectionEventListener(
    private val seasonBattingStatsRepository: SeasonBattingStatsRepositoryPort,
    private val seasonPitchingStatsRepository: SeasonPitchingStatsRepositoryPort,
    private val seasonFieldingStatsRepository: SeasonFieldingStatsRepositoryPort,
    private val careerBattingStatsRepository: CareerBattingStatsRepositoryPort,
    private val careerPitchingStatsRepository: CareerPitchingStatsRepositoryPort,
    private val careerFieldingStatsRepository: CareerFieldingStatsRepositoryPort,
    private val gameRepository: GameRepositoryPort,
    private val gamePlayerRepository: GamePlayerRepositoryPort,
    private val gameTeamRepository: GameTeamRepositoryPort,
    private val gameEventRepository: GameEventRepositoryPort,
    private val cacheManager: CacheManager,
) {
    private val logger = LoggerFactory.getLogger(RecordCorrectionEventListener::class.java)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(
        retryFor = [ObjectOptimisticLockingFailureException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 100),
    )
    fun onRecordCorrected(event: RecordCorrectedEvent) {
        val delta = parseDeltaSafely(event.newValue, event.oldValue)
        if (delta == 0) {
            logger.debug(
                "기록 정정 델타 0 - 스탯 갱신 건너뜀 (gameId={}, playerId={}, field={})",
                event.gameId,
                event.playerId,
                event.fieldName,
            )
            return
        }

        val year = resolveYear(event.gameId)

        logger.info(
            "기록 정정 스탯 반영 시작 (gameId={}, playerId={}, field={}, delta={})",
            event.gameId,
            event.playerId,
            event.fieldName,
            delta,
        )

        when (event.correctionType) {
            CorrectionType.BATTING ->
                applyBattingCorrection(event.gameId, event.playerId, year, event.fieldName, delta)
            CorrectionType.PITCHING ->
                applyPitchingCorrection(event.playerId, year, event.fieldName, delta)
            CorrectionType.FIELDING ->
                applyFieldingCorrection(event.playerId, year, event.fieldName, delta)
        }

        logger.info(
            "기록 정정 스탯 반영 완료 (gameId={}, playerId={}, field={}, delta={})",
            event.gameId,
            event.playerId,
            event.fieldName,
            delta,
        )

        // BATTING 정정 시 GameEvent 타임라인 갱신 (H6)
        if (event.correctionType == CorrectionType.BATTING) {
            applyGameEventCorrection(event)
        }

        // 기록 정정으로 순위가 변경될 수 있으므로 순위 캐시 무효화
        evictStandingsCache(event.gameId)
    }

    private fun applyBattingCorrection(
        gameId: Long,
        playerId: Long,
        year: Int,
        fieldName: String,
        delta: Int,
    ) {
        // 시즌 타격 스탯 갱신
        val seasonStats = seasonBattingStatsRepository.findByPlayerIdAndYear(playerId, year)
        if (seasonStats != null) {
            seasonStats.applyFieldCorrection(fieldName, delta)
            seasonBattingStatsRepository.save(seasonStats)
            logger.debug("시즌 타격 통계 정정 반영 완료 (playerId={}, year={})", playerId, year)
        } else {
            logger.debug("시즌 타격 통계 없음 - 정정 건너뜀 (playerId={}, year={})", playerId, year)
        }

        // 커리어 타격 스탯 갱신
        val careerStats = careerBattingStatsRepository.findByPlayerId(playerId)
        if (careerStats != null) {
            careerStats.applyFieldCorrection(fieldName, delta)
            careerBattingStatsRepository.save(careerStats)
            logger.debug("커리어 타격 통계 정정 반영 완료 (playerId={})", playerId)
        } else {
            logger.debug("커리어 타격 통계 없음 - 정정 건너뜀 (playerId={})", playerId)
        }

        // GameTeam 득점/안타 동기화
        applyGameTeamBattingCorrection(gameId, playerId, fieldName, delta)
    }

    /**
     * 타격 기록 정정 시 GameTeam의 totalScore / totalHits를 동기화합니다.
     *
     * - "runs": 득점 정정 → totalScore 갱신
     * - "hits", "doubles", "triples", "homeRuns": 안타 관련 → totalHits 갱신
     */
    private fun applyGameTeamBattingCorrection(
        gameId: Long,
        playerId: Long,
        fieldName: String,
        delta: Int,
    ) {
        val gamePlayer =
            gamePlayerRepository.findByGameIdAndPlayerId(gameId, playerId)
                ?: run {
                    logger.debug(
                        "GamePlayer 없음 - GameTeam 갱신 건너뜀 (gameId={}, playerId={})",
                        gameId,
                        playerId,
                    )
                    return
                }

        val gameTeam = gamePlayer.gameTeam

        when (fieldName) {
            "runs" -> {
                gameTeam.correctScore(delta)
                gameTeamRepository.save(gameTeam)
                logger.debug(
                    "GameTeam 득점 정정 반영 완료 (gameTeamId={}, delta={})",
                    gameTeam.id,
                    delta,
                )
            }
            "hits", "doubles", "triples", "homeRuns" -> {
                gameTeam.correctHits(delta)
                gameTeamRepository.save(gameTeam)
                logger.debug(
                    "GameTeam 안타 정정 반영 완료 (gameTeamId={}, fieldName={}, delta={})",
                    gameTeam.id,
                    fieldName,
                    delta,
                )
            }
            else -> {
                logger.debug(
                    "GameTeam 갱신 불필요한 필드 (gameTeamId={}, fieldName={})",
                    gameTeam.id,
                    fieldName,
                )
            }
        }
    }

    private fun applyPitchingCorrection(
        playerId: Long,
        year: Int,
        fieldName: String,
        delta: Int,
    ) {
        // 시즌 투수 스탯 갱신
        val seasonStats = seasonPitchingStatsRepository.findByPlayerIdAndYear(playerId, year)
        if (seasonStats != null) {
            seasonStats.applyFieldCorrection(fieldName, delta)
            seasonPitchingStatsRepository.save(seasonStats)
            logger.debug("시즌 투수 통계 정정 반영 완료 (playerId={}, year={})", playerId, year)
        } else {
            logger.debug("시즌 투수 통계 없음 - 정정 건너뜀 (playerId={}, year={})", playerId, year)
        }

        // 커리어 투수 스탯 갱신
        val careerStats = careerPitchingStatsRepository.findByPlayerId(playerId)
        if (careerStats != null) {
            careerStats.applyFieldCorrection(fieldName, delta)
            careerPitchingStatsRepository.save(careerStats)
            logger.debug("커리어 투수 통계 정정 반영 완료 (playerId={})", playerId)
        } else {
            logger.debug("커리어 투수 통계 없음 - 정정 건너뜀 (playerId={})", playerId)
        }
    }

    private fun applyFieldingCorrection(
        playerId: Long,
        year: Int,
        fieldName: String,
        delta: Int,
    ) {
        // 시즌 수비 스탯 갱신
        val seasonStats = seasonFieldingStatsRepository.findByPlayerIdAndYear(playerId, year)
        if (seasonStats != null) {
            seasonStats.applyFieldCorrection(fieldName, delta)
            seasonFieldingStatsRepository.save(seasonStats)
            logger.debug("시즌 수비 통계 정정 반영 완료 (playerId={}, year={})", playerId, year)
        } else {
            logger.debug("시즌 수비 통계 없음 - 정정 건너뜀 (playerId={}, year={})", playerId, year)
        }

        // 커리어 수비 스탯 갱신
        val careerStats = careerFieldingStatsRepository.findByPlayerId(playerId)
        if (careerStats != null) {
            careerStats.applyFieldCorrection(fieldName, delta)
            careerFieldingStatsRepository.save(careerStats)
            logger.debug("커리어 수비 통계 정정 반영 완료 (playerId={})", playerId)
        } else {
            logger.debug("커리어 수비 통계 없음 - 정정 건너뜀 (playerId={})", playerId)
        }
    }

    /**
     * BATTING 정정 시 GameEvent 타임라인의 description을 갱신합니다 (H6).
     *
     * gameEventId가 제공된 경우 해당 GameEvent의 description에 정정 내역을 추가합니다.
     */
    private fun applyGameEventCorrection(event: RecordCorrectedEvent) {
        val gameEventId =
            event.gameEventId ?: run {
                logger.debug(
                    "gameEventId 없음 - GameEvent 정정 건너뜀 (gameId={}, playerId={})",
                    event.gameId,
                    event.playerId,
                )
                return
            }

        val gameEvent =
            gameEventRepository.findByIdOrNull(gameEventId) ?: run {
                logger.warn(
                    "GameEvent를 찾을 수 없음 - GameEvent 정정 건너뜀 (gameEventId={}, gameId={})",
                    gameEventId,
                    event.gameId,
                )
                return
            }

        val correctionNote =
            "[정정] ${event.fieldName}: ${event.oldValue} → ${event.newValue}"
        val updatedDescription =
            if (gameEvent.description.contains("[정정]")) {
                gameEvent.description.replaceAfterLast("[정정]", correctionNote.removePrefix("[정정]"))
            } else {
                "${gameEvent.description} $correctionNote"
            }

        gameEvent.description = updatedDescription
        gameEventRepository.save(gameEvent)

        logger.info(
            "GameEvent 타임라인 정정 완료 (gameEventId={}, gameId={}, field={})",
            gameEventId,
            event.gameId,
            event.fieldName,
        )
    }

    /**
     * 안전한 델타 계산
     *
     * NumberFormatException 방지를 위해 toIntOrNull을 사용합니다.
     * 파싱 실패 시 0을 반환하여 스탯 갱신을 건너뜁니다.
     */
    private fun parseDeltaSafely(
        newValue: String,
        oldValue: String,
    ): Int {
        val newInt = newValue.toIntOrNull()
        val oldInt = oldValue.toIntOrNull()
        if (newInt == null || oldInt == null) {
            logger.warn(
                "기록 정정 델타 계산 실패 - 정수 파싱 불가 (newValue={}, oldValue={})",
                newValue,
                oldValue,
            )
            return 0
        }
        return newInt - oldInt
    }

    private fun evictStandingsCache(gameId: Long) {
        val game = gameRepository.findByIdOrNull(gameId) ?: return
        val competitionId = game.competition.id
        cacheManager.getCache(CacheConfig.STANDINGS_CACHE)?.evict(competitionId)
        cacheManager.getCache(CacheConfig.LEADERBOARD_CACHE)?.clear()
        cacheManager.getCache(CacheConfig.TEAM_STATS_CACHE)?.clear()
        logger.debug(
            "기록 정정으로 캐시 무효화 (competitionId={}, gameId={})",
            competitionId,
            gameId,
        )
    }

    private fun resolveYear(gameId: Long): Int {
        val game =
            gameRepository.findByIdOrNull(gameId)
                ?: throw GameNotFoundException(gameId)
        return game.scheduledAt.year
    }
}
