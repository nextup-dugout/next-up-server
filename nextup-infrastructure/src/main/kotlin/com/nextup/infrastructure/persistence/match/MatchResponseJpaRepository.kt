package com.nextup.infrastructure.persistence.match

import com.nextup.core.domain.match.MatchResponse
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface MatchResponseJpaRepository : JpaRepository<MatchResponse, Long> {
    @Query("SELECT mr FROM MatchResponse mr WHERE mr.matchRequest.id = :matchRequestId")
    fun findByMatchRequestId(matchRequestId: Long): List<MatchResponse>

    @Query("SELECT mr FROM MatchResponse mr WHERE mr.respondTeam.id = :respondTeamId")
    fun findByRespondTeamId(respondTeamId: Long): List<MatchResponse>
}
