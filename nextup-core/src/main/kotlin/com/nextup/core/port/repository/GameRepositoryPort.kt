package com.nextup.core.port.repository

import com.nextup.core.domain.game.Game
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
}
