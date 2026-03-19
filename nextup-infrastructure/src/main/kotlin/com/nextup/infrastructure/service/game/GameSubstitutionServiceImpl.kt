package com.nextup.infrastructure.service.game

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.GamePlayerNotFoundException
import com.nextup.common.exception.InvalidGameStateException
import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameEvent
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.PositionCategory
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.service.game.GameSubstitutionService
import com.nextup.core.service.game.dto.SubstitutionRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 선수 교체 서비스 구현
 *
 * 선수 교체, DH 해제 규칙 검증을 담당합니다.
 * 투수 교체 시 이전 투수의 이닝 마감 및 교체 선수의 기록 자동 생성을 수행합니다.
 */
@Service
@Transactional(readOnly = true)
class GameSubstitutionServiceImpl(
    private val gameRepository: GameRepositoryPort,
    private val gamePlayerRepository: GamePlayerRepositoryPort,
    private val gameEventRepository: GameEventRepositoryPort,
    private val battingRecordRepository: BattingRecordRepositoryPort,
    private val pitchingRecordRepository: PitchingRecordRepositoryPort,
) : GameSubstitutionService {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun substitutePlayer(
        gameId: Long,
        request: SubstitutionRequest,
    ): GameEvent {
        val game = findGame(gameId)

        if (game.status != GameStatus.IN_PROGRESS) {
            throw InvalidGameStateException(
                "진행 중인 경기만 선수 교체를 할 수 있습니다. 현재 상태: ${game.status.displayName}",
            )
        }

        val outgoingPlayer =
            gamePlayerRepository.findByIdOrNull(request.outgoingPlayerId)
                ?: throw GamePlayerNotFoundException(request.outgoingPlayerId)

        val incomingPlayer =
            gamePlayerRepository.findByIdOrNull(request.incomingPlayerId)
                ?: throw GamePlayerNotFoundException(request.incomingPlayerId)

        if (incomingPlayer.hasExited) {
            throw InvalidGameStateException(
                "이미 퇴장한 선수는 재출전할 수 없습니다. (GamePlayer ID: ${request.incomingPlayerId})",
            )
        }

        if (!outgoingPlayer.isCurrentlyPlaying) {
            throw InvalidGameStateException(
                "현재 출전 중인 선수만 교체할 수 있습니다. (GamePlayer ID: ${request.outgoingPlayerId})",
            )
        }

        if (game.gameState.wasDhReleased && request.newPosition == Position.DESIGNATED_HITTER) {
            throw InvalidGameStateException(
                "DH가 이미 해제되었으므로 재지정할 수 없습니다.",
            )
        }

        if (outgoingPlayer.isDesignatedHitter) {
            try {
                outgoingPlayer.validateDhRelease(incomingPlayer, request.newBattingOrder)
            } catch (e: IllegalArgumentException) {
                throw InvalidGameStateException(e.message ?: "DH 해제 규칙 위반")
            }
        }

        val dhReleaseRequired = outgoingPlayer.isDhReleaseRequired(incomingPlayer)
        if (dhReleaseRequired) {
            outgoingPlayer.releaseDH()
            game.gameState.wasDhReleased = true
            gamePlayerRepository.save(outgoingPlayer)
        }

        // M-10: 투수 교체 시 이전 투수의 이닝 마감 처리
        if (outgoingPlayer.isPitcher) {
            val pitchingRecord =
                pitchingRecordRepository.findByGamePlayerId(outgoingPlayer.id)
            pitchingRecord?.closeInning(
                currentInning = game.currentInning,
                currentOuts = game.gameState.outs,
            )
            if (pitchingRecord != null) {
                pitchingRecordRepository.save(pitchingRecord)
                log.debug(
                    "투수 교체 - 이닝 마감 처리 완료 (gamePlayerId={}, inning={}, outs={})",
                    outgoingPlayer.id,
                    game.currentInning,
                    game.gameState.outs,
                )
            }
        }

        outgoingPlayer.exitGame(game.currentInning)
        gamePlayerRepository.save(outgoingPlayer)

        incomingPlayer.enterAsSubstitute(
            inning = game.currentInning,
            newPosition = request.newPosition,
            newBattingOrder = request.newBattingOrder,
        )
        gamePlayerRepository.save(incomingPlayer)

        // M-11: 교체 선수 기록 자동 생성
        createRecordsForSubstitute(incomingPlayer)

        // C2: 투수 교체 시 currentPitcherId 갱신
        if (request.newPosition.category == PositionCategory.PITCHER) {
            game.gameState.currentPitcherId = incomingPlayer.id
            gameRepository.save(game)
            log.debug(
                "currentPitcherId 갱신 (gameId={}, newPitcherId={})",
                game.id,
                incomingPlayer.id,
            )
        }

        val halfInning = if (game.isTopInning) "초" else "말"
        val dhReleaseNote = if (dhReleaseRequired) " (DH 규칙 해제)" else ""
        val description =
            "${game.currentInning}회$halfInning: ${outgoingPlayer.player.name} → ${incomingPlayer.player.name} (${request.newPosition.displayName})$dhReleaseNote"

        val substitutionEvent =
            GameEvent.createSubstitution(
                game = game,
                incomingPlayer = incomingPlayer,
                outgoingPlayer = outgoingPlayer,
                description = description,
            )

        return gameEventRepository.save(substitutionEvent)
    }

    /**
     * 교체 선수의 BattingRecord 및 PitchingRecord를 자동 생성합니다.
     *
     * - 타순이 있는 교체 선수(대타/대주자): BattingRecord 자동 생성
     * - 투수 포지션 교체 선수(구원투수): PitchingRecord 자동 생성
     * - 이미 기록이 존재하는 경우에는 생성하지 않습니다.
     */
    private fun createRecordsForSubstitute(incomingPlayer: com.nextup.core.domain.game.GamePlayer) {
        // 타순이 있는 교체 선수는 BattingRecord 생성
        if (incomingPlayer.battingOrder != null) {
            val existingBattingRecord =
                battingRecordRepository.findByGamePlayerId(incomingPlayer.id)
            if (existingBattingRecord == null) {
                val battingRecord = BattingRecord.create(gamePlayer = incomingPlayer)
                battingRecordRepository.save(battingRecord)
                log.debug(
                    "교체 선수 BattingRecord 자동 생성 (gamePlayerId={})",
                    incomingPlayer.id,
                )
            }
        }

        // 투수 포지션 교체 선수는 PitchingRecord 생성
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
                    "교체 투수 PitchingRecord 자동 생성 (gamePlayerId={})",
                    incomingPlayer.id,
                )
            }
        }
    }

    private fun findGame(id: Long): Game =
        gameRepository.findByIdOrNull(id)
            ?: throw GameNotFoundException(id)
}
