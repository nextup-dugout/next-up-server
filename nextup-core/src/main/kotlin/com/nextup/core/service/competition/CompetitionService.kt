package com.nextup.core.service.competition

import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.common.exception.CrossRegistrationException
import com.nextup.common.exception.InvalidCompetitionStateException
import com.nextup.common.exception.LeagueNotFoundException
import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionPlayer
import com.nextup.core.domain.competition.CompetitionPlayerStatus
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.BracketEntryRepositoryPort
import com.nextup.core.port.repository.CompetitionPlayerRepositoryPort
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.LeagueRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.service.competition.dto.TeamWithdrawalResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 대회 서비스
 *
 * 비즈니스 로직은 Entity에 위임하고, 서비스는 조율(orchestration)만 수행합니다.
 */
@Service
@Transactional(readOnly = true)
class CompetitionService(
    private val competitionRepository: CompetitionRepositoryPort,
    private val leagueRepository: LeagueRepositoryPort,
    private val competitionPlayerRepository: CompetitionPlayerRepositoryPort,
    private val gameRepository: GameRepositoryPort,
    private val teamRepository: TeamRepositoryPort,
    private val bracketEntryRepository: BracketEntryRepositoryPort,
    private val playerRepository: PlayerRepositoryPort,
) {
    /**
     * 대회를 생성합니다.
     */
    @Transactional
    fun create(
        leagueId: Long,
        name: String,
        year: Int,
        season: Int = 1,
        type: CompetitionType = CompetitionType.LEAGUE,
        startDate: LocalDate,
        endDate: LocalDate? = null,
        description: String? = null,
        maxTeams: Int? = null,
    ): Competition {
        // 리그 존재 확인
        val league =
            leagueRepository.findByIdOrNull(leagueId)
                ?: throw LeagueNotFoundException(leagueId)

        // 동일한 리그, 연도, 시즌의 대회가 이미 존재하는지 확인
        competitionRepository.findByLeagueIdAndYearAndSeason(leagueId, year, season)?.let {
            throw InvalidCompetitionStateException(
                "Competition already exists for league $leagueId, year $year, season $season",
            )
        }

        val competition =
            Competition(
                league = league,
                name = name,
                year = year,
                season = season,
                type = type,
                startDate = startDate,
                endDate = endDate,
                description = description,
                maxTeams = maxTeams,
            )

        return competitionRepository.save(competition)
    }

    /**
     * ID로 대회를 조회합니다.
     */
    fun getById(id: Long): Competition =
        competitionRepository.findByIdOrNull(id)
            ?: throw CompetitionNotFoundException(id)

    /**
     * ID로 대회를 조회합니다 (League 함께 조회).
     */
    fun getByIdWithLeague(id: Long): Competition =
        competitionRepository.findByIdWithLeague(id)
            ?: throw CompetitionNotFoundException(id)

    /**
     * 모든 대회를 조회합니다.
     */
    fun getAll(): List<Competition> = competitionRepository.findAll()

    /**
     * 리그별 대회 목록을 조회합니다.
     */
    fun getByLeagueId(leagueId: Long): List<Competition> = competitionRepository.findByLeagueId(leagueId)

    /**
     * 진행 중인 대회 목록을 조회합니다.
     */
    fun getInProgress(): List<Competition> = competitionRepository.findInProgressCompetitions()

    /**
     * 특정 상태의 대회 목록을 조회합니다.
     */
    fun getByStatus(status: CompetitionStatus): List<Competition> = competitionRepository.findByStatus(status)

    /**
     * 대회 정보를 수정합니다.
     */
    @Transactional
    fun update(
        id: Long,
        name: String? = null,
        description: String? = null,
        endDate: LocalDate? = null,
    ): Competition {
        val competition = getById(id)
        try {
            competition.updateInfo(name = name, description = description, endDate = endDate)
        } catch (e: IllegalArgumentException) {
            throw InvalidCompetitionStateException(e.message ?: "Cannot update competition")
        }
        return competition
    }

    /**
     * 대회를 시작합니다.
     */
    @Transactional
    fun start(id: Long): Competition {
        val competition = getById(id)
        try {
            competition.start()
        } catch (e: IllegalArgumentException) {
            throw InvalidCompetitionStateException(e.message ?: "Cannot start competition")
        }
        return competition
    }

    /**
     * 대회를 완료합니다.
     */
    @Transactional
    fun complete(
        id: Long,
        endDate: LocalDate = LocalDate.now(),
    ): Competition {
        val competition = getById(id)
        try {
            competition.complete(endDate)
        } catch (e: IllegalArgumentException) {
            throw InvalidCompetitionStateException(e.message ?: "Cannot complete competition")
        }
        return competition
    }

    /**
     * 대회를 취소합니다.
     */
    @Transactional
    fun cancel(id: Long): Competition {
        val competition = getById(id)
        try {
            competition.cancel()
        } catch (e: IllegalArgumentException) {
            throw InvalidCompetitionStateException(e.message ?: "Cannot cancel competition")
        }
        return competition
    }

    /**
     * 대회를 연기합니다.
     */
    @Transactional
    fun postpone(id: Long): Competition {
        val competition = getById(id)
        try {
            competition.postpone()
        } catch (e: IllegalArgumentException) {
            throw InvalidCompetitionStateException(e.message ?: "Cannot postpone competition")
        }
        return competition
    }

    /**
     * 대회 참가 팀 목록을 조회합니다.
     *
     * 활성 상태(ACTIVE)의 대회 등록 선수를 기준으로
     * 팀별 등록 선수 수를 반환합니다.
     *
     * @param competitionId 대회 ID
     * @return 팀 → 등록 선수 수 매핑
     */
    fun getCompetitionTeams(competitionId: Long): Map<Team, Int> {
        getById(competitionId)
        val activePlayers =
            competitionPlayerRepository.findByCompetitionIdAndStatus(
                competitionId,
                CompetitionPlayerStatus.ACTIVE,
            )
        return activePlayers.groupBy { it.team }.mapValues { it.value.size }
    }

    /**
     * 대회에 선수를 등록합니다.
     *
     * 크로스 등록 방지: 동일 대회에 이미 다른 팀으로 등록된 선수는 등록할 수 없습니다.
     *
     * @param competitionId 대회 ID
     * @param teamId 등록할 팀 ID
     * @param playerId 등록할 선수 ID
     * @return 등록된 CompetitionPlayer
     * @throws CrossRegistrationException 동일 대회 타팀 등록 시
     */
    @Transactional
    fun registerPlayer(
        competitionId: Long,
        teamId: Long,
        playerId: Long,
    ): CompetitionPlayer {
        val competition = getById(competitionId)
        val team =
            teamRepository.findByIdOrNull(teamId)
                ?: throw TeamNotFoundException(teamId)
        val player =
            playerRepository.findByIdOrNull(playerId)
                ?: throw com.nextup.common.exception.PlayerNotFoundException(playerId)

        validateNoCrossRegistration(competitionId, playerId, teamId)

        val competitionPlayer = CompetitionPlayer.register(competition, team, player)
        return competitionPlayerRepository.save(competitionPlayer)
    }

    /**
     * 대회에 여러 선수를 일괄 등록합니다.
     *
     * 크로스 등록 방지: 동일 대회에 이미 다른 팀으로 등록된 선수는 등록할 수 없습니다.
     *
     * @param competition 대회
     * @param team 등록할 팀
     * @param players 등록할 선수 목록
     * @return 등록된 CompetitionPlayer 목록
     * @throws CrossRegistrationException 동일 대회 타팀 등록 시
     */
    @Transactional
    fun registerPlayers(
        competition: Competition,
        team: Team,
        players: List<Player>,
    ): List<CompetitionPlayer> {
        players.forEach { player ->
            validateNoCrossRegistration(competition.id, player.id, team.id)
        }

        val competitionPlayers =
            players.map { player ->
                CompetitionPlayer.register(competition, team, player)
            }
        return competitionPlayerRepository.saveAll(competitionPlayers)
    }

    /**
     * 크로스 등록 검증: 동일 대회에 다른 팀으로 이미 활성 등록된 선수인지 확인합니다.
     *
     * @throws CrossRegistrationException 동일 대회 타팀 등록이 존재할 경우
     */
    fun validateNoCrossRegistration(
        competitionId: Long,
        playerId: Long,
        teamId: Long,
    ) {
        val existing =
            competitionPlayerRepository.findActiveByCompetitionIdAndPlayerId(
                competitionId,
                playerId,
            )
        if (existing != null && existing.team.id != teamId) {
            throw CrossRegistrationException(
                competitionId = competitionId,
                playerId = playerId,
                existingTeamId = existing.team.id,
            )
        }
    }

    /**
     * 대회에서 팀 전체를 탈퇴시킵니다.
     *
     * 1. 해당 팀 소속 CompetitionPlayer 일괄 WITHDRAWN 처리
     * 2. 잔여 경기(SCHEDULED/IN_PROGRESS) 자동 몰수승 처리
     * 3. 대진표(BracketEntry) 부전승 처리
     *
     * @param competitionId 대회 ID
     * @param teamId 탈퇴할 팀 ID
     * @param reason 탈퇴 사유
     * @return 처리 결과 요약
     */
    @Transactional
    fun withdrawTeam(
        competitionId: Long,
        teamId: Long,
        reason: String,
    ): TeamWithdrawalResult {
        val competition = getById(competitionId)
        if (competition.status != CompetitionStatus.IN_PROGRESS) {
            throw InvalidCompetitionStateException(
                "Team withdrawal is only allowed for in-progress competitions. " +
                    "Current status: ${competition.status}",
            )
        }

        val team =
            teamRepository.findByIdOrNull(teamId)
                ?: throw TeamNotFoundException(teamId)

        // 1. CompetitionPlayer 일괄 WITHDRAWN 처리
        val players = competitionPlayerRepository.findByCompetitionIdAndTeamId(competitionId, teamId)
        if (players.isEmpty()) {
            throw InvalidCompetitionStateException(
                "Team $teamId has no registered players in competition $competitionId",
            )
        }
        var withdrawnPlayerCount = 0
        players.forEach { player ->
            if (player.status != com.nextup.core.domain.competition.CompetitionPlayerStatus.WITHDRAWN) {
                player.withdraw()
                withdrawnPlayerCount++
            }
        }
        competitionPlayerRepository.saveAll(players)

        // 2. 잔여 경기 몰수승 처리
        val games = gameRepository.findByCompetitionId(competitionId)
        var forfeitedGameCount = 0
        games.forEach { game ->
            if (game.status == GameStatus.SCHEDULED ||
                game.status == GameStatus.IN_PROGRESS ||
                game.status == GameStatus.POSTPONED
            ) {
                val gameTeams = game.gameTeams
                val isTeamInGame = gameTeams.any { it.team.id == teamId }
                if (isTeamInGame) {
                    val opponentTeam = gameTeams.first { it.team.id != teamId }
                    game.forfeit(
                        winnerTeamId = opponentTeam.team.id,
                        reason = "팀 대회 탈퇴: $reason",
                        gameTeams = gameTeams,
                    )
                    gameRepository.save(game)
                    forfeitedGameCount++
                }
            }
        }

        // 3. 대진표 부전승 처리
        val bracketEntries = bracketEntryRepository.findByCompetitionId(competitionId)
        var updatedBracketCount = 0
        bracketEntries.forEach { entry ->
            if (!entry.isCompleted()) {
                val isTeam1 = entry.team1?.id == teamId
                val isTeam2 = entry.team2?.id == teamId
                if (isTeam1 && entry.team2 != null) {
                    entry.recordWinner(entry.team2!!)
                    bracketEntryRepository.save(entry)
                    updatedBracketCount++
                } else if (isTeam2 && entry.team1 != null) {
                    entry.recordWinner(entry.team1!!)
                    bracketEntryRepository.save(entry)
                    updatedBracketCount++
                }
            }
        }

        return TeamWithdrawalResult(
            competitionId = competitionId,
            teamId = teamId,
            teamName = team.name,
            withdrawnPlayerCount = withdrawnPlayerCount,
            forfeitedGameCount = forfeitedGameCount,
            updatedBracketEntryCount = updatedBracketCount,
            reason = reason,
        )
    }
}
