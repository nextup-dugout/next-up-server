package com.nextup.infrastructure.listener

import com.nextup.core.domain.event.GameCancelledEvent
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.FieldingRecordRepositoryPort
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
 * 경기 취소 이벤트 리스너
 *
 * GameCancelledEvent 수신 시 해당 경기에 반영된 시즌 타격/투구 통계를 롤백합니다.
 *
 * 롤백 정책:
 * - 취소된 경기의 BattingRecord에 집계된 타격 기여분을 SeasonBattingStats에서 차감합니다.
 * - 취소된 경기의 PitchingRecord에 집계된 투구 기여분을 SeasonPitchingStats에서 차감합니다.
 * - 몰수(FORFEITED) 경기는 이 리스너가 처리하지 않습니다. 몰수 시점까지의 개인 기록은
 *   KBO/MLB 기준에 따라 공식 기록으로 유효합니다.
 */
@Component
class GameCancelEventListener(
    private val seasonBattingStatsRepository: SeasonBattingStatsRepositoryPort,
    private val seasonPitchingStatsRepository: SeasonPitchingStatsRepositoryPort,
    private val seasonFieldingStatsRepository: SeasonFieldingStatsRepositoryPort,
    private val battingRecordRepository: BattingRecordRepositoryPort,
    private val pitchingRecordRepository: PitchingRecordRepositoryPort,
    private val fieldingRecordRepository: FieldingRecordRepositoryPort,
) {
    private val logger = LoggerFactory.getLogger(GameCancelEventListener::class.java)

    /**
     * 경기 취소 이벤트를 처리합니다.
     *
     * 취소된 경기에 기여한 모든 선수의 시즌 타격/투구 통계를 롤백합니다.
     * AFTER_COMMIT 단계에서 실행하여 취소 트랜잭션이 완전히 커밋된 이후 통계를 역산합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    fun onGameCancelled(event: GameCancelledEvent) {
        val gameId = event.gameId
        logger.info("경기 취소 스탯 롤백 시작 (gameId={})", gameId)

        val battingRecords = battingRecordRepository.findAllByGameId(gameId)
        val pitchingRecords = pitchingRecordRepository.findAllByGameId(gameId)
        val fieldingRecords = fieldingRecordRepository.findAllByGameId(gameId)

        if (battingRecords.isEmpty() && pitchingRecords.isEmpty() && fieldingRecords.isEmpty()) {
            logger.info(
                "경기 취소 스탯 롤백 완료 - 롤백할 기록 없음 (gameId={})",
                gameId,
            )
            return
        }

        // 타격 스탯 롤백
        if (battingRecords.isNotEmpty()) {
            val seasonBattingStatsList = seasonBattingStatsRepository.findAllByGameId(gameId)
            val statsByPlayerId = seasonBattingStatsList.associateBy { it.player.id }

            for (battingRecord in battingRecords) {
                val playerId = battingRecord.gamePlayer.player.id
                val stats = statsByPlayerId[playerId]

                if (stats == null) {
                    logger.debug(
                        "시즌 타격 통계 없음 - 롤백 건너뜀 (playerId={}, gameId={})",
                        playerId,
                        gameId,
                    )
                } else {
                    stats.revertGameRecord(battingRecord)
                    seasonBattingStatsRepository.save(stats)
                    logger.debug(
                        "시즌 타격 통계 롤백 완료 (playerId={}, gameId={})",
                        playerId,
                        gameId,
                    )
                }
            }
        }

        // 투구 스탯 롤백
        if (pitchingRecords.isNotEmpty()) {
            val seasonPitchingStatsList = seasonPitchingStatsRepository.findAllByGameId(gameId)
            val statsByPlayerId = seasonPitchingStatsList.associateBy { it.player.id }

            for (pitchingRecord in pitchingRecords) {
                val playerId = pitchingRecord.gamePlayer.player.id
                val stats = statsByPlayerId[playerId]

                if (stats == null) {
                    logger.debug(
                        "시즌 투구 통계 없음 - 롤백 건너뜀 (playerId={}, gameId={})",
                        playerId,
                        gameId,
                    )
                } else {
                    stats.revertGameRecord(pitchingRecord)
                    seasonPitchingStatsRepository.save(stats)
                    logger.debug(
                        "시즌 투구 통계 롤백 완료 (playerId={}, gameId={})",
                        playerId,
                        gameId,
                    )
                }
            }
        }

        // 수비 스탯 롤백
        if (fieldingRecords.isNotEmpty()) {
            val seasonFieldingStatsList = seasonFieldingStatsRepository.findAllByGameId(gameId)
            val statsByPlayerId = seasonFieldingStatsList.associateBy { it.player.id }

            for (fieldingRecord in fieldingRecords) {
                val playerId = fieldingRecord.gamePlayer.player.id
                val stats = statsByPlayerId[playerId]

                if (stats == null) {
                    logger.debug(
                        "시즌 수비 통계 없음 - 롤백 건너뜀 (playerId={}, gameId={})",
                        playerId,
                        gameId,
                    )
                } else {
                    stats.revertGameRecord(fieldingRecord)
                    seasonFieldingStatsRepository.save(stats)
                    logger.debug(
                        "시즌 수비 통계 롤백 완료 (playerId={}, gameId={})",
                        playerId,
                        gameId,
                    )
                }
            }
        }

        logger.info(
            "경기 취소 스탯 롤백 완료 (gameId={}, battingRecords={}, pitchingRecords={}, fieldingRecords={})",
            gameId,
            battingRecords.size,
            pitchingRecords.size,
            fieldingRecords.size,
        )
    }
}
