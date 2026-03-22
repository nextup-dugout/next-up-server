package com.nextup.infrastructure.service.game

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.GamePlayerNotFoundException
import com.nextup.common.exception.InvalidGameStateException
import com.nextup.core.domain.event.PositionChangedEvent
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameEvent
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.PositionCategory
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.service.game.GamePositionChangeService
import com.nextup.core.service.game.dto.PositionChangeRequest
import com.nextup.core.service.game.dto.PositionSwapRequest
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 수비 위치 변경 서비스 구현
 *
 * 경기 중 선수의 포지션 변경 및 두 선수 간 포지션 교환을 담당합니다.
 * DH 해제 규칙 검증 및 currentPitcherId 갱신을 포함합니다.
 */
@Service
@Transactional(readOnly = true)
class GamePositionChangeServiceImpl(
    private val gameRepository: GameRepositoryPort,
    private val gamePlayerRepository: GamePlayerRepositoryPort,
    private val gameEventRepository: GameEventRepositoryPort,
    private val eventPublisher: ApplicationEventPublisher,
) : GamePositionChangeService {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun changePosition(
        gameId: Long,
        request: PositionChangeRequest,
        scorerId: Long,
    ): GameEvent {
        val game = findGame(gameId)
        game.validateScorer(scorerId)

        if (game.status != GameStatus.IN_PROGRESS) {
            throw InvalidGameStateException(
                "진행 중인 경기만 포지션 변경을 할 수 있습니다. 현재 상태: ${game.status.displayName}",
            )
        }

        val player =
            gamePlayerRepository.findByIdOrNull(request.playerId)
                ?: throw GamePlayerNotFoundException(request.playerId)

        if (!player.isCurrentlyPlaying) {
            throw InvalidGameStateException(
                "현재 출전 중인 선수만 포지션을 변경할 수 있습니다. (GamePlayer ID: ${request.playerId})",
            )
        }

        val fromPosition = player.position
        val toPosition = request.newPosition

        if (fromPosition == toPosition) {
            throw InvalidGameStateException(
                "변경 전 포지션과 변경 후 포지션이 동일합니다. (포지션: ${toPosition.displayName})",
            )
        }

        // DH가 이미 해제된 경우 DH 포지션 지정 불가
        if (game.gameState.wasDhReleased && toPosition == Position.DESIGNATED_HITTER) {
            throw InvalidGameStateException(
                "DH가 이미 해제되었으므로 재지정할 수 없습니다.",
            )
        }

        // 포지션 변경
        player.changePosition(toPosition, game.currentInning)
        gamePlayerRepository.save(player)

        // 투수 포지션으로 변경 시 currentPitcherId 갱신
        if (toPosition.category == PositionCategory.PITCHER) {
            game.gameState.currentPitcherId = player.id
            gameRepository.save(game)
            log.debug(
                "currentPitcherId 갱신 (gameId={}, newPitcherId={})",
                game.id,
                player.id,
            )
        }

        // 투수→야수 포지션 변경 시 DH 해제 검사
        if (fromPosition.category == PositionCategory.PITCHER &&
            toPosition.category != PositionCategory.PITCHER
        ) {
            checkAndReleaseDhOnPitcherToField(game, player.id)
        }

        val halfInning = if (game.isTopInning) "초" else "말"
        val description =
            "${game.currentInning}회$halfInning: ${player.player.name} ${fromPosition.displayName} → ${toPosition.displayName}"

        val positionChangeEvent =
            GameEvent.createPositionChange(
                game = game,
                player = player,
                fromPosition = fromPosition,
                toPosition = toPosition,
                description = description,
            )

        val savedEvent = gameEventRepository.save(positionChangeEvent)
        eventPublisher.publishEvent(
            PositionChangedEvent(
                gameId = gameId,
                gameEventId = savedEvent.id,
            ),
        )

        log.debug(
            "포지션 변경 완료 (gameId={}, playerId={}, {} → {})",
            gameId,
            request.playerId,
            fromPosition.displayName,
            toPosition.displayName,
        )

        return savedEvent
    }

    @Transactional
    override fun swapPositions(
        gameId: Long,
        request: PositionSwapRequest,
        scorerId: Long,
    ): List<GameEvent> {
        val game = findGame(gameId)
        game.validateScorer(scorerId)

        if (game.status != GameStatus.IN_PROGRESS) {
            throw InvalidGameStateException(
                "진행 중인 경기만 포지션 교환을 할 수 있습니다. 현재 상태: ${game.status.displayName}",
            )
        }

        if (request.player1Id == request.player2Id) {
            throw InvalidGameStateException(
                "같은 선수 간 포지션 교환은 불가합니다. (GamePlayer ID: ${request.player1Id})",
            )
        }

        val player1 =
            gamePlayerRepository.findByIdOrNull(request.player1Id)
                ?: throw GamePlayerNotFoundException(request.player1Id)

        val player2 =
            gamePlayerRepository.findByIdOrNull(request.player2Id)
                ?: throw GamePlayerNotFoundException(request.player2Id)

        if (!player1.isCurrentlyPlaying) {
            throw InvalidGameStateException(
                "현재 출전 중인 선수만 포지션을 교환할 수 있습니다. (GamePlayer ID: ${request.player1Id})",
            )
        }

        if (!player2.isCurrentlyPlaying) {
            throw InvalidGameStateException(
                "현재 출전 중인 선수만 포지션을 교환할 수 있습니다. (GamePlayer ID: ${request.player2Id})",
            )
        }

        val position1 = player1.position
        val position2 = player2.position

        if (position1 == position2) {
            throw InvalidGameStateException(
                "두 선수의 포지션이 동일하여 교환할 수 없습니다. (포지션: ${position1.displayName})",
            )
        }

        // DH가 이미 해제된 경우 DH 포지션으로 교환 불가
        if (game.gameState.wasDhReleased) {
            if (position2 == Position.DESIGNATED_HITTER || position1 == Position.DESIGNATED_HITTER) {
                throw InvalidGameStateException(
                    "DH가 이미 해제되었으므로 DH 포지션 관련 교환은 불가합니다.",
                )
            }
        }

        // 포지션 교환
        player1.changePosition(position2, game.currentInning)
        player2.changePosition(position1, game.currentInning)
        gamePlayerRepository.save(player1)
        gamePlayerRepository.save(player2)

        // 투수 포지션 관련 갱신
        if (position2.category == PositionCategory.PITCHER) {
            // player1이 투수 포지션으로 이동
            game.gameState.currentPitcherId = player1.id
            gameRepository.save(game)
            log.debug(
                "currentPitcherId 갱신 (gameId={}, newPitcherId={})",
                game.id,
                player1.id,
            )
        } else if (position1.category == PositionCategory.PITCHER) {
            // player2가 투수 포지션으로 이동
            game.gameState.currentPitcherId = player2.id
            gameRepository.save(game)
            log.debug(
                "currentPitcherId 갱신 (gameId={}, newPitcherId={})",
                game.id,
                player2.id,
            )
        }

        val halfInning = if (game.isTopInning) "초" else "말"

        val event1Description =
            "${game.currentInning}회$halfInning: ${player1.player.name} ${position1.displayName} → ${position2.displayName}"
        val event2Description =
            "${game.currentInning}회$halfInning: ${player2.player.name} ${position2.displayName} → ${position1.displayName}"

        val positionChangeEvent1 =
            GameEvent.createPositionChange(
                game = game,
                player = player1,
                fromPosition = position1,
                toPosition = position2,
                description = event1Description,
            )
        val positionChangeEvent2 =
            GameEvent.createPositionChange(
                game = game,
                player = player2,
                fromPosition = position2,
                toPosition = position1,
                description = event2Description,
            )

        val savedEvent1 = gameEventRepository.save(positionChangeEvent1)
        val savedEvent2 = gameEventRepository.save(positionChangeEvent2)

        eventPublisher.publishEvent(
            PositionChangedEvent(
                gameId = gameId,
                gameEventId = savedEvent1.id,
            ),
        )

        log.debug(
            "포지션 교환 완료 (gameId={}, player1Id={}, player2Id={}, {} ↔ {})",
            gameId,
            request.player1Id,
            request.player2Id,
            position1.displayName,
            position2.displayName,
        )

        return listOf(savedEvent1, savedEvent2)
    }

    /**
     * 투수가 야수 포지션으로 이동할 때 DH 해제 여부를 확인합니다.
     *
     * 실제 DH 해제(투수가 DH 자리로 타순 이동)는 교체 이벤트에서 처리됩니다.
     * 포지션 변경에서는 단순 수비 위치 변경이므로 경고 로그만 기록합니다.
     */
    private fun checkAndReleaseDhOnPitcherToField(
        game: Game,
        pitcherPlayerId: Long,
    ) {
        log.debug(
            "투수 → 야수 포지션 변경 감지 (gameId={}, gamePlayerId={})",
            game.id,
            pitcherPlayerId,
        )
    }

    private fun findGame(id: Long): Game =
        gameRepository.findByIdOrNull(id)
            ?: throw GameNotFoundException(id)
}
