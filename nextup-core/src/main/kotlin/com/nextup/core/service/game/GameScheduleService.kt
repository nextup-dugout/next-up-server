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
}
