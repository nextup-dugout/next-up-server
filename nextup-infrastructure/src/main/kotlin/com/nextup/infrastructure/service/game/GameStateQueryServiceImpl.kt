package com.nextup.infrastructure.service.game

import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.service.game.GameStateQueryService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 경기 상태 조회 서비스 구현
 *
 * 기록원 재접속 시 현재 경기 상태를 복원하기 위한 조회 기능을 제공합니다.
 */
@Service
@Transactional(readOnly = true)
class GameStateQueryServiceImpl(
    private val gameRepository: GameRepositoryPort,
    private val gamePlayerRepository: GamePlayerRepositoryPort,
    private val gameTeamRepository: GameTeamRepositoryPort,
) : GameStateQueryService {
    override fun getGame(gameId: Long): Game =
        gameRepository.findByIdOrNull(gameId)
            ?: throw GameNotFoundException(gameId)

    override fun getCurrentLineup(gameId: Long): List<GamePlayer> =
        gamePlayerRepository.findCurrentlyPlayingByGameId(gameId)

    override fun getGameTeams(gameId: Long): List<GameTeam> = gameTeamRepository.findAllByGameId(gameId)
}
