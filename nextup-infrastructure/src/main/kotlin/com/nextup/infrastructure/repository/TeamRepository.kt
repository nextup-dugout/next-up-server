package com.nextup.infrastructure.repository

import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.TeamRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TeamRepository :
    JpaRepository<Team, Long>,
    TeamRepositoryPort {
    override fun findByIdOrNull(id: Long): Team? = findById(id).orElse(null)

    override fun findByName(name: String): Team?

    override fun findByLeagueId(leagueId: Long): List<Team>

    @Query("SELECT t FROM Team t WHERE t.isActive = true")
    override fun findActiveTeams(): List<Team>

    @Query(
        """
        SELECT t FROM Team t
        WHERE t.isActive = true
        AND (:name IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:city IS NULL OR LOWER(t.city) LIKE LOWER(CONCAT('%', :city, '%')))
        ORDER BY t.name ASC
        """,
    )
    override fun findActiveTeamsByFilter(
        @Param("name") name: String?,
        @Param("city") city: String?,
    ): List<Team>

    @Query("SELECT t FROM Team t JOIN FETCH t.league WHERE t.id = :id")
    override fun findByIdWithLeague(id: Long): Team?
}
