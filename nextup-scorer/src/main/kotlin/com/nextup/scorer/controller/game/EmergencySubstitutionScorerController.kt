package com.nextup.scorer.controller.game

import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.game.EmergencySubstitutionService
import com.nextup.scorer.dto.game.EjectAndSubstituteRequestDto
import com.nextup.scorer.dto.game.EjectPlayerRequestDto
import com.nextup.scorer.dto.game.EjectionResponse
import com.nextup.scorer.dto.game.EmergencySubstitutionResponse
import com.nextup.scorer.dto.game.toDomain
import com.nextup.scorer.dto.game.toEjectionResponse
import com.nextup.scorer.dto.game.toEmergencySubstitutionResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 기록원 전용 부상 퇴장 및 긴급 교체 컨트롤러
 *
 * 부상, 심판 퇴장 등의 사유로 선수를 퇴장시키고,
 * 필요 시 교체 선수를 긴급 투입하는 API를 제공합니다.
 * 주자 상태인 선수 퇴장 시 교체 선수가 해당 베이스를 계승합니다.
 */
@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/api/v1/scorer/games")
class EmergencySubstitutionScorerController(
    private val emergencySubstitutionService: EmergencySubstitutionService,
) {
    /**
     * 선수를 퇴장 처리합니다 (교체 없음).
     *
     * 교체 선수가 없는 경우 (벤치 소진 등) 퇴장만 처리합니다.
     * 주자 상태인 선수 퇴장 시 해당 베이스를 비웁니다.
     * 투수 퇴장 시 currentPitcherId를 null로 설정합니다.
     */
    @PostMapping("/{gameId}/players/{playerId}/eject")
    @ResponseStatus(HttpStatus.OK)
    fun ejectPlayer(
        @PathVariable gameId: Long,
        @PathVariable playerId: Long,
        @AuthenticationPrincipal scorerId: Long,
        @RequestBody @Valid request: EjectPlayerRequestDto,
    ): ApiResponse<EjectionResponse> {
        val event =
            emergencySubstitutionService.ejectPlayer(
                gameId = gameId,
                request = request.toDomain(playerId),
                scorerId = scorerId,
            )
        return ApiResponse.success(event.toEjectionResponse())
    }

    /**
     * 선수를 퇴장시키고 교체 선수를 긴급 투입합니다.
     *
     * 퇴장과 교체를 원자적으로 처리합니다.
     * 주자 상태인 선수 퇴장 시 교체 선수가 해당 베이스를 계승합니다.
     * 투수 부상 퇴장 시 교체 투수로 currentPitcherId를 갱신합니다.
     */
    @PostMapping("/{gameId}/players/{playerId}/eject-and-substitute")
    @ResponseStatus(HttpStatus.OK)
    fun ejectAndSubstitute(
        @PathVariable gameId: Long,
        @PathVariable playerId: Long,
        @AuthenticationPrincipal scorerId: Long,
        @RequestBody @Valid request: EjectAndSubstituteRequestDto,
    ): ApiResponse<EmergencySubstitutionResponse> {
        val event =
            emergencySubstitutionService.ejectAndSubstitute(
                gameId = gameId,
                request = request.toDomain(playerId),
                scorerId = scorerId,
            )
        return ApiResponse.success(event.toEmergencySubstitutionResponse())
    }
}
