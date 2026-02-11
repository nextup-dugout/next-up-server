package com.nextup.api.dto.match

import com.nextup.core.domain.match.MatchResponse
import com.nextup.core.domain.match.MatchResponseStatus
import java.time.Instant

data class MatchResponseResponse(
    val id: Long,
    val matchRequestId: Long,
    val respondTeamId: Long,
    val respondTeamName: String,
    val message: String?,
    val status: MatchResponseStatus,
    val createdAt: Instant,
) {
    companion object {
        fun from(matchResponse: MatchResponse): MatchResponseResponse =
            MatchResponseResponse(
                id = matchResponse.id,
                matchRequestId = matchResponse.matchRequest.id,
                respondTeamId = matchResponse.respondTeam.id,
                respondTeamName = matchResponse.respondTeam.name,
                message = matchResponse.message,
                status = matchResponse.status,
                createdAt = matchResponse.createdAt,
            )
    }
}
