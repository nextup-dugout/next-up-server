package com.nextup.core.port.repository

import com.nextup.core.common.PageCommand
import com.nextup.core.common.PageResult
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import java.time.LocalDate

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

    fun findPlayersByTeamIdAtDate(
        teamId: Long,
        date: LocalDate,
    ): List<Player>

    /**
     * 이름(부분 일치), 팀 ID, 포지션 필터로 선수를 검색합니다.
     * null 파라미터는 해당 조건을 무시합니다.
     */
    fun search(
        name: String?,
        teamId: Long?,
        position: Position?,
        pageCommand: PageCommand,
    ): PageResult<Player>
}
