package com.nextup.scorer.dto.game

import com.nextup.core.domain.player.Position
import com.nextup.core.service.game.dto.PositionChangeRequest
import com.nextup.core.service.game.dto.PositionSwapRequest
import jakarta.validation.constraints.NotNull

/**
 * 포지션 변경 요청 DTO
 */
data class PositionChangeRequestDto(
    @field:NotNull(message = "선수 ID는 필수입니다")
    val playerId: Long?,
    @field:NotNull(message = "새 포지션은 필수입니다")
    val newPosition: Position?,
)

/**
 * PositionChangeRequestDto를 PositionChangeRequest 도메인 DTO로 변환합니다.
 */
fun PositionChangeRequestDto.toDomain(): PositionChangeRequest =
    PositionChangeRequest(
        playerId = this.playerId!!,
        newPosition = this.newPosition!!,
    )

/**
 * 포지션 교환 요청 DTO
 */
data class PositionSwapRequestDto(
    @field:NotNull(message = "첫 번째 선수 ID는 필수입니다")
    val player1Id: Long?,
    @field:NotNull(message = "두 번째 선수 ID는 필수입니다")
    val player2Id: Long?,
)

/**
 * PositionSwapRequestDto를 PositionSwapRequest 도메인 DTO로 변환합니다.
 */
fun PositionSwapRequestDto.toDomain(): PositionSwapRequest =
    PositionSwapRequest(
        player1Id = this.player1Id!!,
        player2Id = this.player2Id!!,
    )
