package com.nextup.infrastructure.service.stats

import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.core.domain.stats.SeasonAward
import com.nextup.core.domain.stats.SeasonAwardTitle
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.SeasonAwardRepositoryPort
import com.nextup.core.port.repository.SeasonBattingStatsRepositoryPort
import com.nextup.core.port.repository.SeasonPitchingStatsRepositoryPort
import com.nextup.core.service.stats.SeasonAwardService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import kotlin.math.roundToInt

/**
 * 시즌 타이틀(개인상) 서비스 구현
 *
 * 시즌 종료 시 각 부문별 타이틀을 자동으로 계산하고 부여합니다.
 * 규정타석, 규정이닝 등의 기준을 적용하여 자격을 검증합니다.
 */
@Service
@Transactional(readOnly = true)
class SeasonAwardServiceImpl(
    private val seasonAwardRepository: SeasonAwardRepositoryPort,
    private val seasonBattingStatsRepository: SeasonBattingStatsRepositoryPort,
    private val seasonPitchingStatsRepository: SeasonPitchingStatsRepositoryPort,
    private val competitionRepository: CompetitionRepositoryPort,
    private val gameRepository: GameRepositoryPort,
) : SeasonAwardService {
    private val logger = LoggerFactory.getLogger(SeasonAwardServiceImpl::class.java)

    @Transactional
    override fun calculateAndAwardTitles(
        year: Int,
        minPlateAppearances: Int,
        minInningsPitchedOuts: Int,
    ): List<SeasonAward> {
        require(year > 0) { "연도는 양수여야 합니다." }
        require(minPlateAppearances >= 0) { "규정타석은 0 이상이어야 합니다." }
        require(minInningsPitchedOuts >= 0) { "규정이닝은 0 이상이어야 합니다." }

        logger.info(
            "시즌 타이틀 계산 시작 (year={}, 규정타석={}, 규정이닝아웃={})",
            year,
            minPlateAppearances,
            minInningsPitchedOuts,
        )

        // 기존 타이틀 삭제 (재계산)
        seasonAwardRepository.deleteAllByYear(year)

        val awards = mutableListOf<SeasonAward>()

        // 타격 부문 타이틀
        awards.addAll(calculateBattingAwards(year, minPlateAppearances))

        // 투수 부문 타이틀
        awards.addAll(calculatePitchingAwards(year, minInningsPitchedOuts))

        val savedAwards = seasonAwardRepository.saveAll(awards)

        logger.info(
            "시즌 타이틀 계산 완료 (year={}, 부여된 타이틀 수={})",
            year,
            savedAwards.size,
        )

        return savedAwards
    }

    override fun getAwardsByYear(year: Int): List<SeasonAward> = seasonAwardRepository.findAllByYear(year)

    override fun getAwardsByPlayerId(playerId: Long): List<SeasonAward> =
        seasonAwardRepository.findAllByPlayerId(playerId)

    override fun getAwardsByCompetitionId(competitionId: Long): List<SeasonAward> =
        seasonAwardRepository.findAllByCompetitionId(competitionId)

    @Transactional
    override fun calculateAndAwardTitlesByCompetition(competitionId: Long): List<SeasonAward> {
        val competition =
            competitionRepository.findByIdOrNull(competitionId)
                ?: throw CompetitionNotFoundException(competitionId)

        val year = competition.year
        val gameRules = competition.gameRules

        // 팀당 경기 수 계산: 대회 전체 경기 수에서 팀별 출전 횟수 평균
        val games = gameRepository.findByCompetitionId(competitionId)
        val teamGameCounts = mutableMapOf<Long, Int>()
        for (game in games) {
            game.gameTeams.forEach { gameTeam ->
                teamGameCounts.merge(gameTeam.team.id, 1) { a, b -> a + b }
            }
        }
        val gamesPerTeam =
            if (teamGameCounts.isEmpty()) 0 else teamGameCounts.values.average().roundToInt()

        // 규정타석 = 팀당경기수 * qualificationPAMultiplier
        val minPlateAppearances = (gamesPerTeam * gameRules.qualificationPAMultiplier).roundToInt()
        // 규정이닝 아웃 수 = 팀당경기수 * qualificationIPMultiplier * 3 (아웃 단위)
        val minInningsPitchedOuts = (gamesPerTeam * gameRules.qualificationIPMultiplier * 3).roundToInt()

        logger.info(
            "대회 타이틀 계산 시작 (competitionId={}, year={}, 팀당경기수={}, 규정타석={}, 규정이닝아웃={})",
            competitionId,
            year,
            gamesPerTeam,
            minPlateAppearances,
            minInningsPitchedOuts,
        )

        // 기존 대회 타이틀 삭제 (재계산)
        seasonAwardRepository.deleteAllByCompetitionId(competitionId)

        val awards = mutableListOf<SeasonAward>()

        // 타격 부문 타이틀
        awards.addAll(calculateBattingAwards(year, minPlateAppearances, competitionId))

        // 투수 부문 타이틀
        awards.addAll(calculatePitchingAwards(year, minInningsPitchedOuts, competitionId))

        val savedAwards = seasonAwardRepository.saveAll(awards)

        logger.info(
            "대회 타이틀 계산 완료 (competitionId={}, year={}, 부여된 타이틀 수={})",
            competitionId,
            year,
            savedAwards.size,
        )

        return savedAwards
    }

    /**
     * 타격 부문 타이틀을 계산합니다.
     *
     * - 타격왕: 규정타석 이상 최고 타율
     * - 홈런왕: 최다 홈런
     * - 타점왕: 최다 타점
     * - 도루왕: 최다 도루
     * - 최다안타: 최다 안타
     */
    private fun calculateBattingAwards(
        year: Int,
        minPlateAppearances: Int,
        competitionId: Long? = null,
    ): List<SeasonAward> {
        val awards = mutableListOf<SeasonAward>()

        // 타격왕 (규정타석 이상 최고 타율)
        val battingLeaders =
            seasonBattingStatsRepository.findTopByBattingAverage(year, minPlateAppearances, 1)
        battingLeaders.firstOrNull()?.let { leader ->
            awards.add(
                SeasonAward.create(
                    player = leader.player,
                    year = year,
                    title = SeasonAwardTitle.BATTING_CHAMPION,
                    statValue = leader.battingAverage,
                    competitionId = competitionId,
                ),
            )
            logger.debug(
                "타격왕: {} (타율={})",
                leader.player.name,
                leader.battingAverage,
            )
        }

        // 홈런왕 (최다 홈런)
        val homeRunLeaders =
            seasonBattingStatsRepository.findTopByHomeRuns(year, 1)
        homeRunLeaders.firstOrNull()?.let { leader ->
            if (leader.homeRuns > 0) {
                awards.add(
                    SeasonAward.create(
                        player = leader.player,
                        year = year,
                        title = SeasonAwardTitle.HOME_RUN_KING,
                        statValue = BigDecimal(leader.homeRuns),
                        competitionId = competitionId,
                    ),
                )
                logger.debug("홈런왕: {} (홈런={})", leader.player.name, leader.homeRuns)
            }
        }

        // 타점왕 (최다 타점)
        val rbiLeaders =
            seasonBattingStatsRepository.findTopByRunsBattedIn(year, 1)
        rbiLeaders.firstOrNull()?.let { leader ->
            if (leader.runsBattedIn > 0) {
                awards.add(
                    SeasonAward.create(
                        player = leader.player,
                        year = year,
                        title = SeasonAwardTitle.RBI_KING,
                        statValue = BigDecimal(leader.runsBattedIn),
                        competitionId = competitionId,
                    ),
                )
                logger.debug("타점왕: {} (타점={})", leader.player.name, leader.runsBattedIn)
            }
        }

        // 도루왕 (최다 도루)
        val stolenBaseLeaders =
            seasonBattingStatsRepository.findTopByStolenBases(year, 1)
        stolenBaseLeaders.firstOrNull()?.let { leader ->
            if (leader.stolenBases > 0) {
                awards.add(
                    SeasonAward.create(
                        player = leader.player,
                        year = year,
                        title = SeasonAwardTitle.STOLEN_BASE_KING,
                        statValue = BigDecimal(leader.stolenBases),
                        competitionId = competitionId,
                    ),
                )
                logger.debug("도루왕: {} (도루={})", leader.player.name, leader.stolenBases)
            }
        }

        // 최다안타
        val hitsLeaders =
            seasonBattingStatsRepository.findTopByHits(year, 1)
        hitsLeaders.firstOrNull()?.let { leader ->
            if (leader.hits > 0) {
                awards.add(
                    SeasonAward.create(
                        player = leader.player,
                        year = year,
                        title = SeasonAwardTitle.HITS_LEADER,
                        statValue = BigDecimal(leader.hits),
                        competitionId = competitionId,
                    ),
                )
                logger.debug("최다안타: {} (안타={})", leader.player.name, leader.hits)
            }
        }

        return awards
    }

    /**
     * 투수 부문 타이틀을 계산합니다.
     *
     * - 다승왕: 최다 승리
     * - 방어율 1위: 규정이닝 이상 최저 방어율
     * - 탈삼진왕: 최다 탈삼진
     * - 세이브왕: 최다 세이브
     */
    private fun calculatePitchingAwards(
        year: Int,
        minInningsPitchedOuts: Int,
        competitionId: Long? = null,
    ): List<SeasonAward> {
        val awards = mutableListOf<SeasonAward>()

        // 다승왕 (최다 승리)
        val winsLeaders =
            seasonPitchingStatsRepository.findTopByWins(year, 1)
        winsLeaders.firstOrNull()?.let { leader ->
            if (leader.wins > 0) {
                awards.add(
                    SeasonAward.create(
                        player = leader.player,
                        year = year,
                        title = SeasonAwardTitle.WINS_LEADER,
                        statValue = BigDecimal(leader.wins),
                        competitionId = competitionId,
                    ),
                )
                logger.debug("다승왕: {} (승={})", leader.player.name, leader.wins)
            }
        }

        // 방어율 1위 (규정이닝 이상 최저 ERA)
        val eraLeaders =
            seasonPitchingStatsRepository.findTopByEra(year, minInningsPitchedOuts, 1)
        eraLeaders.firstOrNull()?.let { leader ->
            leader.earnedRunAverage?.let { era ->
                awards.add(
                    SeasonAward.create(
                        player = leader.player,
                        year = year,
                        title = SeasonAwardTitle.ERA_TITLE,
                        statValue = era,
                        competitionId = competitionId,
                    ),
                )
                logger.debug("방어율 1위: {} (ERA={})", leader.player.name, era)
            }
        }

        // 탈삼진왕 (최다 탈삼진)
        val strikeoutLeaders =
            seasonPitchingStatsRepository.findTopByStrikeouts(year, 1)
        strikeoutLeaders.firstOrNull()?.let { leader ->
            if (leader.strikeouts > 0) {
                awards.add(
                    SeasonAward.create(
                        player = leader.player,
                        year = year,
                        title = SeasonAwardTitle.STRIKEOUT_KING,
                        statValue = BigDecimal(leader.strikeouts),
                        competitionId = competitionId,
                    ),
                )
                logger.debug("탈삼진왕: {} (삼진={})", leader.player.name, leader.strikeouts)
            }
        }

        // 세이브왕 (최다 세이브)
        val savesLeaders =
            seasonPitchingStatsRepository.findTopBySaves(year, 1)
        savesLeaders.firstOrNull()?.let { leader ->
            if (leader.saves > 0) {
                awards.add(
                    SeasonAward.create(
                        player = leader.player,
                        year = year,
                        title = SeasonAwardTitle.SAVES_LEADER,
                        statValue = BigDecimal(leader.saves),
                        competitionId = competitionId,
                    ),
                )
                logger.debug("세이브왕: {} (세이브={})", leader.player.name, leader.saves)
            }
        }

        return awards
    }
}
