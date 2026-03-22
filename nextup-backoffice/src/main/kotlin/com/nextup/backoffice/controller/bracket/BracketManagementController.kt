package com.nextup.backoffice.controller.bracket

import com.nextup.backoffice.dto.bracket.AdvanceWinnerRequest
import com.nextup.backoffice.dto.bracket.BracketEntryResponse
import com.nextup.backoffice.dto.bracket.CreateGameFromBracketRequest
import com.nextup.backoffice.dto.bracket.GenerateBracketRequest
import com.nextup.backoffice.dto.bracket.toResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.domain.competition.TournamentType
import com.nextup.core.service.bracket.BracketGeneratorService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/backoffice/competitions/{competitionId}/bracket")
class BracketManagementController(
    private val bracketGeneratorService: BracketGeneratorService,
) {
    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    fun generateBracket(
        @PathVariable competitionId: Long,
        @RequestBody @Valid request: GenerateBracketRequest,
    ): ApiResponse<List<BracketEntryResponse>> {
        val entries =
            when (request.tournamentType) {
                TournamentType.SINGLE_ELIMINATION ->
                    bracketGeneratorService.generateSingleElimination(competitionId, request.seededTeamIds)

                TournamentType.DOUBLE_ELIMINATION ->
                    bracketGeneratorService.generateDoubleElimination(competitionId, request.seededTeamIds)
            }

        return ApiResponse.success(entries.toResponse())
    }

    @PutMapping("/{entryId}/advance")
    fun advanceWinner(
        @PathVariable competitionId: Long,
        @PathVariable entryId: Long,
        @RequestBody @Valid request: AdvanceWinnerRequest,
    ): ApiResponse<BracketEntryResponse> {
        val entry = bracketGeneratorService.advanceWinner(entryId, request.winnerTeamId)
        return ApiResponse.success(entry.toResponse())
    }

    @PostMapping("/{entryId}/create-game")
    @ResponseStatus(HttpStatus.CREATED)
    fun createGameForBracketEntry(
        @PathVariable competitionId: Long,
        @PathVariable entryId: Long,
        @RequestBody @Valid request: CreateGameFromBracketRequest,
    ): ApiResponse<BracketEntryResponse> {
        val entry =
            bracketGeneratorService.createGameForBracketEntry(
                bracketEntryId = entryId,
                scheduledAt = request.scheduledAt,
                location = request.location,
                fieldName = request.fieldName,
            )
        return ApiResponse.success(entry.toResponse())
    }
}
