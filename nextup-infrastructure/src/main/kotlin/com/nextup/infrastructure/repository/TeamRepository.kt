package com.nextup.infrastructure.repository

import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.TeamRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TeamRepository :
    JpaRepository<Team, Long>,
    TeamRepositoryPort {
    override fun findByIdOrNull(id: Long): Team? = findById(id).orElse(null)

    override fun findByName(name: String): Team?

    override fun findByLeagueId(leagueId: Long): List<Team>

    @Query("SELECT t FROM Team t WHERE t.isActive = true")
    override fun findActiveTeams(): List<Team>

    @Query("SELECT t FROM Team t JOIN FETCH t.league WHERE t.id = :id")
    override fun findByIdWithLeague(id: Long): Team?
}
