package com.nextup.infrastructure.service.eventgame

import com.nextup.common.exception.EventGameNotFoundException
import com.nextup.common.exception.EventGameParticipantNotFoundException
import com.nextup.core.domain.eventgame.EventGame
import com.nextup.core.domain.eventgame.EventGameParticipant
import com.nextup.core.domain.eventgame.EventGameParticipantStatus
import com.nextup.core.domain.eventgame.EventGameStatus
import com.nextup.core.domain.eventgame.TeamAssignment
import com.nextup.core.port.repository.EventGameParticipantRepositoryPort
import com.nextup.core.port.repository.EventGameRepositoryPort
import com.nextup.core.service.eventgame.CreateEventGameCommand
import com.nextup.core.service.eventgame.EventGameService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 이벤트 게임 서비스 구현
 */
@Service
@Transactional(readOnly = true)
class EventGameServiceImpl(
    private val eventGameRepository: EventGameRepositoryPort,
    private val participantRepository: EventGameParticipantRepositoryPort,
) : EventGameService {
    @Transactional
    override fun createEventGame(command: CreateEventGameCommand): EventGame {
        val eventGame =
            EventGame.create(
                organizerId = command.organizerId,
                title = command.title,
                description = command.description,
                scheduledAt = command.scheduledAt,
                location = command.location,
                fieldName = command.fieldName,
                maxParticipants = command.maxParticipants,
                innings = command.innings,
                teamAName = command.teamAName,
                teamBName = command.teamBName,
            )
        return eventGameRepository.save(eventGame)
    }

    override fun getEventGame(id: Long): EventGame =
        eventGameRepository.findByIdOrNull(id)
            ?: throw EventGameNotFoundException(id)

    override fun getRecruitingEventGames(): List<EventGame> =
        eventGameRepository.findByStatus(EventGameStatus.RECRUITING)

    override fun getMyEventGames(organizerId: Long): List<EventGame> =
        eventGameRepository.findByOrganizerId(organizerId)

    @Transactional
    override fun joinEventGame(
        eventGameId: Long,
        playerId: Long,
        message: String?,
    ): EventGameParticipant {
        val eventGame = getEventGame(eventGameId)
        val participant =
            EventGameParticipant.create(
                eventGame = eventGame,
                playerId = playerId,
                message = message,
            )
        eventGame.addParticipant(participant)
        eventGameRepository.save(eventGame)
        return participant
    }

    @Transactional
    override fun confirmParticipant(
        eventGameId: Long,
        participantId: Long,
    ): EventGameParticipant {
        getEventGame(eventGameId)
        val participant = findParticipant(participantId)
        participant.confirm()
        return participantRepository.save(participant)
    }

    @Transactional
    override fun cancelParticipation(
        eventGameId: Long,
        participantId: Long,
    ): EventGameParticipant {
        getEventGame(eventGameId)
        val participant = findParticipant(participantId)
        participant.cancel()
        return participantRepository.save(participant)
    }

    @Transactional
    override fun closeRecruitment(eventGameId: Long): EventGame {
        val eventGame = getEventGame(eventGameId)
        eventGame.closeRecruitment()
        return eventGameRepository.save(eventGame)
    }

    @Transactional
    override fun assignTeam(
        eventGameId: Long,
        participantId: Long,
        team: TeamAssignment,
    ): EventGameParticipant {
        getEventGame(eventGameId)
        val participant = findParticipant(participantId)
        participant.assignTeam(team)
        return participantRepository.save(participant)
    }

    @Transactional
    override fun autoAssignTeams(eventGameId: Long): EventGame {
        val eventGame = getEventGame(eventGameId)
        val confirmedParticipants =
            eventGame.participants
                .filter { it.status == EventGameParticipantStatus.CONFIRMED }
                .shuffled()

        confirmedParticipants.forEachIndexed { index, participant ->
            val team = if (index % 2 == 0) TeamAssignment.TEAM_A else TeamAssignment.TEAM_B
            participant.assignTeam(team)
            participantRepository.save(participant)
        }

        return eventGameRepository.save(eventGame)
    }

    @Transactional
    override fun completeTeamAssignment(eventGameId: Long): EventGame {
        val eventGame = getEventGame(eventGameId)
        eventGame.completeTeamAssignment()
        return eventGameRepository.save(eventGame)
    }

    @Transactional
    override fun startGame(eventGameId: Long): EventGame {
        val eventGame = getEventGame(eventGameId)
        eventGame.start()
        return eventGameRepository.save(eventGame)
    }

    @Transactional
    override fun finishGame(
        eventGameId: Long,
        teamAScore: Int,
        teamBScore: Int,
    ): EventGame {
        val eventGame = getEventGame(eventGameId)
        eventGame.finish(teamAScore, teamBScore)
        return eventGameRepository.save(eventGame)
    }

    @Transactional
    override fun cancelGame(
        eventGameId: Long,
        reason: String,
    ): EventGame {
        val eventGame = getEventGame(eventGameId)
        eventGame.cancel(reason)
        return eventGameRepository.save(eventGame)
    }

    override fun getParticipants(eventGameId: Long): List<EventGameParticipant> {
        getEventGame(eventGameId)
        return participantRepository.findByEventGameId(eventGameId)
    }

    override fun getPlayerHistory(playerId: Long): List<EventGameParticipant> =
        participantRepository.findByPlayerId(playerId)

    private fun findParticipant(participantId: Long): EventGameParticipant =
        participantRepository.findByIdOrNull(participantId)
            ?: throw EventGameParticipantNotFoundException(participantId)
}
