package com.nextup.infrastructure.service.stats

import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.core.domain.stats.SeasonBattingStats
import com.nextup.core.domain.stats.SeasonPitchingStats
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.SeasonBattingStatsRepositoryPort
import com.nextup.core.port.repository.SeasonPitchingStatsRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import com.nextup.core.service.stats.IndividualRankingService
import com.nextup.core.service.stats.dto.BattingCategory
import com.nextup.core.service.stats.dto.BattingLeaderDto
import com.nextup.core.service.stats.dto.PitchingCategory
import com.nextup.core.service.stats.dto.PitchingLeaderDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class IndividualRankingServiceImpl(
    private val competitionRepository: CompetitionRepositoryPort,
    private val seasonBattingStatsRepository: SeasonBattingStatsRepositoryPort,
    private val seasonPitchingStatsRepository: SeasonPitchingStatsRepositoryPort,
    private val teamMemberRepository: TeamMemberRepositoryPort,
) : IndividualRankingService {
    companion object {
        // 사회인 야구 규정 타석 계수 (팀 경기 수 * 3.1)
        const val QUALIFYING_PA_FACTOR = 3.1

        // 사회인 야구 규정 이닝 계수 (팀 경기 수 * 1.0)
        const val QUALIFYING_IP_FACTOR = 1.0

        // 기본 팀 경기 수 (규정 타석/이닝 산출 기본값)
        const val DEFAULT_TEAM_GAMES = 10
    }

    override fun getBattingLeaders(
        competitionId: Long,
        category: BattingCategory,
        limit: Int,
    ): List<BattingLeaderDto> {
        val competition =
            competitionRepository.findByIdOrNull(competitionId)
                ?: throw CompetitionNotFoundException(competitionId)
        val year = competition.year

        val qualifyingPA = (DEFAULT_TEAM_GAMES * QUALIFYING_PA_FACTOR).toInt()

        val stats =
            when (category) {
                BattingCategory.BATTING_AVG ->
                    seasonBattingStatsRepository.findTopByBattingAverage(year, qualifyingPA, limit)
                BattingCategory.OBP ->
                    seasonBattingStatsRepository.findTopByOnBasePercentage(year, qualifyingPA, limit)
                BattingCategory.SLG ->
                    seasonBattingStatsRepository.findTopBySlugging(year, qualifyingPA, limit)
                BattingCategory.OPS ->
                    seasonBattingStatsRepository.findTopByOps(year, qualifyingPA, limit)
                BattingCategory.HOME_RUNS ->
                    seasonBattingStatsRepository.findTopByHomeRuns(year, limit)
                BattingCategory.RBI ->
                    seasonBattingStatsRepository.findTopByRunsBattedIn(year, limit)
                BattingCategory.HITS ->
                    seasonBattingStatsRepository.findTopByHits(year, limit)
                BattingCategory.STOLEN_BASES ->
                    seasonBattingStatsRepository.findTopByStolenBases(year, limit)
            }

        return stats.mapIndexed { index, stat ->
            stat.toBattingLeaderDto(index + 1, category)
        }
    }

    override fun getPitchingLeaders(
        competitionId: Long,
        category: PitchingCategory,
        limit: Int,
    ): List<PitchingLeaderDto> {
        val competition =
            competitionRepository.findByIdOrNull(competitionId)
                ?: throw CompetitionNotFoundException(competitionId)
        val year = competition.year

        val qualifyingIPOuts = (DEFAULT_TEAM_GAMES * QUALIFYING_IP_FACTOR * 3).toInt()

        val stats =
            when (category) {
                PitchingCategory.ERA ->
                    seasonPitchingStatsRepository.findTopByEra(year, qualifyingIPOuts, limit)
                PitchingCategory.WHIP ->
                    seasonPitchingStatsRepository.findTopByWhip(year, qualifyingIPOuts, limit)
                PitchingCategory.WINS ->
                    seasonPitchingStatsRepository.findTopByWins(year, limit)
                PitchingCategory.SAVES ->
                    seasonPitchingStatsRepository.findTopBySaves(year, limit)
                PitchingCategory.STRIKEOUTS ->
                    seasonPitchingStatsRepository.findTopByStrikeouts(year, limit)
            }

        return stats.mapIndexed { index, stat ->
            stat.toPitchingLeaderDto(index + 1, category)
        }
    }

    private fun SeasonBattingStats.toBattingLeaderDto(
        rank: Int,
        category: BattingCategory,
    ): BattingLeaderDto {
        val teamName = resolveTeamName(this.player.id)
        val value =
            when (category) {
                BattingCategory.BATTING_AVG -> this.battingAverage.toDouble()
                BattingCategory.OBP -> this.onBasePercentage.toDouble()
                BattingCategory.SLG -> this.sluggingPercentage.toDouble()
                BattingCategory.OPS -> this.ops.toDouble()
                BattingCategory.HOME_RUNS -> this.homeRuns.toDouble()
                BattingCategory.RBI -> this.runsBattedIn.toDouble()
                BattingCategory.HITS -> this.hits.toDouble()
                BattingCategory.STOLEN_BASES -> this.stolenBases.toDouble()
            }
        return BattingLeaderDto(
            rank = rank,
            playerId = this.player.id,
            playerName = this.player.name,
            teamName = teamName,
            value = value,
            games = this.gamesPlayed,
            plateAppearances = this.plateAppearances,
        )
    }

    private fun SeasonPitchingStats.toPitchingLeaderDto(
        rank: Int,
        category: PitchingCategory,
    ): PitchingLeaderDto {
        val teamName = resolveTeamName(this.player.id)
        val value =
            when (category) {
                PitchingCategory.ERA -> this.earnedRunAverage.toDouble()
                PitchingCategory.WHIP -> this.whip.toDouble()
                PitchingCategory.WINS -> this.wins.toDouble()
                PitchingCategory.SAVES -> this.saves.toDouble()
                PitchingCategory.STRIKEOUTS -> this.strikeouts.toDouble()
            }
        return PitchingLeaderDto(
            rank = rank,
            playerId = this.player.id,
            playerName = this.player.name,
            teamName = teamName,
            value = value,
            games = this.gamesPlayed,
            inningsPitched = this.inningsPitched.toDouble(),
        )
    }

    private fun resolveTeamName(playerId: Long): String {
        val members = teamMemberRepository.findByPlayerIdActive(playerId)
        return members.firstOrNull()?.team?.name ?: "-"
    }
}
