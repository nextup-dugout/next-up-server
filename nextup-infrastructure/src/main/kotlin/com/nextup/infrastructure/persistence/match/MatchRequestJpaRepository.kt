package com.nextup.infrastructure.persistence.match

import com.nextup.core.domain.match.MatchRequest
import com.nextup.core.domain.match.MatchRequestStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface MatchRequestJpaRepository : JpaRepository<MatchRequest, Long> {
    @Query("SELECT mr FROM MatchRequest mr WHERE mr.team.id = :teamId")
    fun findByTeamId(teamId: Long): List<MatchRequest>

    @Query("SELECT mr FROM MatchRequest mr WHERE mr.status = 'OPEN'")
    fun findAllOpen(): List<MatchRequest>

    fun findByStatus(status: MatchRequestStatus): List<MatchRequest>
}
