package com.nextup.core.service.competition

import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.common.exception.InvalidCompetitionStateException
import com.nextup.core.domain.competition.CompetitionPlayer
import com.nextup.core.domain.competition.CompetitionPlayerStatus
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.port.repository.CompetitionPlayerRepositoryPort
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.service.competition.dto.NextSeasonPreparationResult
import com.nextup.core.service.competition.dto.SeasonSummaryDto
import com.nextup.core.service.standings.StandingsService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 시즌 전환 서비스
 *
 * 시즌 종료 후 요약 조회, 다음 시즌 대회 생성 및 선수 재등록을 처리합니다.
 */
@Service
@Transactional(readOnly = true)
class SeasonTransitionService(
    private val competitionRepository: CompetitionRepositoryPort,
    private val competitionPlayerRepository: CompetitionPlayerRepositoryPort,
    private val standingsService: StandingsService,
    private val competitionService: CompetitionService,
) {
    /**
     * 완료된 대회의 시즌 요약을 조회합니다.
     *
     * 최종 순위, 참가 팀/선수 통계를 반환합니다.
     */
    fun getSeasonSummary(competitionId: Long): SeasonSummaryDto {
        val competition =
            competitionRepository.findByIdWithLeague(competitionId)
                ?: throw CompetitionNotFoundException(competitionId)

        if (competition.status != CompetitionStatus.COMPLETED) {
            throw InvalidCompetitionStateException(
                "Season summary is only available for completed competitions",
            )
        }

        val standings = standingsService.getStandings(competitionId)
        val players = competitionPlayerRepository.findByCompetitionId(competitionId)
        val teamIds = players.map { it.team.id }.distinct()

        return SeasonSummaryDto(
            competitionId = competition.id,
            competitionName = competition.name,
            year = competition.year,
            season = competition.season,
            startDate = competition.startDate,
            endDate = competition.endDate,
            totalTeams = teamIds.size,
            totalPlayers = players.size,
            finalStandings = standings.standings,
        )
    }

    /**
     * 다음 시즌 대회를 준비합니다.
     *
     * 이전 시즌의 참가 팀/선수를 기반으로 새 대회를 생성하고,
     * 이전 시즌 활성(ACTIVE) 선수를 자동 등록합니다.
     * 탈퇴(WITHDRAWN)/정지(SUSPENDED) 선수는 제외됩니다.
     */
    @Transactional
    fun prepareNextSeason(
        previousCompetitionId: Long,
        name: String,
        startDate: LocalDate,
        endDate: LocalDate? = null,
        description: String? = null,
        maxTeams: Int? = null,
    ): NextSeasonPreparationResult {
        val previousCompetition =
            competitionRepository.findByIdWithLeague(previousCompetitionId)
                ?: throw CompetitionNotFoundException(previousCompetitionId)

        if (previousCompetition.status != CompetitionStatus.COMPLETED) {
            throw InvalidCompetitionStateException(
                "Next season can only be prepared from a completed competition",
            )
        }

        // 다음 시즌 번호 결정 (같은 연도 내 시즌 증가 또는 다음 해 시즌 1)
        val nextYear = if (startDate.year > previousCompetition.year) startDate.year else previousCompetition.year
        val nextSeason =
            if (nextYear > previousCompetition.year) {
                1
            } else {
                previousCompetition.season + 1
            }

        // 새 대회 생성
        val newCompetition =
            competitionService.create(
                leagueId = previousCompetition.league.id,
                name = name,
                year = nextYear,
                season = nextSeason,
                type = previousCompetition.type,
                startDate = startDate,
                endDate = endDate,
                description = description,
                maxTeams = maxTeams ?: previousCompetition.maxTeams,
            )

        // 이전 시즌 활성 선수 조회
        val activePlayers =
            competitionPlayerRepository.findByCompetitionIdAndStatus(
                previousCompetitionId,
                CompetitionPlayerStatus.ACTIVE,
            )

        // 이전 시즌의 전체 선수 수 (스킵된 선수 수 계산용)
        val allPreviousPlayers =
            competitionPlayerRepository.findByCompetitionId(previousCompetitionId)
        val skippedCount = allPreviousPlayers.size - activePlayers.size

        // 활성 선수를 새 대회에 등록
        val newRegistrations =
            activePlayers.map { previousPlayer ->
                CompetitionPlayer.register(
                    competition = newCompetition,
                    team = previousPlayer.team,
                    player = previousPlayer.player,
                )
            }
        competitionPlayerRepository.saveAll(newRegistrations)

        val registeredTeamIds = newRegistrations.map { it.team.id }.distinct()

        return NextSeasonPreparationResult(
            newCompetitionId = newCompetition.id,
            newCompetitionName = newCompetition.name,
            year = nextYear,
            season = nextSeason,
            previousCompetitionId = previousCompetitionId,
            registeredTeamCount = registeredTeamIds.size,
            registeredPlayerCount = newRegistrations.size,
            skippedPlayerCount = skippedCount,
        )
    }
}
