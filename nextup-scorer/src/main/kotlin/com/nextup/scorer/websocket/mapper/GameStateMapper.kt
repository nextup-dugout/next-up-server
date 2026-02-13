package com.nextup.scorer.websocket.mapper

import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameState
import com.nextup.scorer.dto.websocket.GameStateMessage
import com.nextup.scorer.dto.websocket.PlayerBriefDto
import com.nextup.scorer.dto.websocket.RunnersDto
import org.springframework.stereotype.Component

/**
 * Game.gameState → GameStateMessage 매퍼
 *
 * 경기 실시간 상태를 WebSocket 메시지로 변환합니다.
 */
@Component
class GameStateMapper {

    /**
     * 경기 상태 메시지를 생성합니다.
     *
     * @param game 경기
     * @param gameState 경기 상태
     * @param currentBatter 현재 타자 (GamePlayer)
     * @param currentPitcher 현재 투수 (GamePlayer)
     * @param runnerOnFirst 1루 주자 (GamePlayer)
     * @param runnerOnSecond 2루 주자 (GamePlayer)
     * @param runnerOnThird 3루 주자 (GamePlayer)
     * @return GameStateMessage
     */
    fun toGameStateMessage(
        game: Game,
        gameState: GameState,
        currentBatter: GamePlayer?,
        currentPitcher: GamePlayer?,
        runnerOnFirst: GamePlayer?,
        runnerOnSecond: GamePlayer?,
        runnerOnThird: GamePlayer?
    ): GameStateMessage {
        return GameStateMessage(
            gameId = game.id,
            inning = game.currentInning,
            isTopInning = game.isTopInning,
            outs = gameState.outs,
            balls = gameState.balls,
            strikes = gameState.strikes,
            runners =
                RunnersDto(
                    first = runnerOnFirst?.let { toPlayerBriefDto(it) },
                    second = runnerOnSecond?.let { toPlayerBriefDto(it) },
                    third = runnerOnThird?.let { toPlayerBriefDto(it) }
                ),
            currentBatter = currentBatter?.let { toPlayerBriefDto(it) },
            currentPitcher = currentPitcher?.let { toPlayerBriefDto(it) }
        )
    }

    /**
     * GamePlayer를 PlayerBriefDto로 변환합니다.
     */
    private fun toPlayerBriefDto(gamePlayer: GamePlayer): PlayerBriefDto {
        return PlayerBriefDto(
            id = gamePlayer.player.id,
            name = gamePlayer.player.name,
            backNumber = gamePlayer.backNumber
        )
    }

    /**
     * 선수 ID로 간략 정보를 생성합니다.
     * 실제 구현에서는 Repository를 통해 선수 정보를 조회해야 합니다.
     */
    fun toPlayerBriefDtoById(
        playerId: Long,
        name: String,
        backNumber: Int?
    ): PlayerBriefDto {
        return PlayerBriefDto(
            id = playerId,
            name = name,
            backNumber = backNumber
        )
    }
}
