package com.nextup.api.dto.player

import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * 무소속 선수 프로필 생성 요청
 *
 * 팀에 소속되지 않은 사용자가 이벤트 게임 등에 참가하기 위해
 * 선수 프로필을 생성할 때 사용합니다.
 */
data class CreateUnaffiliatedPlayerApiRequest(
    @field:NotBlank(message = "이름은 필수입니다.")
    @field:Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    val name: String,
    @field:NotNull(message = "주 포지션은 필수입니다.")
    val primaryPosition: Position,
    val throwingHand: ThrowingHand? = null,
    val battingHand: BattingHand? = null,
)
