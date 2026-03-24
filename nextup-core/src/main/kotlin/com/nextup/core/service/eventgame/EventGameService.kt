package com.nextup.core.service.eventgame

import com.nextup.core.domain.eventgame.EventGame
import com.nextup.core.domain.eventgame.EventGameParticipant
import com.nextup.core.domain.eventgame.TeamAssignment

/**
 * 이벤트 게임 서비스
 *
 * 이벤트 게임의 생성, 참가, 팀 배정, 진행을 관리합니다.
 */
interface EventGameService {
    fun createEventGame(command: CreateEventGameCommand): EventGame

    fun getEventGame(id: Long): EventGame

    fun getRecruitingEventGames(): List<EventGame>

    fun getMyEventGames(organizerId: Long): List<EventGame>

    /**
     * 이벤트 게임에 참가 신청합니다.
     *
     * userId를 받아 연결된 Player를 조회하여 참가합니다.
     * 무소속 선수도 Player 프로필이 있으면 참가 가능합니다.
     *
     * @param eventGameId 이벤트 게임 ID
     * @param userId 참가 신청하는 사용자 ID
     * @param message 참가 메시지 (선택)
     * @return 생성된 참가자
     */
    fun joinEventGame(
        eventGameId: Long,
        userId: Long,
        message: String? = null,
    ): EventGameParticipant

    fun confirmParticipant(
        eventGameId: Long,
        participantId: Long,
    ): EventGameParticipant

    fun cancelParticipation(
        eventGameId: Long,
        participantId: Long,
    ): EventGameParticipant

    fun closeRecruitment(eventGameId: Long): EventGame

    fun assignTeam(
        eventGameId: Long,
        participantId: Long,
        team: TeamAssignment,
    ): EventGameParticipant

    fun autoAssignTeams(eventGameId: Long): EventGame

    fun completeTeamAssignment(eventGameId: Long): EventGame

    fun startGame(eventGameId: Long): EventGame

    fun finishGame(
        eventGameId: Long,
        teamAScore: Int,
        teamBScore: Int,
    ): EventGame

    fun cancelGame(
        eventGameId: Long,
        reason: String,
    ): EventGame

    fun getParticipants(eventGameId: Long): List<EventGameParticipant>

    fun getPlayerHistory(playerId: Long): List<EventGameParticipant>
}

data class CreateEventGameCommand(
    val organizerId: Long,
    val title: String,
    val description: String? = null,
    val scheduledAt: java.time.LocalDateTime,
    val location: String? = null,
    val fieldName: String? = null,
    val maxParticipants: Int,
    val innings: Int = 7,
    val teamAName: String = "Team A",
    val teamBName: String = "Team B",
)
