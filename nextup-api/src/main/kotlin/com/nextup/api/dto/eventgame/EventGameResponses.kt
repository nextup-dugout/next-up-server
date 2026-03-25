package com.nextup.api.dto.eventgame

import com.nextup.core.domain.eventgame.EventGame
import com.nextup.core.domain.eventgame.EventGameParticipant
import java.time.LocalDateTime

/**
 * 이벤트 게임 응답
 */
data class EventGameResponse(
    val id: Long,
    val organizerId: Long,
    val title: String,
    val description: String?,
    val scheduledAt: LocalDateTime,
    val location: String?,
    val fieldName: String?,
    val maxParticipants: Int,
    val activeParticipantCount: Int,
    val innings: Int,
    val status: String,
    val teamAName: String,
    val teamBName: String,
    val teamAScore: Int?,
    val teamBScore: Int?,
    val startedAt: LocalDateTime?,
    val endedAt: LocalDateTime?,
    val cancelReason: String?,
) {
    companion object {
        fun from(eventGame: EventGame): EventGameResponse =
            EventGameResponse(
                id = eventGame.id,
                organizerId = eventGame.organizerId,
                title = eventGame.title,
                description = eventGame.description,
                scheduledAt = eventGame.scheduledAt,
                location = eventGame.location,
                fieldName = eventGame.fieldName,
                maxParticipants = eventGame.maxParticipants,
                activeParticipantCount = eventGame.activeParticipantCount,
                innings = eventGame.innings,
                status = eventGame.status.name,
                teamAName = eventGame.teamAName,
                teamBName = eventGame.teamBName,
                teamAScore = eventGame.teamAScore,
                teamBScore = eventGame.teamBScore,
                startedAt = eventGame.startedAt,
                endedAt = eventGame.endedAt,
                cancelReason = eventGame.cancelReason,
            )
    }
}

/**
 * 이벤트 게임 참가자 응답
 */
data class EventGameParticipantResponse(
    val id: Long,
    val eventGameId: Long,
    val playerId: Long,
    val status: String,
    val teamAssignment: String?,
    val message: String?,
) {
    companion object {
        fun from(participant: EventGameParticipant): EventGameParticipantResponse =
            EventGameParticipantResponse(
                id = participant.id,
                eventGameId = participant.eventGame.id,
                playerId = participant.playerId,
                status = participant.status.name,
                teamAssignment = participant.teamAssignment?.name,
                message = participant.message,
            )
    }
}
