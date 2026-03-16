package com.nextup.infrastructure.listener

import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.domain.event.SpecialGameRecordDetectedEvent
import com.nextup.core.domain.game.GameEvent
import com.nextup.core.domain.game.GameEventType
import com.nextup.core.domain.game.SpecialGameRecord
import com.nextup.core.domain.game.SpecialGameRecordDetector
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.FieldingRecordRepositoryPort
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 특수 경기 기록 감지 이벤트 리스너
 *
 * 경기 결과 확정 이벤트를 수신하여 노히트/퍼펙트게임을 자동 감지합니다.
 * 감지된 특수 기록은 GameEvent로 저장하고, SpecialGameRecordDetectedEvent를 발행합니다.
 */
@Component
class SpecialGameRecordEventListener(
    private val gameRepository: GameRepositoryPort,
    private val gameTeamRepository: GameTeamRepositoryPort,
    private val battingRecordRepository: BattingRecordRepositoryPort,
    private val pitchingRecordRepository: PitchingRecordRepositoryPort,
    private val fieldingRecordRepository: FieldingRecordRepositoryPort,
    private val gameEventRepository: GameEventRepositoryPort,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val logger = LoggerFactory.getLogger(SpecialGameRecordEventListener::class.java)

    /**
     * 경기 결과 확정 시 특수 기록을 감지합니다.
     *
     * BEFORE_COMMIT 단계에서 실행하여 GameEvent 저장을 동일 트랜잭션 내에서 처리합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun onGameResultConfirmed(event: GameResultConfirmedEvent) {
        val game =
            gameRepository.findByIdOrNull(event.gameId)
                ?: throw GameNotFoundException(event.gameId)

        val gameTeams = gameTeamRepository.findAllByGameId(event.gameId)
        if (gameTeams.size != 2) {
            logger.debug(
                "특수 기록 감지 건너뜀 - 팀 수가 2가 아님 (gameId={}, teamCount={})",
                event.gameId,
                gameTeams.size,
            )
            return
        }

        // 팀별 기록 조회 - 각 팀의 투수/수비 기록을 팀 ID 기준으로 그룹화
        val allBattingRecords = battingRecordRepository.findAllByGameId(event.gameId)
        val allPitchingRecords = pitchingRecordRepository.findAllByGameId(event.gameId)
        val allFieldingRecords = fieldingRecordRepository.findAllByGameId(event.gameId)

        val battingByTeamId =
            allBattingRecords.groupBy { it.gamePlayer.gameTeam.team.id }
        val pitchingByTeamId =
            allPitchingRecords.groupBy { it.gamePlayer.gameTeam.team.id }
        val fieldingByTeamId =
            allFieldingRecords.groupBy { it.gamePlayer.gameTeam.team.id }

        val detectionResults =
            SpecialGameRecordDetector.detect(
                game = game,
                gameTeams = gameTeams,
                battingRecordsByTeamId = battingByTeamId,
                pitchingRecordsByTeamId = pitchingByTeamId,
                fieldingRecordsByTeamId = fieldingByTeamId,
            )

        for (result in detectionResults) {
            val eventType =
                when (result.record) {
                    SpecialGameRecord.PERFECT_GAME -> GameEventType.PERFECT_GAME
                    SpecialGameRecord.NO_HITTER -> GameEventType.NO_HITTER
                }

            val description =
                when (result.record) {
                    SpecialGameRecord.PERFECT_GAME ->
                        "퍼펙트게임 달성! 상대 팀에 안타, 볼넷, 사구, 실책 없이 경기를 마무리했습니다."
                    SpecialGameRecord.NO_HITTER ->
                        "노히트노런 달성! 상대 팀에 안타를 허용하지 않았습니다."
                }

            // GameEvent로 기록
            val gameEvent =
                GameEvent(
                    game = game,
                    inning = game.currentInning,
                    isTopInning = game.isTopInning,
                    outCountBefore = game.gameState.outs,
                    outCountAfter = game.gameState.outs,
                    eventType = eventType,
                    description = description,
                )
            gameEventRepository.save(gameEvent)

            // 후속 처리를 위한 도메인 이벤트 발행 (알림 등)
            eventPublisher.publishEvent(
                SpecialGameRecordDetectedEvent(
                    gameId = event.gameId,
                    teamId = result.teamId,
                    opponentTeamId = result.opponentTeamId,
                    record = result.record,
                ),
            )

            logger.info(
                "{} 감지 (gameId={}, teamId={}, opponentTeamId={})",
                result.record.displayName,
                event.gameId,
                result.teamId,
                result.opponentTeamId,
            )
        }
    }
}
