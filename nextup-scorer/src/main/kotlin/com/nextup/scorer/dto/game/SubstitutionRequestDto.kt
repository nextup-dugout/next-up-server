package com.nextup.scorer.dto.game

import com.nextup.core.domain.player.Position
import com.nextup.core.service.game.dto.SubstitutionRequest
import jakarta.validation.constraints.NotNull

/**
 * 선수 교체 요청 DTO
 */
data class SubstitutionRequestDto(
    @field:NotNull(message = "GameTeam ID는 필수입니다")
    val gameTeamId: Long?,
    @field:NotNull(message = "교체 나가는 선수 ID는 필수입니다")
    val outgoingPlayerId: Long?,
    @field:NotNull(message = "교체 들어오는 선수 ID는 필수입니다")
    val incomingPlayerId: Long?,
    @field:NotNull(message = "포지션은 필수입니다")
    val newPosition: Position?,
    val newBattingOrder: Int? = null,
)

/**
 * SubstitutionRequestDto를 SubstitutionRequest 도메인 DTO로 변환합니다.
 */
fun SubstitutionRequestDto.toDomain(): SubstitutionRequest =
    SubstitutionRequest(
        gameTeamId = this.gameTeamId!!,
        outgoingPlayerId = this.outgoingPlayerId!!,
        incomingPlayerId = this.incomingPlayerId!!,
        newPosition = this.newPosition!!,
        newBattingOrder = this.newBattingOrder,
    )
