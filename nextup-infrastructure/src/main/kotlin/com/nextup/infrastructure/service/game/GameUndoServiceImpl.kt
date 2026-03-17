package com.nextup.infrastructure.service.game

import com.nextup.common.exception.BattingRecordNotFoundException
import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.NoEventToUndoException
import com.nextup.common.exception.PitchingRecordNotFoundException
import com.nextup.common.exception.UndoNotAvailableException
import com.nextup.core.domain.event.PlateAppearanceUndoneEvent
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameEvent
import com.nextup.core.domain.game.GameEventType
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.service.game.GameUndoService
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 경기 이벤트 되돌리기 서비스 구현
 *
 * 마지막 이벤트를 되돌리는 Undo 기능을 담당합니다.
 */
@Service
@Transactional(readOnly = true)
class GameUndoServiceImpl(
    private val gameRepository: GameRepositoryPort,
    private val gameEventRepository: GameEventRepositoryPort,
    private val battingRecordRepository: BattingRecordRepositoryPort,
    private val pitchingRecordRepository: PitchingRecordRepositoryPort,
    private val eventPublisher: ApplicationEventPublisher,
) : GameUndoService {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun undoLastEvent(gameId: Long): GameEvent {
        val game = findGame(gameId)

        if (!game.canUndo()) {
            throw UndoNotAvailableException(
                "진행 중인 경기만 Undo할 수 있습니다. 현재 상태: ${game.status.displayName}",
            )
        }

        val lastEvent =
            gameEventRepository.findLastActiveEvent(gameId)
                ?: throw NoEventToUndoException()

        when (lastEvent.eventType) {
            GameEventType.PLATE_APPEARANCE -> undoPlateAppearance(game, lastEvent)
            GameEventType.INNING_CHANGE -> undoInningChange(game, lastEvent)
            else -> {
                // GAME_STATUS, BASE_RUNNING, POSITION_CHANGE 등은 단순 마킹만 처리
            }
        }

        lastEvent.markUndone()
        gameEventRepository.save(lastEvent)

        gameRepository.save(game)

        if (lastEvent.eventType == GameEventType.PLATE_APPEARANCE) {
            lastEvent.plateAppearanceResult?.let { result ->
                lastEvent.batter?.let { batter ->
                    lastEvent.pitcher?.let { pitcher ->
                        eventPublisher.publishEvent(
                            PlateAppearanceUndoneEvent(
                                gameId = gameId,
                                playerId = batter.player.id,
                                pitcherId = pitcher.player.id,
                                result = result,
                            ),
                        )
                    }
                }
            }
        }

        return lastEvent
    }

    private fun undoPlateAppearance(
        game: Game,
        event: GameEvent,
    ) {
        val result = event.plateAppearanceResult ?: return

        game.restoreInningState(
            inning = event.inning,
            isTop = event.isTopInning,
            outs = event.outCountBefore,
        )
        game.gameState.restoreRunners(event.runnersBeforeJson)
        game.gameState.resetCount()

        game.revertBatter()

        event.batter?.let { batter ->
            val battingRecord =
                battingRecordRepository.findByGamePlayer(batter)
                    ?: throw BattingRecordNotFoundException(batter.id)

            battingRecord.revertPlateAppearanceResult(result, event.rbis)
        }

        if (event.runsScored > 0) {
            event.batter?.let { batter ->
                batter.gameTeam.subtractRunInInning(event.inning, event.runsScored)
            }
        }

        event.pitcher?.let { pitcher ->
            val pitchingRecord =
                pitchingRecordRepository.findByGamePlayer(pitcher)
                    ?: throw PitchingRecordNotFoundException(pitcher.id)

            pitchingRecord.revertBatterFaced(result)

            if (!result.isOnBase) {
                pitchingRecord.revertInningOut()
            }

            if (event.runsScored > 0) {
                pitchingRecord.revertEarnedRun(event.runsScored)
            }
        }

        if (result.isHit) {
            event.batter?.let { batter ->
                batter.gameTeam.subtractHit()
            }
        }
    }

    private fun undoInningChange(
        game: Game,
        event: GameEvent,
    ) {
        game.restoreInningState(
            inning = event.inning,
            isTop = event.isTopInning,
            outs = event.outCountBefore,
        )
        game.gameState.restoreRunners(event.runnersBeforeJson)
    }

    private fun findGame(id: Long): Game =
        gameRepository.findByIdOrNull(id)
            ?: throw GameNotFoundException(id)
}
