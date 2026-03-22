package com.nextup.core.port.repository

import com.nextup.core.common.PageCommand
import com.nextup.core.common.PageResult
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameStatus
import java.time.LocalDateTime

/**
 * Game Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface GameRepositoryPort {
    fun save(game: Game): Game

    fun findAll(): List<Game>

    fun findByIdOrNull(id: Long): Game?

    fun delete(game: Game)

    fun deleteById(id: Long)

    /**
     * 여러 ID로 Game을 한 번에 조회합니다. (N+1 방지용 배치 쿼리)
     */
    fun findAllByIds(ids: List<Long>): List<Game>

    /**
     * 대회 ID로 경기 목록을 조회합니다.
     */
    fun findByCompetitionId(competitionId: Long): List<Game>

    /**
     * 날짜 범위로 경기를 조회합니다.
     */
    fun findByScheduledAtBetween(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<Game>

    /**
     * Game을 GameTeam과 함께 조회합니다. (N+1 방지)
     */
    fun findByIdWithTeams(id: Long): Game?

    /**
     * 대회의 전체 경기 수를 반환합니다.
     */
    fun countByCompetitionId(competitionId: Long): Long

    /**
     * 대회에서 완료된 경기 수를 반환합니다.
     * 완료 상태: FINISHED, CALLED, FORFEITED, CANCELLED
     */
    fun countCompletedOrCancelledByCompetitionId(competitionId: Long): Long

    /**
     * 특정 연월의 경기 날짜(일) 목록을 조회합니다. (캘린더 뷰용)
     * teamId가 제공되면 해당 팀의 경기만 조회합니다.
     */
    fun findGameDaysInMonth(
        year: Int,
        month: Int,
        teamId: Long?,
    ): List<Int>

    /**
     * 기록원이 잠금한 경기 중 lockedAt이 threshold 이전인 경기를 조회합니다.
     * (잠금 만료 자동 해제용)
     */
    fun findLockedGamesBefore(threshold: LocalDateTime): List<Game>

    /**
     * 경기 목록을 페이징하여 조회합니다. (날짜/팀/대회 필터 지원)
     * Core 모듈이 Spring에 의존하지 않도록 PageCommand/PageResult 커스텀 타입을 사용합니다.
     */
    fun findGames(
        date: java.time.LocalDate?,
        teamId: Long?,
        competitionId: Long?,
        status: GameStatus?,
        pageCommand: PageCommand,
    ): PageResult<Game>
}
