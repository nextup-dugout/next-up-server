package com.nextup.core.port.repository

import com.nextup.core.domain.game.Game

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
}
