package com.nextup.api.controller.eventgame

import com.nextup.api.dto.eventgame.AssignTeamApiRequest
import com.nextup.api.dto.eventgame.CancelEventGameApiRequest
import com.nextup.api.dto.eventgame.CreateEventGameApiRequest
import com.nextup.api.dto.eventgame.EventGameParticipantResponse
import com.nextup.api.dto.eventgame.EventGameResponse
import com.nextup.api.dto.eventgame.FinishEventGameApiRequest
import com.nextup.api.dto.eventgame.JoinEventGameApiRequest
import com.nextup.common.dto.ApiResponse
import com.nextup.core.domain.eventgame.TeamAssignment
import com.nextup.core.service.eventgame.CreateEventGameCommand
import com.nextup.core.service.eventgame.EventGameService
import com.nextup.core.service.player.PlayerService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 이벤트 게임 API Controller
 *
 * 팀 상관 없이 개인이 참가하는 ad-hoc 픽업 게임 관리
 */
@RestController
@RequestMapping("/api/v1/event-games")
class EventGameController(
    private val eventGameService: EventGameService,
    private val playerService: PlayerService,
) {
    /**
     * 이벤트 게임을 생성합니다.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    fun createEventGame(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: CreateEventGameApiRequest,
    ): ApiResponse<EventGameResponse> {
        val eventGame =
            eventGameService.createEventGame(
                CreateEventGameCommand(
                    organizerId = userId,
                    title = request.title,
                    description = request.description,
                    scheduledAt = request.scheduledAt,
                    location = request.location,
                    fieldName = request.fieldName,
                    maxParticipants = request.maxParticipants,
                    innings = request.innings,
                    teamAName = request.teamAName,
                    teamBName = request.teamBName,
                ),
            )
        return ApiResponse.success(EventGameResponse.from(eventGame))
    }

    /**
     * 모집 중인 이벤트 게임 목록을 조회합니다.
     */
    @GetMapping
    fun getRecruitingEventGames(): ApiResponse<List<EventGameResponse>> {
        val eventGames = eventGameService.getRecruitingEventGames()
        return ApiResponse.success(eventGames.map { EventGameResponse.from(it) })
    }

    /**
     * 이벤트 게임 상세를 조회합니다.
     */
    @GetMapping("/{id}")
    fun getEventGame(
        @PathVariable id: Long,
    ): ApiResponse<EventGameResponse> {
        val eventGame = eventGameService.getEventGame(id)
        return ApiResponse.success(EventGameResponse.from(eventGame))
    }

    /**
     * 내가 주최한 이벤트 게임 목록을 조회합니다.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    fun getMyEventGames(
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<List<EventGameResponse>> {
        val eventGames = eventGameService.getMyEventGames(userId)
        return ApiResponse.success(eventGames.map { EventGameResponse.from(it) })
    }

    /**
     * 이벤트 게임에 참가 신청합니다.
     */
    @PostMapping("/{id}/join")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    fun joinEventGame(
        @PathVariable id: Long,
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: JoinEventGameApiRequest,
    ): ApiResponse<EventGameParticipantResponse> {
        val participant =
            eventGameService.joinEventGame(
                eventGameId = id,
                userId = userId,
                message = request.message,
            )
        return ApiResponse.success(EventGameParticipantResponse.from(participant))
    }

    /**
     * 참가자를 확정합니다.
     */
    @PatchMapping("/{id}/participants/{participantId}/confirm")
    @PreAuthorize("isAuthenticated()")
    fun confirmParticipant(
        @PathVariable id: Long,
        @PathVariable participantId: Long,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<EventGameParticipantResponse> {
        val participant = eventGameService.confirmParticipant(id, participantId)
        return ApiResponse.success(EventGameParticipantResponse.from(participant))
    }

    /**
     * 참가를 취소합니다.
     */
    @DeleteMapping("/{id}/participants/{participantId}")
    @PreAuthorize("isAuthenticated()")
    fun cancelParticipation(
        @PathVariable id: Long,
        @PathVariable participantId: Long,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<EventGameParticipantResponse> {
        val participant = eventGameService.cancelParticipation(id, participantId)
        return ApiResponse.success(EventGameParticipantResponse.from(participant))
    }

    /**
     * 참가자 목록을 조회합니다.
     */
    @GetMapping("/{id}/participants")
    fun getParticipants(
        @PathVariable id: Long,
    ): ApiResponse<List<EventGameParticipantResponse>> {
        val participants = eventGameService.getParticipants(id)
        return ApiResponse.success(participants.map { EventGameParticipantResponse.from(it) })
    }

    /**
     * 모집을 마감합니다.
     */
    @PatchMapping("/{id}/close")
    @PreAuthorize("isAuthenticated()")
    fun closeRecruitment(
        @PathVariable id: Long,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<EventGameResponse> {
        val eventGame = eventGameService.closeRecruitment(id)
        return ApiResponse.success(EventGameResponse.from(eventGame))
    }

    /**
     * 참가자에게 팀을 수동 배정합니다.
     */
    @PatchMapping("/{id}/participants/{participantId}/assign-team")
    @PreAuthorize("isAuthenticated()")
    fun assignTeam(
        @PathVariable id: Long,
        @PathVariable participantId: Long,
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: AssignTeamApiRequest,
    ): ApiResponse<EventGameParticipantResponse> {
        val team = TeamAssignment.valueOf(request.team)
        val participant = eventGameService.assignTeam(id, participantId, team)
        return ApiResponse.success(EventGameParticipantResponse.from(participant))
    }

    /**
     * 참가자를 자동으로 팀에 배정합니다.
     */
    @PostMapping("/{id}/auto-assign")
    @PreAuthorize("isAuthenticated()")
    fun autoAssignTeams(
        @PathVariable id: Long,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<EventGameResponse> {
        val eventGame = eventGameService.autoAssignTeams(id)
        return ApiResponse.success(EventGameResponse.from(eventGame))
    }

    /**
     * 팀 배정을 완료합니다.
     */
    @PatchMapping("/{id}/complete-assignment")
    @PreAuthorize("isAuthenticated()")
    fun completeTeamAssignment(
        @PathVariable id: Long,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<EventGameResponse> {
        val eventGame = eventGameService.completeTeamAssignment(id)
        return ApiResponse.success(EventGameResponse.from(eventGame))
    }

    /**
     * 경기를 시작합니다.
     */
    @PatchMapping("/{id}/start")
    @PreAuthorize("isAuthenticated()")
    fun startGame(
        @PathVariable id: Long,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<EventGameResponse> {
        val eventGame = eventGameService.startGame(id)
        return ApiResponse.success(EventGameResponse.from(eventGame))
    }

    /**
     * 경기를 종료합니다.
     */
    @PatchMapping("/{id}/finish")
    @PreAuthorize("isAuthenticated()")
    fun finishGame(
        @PathVariable id: Long,
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: FinishEventGameApiRequest,
    ): ApiResponse<EventGameResponse> {
        val eventGame =
            eventGameService.finishGame(id, request.teamAScore, request.teamBScore)
        return ApiResponse.success(EventGameResponse.from(eventGame))
    }

    /**
     * 경기를 취소합니다.
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    fun cancelGame(
        @PathVariable id: Long,
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: CancelEventGameApiRequest,
    ): ApiResponse<EventGameResponse> {
        val eventGame = eventGameService.cancelGame(id, request.reason)
        return ApiResponse.success(EventGameResponse.from(eventGame))
    }

    /**
     * 내 참가 이력을 조회합니다.
     */
    @GetMapping("/me/history")
    @PreAuthorize("isAuthenticated()")
    fun getMyHistory(
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<List<EventGameParticipantResponse>> {
        val player = playerService.getLinkedPlayer(userId)
        val participations = eventGameService.getPlayerHistory(player.id)
        return ApiResponse.success(participations.map { EventGameParticipantResponse.from(it) })
    }
}
