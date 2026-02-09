package com.nextup.core.port.repository

import com.nextup.core.domain.match.MatchResponse

interface MatchResponseRepositoryPort {
    fun save(matchResponse: MatchResponse): MatchResponse

    fun findByIdOrNull(id: Long): MatchResponse?

    fun findByMatchRequestId(matchRequestId: Long): List<MatchResponse>

    fun findByRespondTeamId(respondTeamId: Long): List<MatchResponse>
}
