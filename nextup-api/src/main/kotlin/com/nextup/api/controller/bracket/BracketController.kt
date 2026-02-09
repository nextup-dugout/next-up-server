package com.nextup.api.controller.bracket

import com.nextup.api.dto.bracket.BracketResponse
import com.nextup.api.dto.bracket.toBracketResponse
import com.nextup.api.dto.common.ApiResponse
import com.nextup.core.service.bracket.BracketGeneratorService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/competitions/{competitionId}/bracket")
class BracketController(
    private val bracketGeneratorService: BracketGeneratorService,
) {
    @GetMapping
    fun getBracket(
        @PathVariable competitionId: Long,
    ): ApiResponse<BracketResponse> {
        val entries = bracketGeneratorService.getBracket(competitionId)
        return ApiResponse.success(entries.toBracketResponse(competitionId))
    }
}
