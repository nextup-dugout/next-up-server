package com.nextup.scorer.controller

import com.nextup.common.dto.ApiResponse
import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.scorer.dto.websocket.ScoreboardMessage
import com.nextup.scorer.websocket.mapper.ScoreboardMapper
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 스코어보드 REST Controller (Scorer)
 *
 * WebSocket 단절 시 현재 스코어보드를 REST로 조회할 수 있는 fallback 엔드포인트를 제공합니다.
 */
@RestController
@RequestMapping("/api/scorer/games/{gameId}/scoreboard")
class ScoreboardController(
    private val gameRepository: GameRepositoryPort,
    private val gameTeamRepository: GameTeamRepositoryPort,
    private val scoreboardMapper: ScoreboardMapper,
) {
    /**
     * 경기 스코어보드를 조회합니다.
     *
     * WebSocket 연결이 끊어진 경우 현재 스코어보드 상태를 REST로 제공합니다.
     *
     * @param gameId 경기 ID
     * @return 현재 스코어보드 상태
     */
    @GetMapping
    fun getScoreboard(
        @PathVariable gameId: Long,
    ): ResponseEntity<ApiResponse<ScoreboardMessage>> {
        val game =
            gameRepository.findByIdOrNull(gameId)
                ?: throw GameNotFoundException(gameId)

        val gameTeams = gameTeamRepository.findAllByGameId(gameId)
        val homeTeam =
            gameTeams.firstOrNull { it.homeAway == HomeAway.HOME }
                ?: throw GameNotFoundException(gameId)
        val awayTeam =
            gameTeams.firstOrNull { it.homeAway == HomeAway.AWAY }
                ?: throw GameNotFoundException(gameId)

        val scoreboard = scoreboardMapper.toScoreboardMessage(game, homeTeam, awayTeam)

        return ResponseEntity.ok(ApiResponse.success(scoreboard))
    }
}
