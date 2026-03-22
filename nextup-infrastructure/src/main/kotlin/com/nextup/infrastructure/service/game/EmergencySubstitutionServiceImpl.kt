package com.nextup.infrastructure.service.game

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.GamePlayerNotFoundException
import com.nextup.common.exception.InvalidGameStateException
import com.nextup.core.domain.game.Base
import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameEvent
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.domain.player.PositionCategory
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.service.game.EmergencySubstitutionService
import com.nextup.core.service.game.dto.EjectAndSubstituteRequest
import com.nextup.core.service.game.dto.EjectionRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 긴급 교체 서비스 구현
 *
 * 부상 퇴장 및 긴급 교체를 처리합니다.
 * - 퇴장만 처리 (교체 선수 없는 경우)
 * - 퇴장 + 교체를 원자적으로 처리
 * - 주자 상태 선수 퇴장 시 베이스 계승
 * - 투수 부상 퇴장 시 currentPitcherId 갱신
 */
@Service
@Transactional(readOnly = true)
class EmergencySubstitutionServiceImpl(
    private val gameRepository: GameRepositoryPort,
    private val gamePlayerRepository: GamePlayerRepositoryPort,
    private val gameEventRepository: GameEventRepositoryPort,
    private val battingRecordRepository: BattingRecordRepositoryPort,
    private val pitchingRecordRepository: PitchingRecordRepositoryPort,
) : EmergencySubstitutionService {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun ejectPlayer(
        gameId: Long,
        request: EjectionRequest,
        scorerId: Long,
    ): GameEvent {
        val game = findGame(gameId)
        game.validateScorer(scorerId)
        validateGameInProgress(game)

        val ejectedPlayer =
            gamePlayerRepository.findByIdOrNull(request.ejectedPlayerId)
                ?: throw GamePlayerNotFoundException(request.ejectedPlayerId)

        if (!ejectedPlayer.isCurrentlyPlaying) {
            throw InvalidGameStateException(
                "현재 출전 중인 선수만 퇴장할 수 있습니다. (GamePlayer ID: ${request.ejectedPlayerId})",
            )
        }

        // 주자 상태인 선수 퇴장 시 베이스 비움
        clearRunnerBase(game, ejectedPlayer.id)

        // 투수 퇴장 시 이닝 마감 처리
        if (ejectedPlayer.isPitcher) {
            closePitcherInning(ejectedPlayer, game)
            game.gameState.currentPitcherId = null
            gameRepository.save(game)
        }

        // 퇴장 처리
        ejectedPlayer.eject(game.currentInning, request.reason)
        gamePlayerRepository.save(ejectedPlayer)

        val halfInning = if (game.isTopInning) "초" else "말"
        val description =
            "${game.currentInning}회$halfInning: ${ejectedPlayer.player.name} 퇴장 (${request.reason.displayName})"

        val ejectionEvent =
            GameEvent.createEjection(
                game = game,
                ejectedPlayer = ejectedPlayer,
                description = description,
            )

        val savedEvent = gameEventRepository.save(ejectionEvent)
        log.info(
            "선수 퇴장 처리 완료 (gameId={}, gamePlayerId={}, reason={})",
            gameId,
            request.ejectedPlayerId,
            request.reason,
        )
        return savedEvent
    }

    @Transactional
    override fun ejectAndSubstitute(
        gameId: Long,
        request: EjectAndSubstituteRequest,
        scorerId: Long,
    ): GameEvent {
        val game = findGame(gameId)
        game.validateScorer(scorerId)
        validateGameInProgress(game)

        val ejectedPlayer =
            gamePlayerRepository.findByIdOrNull(request.ejectedPlayerId)
                ?: throw GamePlayerNotFoundException(request.ejectedPlayerId)

        val replacementPlayer =
            gamePlayerRepository.findByIdOrNull(request.replacementPlayerId)
                ?: throw GamePlayerNotFoundException(request.replacementPlayerId)

        if (!ejectedPlayer.isCurrentlyPlaying) {
            throw InvalidGameStateException(
                "현재 출전 중인 선수만 퇴장할 수 있습니다. (GamePlayer ID: ${request.ejectedPlayerId})",
            )
        }

        if (replacementPlayer.hasExited) {
            throw InvalidGameStateException(
                "이미 퇴장한 선수는 재출전할 수 없습니다. (GamePlayer ID: ${request.replacementPlayerId})",
            )
        }

        if (replacementPlayer.isCurrentlyPlaying) {
            throw InvalidGameStateException(
                "이미 출전 중인 선수는 교체 투입할 수 없습니다. (GamePlayer ID: ${request.replacementPlayerId})",
            )
        }

        // 주자 베이스 계승: 퇴장 선수가 주자이면 교체 선수가 같은 베이스를 계승
        inheritRunnerBase(game, ejectedPlayer.id, replacementPlayer.id)

        // 투수 퇴장 시 이닝 마감 처리
        if (ejectedPlayer.isPitcher) {
            closePitcherInning(ejectedPlayer, game)
        }

        // 퇴장 처리
        ejectedPlayer.eject(game.currentInning, request.reason)
        gamePlayerRepository.save(ejectedPlayer)

        // 교체 선수 투입
        replacementPlayer.enterAsSubstitute(
            inning = game.currentInning,
            newPosition = request.newPosition,
            newBattingOrder = ejectedPlayer.battingOrder,
        )
        gamePlayerRepository.save(replacementPlayer)

        // 교체 선수 기록 자동 생성
        createRecordsForSubstitute(replacementPlayer)

        // 투수 교체 시 currentPitcherId 갱신
        if (request.newPosition.category == PositionCategory.PITCHER) {
            game.gameState.currentPitcherId = replacementPlayer.id
            gameRepository.save(game)
            log.debug(
                "투수 긴급 교체 - currentPitcherId 갱신 (gameId={}, newPitcherId={})",
                game.id,
                replacementPlayer.id,
            )
        }

        val halfInning = if (game.isTopInning) "초" else "말"
        val description =
            "${game.currentInning}회$halfInning: ${ejectedPlayer.player.name} 퇴장 (${request.reason.displayName}) → ${replacementPlayer.player.name} 긴급 교체 (${request.newPosition.displayName})"

        val emergencySubstitutionEvent =
            GameEvent.createEmergencySubstitution(
                game = game,
                incomingPlayer = replacementPlayer,
                outgoingPlayer = ejectedPlayer,
                reason = request.reason,
                description = description,
            )

        val savedEvent = gameEventRepository.save(emergencySubstitutionEvent)
        log.info(
            "긴급 교체 처리 완료 (gameId={}, ejectedPlayerId={}, replacementPlayerId={}, reason={})",
            gameId,
            request.ejectedPlayerId,
            request.replacementPlayerId,
            request.reason,
        )
        return savedEvent
    }

    /**
     * 퇴장 선수가 주자인 경우 베이스를 비웁니다.
     */
    private fun clearRunnerBase(
        game: Game,
        ejectedPlayerId: Long,
    ) {
        for (base in listOf(Base.FIRST, Base.SECOND, Base.THIRD)) {
            if (game.gameState.getRunner(base) == ejectedPlayerId) {
                game.gameState.setRunner(base, null)
                gameRepository.save(game)
                log.debug(
                    "퇴장 선수 베이스 비움 (gameId={}, playerId={}, base={})",
                    game.id,
                    ejectedPlayerId,
                    base,
                )
                break
            }
        }
    }

    /**
     * 퇴장 선수가 주자인 경우 교체 선수가 해당 베이스를 계승합니다.
     * 담당 투수(책임투수) ID는 기존 값을 유지합니다.
     */
    private fun inheritRunnerBase(
        game: Game,
        ejectedPlayerId: Long,
        replacementPlayerId: Long,
    ) {
        for (base in listOf(Base.FIRST, Base.SECOND, Base.THIRD)) {
            if (game.gameState.getRunner(base) == ejectedPlayerId) {
                val existingPitcherId = game.gameState.getRunnerPitcherId(base)
                game.gameState.setRunner(base, replacementPlayerId, existingPitcherId)
                gameRepository.save(game)
                log.debug(
                    "주자 베이스 계승 (gameId={}, base={}, ejected={}, replacement={}, pitcherId={})",
                    game.id,
                    base,
                    ejectedPlayerId,
                    replacementPlayerId,
                    existingPitcherId,
                )
                break
            }
        }
    }

    /**
     * 투수 퇴장 시 이전 투수의 이닝 마감 처리를 수행합니다.
     */
    private fun closePitcherInning(
        pitcher: GamePlayer,
        game: Game,
    ) {
        val pitchingRecord =
            pitchingRecordRepository.findByGamePlayerId(pitcher.id)
        pitchingRecord?.closeInning(
            currentInning = game.currentInning,
            currentOuts = game.gameState.outs,
        )
        if (pitchingRecord != null) {
            pitchingRecordRepository.save(pitchingRecord)
            log.debug(
                "투수 퇴장 - 이닝 마감 처리 완료 (gamePlayerId={}, inning={}, outs={})",
                pitcher.id,
                game.currentInning,
                game.gameState.outs,
            )
        }
    }

    /**
     * 교체 선수의 BattingRecord 및 PitchingRecord를 자동 생성합니다.
     */
    private fun createRecordsForSubstitute(incomingPlayer: GamePlayer) {
        if (incomingPlayer.battingOrder != null) {
            val existingBattingRecord =
                battingRecordRepository.findByGamePlayerId(incomingPlayer.id)
            if (existingBattingRecord == null) {
                val battingRecord = BattingRecord.create(gamePlayer = incomingPlayer)
                battingRecordRepository.save(battingRecord)
                log.debug(
                    "긴급 교체 선수 BattingRecord 자동 생성 (gamePlayerId={})",
                    incomingPlayer.id,
                )
            }
        }

        if (incomingPlayer.isPitcher) {
            val existingPitchingRecord =
                pitchingRecordRepository.findByGamePlayerId(incomingPlayer.id)
            if (existingPitchingRecord == null) {
                val pitchingRecord =
                    PitchingRecord.create(
                        gamePlayer = incomingPlayer,
                        isStartingPitcher = false,
                    )
                pitchingRecordRepository.save(pitchingRecord)
                log.debug(
                    "긴급 교체 투수 PitchingRecord 자동 생성 (gamePlayerId={})",
                    incomingPlayer.id,
                )
            }
        }
    }

    private fun findGame(id: Long): Game =
        gameRepository.findByIdOrNull(id)
            ?: throw GameNotFoundException(id)

    private fun validateGameInProgress(game: Game) {
        if (game.status != GameStatus.IN_PROGRESS) {
            throw InvalidGameStateException(
                "진행 중인 경기만 퇴장/긴급 교체를 할 수 있습니다. 현재 상태: ${game.status.displayName}",
            )
        }
    }
}
