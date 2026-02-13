package com.nextup.infrastructure.service.report

import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.service.report.CompetitionReportService
import com.nextup.core.service.report.dto.CompetitionReportDto
import com.nextup.core.service.report.dto.CompetitionSummaryDto
import com.nextup.core.service.report.dto.HighScoringGameDto
import com.nextup.core.service.report.dto.WinStreakDto
import com.nextup.core.service.standings.StandingsService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 대회 리포트 서비스 구현
 */
@Service
@Transactional(readOnly = true)
class CompetitionReportServiceImpl(
    private val competitionRepository: CompetitionRepositoryPort,
    private val standingsService: StandingsService,
    private val gameRepository: GameRepositoryPort,
    private val gameTeamRepository: GameTeamRepositoryPort,
    private val battingRecordRepository: BattingRecordRepositoryPort,
    private val pitchingRecordRepository: PitchingRecordRepositoryPort,
) : CompetitionReportService {
    override fun getReport(competitionId: Long): CompetitionReportDto {
        // 1. 대회 조회
        val competition =
            competitionRepository.findByIdOrNull(competitionId)
                ?: throw CompetitionNotFoundException(competitionId)

        // 2. 순위표 조회 (기존 StandingsService 재사용)
        val standings = standingsService.getStandings(competitionId)

        // 3. 요약 통계 조회
        val summary = getReportSummary(competitionId)

        return CompetitionReportDto(
            competitionId = competition.id,
            competitionName = competition.name,
            season = competition.season,
            standings = standings.standings,
            summary = summary,
        )
    }

    override fun getReportSummary(competitionId: Long): CompetitionSummaryDto {
        // 1. 대회 존재 확인
        val competition =
            competitionRepository.findByIdOrNull(competitionId)
                ?: throw CompetitionNotFoundException(competitionId)

        // 2. 대회의 모든 경기 조회
        val allGames = gameRepository.findByCompetitionId(competitionId)
        val completedGames = allGames.filter { it.status.isCompleted() }

        // 3. 완료된 경기의 GameTeam 조회
        val completedGameIds = completedGames.map { it.id }
        val gameTeams =
            if (completedGameIds.isEmpty()) {
                emptyList()
            } else {
                gameTeamRepository.findAllByGameIds(completedGameIds)
            }

        // 4. 통계 계산
        val totalRuns = gameTeams.sumOf { it.totalScore }
        val totalHits = gameTeams.sumOf { it.totalHits }
        val averageRunsPerGame =
            if (completedGames.isNotEmpty()) {
                BigDecimal(totalRuns)
                    .divide(BigDecimal(completedGames.size), 2, RoundingMode.HALF_UP)
                    .toDouble()
            } else {
                0.0
            }

        // 5. 타격/투구 통계 조회
        val battingRecords =
            if (completedGameIds.isEmpty()) {
                emptyList()
            } else {
                completedGameIds.flatMap { battingRecordRepository.findAllByGameId(it) }
            }

        val pitchingRecords =
            if (completedGameIds.isEmpty()) {
                emptyList()
            } else {
                completedGameIds.flatMap { pitchingRecordRepository.findAllByGameId(it) }
            }

        val totalHomeRuns = battingRecords.sumOf { it.homeRuns }
        val totalStrikeouts = pitchingRecords.sumOf { it.strikeouts }

        // 6. 최다 득점 경기 계산
        val highestScoringGame = calculateHighestScoringGame(completedGames, gameTeams)

        // 7. 최장 연승 팀 계산
        val longestWinStreak = calculateLongestWinStreak(competitionId)

        return CompetitionSummaryDto(
            competitionId = competition.id,
            totalGames = allGames.size,
            completedGames = completedGames.size,
            totalRuns = totalRuns,
            averageRunsPerGame = averageRunsPerGame,
            totalHits = totalHits,
            totalHomeRuns = totalHomeRuns,
            totalStrikeouts = totalStrikeouts,
            highestScoringGame = highestScoringGame,
            longestWinStreak = longestWinStreak,
        )
    }

    /**
     * 최다 득점 경기 계산
     */
    private fun calculateHighestScoringGame(
        completedGames: List<com.nextup.core.domain.game.Game>,
        gameTeams: List<com.nextup.core.domain.game.GameTeam>,
    ): HighScoringGameDto? {
        if (completedGames.isEmpty()) return null

        // 경기별 총 득점 계산
        val gameScores =
            completedGames.map { game ->
                val teams = gameTeams.filter { it.game.id == game.id }
                val totalScore = teams.sumOf { it.totalScore }
                Triple(game, teams, totalScore)
            }

        // 최다 득점 경기 찾기
        val highestScoring = gameScores.maxByOrNull { it.third } ?: return null
        val (game, teams) = highestScoring

        if (teams.size < 2) return null

        val homeTeam = teams.find { it.isHome }
        val awayTeam = teams.find { it.isAway }

        return HighScoringGameDto(
            gameId = game.id,
            homeTeamName = homeTeam?.team?.name ?: "Unknown",
            awayTeamName = awayTeam?.team?.name ?: "Unknown",
            totalRuns = highestScoring.third,
            date = game.scheduledAt.toLocalDate(),
        )
    }

    /**
     * 최장 연승 팀 계산
     */
    private fun calculateLongestWinStreak(competitionId: Long): WinStreakDto? {
        val decidedGameTeams =
            gameTeamRepository
                .findAllByCompetitionIdWithDecidedResult(competitionId)
                .filter { it.game.status.isCompleted() }
                .sortedBy { it.game.scheduledAt }

        if (decidedGameTeams.isEmpty()) return null

        // 팀별 경기 그룹화
        val teamGames = decidedGameTeams.groupBy { it.team.id }

        // 각 팀의 최장 연승 계산
        val teamStreaks =
            teamGames.mapValues { (_, games) ->
                var maxStreak = 0
                var currentStreak = 0

                games.forEach { gameTeam ->
                    if (gameTeam.result == com.nextup.core.domain.game.GameResult.WIN) {
                        currentStreak++
                        maxStreak = maxOf(maxStreak, currentStreak)
                    } else {
                        currentStreak = 0
                    }
                }

                maxStreak
            }

        // 최장 연승 팀 찾기
        val longestStreak = teamStreaks.maxByOrNull { it.value } ?: return null
        if (longestStreak.value == 0) return null

        val teamName =
            decidedGameTeams
                .find { it.team.id == longestStreak.key }
                ?.team?.name ?: "Unknown"

        return WinStreakDto(
            teamName = teamName,
            streakLength = longestStreak.value,
        )
    }
}
