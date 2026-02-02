package com.nextup.core.port.repository

import com.nextup.core.domain.player.Player
import java.time.LocalDate
import java.util.Optional

/**
 * Player Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface PlayerRepositoryPort {

    fun save(player: Player): Player

    fun findAll(): List<Player>

    fun findByIdOrNull(id: Long): Player?

    fun delete(player: Player)

    fun deleteById(id: Long)

    fun existsById(id: Long): Boolean

    fun findByName(name: String): List<Player>

    fun findByNameContaining(name: String): List<Player>

    fun findActivePlayers(): List<Player>

    fun findCurrentPlayersByTeamId(teamId: Long): List<Player>

    fun findPlayersByTeamIdAtDate(teamId: Long, date: LocalDate): List<Player>
}
