package com.nextup.scorer.dto.game

import com.nextup.core.domain.game.EjectionReason
import com.nextup.core.domain.player.Position
import com.nextup.core.service.game.dto.EjectAndSubstituteRequest
import com.nextup.core.service.game.dto.EjectionRequest
import jakarta.validation.constraints.NotNull

/**
 * 퇴장 처리 요청 DTO (교체 없음)
 */
data class EjectPlayerRequestDto(
    @field:NotNull(message = "퇴장 사유는 필수입니다")
    val reason: EjectionReason?,
)

/**
 * EjectPlayerRequestDto를 EjectionRequest 도메인 DTO로 변환합니다.
 */
fun EjectPlayerRequestDto.toDomain(ejectedPlayerId: Long): EjectionRequest =
    EjectionRequest(
        ejectedPlayerId = ejectedPlayerId,
        reason = this.reason!!,
    )

/**
 * 퇴장 + 긴급 교체 요청 DTO
 */
data class EjectAndSubstituteRequestDto(
    @field:NotNull(message = "퇴장 사유는 필수입니다")
    val reason: EjectionReason?,
    @field:NotNull(message = "교체 선수 ID는 필수입니다")
    val replacementPlayerId: Long?,
    @field:NotNull(message = "포지션은 필수입니다")
    val position: Position?,
)

/**
 * EjectAndSubstituteRequestDto를 EjectAndSubstituteRequest 도메인 DTO로 변환합니다.
 */
fun EjectAndSubstituteRequestDto.toDomain(ejectedPlayerId: Long): EjectAndSubstituteRequest =
    EjectAndSubstituteRequest(
        ejectedPlayerId = ejectedPlayerId,
        replacementPlayerId = this.replacementPlayerId!!,
        reason = this.reason!!,
        newPosition = this.position!!,
    )
