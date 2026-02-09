package com.nextup.core.port.repository

import com.nextup.core.domain.match.MatchRequest
import com.nextup.core.domain.match.MatchRequestStatus

interface MatchRequestRepositoryPort {
    fun save(matchRequest: MatchRequest): MatchRequest

    fun findByIdOrNull(id: Long): MatchRequest?

    fun findByTeamId(teamId: Long): List<MatchRequest>

    fun findAllOpen(): List<MatchRequest>

    fun findByStatus(status: MatchRequestStatus): List<MatchRequest>
}
