package com.nextup.api.controller.stats

import com.nextup.api.dto.stats.CareerBattingStatsResponse
import com.nextup.api.dto.stats.CareerPitchingStatsResponse
import com.nextup.api.dto.stats.CompetitionBattingStatsResponse
import com.nextup.api.dto.stats.CompetitionPitchingStatsResponse
import com.nextup.api.dto.stats.SeasonBattingStatsResponse
import com.nextup.api.dto.stats.SeasonPitchingStatsResponse
import com.nextup.api.mapper.stats.toResponse
import com.nextup.api.mapper.stats.toSeasonBattingResponse
import com.nextup.api.mapper.stats.toSeasonPitchingResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.stats.PlayerStatsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 선수 통계 조회 API
 *
 * 선수별 시즌/통산 타격/투수 통계를 조회합니다.
 * 모든 응답은 ApiResponse로 래핑됩니다.
 */
@RestController
@RequestMapping("/api/v1/players/{playerId}/stats")
class PlayerStatsController(
    private val playerStatsService: PlayerStatsService,
) {
    /**
     * 시즌 타격 통계 조회
     *
     * GET /api/v1/players/{playerId}/stats/batting/season/{year} — 전체 합산 (첫 번째 팀 기록)
     * GET /api/v1/players/{playerId}/stats/batting/season/{year}?teamId={teamId} — 팀별 필터
     *
     * @param playerId 선수 ID
     * @param year 시즌 연도
     * @param teamId 팀 ID (선택, 미지정 시 첫 번째 팀 기록 반환)
     * @return 시즌 타격 통계 응답
     */
    @GetMapping("/batting/season/{year}")
    fun getSeasonBattingStats(
        @PathVariable playerId: Long,
        @PathVariable year: Int,
        @RequestParam(required = false) teamId: Long?,
    ): ApiResponse<SeasonBattingStatsResponse> {
        val stats = playerStatsService.getSeasonBattingStats(playerId, year, teamId)
        return ApiResponse.success(stats.toResponse())
    }

    /**
     * 시즌 타격 통계 팀별 목록 조회
     *
     * GET /api/v1/players/{playerId}/stats/batting/season/{year}/teams — 팀별 분리 기록 전체
     *
     * @param playerId 선수 ID
     * @param year 시즌 연도
     * @return 팀별 시즌 타격 통계 목록
     */
    @GetMapping("/batting/season/{year}/teams")
    fun getSeasonBattingStatsByTeam(
        @PathVariable playerId: Long,
        @PathVariable year: Int,
    ): ApiResponse<List<SeasonBattingStatsResponse>> {
        val statsList = playerStatsService.getSeasonBattingStatsByTeam(playerId, year)
        return ApiResponse.success(statsList.toSeasonBattingResponse())
    }

    /**
     * 시즌 투수 통계 조회
     *
     * GET /api/v1/players/{playerId}/stats/pitching/season/{year} — 전체 합산 (첫 번째 팀 기록)
     * GET /api/v1/players/{playerId}/stats/pitching/season/{year}?teamId={teamId} — 팀별 필터
     *
     * @param playerId 선수 ID
     * @param year 시즌 연도
     * @param teamId 팀 ID (선택, 미지정 시 첫 번째 팀 기록 반환)
     * @return 시즌 투수 통계 응답
     */
    @GetMapping("/pitching/season/{year}")
    fun getSeasonPitchingStats(
        @PathVariable playerId: Long,
        @PathVariable year: Int,
        @RequestParam(required = false) teamId: Long?,
    ): ApiResponse<SeasonPitchingStatsResponse> {
        val stats = playerStatsService.getSeasonPitchingStats(playerId, year, teamId)
        return ApiResponse.success(stats.toResponse())
    }

    /**
     * 시즌 투수 통계 팀별 목록 조회
     *
     * GET /api/v1/players/{playerId}/stats/pitching/season/{year}/teams — 팀별 분리 기록 전체
     *
     * @param playerId 선수 ID
     * @param year 시즌 연도
     * @return 팀별 시즌 투수 통계 목록
     */
    @GetMapping("/pitching/season/{year}/teams")
    fun getSeasonPitchingStatsByTeam(
        @PathVariable playerId: Long,
        @PathVariable year: Int,
    ): ApiResponse<List<SeasonPitchingStatsResponse>> {
        val statsList = playerStatsService.getSeasonPitchingStatsByTeam(playerId, year)
        return ApiResponse.success(statsList.toSeasonPitchingResponse())
    }

    /**
     * 통산 타격 통계 조회
     *
     * GET /api/v1/players/{playerId}/stats/batting/career
     *
     * @param playerId 선수 ID
     * @return 통산 타격 통계 응답
     */
    @GetMapping("/batting/career")
    fun getCareerBattingStats(
        @PathVariable playerId: Long,
    ): ApiResponse<CareerBattingStatsResponse> {
        val stats = playerStatsService.getCareerBattingStats(playerId)
        return ApiResponse.success(stats.toResponse())
    }

    /**
     * 통산 투수 통계 조회
     *
     * GET /api/v1/players/{playerId}/stats/pitching/career
     *
     * @param playerId 선수 ID
     * @return 통산 투수 통계 응답
     */
    @GetMapping("/pitching/career")
    fun getCareerPitchingStats(
        @PathVariable playerId: Long,
    ): ApiResponse<CareerPitchingStatsResponse> {
        val stats = playerStatsService.getCareerPitchingStats(playerId)
        return ApiResponse.success(stats.toResponse())
    }

    /**
     * 모든 시즌 타격 통계 조회
     *
     * GET /api/v1/players/{playerId}/stats/batting/seasons
     *
     * @param playerId 선수 ID
     * @return 모든 시즌 타격 통계 리스트
     */
    @GetMapping("/batting/seasons")
    fun getAllSeasonBattingStats(
        @PathVariable playerId: Long,
    ): ApiResponse<List<SeasonBattingStatsResponse>> {
        val statsList = playerStatsService.getAllSeasonBattingStats(playerId)
        return ApiResponse.success(statsList.toSeasonBattingResponse())
    }

    /**
     * 모든 시즌 투수 통계 조회
     *
     * GET /api/v1/players/{playerId}/stats/pitching/seasons
     *
     * @param playerId 선수 ID
     * @return 모든 시즌 투수 통계 리스트
     */
    @GetMapping("/pitching/seasons")
    fun getAllSeasonPitchingStats(
        @PathVariable playerId: Long,
    ): ApiResponse<List<SeasonPitchingStatsResponse>> {
        val statsList = playerStatsService.getAllSeasonPitchingStats(playerId)
        return ApiResponse.success(statsList.toSeasonPitchingResponse())
    }

    /**
     * 대회별 타격 통계 조회
     *
     * GET /api/v1/players/{playerId}/stats/batting/competition?competitionId=1
     *
     * @param playerId 선수 ID
     * @param competitionId 대회 ID
     * @return 대회별 타격 통계 응답
     */
    @GetMapping("/batting/competition")
    fun getBattingStatsByCompetition(
        @PathVariable playerId: Long,
        @RequestParam competitionId: Long,
    ): ApiResponse<CompetitionBattingStatsResponse> {
        val stats = playerStatsService.getBattingStatsByCompetition(playerId, competitionId)
        return ApiResponse.success(
            CompetitionBattingStatsResponse(
                playerId = stats.playerId,
                competitionId = stats.competitionId,
                gamesPlayed = stats.gamesPlayed,
                plateAppearances = stats.plateAppearances,
                atBats = stats.atBats,
                hits = stats.hits,
                doubles = stats.doubles,
                triples = stats.triples,
                homeRuns = stats.homeRuns,
                runs = stats.runs,
                runsBattedIn = stats.runsBattedIn,
                walks = stats.walks,
                strikeouts = stats.strikeouts,
                stolenBases = stats.stolenBases,
                battingAverage = stats.battingAverage,
                onBasePercentage = stats.onBasePercentage,
                sluggingPercentage = stats.sluggingPercentage,
                ops = stats.ops,
            ),
        )
    }

    /**
     * 대회별 투수 통계 조회
     *
     * GET /api/v1/players/{playerId}/stats/pitching/competition?competitionId=1
     *
     * @param playerId 선수 ID
     * @param competitionId 대회 ID
     * @return 대회별 투수 통계 응답
     */
    @GetMapping("/pitching/competition")
    fun getPitchingStatsByCompetition(
        @PathVariable playerId: Long,
        @RequestParam competitionId: Long,
    ): ApiResponse<CompetitionPitchingStatsResponse> {
        val stats = playerStatsService.getPitchingStatsByCompetition(playerId, competitionId)
        return ApiResponse.success(
            CompetitionPitchingStatsResponse(
                playerId = stats.playerId,
                competitionId = stats.competitionId,
                gamesPlayed = stats.gamesPlayed,
                gamesStarted = stats.gamesStarted,
                inningsPitchedDisplay = stats.inningsPitchedDisplay,
                wins = stats.wins,
                losses = stats.losses,
                saves = stats.saves,
                holds = stats.holds,
                earnedRuns = stats.earnedRuns,
                hitsAllowed = stats.hitsAllowed,
                walksAllowed = stats.walksAllowed,
                strikeouts = stats.strikeouts,
                homeRunsAllowed = stats.homeRunsAllowed,
                earnedRunAverage = stats.earnedRunAverage,
                whip = stats.whip,
            ),
        )
    }
}
