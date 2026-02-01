package com.nextup.api.controller.game

import com.nextup.api.dto.common.ApiResponse
import com.nextup.api.dto.game.CreatePitchingRecordRequest
import com.nextup.api.dto.game.PitchingRecordResponse
import com.nextup.api.mapper.game.toResponse
import com.nextup.common.exception.GamePlayerNotFoundByGameAndPlayerException
import com.nextup.infrastructure.repository.game.GamePlayerRepository
import com.nextup.infrastructure.service.game.PitchingRecordService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/games/{gameId}/pitching-records")
class PitchingRecordController(
    private val pitchingRecordService: PitchingRecordService,
    private val gamePlayerRepository: GamePlayerRepository
) {

    @GetMapping
    fun getPitchingRecordsByGame(
        @PathVariable gameId: Long
    ): ApiResponse<List<PitchingRecordResponse>> {
        val records = pitchingRecordService.getAllByGameId(gameId)
        return ApiResponse.success(records.toResponse())
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createPitchingRecord(
        @PathVariable gameId: Long,
        @Valid @RequestBody request: CreatePitchingRecordRequest
    ): ApiResponse<PitchingRecordResponse> {
        val gamePlayer = gamePlayerRepository.findByGameIdAndPlayerId(gameId, request.playerId)
            ?: throw GamePlayerNotFoundByGameAndPlayerException(gameId, request.playerId)

        val record = pitchingRecordService.createRecord(gamePlayer.id, request.isStartingPitcher)
        return ApiResponse.success(record.toResponse())
    }
}
