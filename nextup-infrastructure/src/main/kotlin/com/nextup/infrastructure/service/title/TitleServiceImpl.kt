package com.nextup.infrastructure.service.title

import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.core.domain.stats.SeasonBattingStats
import com.nextup.core.domain.stats.SeasonPitchingStats
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.SeasonBattingStatsRepositoryPort
import com.nextup.core.port.repository.SeasonPitchingStatsRepositoryPort
import com.nextup.core.service.title.TitleCategory
import com.nextup.core.service.title.TitleService
import com.nextup.core.service.title.dto.TitleCandidateDto
import com.nextup.core.service.title.dto.TitleDto
import com.nextup.core.service.title.dto.TitleWinnerDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 타이틀 서비스 구현
 */
@Service
@Transactional(readOnly = true)
class TitleServiceImpl(
    private val competitionRepository: CompetitionRepositoryPort,
    private val gameTeamRepository: GameTeamRepositoryPort,
    private val seasonBattingStatsRepository: SeasonBattingStatsRepositoryPort,
    private val seasonPitchingStatsRepository: SeasonPitchingStatsRepositoryPort,
) : TitleService {
    companion object {
        private const val TOP_CANDIDATES_LIMIT = 10
        private const val QUALIFICATION_PLATE_APPEARANCES_MULTIPLIER = 3.1
        private const val QUALIFICATION_INNINGS_MULTIPLIER = 1.0
    }

    override fun getTitles(competitionId: Long): List<TitleDto> {
        val competition =
            competitionRepository.findByIdOrNull(competitionId)
                ?: throw CompetitionNotFoundException(competitionId)

        val year = competition.year
        val teamGamesCount = calculateTeamGamesCount(competitionId)

        return TitleCategory.entries.map { category ->
            calculateTitle(category, year, teamGamesCount)
        }
    }

    override fun getTitleByCategory(
        competitionId: Long,
        category: TitleCategory,
    ): TitleDto {
        val competition =
            competitionRepository.findByIdOrNull(competitionId)
                ?: throw CompetitionNotFoundException(competitionId)

        val year = competition.year
        val teamGamesCount = calculateTeamGamesCount(competitionId)

        return calculateTitle(category, year, teamGamesCount)
    }

    /**
     * 팀당 경기 수를 계산합니다 (규정 타석/이닝 계산용).
     */
    private fun calculateTeamGamesCount(competitionId: Long): Int {
        val allGameTeams = gameTeamRepository.findAllByCompetitionId(competitionId)
        if (allGameTeams.isEmpty()) {
            return 0
        }

        // 팀별 예정된 총 경기 수의 최댓값 반환
        return allGameTeams.groupBy { it.team.id }
            .mapValues { (_, gameTeams) -> gameTeams.size }
            .values
            .maxOrNull() ?: 0
    }

    /**
     * 특정 카테고리의 타이틀을 계산합니다.
     */
    private fun calculateTitle(
        category: TitleCategory,
        year: Int,
        teamGamesCount: Int,
    ): TitleDto {
        val candidates =
            when (category) {
                TitleCategory.BATTING_AVG -> calculateBattingAvgTitle(year, teamGamesCount)
                TitleCategory.HOME_RUNS -> calculateHomeRunsTitle(year)
                TitleCategory.RBI -> calculateRbiTitle(year)
                TitleCategory.STOLEN_BASES -> calculateStolenBasesTitle(year)
                TitleCategory.HITS -> calculateHitsTitle(year)
                TitleCategory.WINS -> calculateWinsTitle(year)
                TitleCategory.ERA -> calculateEraTitle(year, teamGamesCount)
                TitleCategory.SAVES -> calculateSavesTitle(year)
                TitleCategory.STRIKEOUTS -> calculateStrikeoutsTitle(year)
            }

        // 후보자 중 규정 충족자 중에서 1위를 우승자로 선정
        val winner =
            candidates
                .firstOrNull { it.isQualified }
                ?.let {
                    TitleWinnerDto(
                        playerId = it.playerId,
                        playerName = it.playerName,
                        teamName = it.teamName,
                        statValue = it.statValue,
                    )
                }

        return TitleDto(
            category = category,
            displayName = category.displayName,
            winner = winner,
            topCandidates = candidates,
        )
    }

    // ========== 타격 타이틀 ==========

    private fun calculateBattingAvgTitle(
        year: Int,
        teamGamesCount: Int,
    ): List<TitleCandidateDto> {
        val minPlateAppearances = (teamGamesCount * QUALIFICATION_PLATE_APPEARANCES_MULTIPLIER).toInt()
        val allStats = seasonBattingStatsRepository.findAllByYear(year)

        return allStats
            .sortedByDescending { it.battingAverage }
            .take(TOP_CANDIDATES_LIMIT)
            .mapIndexed { index, stats ->
                TitleCandidateDto(
                    rank = index + 1,
                    playerId = stats.player.id,
                    playerName = stats.player.name,
                    teamName = getPlayerTeamName(stats),
                    statValue = stats.battingAverage.toDouble(),
                    isQualified = stats.plateAppearances >= minPlateAppearances,
                )
            }
    }

    private fun calculateHomeRunsTitle(year: Int): List<TitleCandidateDto> {
        val stats = seasonBattingStatsRepository.findTopByHomeRuns(year, TOP_CANDIDATES_LIMIT)

        return stats.mapIndexed { index, stat ->
            TitleCandidateDto(
                rank = index + 1,
                playerId = stat.player.id,
                playerName = stat.player.name,
                teamName = getPlayerTeamName(stat),
                statValue = stat.homeRuns.toDouble(),
                isQualified = true, // 홈런왕은 규정 타석 불필요
            )
        }
    }

    private fun calculateRbiTitle(year: Int): List<TitleCandidateDto> {
        val stats = seasonBattingStatsRepository.findTopByRunsBattedIn(year, TOP_CANDIDATES_LIMIT)

        return stats.mapIndexed { index, stat ->
            TitleCandidateDto(
                rank = index + 1,
                playerId = stat.player.id,
                playerName = stat.player.name,
                teamName = getPlayerTeamName(stat),
                statValue = stat.runsBattedIn.toDouble(),
                isQualified = true, // 타점왕은 규정 타석 불필요
            )
        }
    }

    private fun calculateStolenBasesTitle(year: Int): List<TitleCandidateDto> {
        val stats = seasonBattingStatsRepository.findTopByStolenBases(year, TOP_CANDIDATES_LIMIT)

        return stats.mapIndexed { index, stat ->
            TitleCandidateDto(
                rank = index + 1,
                playerId = stat.player.id,
                playerName = stat.player.name,
                teamName = getPlayerTeamName(stat),
                statValue = stat.stolenBases.toDouble(),
                isQualified = true, // 도루왕은 규정 타석 불필요
            )
        }
    }

    private fun calculateHitsTitle(year: Int): List<TitleCandidateDto> {
        val stats = seasonBattingStatsRepository.findTopByHits(year, TOP_CANDIDATES_LIMIT)

        return stats.mapIndexed { index, stat ->
            TitleCandidateDto(
                rank = index + 1,
                playerId = stat.player.id,
                playerName = stat.player.name,
                teamName = getPlayerTeamName(stat),
                statValue = stat.hits.toDouble(),
                isQualified = true, // 최다안타는 규정 타석 불필요
            )
        }
    }

    // ========== 투수 타이틀 ==========

    private fun calculateWinsTitle(year: Int): List<TitleCandidateDto> {
        val stats = seasonPitchingStatsRepository.findTopByWins(year, TOP_CANDIDATES_LIMIT)

        return stats.mapIndexed { index, stat ->
            TitleCandidateDto(
                rank = index + 1,
                playerId = stat.player.id,
                playerName = stat.player.name,
                teamName = getPlayerTeamName(stat),
                statValue = stat.wins.toDouble(),
                isQualified = true, // 다승왕은 규정 이닝 불필요
            )
        }
    }

    private fun calculateEraTitle(
        year: Int,
        teamGamesCount: Int,
    ): List<TitleCandidateDto> {
        val minInningsPitchedOuts = (teamGamesCount * QUALIFICATION_INNINGS_MULTIPLIER * 3).toInt()
        val allStats = seasonPitchingStatsRepository.findAllByYear(year)

        return allStats
            .sortedBy { it.earnedRunAverage } // ERA는 낮을수록 좋음
            .take(TOP_CANDIDATES_LIMIT)
            .mapIndexed { index, stats ->
                TitleCandidateDto(
                    rank = index + 1,
                    playerId = stats.player.id,
                    playerName = stats.player.name,
                    teamName = getPlayerTeamName(stats),
                    statValue = stats.earnedRunAverage.toDouble(),
                    isQualified = stats.inningsPitchedOuts >= minInningsPitchedOuts,
                )
            }
    }

    private fun calculateSavesTitle(year: Int): List<TitleCandidateDto> {
        val stats = seasonPitchingStatsRepository.findTopBySaves(year, TOP_CANDIDATES_LIMIT)

        return stats.mapIndexed { index, stat ->
            TitleCandidateDto(
                rank = index + 1,
                playerId = stat.player.id,
                playerName = stat.player.name,
                teamName = getPlayerTeamName(stat),
                statValue = stat.saves.toDouble(),
                isQualified = true, // 세이브왕은 규정 이닝 불필요
            )
        }
    }

    private fun calculateStrikeoutsTitle(year: Int): List<TitleCandidateDto> {
        val stats = seasonPitchingStatsRepository.findTopByStrikeouts(year, TOP_CANDIDATES_LIMIT)

        return stats.mapIndexed { index, stat ->
            TitleCandidateDto(
                rank = index + 1,
                playerId = stat.player.id,
                playerName = stat.player.name,
                teamName = getPlayerTeamName(stat),
                statValue = stat.strikeouts.toDouble(),
                isQualified = true, // 탈삼진왕은 규정 이닝 불필요
            )
        }
    }

    // ========== 헬퍼 메서드 ==========

    private fun getPlayerTeamName(stats: SeasonBattingStats): String {
        // TODO: PlayerTeamHistory에서 해당 시즌의 팀 정보를 조회
        // 현재는 임시로 "알 수 없음" 반환
        return "알 수 없음"
    }

    private fun getPlayerTeamName(stats: SeasonPitchingStats): String {
        // TODO: PlayerTeamHistory에서 해당 시즌의 팀 정보를 조회
        // 현재는 임시로 "알 수 없음" 반환
        return "알 수 없음"
    }
}
