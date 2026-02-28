package com.nextup.api.controller.game

import com.nextup.api.dto.game.BattingRecordResponse
import com.nextup.api.dto.game.CreateBattingRecordRequest
import com.nextup.api.mapper.game.toResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.common.exception.GamePlayerNotFoundByGameAndPlayerException
import com.nextup.core.service.game.BattingRecordService
import com.nextup.infrastructure.repository.game.GamePlayerRepository
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
@RequestMapping("/api/v1/games/{gameId}/batting-records")
class BattingRecordController(
    private val battingRecordService: BattingRecordService,
    private val gamePlayerRepository: GamePlayerRepository,
) {
    @GetMapping
    fun getBattingRecordsByGame(
        @PathVariable gameId: Long,
    ): ApiResponse<List<BattingRecordResponse>> {
        val records = battingRecordService.getAllByGameId(gameId)
        return ApiResponse.success(records.toResponse())
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createBattingRecord(
        @PathVariable gameId: Long,
        @Valid @RequestBody request: CreateBattingRecordRequest,
    ): ApiResponse<BattingRecordResponse> {
        val gamePlayer =
            gamePlayerRepository.findByGameIdAndPlayerId(gameId, request.playerId)
                ?: throw GamePlayerNotFoundByGameAndPlayerException(gameId, request.playerId)

        val record = battingRecordService.createRecord(gamePlayer.id)
        return ApiResponse.success(record.toResponse())
    }
}
