package com.nextup.infrastructure.service.game

import com.nextup.common.exception.BattingRecordNotFoundException
import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.GamePlayerNotFoundException
import com.nextup.common.exception.InvalidGameStateException
import com.nextup.core.domain.game.Base
import com.nextup.core.domain.game.BaseRunningResult
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameEvent
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.service.game.BaseRunningRecordService
import com.nextup.core.service.game.dto.BaseRunningRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 주루 플레이 기록 서비스 구현
 *
 * 도루, 도루 실패, 견제사, 폭투 진루 등 타석 외 주루 이벤트를 기록합니다.
 */
@Service
@Transactional(readOnly = true)
class BaseRunningRecordServiceImpl(
    private val gameRepository: GameRepositoryPort,
    private val gamePlayerRepository: GamePlayerRepositoryPort,
    private val gameEventRepository: GameEventRepositoryPort,
    private val battingRecordRepository: BattingRecordRepositoryPort,
) : BaseRunningRecordService {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun recordBaseRunning(
        gameId: Long,
        request: BaseRunningRequest,
    ): GameEvent {
        val game = findGame(gameId)

        if (game.status != GameStatus.IN_PROGRESS) {
            throw InvalidGameStateException(
                "진행 중인 경기만 주루 기록을 입력할 수 있습니다. 현재 상태: ${game.status.displayName}",
            )
        }

        val runner =
            gamePlayerRepository.findByIdOrNull(request.runnerId)
                ?: throw GamePlayerNotFoundException(request.runnerId)

        val outCountBefore = game.gameState.outs

        val runnersBeforeJson = serializeRunners(game.gameState)

        val outCountAfter: Int
        when (request.result) {
            BaseRunningResult.STOLEN_BASE,
            BaseRunningResult.ADVANCED_ON_ERROR,
            BaseRunningResult.ADVANCED_ON_WILD_PITCH,
            BaseRunningResult.ADVANCED_ON_PASSED_BALL,
            BaseRunningResult.ADVANCED_ON_BALK,
            -> {
                game.gameState.setRunner(request.fromBase, null)
                if (request.toBase != Base.HOME) {
                    game.gameState.setRunner(request.toBase, runner.id)
                }
                if (request.result == BaseRunningResult.STOLEN_BASE) {
                    val battingRecord =
                        battingRecordRepository.findByGamePlayer(runner)
                            ?: throw BattingRecordNotFoundException(runner.id)
                    battingRecord.recordStolenBase()
                }
                outCountAfter = game.gameState.outs
            }
            BaseRunningResult.CAUGHT_STEALING,
            BaseRunningResult.PICKED_OFF,
            -> {
                game.gameState.setRunner(request.fromBase, null)
                game.recordOut()
                if (request.result == BaseRunningResult.CAUGHT_STEALING) {
                    val battingRecord =
                        battingRecordRepository.findByGamePlayer(runner)
                            ?: throw BattingRecordNotFoundException(runner.id)
                    battingRecord.recordCaughtStealing()
                }
                outCountAfter = game.gameState.outs
            }
        }

        val runnersAfterJson = serializeRunners(game.gameState)

        val description = buildBaseRunningDescription(request)

        val event =
            GameEvent.createBaseRunning(
                game = game,
                runner = runner,
                fromBase = request.fromBase,
                toBase = request.toBase,
                result = request.result,
                description = description,
                outCountBefore = outCountBefore,
                outCountAfter = outCountAfter,
                runnersBeforeJson = runnersBeforeJson,
                runnersAfterJson = runnersAfterJson,
            )

        val savedEvent = gameEventRepository.save(event)
        gameRepository.save(game)
        return savedEvent
    }

    private fun serializeRunners(gameState: com.nextup.core.domain.game.GameState): String? {
        val entries =
            buildList {
                gameState.runnerOnFirstId?.let { add("1루:$it") }
                gameState.runnerOnSecondId?.let { add("2루:$it") }
                gameState.runnerOnThirdId?.let { add("3루:$it") }
            }
        return if (entries.isEmpty()) null else entries.joinToString(",")
    }

    private fun buildBaseRunningDescription(request: BaseRunningRequest): String {
        val fromDisplay = baseDisplay(request.fromBase)
        val toDisplay = baseDisplay(request.toBase)
        return "${request.result.displayName}: $fromDisplay → $toDisplay"
    }

    private fun baseDisplay(base: Base): String =
        when (base) {
            Base.FIRST -> "1루"
            Base.SECOND -> "2루"
            Base.THIRD -> "3루"
            Base.HOME -> "홈"
        }

    private fun findGame(id: Long): Game =
        gameRepository.findByIdOrNull(id)
            ?: throw GameNotFoundException(id)
}
