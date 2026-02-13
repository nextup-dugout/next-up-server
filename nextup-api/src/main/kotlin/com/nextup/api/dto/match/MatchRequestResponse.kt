package com.nextup.api.dto.match

import com.nextup.core.domain.match.MatchRequest
import com.nextup.core.domain.match.MatchRequestStatus
import com.nextup.core.domain.match.SkillLevel
import java.time.Instant
import java.time.LocalDate

data class MatchRequestResponse(
    val id: Long,
    val teamId: Long,
    val teamName: String,
    val preferredDate: LocalDate,
    val preferredTime: String?,
    val preferredLocation: String?,
    val message: String?,
    val skillLevel: SkillLevel,
    val status: MatchRequestStatus,
    val createdAt: Instant,
) {
    companion object {
        fun from(matchRequest: MatchRequest): MatchRequestResponse =
            MatchRequestResponse(
                id = matchRequest.id,
                teamId = matchRequest.team.id,
                teamName = matchRequest.team.name,
                preferredDate = matchRequest.preferredDate,
                preferredTime = matchRequest.preferredTime,
                preferredLocation = matchRequest.preferredLocation,
                message = matchRequest.message,
                skillLevel = matchRequest.skillLevel,
                status = matchRequest.status,
                createdAt = matchRequest.createdAt,
            )
    }
}
