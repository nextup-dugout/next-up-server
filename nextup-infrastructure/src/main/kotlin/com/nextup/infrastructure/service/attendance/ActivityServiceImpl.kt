package com.nextup.infrastructure.service.attendance

import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.common.PageCommand
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.service.attendance.ActivityService
import com.nextup.core.service.attendance.PlayerParticipationRate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 활동 점수 서비스 구현 (자동 집계)
 *
 * game_players 테이블에서 경기참여율을 자동 집계합니다.
 */
@Service
@Transactional(readOnly = true)
class ActivityServiceImpl(
    private val teamRepository: TeamRepositoryPort,
    private val teamMemberRepository: TeamMemberRepositoryPort,
    private val gameRepositoryPort: GameRepositoryPort,
    private val gamePlayerRepositoryPort: GamePlayerRepositoryPort,
) : ActivityService {
    override fun getGameParticipationRate(
        teamId: Long,
        playerId: Long,
    ): BigDecimal {
        if (!teamRepository.existsById(teamId)) {
            throw TeamNotFoundException(teamId)
        }

        val teamGameIds = getTeamGameIds(teamId)
        if (teamGameIds.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }

        val playerGameIds: Set<Long> =
            gamePlayerRepositoryPort.findAllByPlayerId(playerId)
                .map { gp: GamePlayer -> gp.gameTeam.game.id }
                .toSet()

        val gamesPlayed = playerGameIds.count { it in teamGameIds }

        return BigDecimal(gamesPlayed)
            .multiply(BigDecimal(100))
            .divide(BigDecimal(teamGameIds.size), 2, RoundingMode.HALF_UP)
    }

    override fun listGameParticipationRates(teamId: Long): List<PlayerParticipationRate> {
        if (!teamRepository.existsById(teamId)) {
            throw TeamNotFoundException(teamId)
        }

        val teamGameIds = getTeamGameIds(teamId)
        val totalGames = teamGameIds.size

        if (totalGames == 0) {
            return emptyList()
        }

        val members = teamMemberRepository.findByTeamId(teamId)

        return members.map { member ->
            val playerGameIds: Set<Long> =
                gamePlayerRepositoryPort.findAllByPlayerId(member.player.id)
                    .map { gp: GamePlayer -> gp.gameTeam.game.id }
                    .toSet()

            val gamesPlayed = playerGameIds.count { it in teamGameIds }
            val rate =
                BigDecimal(gamesPlayed)
                    .multiply(BigDecimal(100))
                    .divide(BigDecimal(totalGames), 2, RoundingMode.HALF_UP)

            PlayerParticipationRate(
                playerId = member.player.id,
                playerName = member.player.name,
                gamesPlayed = gamesPlayed,
                totalTeamGames = totalGames,
                participationRate = rate,
            )
        }
    }

    /**
     * 팀의 경기 ID 목록을 조회합니다.
     */
    private fun getTeamGameIds(teamId: Long): Set<Long> {
        val page =
            gameRepositoryPort.findGames(
                date = null,
                teamId = teamId,
                competitionId = null,
                status = null,
                pageCommand = PageCommand(page = 0, size = 10000),
            )
        return page.content.map { it.id }.toSet()
    }
}
