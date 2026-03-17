package com.nextup.core.service.game

import com.nextup.core.service.game.dto.GameDetailDto
import com.nextup.core.service.game.dto.GameSummaryDto
import java.time.LocalDate

/**
 * 경기 일정 조회 서비스 인터페이스
 */
interface GameScheduleService {
    /**
     * 경기 목록을 조회합니다. (날짜/팀 필터, 페이징)
     */
    fun getGames(
        date: LocalDate? = null,
        teamId: Long? = null,
        competitionId: Long? = null,
        page: Int = 0,
        size: Int = 20,
    ): List<GameSummaryDto>

    /**
     * 경기 상세 정보를 조회합니다.
     */
    fun getGameDetail(gameId: Long): GameDetailDto

    /**
     * 팀별 경기 일정을 조회합니다.
     */
    fun getGamesByTeam(teamId: Long): List<GameSummaryDto>

    /**
     * 팀의 다가오는 경기를 조회합니다.
     */
    fun getUpcomingGamesByTeam(
        teamId: Long,
        limit: Int = 5,
    ): List<GameSummaryDto>

    /**
     * 여러 팀의 다가오는 경기를 통합 조회합니다.
     */
    fun getUpcomingGamesByTeamIds(
        teamIds: List<Long>,
        limit: Int = 10,
    ): List<GameSummaryDto>

    /**
     * 특정 연월에 경기가 있는 날짜(일) 목록을 반환합니다. (캘린더 뷰용)
     */
    fun getGameDaysInMonth(
        year: Int,
        month: Int,
        teamId: Long?,
    ): List<Int>
}
