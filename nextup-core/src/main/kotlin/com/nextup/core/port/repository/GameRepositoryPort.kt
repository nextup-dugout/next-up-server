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
}
