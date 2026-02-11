package com.nextup.infrastructure.service.stats

import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.game.GameResult
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.service.stats.TeamStatsService
import com.nextup.core.service.stats.dto.TeamBattingStatsDto
import com.nextup.core.service.stats.dto.TeamPitchingStatsDto
import com.nextup.core.service.stats.dto.TeamRecordDto
import com.nextup.core.service.stats.dto.TeamStatsDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

@Service
@Transactional(readOnly = true)
class TeamStatsServiceImpl(
    private val teamRepositoryPort: TeamRepositoryPort,
    private val gameTeamRepositoryPort: GameTeamRepositoryPort,
    private val battingRecordRepositoryPort: BattingRecordRepositoryPort,
    private val pitchingRecordRepositoryPort: PitchingRecordRepositoryPort,
) : TeamStatsService {
    override fun getTeamStats(
        teamId: Long,
        year: Int?,
        competitionId: Long?,
    ): TeamStatsDto {
        // 팀 존재 여부 확인
        val team =
            teamRepositoryPort.findByIdWithLeague(teamId)
                ?: throw TeamNotFoundException(teamId)

        // GameTeam 조회 (year 필터링)
        val gameTeams =
            if (year != null) {
                gameTeamRepositoryPort.findAllByTeamIdAndYear(teamId, year)
            } else {
                gameTeamRepositoryPort.findAllByTeamId(teamId)
            }

        // 경기 결과 집계
        val record = calculateTeamRecord(gameTeams)

        // 타격 통계 계산
        val battingStats = calculateBattingStats(gameTeams, year)

        // 투수 통계 계산
        val pitchingStats = calculatePitchingStats(gameTeams, year)

        return TeamStatsDto(
            teamId = team.id,
            teamName = team.name,
            year = year,
            competitionId = competitionId,
            competitionName = null, // TODO: Competition 연결 시 구현
            record = record,
            batting = battingStats,
            pitching = pitchingStats,
        )
    }

    private fun calculateTeamRecord(gameTeams: List<com.nextup.core.domain.game.GameTeam>): TeamRecordDto {
        val wins = gameTeams.count { it.result == GameResult.WIN }
        val losses = gameTeams.count { it.result == GameResult.LOSS }
        val draws = gameTeams.count { it.result == GameResult.DRAW }
        val gamesPlayed = wins + losses + draws

        val winningPercentage =
            if (wins + losses == 0) {
                BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP)
            } else {
                BigDecimal(wins)
                    .divide(BigDecimal(wins + losses), 3, RoundingMode.HALF_UP)
            }

        return TeamRecordDto(
            gamesPlayed = gamesPlayed,
            wins = wins,
            losses = losses,
            draws = draws,
            winningPercentage = winningPercentage,
        )
    }

    private fun calculateBattingStats(
        gameTeams: List<com.nextup.core.domain.game.GameTeam>,
        year: Int?,
    ): TeamBattingStatsDto {
        // GameTeam에서 Game을 거쳐 BattingRecord 조회
        val teamId = gameTeams.firstOrNull()?.team?.id ?: return emptyBattingStats()
        val gameIds = gameTeams.map { it.game.id }.distinct()

        // 배치 쿼리로 모든 경기의 타격 기록을 한 번에 조회 (N+1 방지)
        val battingRecords = battingRecordRepositoryPort.findAllByTeamIdAndGameIds(teamId, gameIds)

        val totalAtBats = battingRecords.sumOf { it.atBats }
        val totalHits = battingRecords.sumOf { it.hits }
        val totalHomeRuns = battingRecords.sumOf { it.homeRuns }
        val totalRunsBattedIn = battingRecords.sumOf { it.runsBattedIn }
        val totalRuns = battingRecords.sumOf { it.runs }
        val totalWalks = battingRecords.sumOf { it.totalWalks }
        val totalHitByPitch = battingRecords.sumOf { it.hitByPitch }
        val totalSacrificeFlies = battingRecords.sumOf { it.sacrificeFlies }
        val totalBases = battingRecords.sumOf { it.totalBases }

        // 팀 타율 계산
        val teamBattingAverage =
            if (totalAtBats == 0) {
                BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP)
            } else {
                BigDecimal(totalHits)
                    .divide(BigDecimal(totalAtBats), 3, RoundingMode.HALF_UP)
            }

        // 팀 출루율 계산
        val obpDenominator = totalAtBats + totalWalks + totalHitByPitch + totalSacrificeFlies
        val teamOnBasePercentage =
            if (obpDenominator == 0) {
                BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP)
            } else {
                BigDecimal(totalHits + totalWalks + totalHitByPitch)
                    .divide(BigDecimal(obpDenominator), 3, RoundingMode.HALF_UP)
            }

        // 팀 장타율 계산
        val teamSluggingPercentage =
            if (totalAtBats == 0) {
                BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP)
            } else {
                BigDecimal(totalBases)
                    .divide(BigDecimal(totalAtBats), 3, RoundingMode.HALF_UP)
            }

        return TeamBattingStatsDto(
            totalAtBats = totalAtBats,
            totalHits = totalHits,
            totalHomeRuns = totalHomeRuns,
            totalRunsBattedIn = totalRunsBattedIn,
            totalRuns = totalRuns,
            teamBattingAverage = teamBattingAverage,
            teamOnBasePercentage = teamOnBasePercentage,
            teamSluggingPercentage = teamSluggingPercentage,
        )
    }

    private fun calculatePitchingStats(
        gameTeams: List<com.nextup.core.domain.game.GameTeam>,
        year: Int?,
    ): TeamPitchingStatsDto {
        // GameTeam에서 Game을 거쳐 PitchingRecord 조회
        val teamId = gameTeams.firstOrNull()?.team?.id ?: return emptyPitchingStats()
        val gameIds = gameTeams.map { it.game.id }.distinct()

        // 배치 쿼리로 모든 경기의 투수 기록을 한 번에 조회 (N+1 방지)
        val pitchingRecords = pitchingRecordRepositoryPort.findAllByTeamIdAndGameIds(teamId, gameIds)

        val totalInningsPitchedOuts = pitchingRecords.sumOf { it.inningsPitchedOuts }
        val totalEarnedRuns = pitchingRecords.sumOf { it.earnedRuns }
        val totalStrikeouts = pitchingRecords.sumOf { it.strikeouts }
        val totalWalksAllowed = pitchingRecords.sumOf { it.walksAllowed }
        val totalHitsAllowed = pitchingRecords.sumOf { it.hitsAllowed }

        // 이닝 표시 문자열 생성
        val completeInnings = totalInningsPitchedOuts / 3
        val remainingOuts = totalInningsPitchedOuts % 3
        val inningsPitchedDisplay = "$completeInnings.$remainingOuts"

        // 팀 ERA 계산
        val teamEra =
            if (totalInningsPitchedOuts == 0) {
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            } else {
                val innings =
                    BigDecimal(totalInningsPitchedOuts)
                        .divide(BigDecimal(3), 10, RoundingMode.HALF_UP)
                BigDecimal(totalEarnedRuns)
                    .multiply(BigDecimal(9))
                    .divide(innings, 2, RoundingMode.HALF_UP)
            }

        // 팀 WHIP 계산
        val teamWhip =
            if (totalInningsPitchedOuts == 0) {
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            } else {
                val innings =
                    BigDecimal(totalInningsPitchedOuts)
                        .divide(BigDecimal(3), 10, RoundingMode.HALF_UP)
                BigDecimal(totalHitsAllowed + totalWalksAllowed)
                    .divide(innings, 2, RoundingMode.HALF_UP)
            }

        return TeamPitchingStatsDto(
            totalInningsPitchedOuts = totalInningsPitchedOuts,
            inningsPitchedDisplay = inningsPitchedDisplay,
            totalEarnedRuns = totalEarnedRuns,
            totalStrikeouts = totalStrikeouts,
            totalWalksAllowed = totalWalksAllowed,
            teamEra = teamEra,
            teamWhip = teamWhip,
        )
    }

    private fun emptyBattingStats(): TeamBattingStatsDto =
        TeamBattingStatsDto(
            totalAtBats = 0,
            totalHits = 0,
            totalHomeRuns = 0,
            totalRunsBattedIn = 0,
            totalRuns = 0,
            teamBattingAverage = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP),
            teamOnBasePercentage = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP),
            teamSluggingPercentage = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP),
        )

    private fun emptyPitchingStats(): TeamPitchingStatsDto =
        TeamPitchingStatsDto(
            totalInningsPitchedOuts = 0,
            inningsPitchedDisplay = "0.0",
            totalEarnedRuns = 0,
            totalStrikeouts = 0,
            totalWalksAllowed = 0,
            teamEra = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
            teamWhip = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
        )
}
