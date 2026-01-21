package com.nextup.infrastructure.repository

import com.nextup.core.domain.league.League
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface LeagueRepository : JpaRepository<League, Long> {

    fun findByName(name: String): League?

    fun findByAbbreviation(abbreviation: String): League?

    @Query("SELECT l FROM League l WHERE l.isActive = true")
    fun findActiveLeagues(): List<League>

    @Query("SELECT l FROM League l JOIN FETCH l._teams WHERE l.id = :id")
    fun findByIdWithTeams(id: Long): League?
}
