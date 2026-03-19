package com.nextup.api.dto.mercenary

import com.nextup.core.domain.player.Position
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.Instant

data class CreateMercenaryRequestApiRequest(
    @field:NotNull(message = "팀 ID는 필수입니다")
    val teamId: Long,
    @field:NotNull(message = "경기 ID는 필수입니다")
    val gameId: Long,
    @field:NotEmpty(message = "필요 포지션을 최소 1개 이상 지정해야 합니다")
    val positions: Set<Position>,
    @field:Min(value = 1, message = "최대 모집 인원은 1명 이상이어야 합니다")
    val maxCount: Int,
    @field:NotNull(message = "마감 시한은 필수입니다")
    val deadline: Instant,
    val description: String? = null,
)
