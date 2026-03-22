package com.nextup.api.controller.game

import com.nextup.api.dto.common.PagedResponse
import com.nextup.api.dto.game.AvailableRosterResponse
import com.nextup.api.dto.game.GameDetailResponse
import com.nextup.api.dto.game.GameSummaryResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.game.AvailableRosterService
import com.nextup.core.service.game.GameScheduleService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * 경기 일정 조회 API Controller (일반 사용자 - 조회 전용)
 */
@RestController
@RequestMapping("/api/v1")
class GameScheduleController(
    private val gameScheduleService: GameScheduleService,
    private val availableRosterService: AvailableRosterService,
) {
    /**
     * 경기 목록을 조회합니다. (날짜/팀/대회 필터, DB 페이징)
     */
    @GetMapping("/games")
    fun getGames(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?,
        @RequestParam(required = false) teamId: Long?,
        @RequestParam(required = false) competitionId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PagedResponse<GameSummaryResponse>> {
        val pageResult =
            gameScheduleService.getGames(
                date = date,
                teamId = teamId,
                competitionId = competitionId,
                page = page,
                size = size,
            )
        val mapped = pageResult.map { GameSummaryResponse.from(it) }
        return ApiResponse.success(PagedResponse.from(mapped))
    }

    /**
     * 경기 상세 정보를 조회합니다.
     */
    @GetMapping("/games/{gameId}")
    fun getGameDetail(
        @PathVariable gameId: Long,
    ): ApiResponse<GameDetailResponse> {
        val detail = gameScheduleService.getGameDetail(gameId)
        return ApiResponse.success(GameDetailResponse.from(detail))
    }

    /**
     * 팀별 경기 일정을 조회합니다.
     */
    @GetMapping("/teams/{teamId}/games")
    fun getGamesByTeam(
        @PathVariable teamId: Long,
    ): ApiResponse<List<GameSummaryResponse>> {
        val games = gameScheduleService.getGamesByTeam(teamId)
        return ApiResponse.success(games.map { GameSummaryResponse.from(it) })
    }

    /**
     * 팀의 다가오는 경기를 조회합니다.
     */
    @GetMapping("/teams/{teamId}/games/upcoming")
    fun getUpcomingGames(
        @PathVariable teamId: Long,
        @RequestParam(defaultValue = "5") limit: Int,
    ): ApiResponse<List<GameSummaryResponse>> {
        val games = gameScheduleService.getUpcomingGamesByTeam(teamId, limit)
        return ApiResponse.success(games.map { GameSummaryResponse.from(it) })
    }

    /**
     * 특정 연월의 경기 있는 날짜(일) 목록을 반환합니다. (A-05: 캘린더 뷰)
     *
     * GET /api/v1/games/calendar?year={year}&month={month}&teamId={teamId}
     */
    @GetMapping("/games/calendar")
    fun getGameCalendar(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam(required = false) teamId: Long?,
    ): ApiResponse<List<Int>> {
        val days = gameScheduleService.getGameDaysInMonth(year, month, teamId)
        return ApiResponse.success(days)
    }

    /**
     * 라인업 제출용 출전 가능 선수 목록을 조회합니다. (A-07: 로스터)
     *
     * GET /api/v1/games/{gameId}/available-roster?teamId={teamId}
     */
    @GetMapping("/games/{gameId}/available-roster")
    fun getAvailableRoster(
        @PathVariable gameId: Long,
        @RequestParam teamId: Long,
    ): ApiResponse<AvailableRosterResponse> {
        val roster = availableRosterService.getAvailableRoster(gameId, teamId)
        return ApiResponse.success(AvailableRosterResponse.from(roster))
    }
}
