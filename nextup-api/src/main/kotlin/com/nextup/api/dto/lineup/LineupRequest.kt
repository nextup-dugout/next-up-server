package com.nextup.api.dto.lineup

import com.nextup.core.domain.player.Position
import com.nextup.core.service.lineup.LineupEntryInput
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * 라인업 생성 요청 DTO
 */
data class CreateLineupRequest(
    @field:NotNull(message = "경기 ID는 필수입니다.")
    val gameId: Long,
    @field:NotNull(message = "팀 ID는 필수입니다.")
    val teamId: Long,
)

/**
 * 라인업 엔트리 추가 요청 DTO
 */
data class AddLineupEntryRequest(
    @field:NotNull(message = "선수 ID는 필수입니다.")
    val playerId: Long,
    @field:NotNull(message = "포지션은 필수입니다.")
    val position: Position,
    @field:Min(value = 1, message = "타순은 1 이상이어야 합니다.")
    @field:Max(value = 10, message = "타순은 10 이하여야 합니다.")
    val battingOrder: Int?,
    val backNumber: Int?,
    val isStarter: Boolean = true,
)

/**
 * 라인업 엔트리 일괄 설정 요청 DTO
 */
data class SetLineupEntriesRequest(
    @field:NotEmpty(message = "라인업 엔트리는 필수입니다.")
    @field:Size(min = 9, max = 30, message = "라인업은 9명 이상 30명 이하여야 합니다.")
    @field:Valid
    val entries: List<LineupEntryDto>,
)

/**
 * 라인업 엔트리 DTO
 */
data class LineupEntryDto(
    @field:NotNull(message = "선수 ID는 필수입니다.")
    val playerId: Long,
    @field:NotNull(message = "포지션은 필수입니다.")
    val position: Position,
    @field:Min(value = 1, message = "타순은 1 이상이어야 합니다.")
    @field:Max(value = 10, message = "타순은 10 이하여야 합니다.")
    val battingOrder: Int?,
    val backNumber: Int?,
    val isStarter: Boolean = true,
) {
    fun toInput(): LineupEntryInput =
        LineupEntryInput(
            playerId = playerId,
            position = position,
            battingOrder = battingOrder,
            backNumber = backNumber,
            isStarter = isStarter,
        )
}
