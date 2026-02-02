package com.nextup.core.port.repository

import com.nextup.core.domain.team.Team
import java.util.Optional

/**
 * Team Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface TeamRepositoryPort {

    fun save(team: Team): Team

    fun findAll(): List<Team>

    fun delete(team: Team)

    fun deleteById(id: Long)

    fun existsById(id: Long): Boolean

    fun findByName(name: String): Team?

    fun findByLeagueId(leagueId: Long): List<Team>

    fun findActiveTeams(): List<Team>

    fun findByIdWithLeague(id: Long): Team?
}
