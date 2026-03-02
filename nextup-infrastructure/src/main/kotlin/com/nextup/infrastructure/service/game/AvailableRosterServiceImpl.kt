package com.nextup.infrastructure.service.game

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.port.repository.CompetitionPlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.service.game.AvailableRosterService
import com.nextup.core.service.game.dto.AvailableRosterDto
import com.nextup.core.service.game.dto.RosterPlayerDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 라인업 제출용 로스터 조회 서비스 구현
 *
 * 특정 경기의 대회에 등록된 팀 소속 선수 목록을
 * CompetitionPlayer 징계/자격 상태와 함께 반환합니다.
 */
@Service
@Transactional(readOnly = true)
class AvailableRosterServiceImpl(
    private val gameRepository: GameRepositoryPort,
    private val teamRepository: TeamRepositoryPort,
    private val competitionPlayerRepository: CompetitionPlayerRepositoryPort,
) : AvailableRosterService {
    override fun getAvailableRoster(
        gameId: Long,
        teamId: Long,
    ): AvailableRosterDto {
        val game =
            gameRepository.findByIdWithTeams(gameId)
                ?: throw GameNotFoundException(gameId)

        teamRepository.findByIdOrNull(teamId)
            ?: throw TeamNotFoundException(teamId)

        val competitionId = game.competition.id

        val competitionPlayers =
            competitionPlayerRepository.findByCompetitionIdAndTeamId(
                competitionId = competitionId,
                teamId = teamId,
            )

        val players =
            competitionPlayers.map { cp ->
                RosterPlayerDto(
                    playerId = cp.player.id,
                    playerName = cp.player.name,
                    primaryPosition = cp.player.primaryPosition,
                    profileImageUrl = cp.player.profileImageUrl,
                    competitionPlayerStatus = cp.status,
                    isEligible = cp.isEligible,
                )
            }

        return AvailableRosterDto(players = players)
    }
}
