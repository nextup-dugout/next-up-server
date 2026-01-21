package com.nextup.infrastructure.repository

import com.nextup.core.domain.team.Team
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TeamRepository : JpaRepository<Team, Long> {

    fun findByName(name: String): Team?

    fun findByLeagueId(leagueId: Long): List<Team>

    @Query("SELECT t FROM Team t WHERE t.isActive = true")
    fun findActiveTeams(): List<Team>

    @Query("SELECT t FROM Team t JOIN FETCH t.league WHERE t.id = :id")
    fun findByIdWithLeague(id: Long): Team?
}
