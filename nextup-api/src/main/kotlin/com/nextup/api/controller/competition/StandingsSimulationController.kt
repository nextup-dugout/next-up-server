package com.nextup.api.controller.competition

import com.nextup.api.dto.standings.MagicNumberResponse
import com.nextup.api.dto.standings.PlayoffScenarioResponse
import com.nextup.api.dto.standings.SimulationApiRequest
import com.nextup.api.dto.standings.SimulationResultResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.standings.StandingsSimulationService
import com.nextup.core.service.standings.dto.SimulatedGameResult
import com.nextup.core.service.standings.dto.SimulationRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 순위 시뮬레이션 Controller (일반 사용자 - 조회 전용)
 */
@Validated
@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/api/v1/competitions/{competitionId}/standings")
class StandingsSimulationController(
    private val standingsSimulationService: StandingsSimulationService,
) {
    /**
     * 대회 각 팀의 매직넘버를 조회합니다.
     */
    @GetMapping("/magic-numbers")
    fun getMagicNumbers(
        @PathVariable competitionId: Long,
    ): ApiResponse<List<MagicNumberResponse>> {
        val magicNumbers = standingsSimulationService.calculateMagicNumbers(competitionId)
        return ApiResponse.success(magicNumbers.map { MagicNumberResponse.from(it) })
    }

    /**
     * 가상의 경기 결과를 적용한 예상 순위표를 반환합니다.
     */
    @PostMapping("/simulation")
    fun simulateStandings(
        @PathVariable competitionId: Long,
        @RequestBody @Valid request: SimulationApiRequest,
    ): ApiResponse<SimulationResultResponse> {
        val coreRequest =
            SimulationRequest(
                gameResults =
                    request.gameResults.map { result ->
                        SimulatedGameResult(
                            gameId = result.gameId,
                            homeScore = result.homeScore,
                            awayScore = result.awayScore,
                        )
                    },
            )
        val result = standingsSimulationService.simulateStandings(competitionId, coreRequest)
        return ApiResponse.success(SimulationResultResponse.from(result))
    }

    /**
     * 특정 팀의 플레이오프 진출 시나리오를 조회합니다.
     */
    @GetMapping("/playoff-scenarios")
    fun getPlayoffScenarios(
        @PathVariable competitionId: Long,
        @RequestParam teamId: Long,
        @RequestParam @Min(1) playoffTeams: Int,
    ): ApiResponse<PlayoffScenarioResponse> {
        val result =
            standingsSimulationService.calculatePlayoffScenarios(
                competitionId = competitionId,
                teamId = teamId,
                playoffTeams = playoffTeams,
            )
        return ApiResponse.success(PlayoffScenarioResponse.from(result))
    }
}
