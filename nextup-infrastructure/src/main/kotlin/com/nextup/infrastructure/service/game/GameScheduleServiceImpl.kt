package com.nextup.infrastructure.service.game

import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.common.PageCommand
import com.nextup.core.common.PageResult
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.service.game.GameScheduleService
import com.nextup.core.service.game.dto.GameDetailDto
import com.nextup.core.service.game.dto.GameSummaryDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 경기 일정 조회 서비스 구현
 */
@Service
@Transactional(readOnly = true)
class GameScheduleServiceImpl(
    private val gameRepository: GameRepositoryPort,
    private val gameTeamRepository: GameTeamRepositoryPort,
) : GameScheduleService {
    override fun getGames(
        date: LocalDate?,
        teamId: Long?,
        competitionId: Long?,
        page: Int,
        size: Int,
    ): PageResult<GameSummaryDto> {
        val pageCommand = PageCommand(page = page, size = size)
        val pageResult =
            gameRepository.findGames(
                date = date,
                teamId = teamId,
                competitionId = competitionId,
                status = null,
                pageCommand = pageCommand,
            )

        val gameTeams = gameTeamRepository.findAllByGameIds(pageResult.content.map { it.id })
        val gameTeamsByGameId = gameTeams.groupBy { it.game.id }

        return pageResult.map { game ->
            val teams = gameTeamsByGameId[game.id] ?: emptyList()
            toSummaryDto(game, teams)
        }
    }

    override fun getGameDetail(gameId: Long): GameDetailDto {
        val game =
            gameRepository.findByIdWithTeams(gameId)
                ?: throw GameNotFoundException(gameId)

        val gameTeams = gameTeamRepository.findAllByGameId(gameId)
        val homeTeam = gameTeams.firstOrNull { it.homeAway == HomeAway.HOME }
        val awayTeam = gameTeams.firstOrNull { it.homeAway == HomeAway.AWAY }

        return GameDetailDto(
            gameId = game.id,
            competitionId = game.competition.id,
            competitionName = game.competition.name,
            homeTeamId = homeTeam?.team?.id ?: 0L,
            homeTeamName = homeTeam?.team?.name ?: "",
            homeTeamLogoUrl = homeTeam?.team?.logoUrl,
            awayTeamId = awayTeam?.team?.id ?: 0L,
            awayTeamName = awayTeam?.team?.name ?: "",
            awayTeamLogoUrl = awayTeam?.team?.logoUrl,
            scheduledAt = game.scheduledAt,
            status = game.status,
            homeScore = homeTeam?.totalScore ?: 0,
            awayScore = awayTeam?.totalScore ?: 0,
            location = game.location,
            fieldName = game.fieldName,
            gameNumber = game.gameNumber,
            currentInning = game.currentInningDisplay,
            totalInnings = game.totalInnings,
            startedAt = game.startedAt,
            endedAt = game.endedAt,
            note = game.note,
            forfeitReason = game.forfeitReason,
        )
    }

    override fun getGamesByTeam(teamId: Long): List<GameSummaryDto> {
        val gameTeams = gameTeamRepository.findAllByTeamId(teamId)
        val gameIds = gameTeams.map { it.game.id }.distinct()
        val games =
            gameRepository.findAllByIds(gameIds)
                .sortedBy { it.scheduledAt }

        return toSummaryDtos(games)
    }

    override fun getUpcomingGamesByTeam(
        teamId: Long,
        limit: Int,
    ): List<GameSummaryDto> {
        val now = LocalDateTime.now()
        val gameTeams = gameTeamRepository.findAllByTeamId(teamId)
        val gameIds = gameTeams.map { it.game.id }.distinct()
        val games =
            gameRepository.findAllByIds(gameIds)
                .filter { it.scheduledAt.isAfter(now) && it.status == GameStatus.SCHEDULED }
                .sortedBy { it.scheduledAt }
                .take(limit)

        return toSummaryDtos(games)
    }

    override fun getUpcomingGamesByTeamIds(
        teamIds: List<Long>,
        limit: Int,
    ): List<GameSummaryDto> {
        if (teamIds.isEmpty()) return emptyList()

        val now = LocalDateTime.now()
        // 배치 조회로 N+1 방지 — teamIds.flatMap 대신 단일 쿼리
        val gameTeams = gameTeamRepository.findAllByTeamIdIn(teamIds)
        val gameIds = gameTeams.map { it.game.id }.distinct()
        val games =
            gameRepository.findAllByIds(gameIds)
                .filter { it.scheduledAt.isAfter(now) && it.status == GameStatus.SCHEDULED }
                .sortedBy { it.scheduledAt }
                .take(limit)

        return toSummaryDtos(games)
    }

    override fun getGameDaysInMonth(
        year: Int,
        month: Int,
        teamId: Long?,
    ): List<Int> = gameRepository.findGameDaysInMonth(year, month, teamId)

    private fun toSummaryDtos(games: List<Game>): List<GameSummaryDto> {
        if (games.isEmpty()) return emptyList()

        val gameIds = games.map { it.id }
        val allGameTeams = gameTeamRepository.findAllByGameIds(gameIds)
        val gameTeamsByGameId = allGameTeams.groupBy { it.game.id }

        return games.map { game ->
            val teams = gameTeamsByGameId[game.id] ?: emptyList()
            toSummaryDto(game, teams)
        }
    }

    private fun toSummaryDto(
        game: Game,
        gameTeams: List<GameTeam>,
    ): GameSummaryDto {
        val homeTeam = gameTeams.firstOrNull { it.homeAway == HomeAway.HOME }
        val awayTeam = gameTeams.firstOrNull { it.homeAway == HomeAway.AWAY }

        return GameSummaryDto(
            gameId = game.id,
            competitionId = game.competition.id,
            competitionName = game.competition.name,
            homeTeamId = homeTeam?.team?.id ?: 0L,
            homeTeamName = homeTeam?.team?.name ?: "",
            homeTeamLogoUrl = homeTeam?.team?.logoUrl,
            awayTeamId = awayTeam?.team?.id ?: 0L,
            awayTeamName = awayTeam?.team?.name ?: "",
            awayTeamLogoUrl = awayTeam?.team?.logoUrl,
            scheduledAt = game.scheduledAt,
            status = game.status,
            homeScore = homeTeam?.totalScore ?: 0,
            awayScore = awayTeam?.totalScore ?: 0,
            location = game.location,
            fieldName = game.fieldName,
        )
    }
}
