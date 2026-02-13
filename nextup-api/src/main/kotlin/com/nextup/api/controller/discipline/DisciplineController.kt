package com.nextup.api.controller.discipline

import com.nextup.api.dto.common.ApiResponse
import com.nextup.api.dto.discipline.DisciplineResponse
import com.nextup.api.dto.discipline.PlayerDisciplineHistoryResponse
import com.nextup.core.service.discipline.DisciplineService
import com.nextup.core.service.player.PlayerService
import org.springframework.web.bind.annotation.*

/**
 * 징계 조회 API Controller (공개 API)
 *
 * 조회 전용
 */
@RestController
@RequestMapping("/api/v1/disciplines")
class DisciplineController(
    private val disciplineService: DisciplineService,
    private val playerService: PlayerService,
) {
    /**
     * 선수의 징계 이력을 조회합니다.
     */
    @GetMapping("/players/{playerId}")
    fun getPlayerDisciplines(
        @PathVariable playerId: Long,
        @RequestParam(required = false) competitionId: Long?,
    ): ApiResponse<PlayerDisciplineHistoryResponse> {
        val player = playerService.getById(playerId)

        val disciplines =
            if (competitionId != null) {
                disciplineService.getDisciplinesByPlayerAndCompetition(playerId, competitionId)
            } else {
                disciplineService.getDisciplinesByPlayer(playerId)
            }

        val activeDisciplines = disciplines.filter { it.isEffective() }

        val response =
            PlayerDisciplineHistoryResponse(
                playerId = player.id,
                playerName = player.name,
                totalDisciplines = disciplines.size,
                activeDisciplines = activeDisciplines.size,
                disciplines = disciplines.map { DisciplineResponse.from(it) },
            )

        return ApiResponse.success(response)
    }

    /**
     * 선수가 출장 가능한지 확인합니다.
     */
    @GetMapping("/players/{playerId}/eligibility")
    fun checkPlayerEligibility(
        @PathVariable playerId: Long,
        @RequestParam competitionId: Long,
    ): ApiResponse<Map<String, Any>> {
        val canPlay = disciplineService.canPlayerPlay(playerId, competitionId)
        val activeDisciplines =
            disciplineService.getActiveDisciplines(playerId, competitionId)

        return ApiResponse.success(
            mapOf(
                "canPlay" to canPlay,
                "activeDisciplinesCount" to activeDisciplines.size,
                "disciplines" to activeDisciplines.map { DisciplineResponse.from(it) },
            )
        )
    }
}
