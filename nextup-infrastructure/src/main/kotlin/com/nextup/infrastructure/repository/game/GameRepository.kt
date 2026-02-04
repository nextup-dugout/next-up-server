package com.nextup.infrastructure.repository.game

import com.nextup.core.domain.game.Game
import com.nextup.core.port.repository.GameRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository

interface GameRepository :
    JpaRepository<Game, Long>,
    GameRepositoryPort {
    override fun findByIdOrNull(id: Long): Game? = findById(id).orElse(null)

    override fun findAllByIds(ids: List<Long>): List<Game> = findAllById(ids)
}
