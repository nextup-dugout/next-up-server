package com.nextup.api.dto.player

import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

/**
 * 선수 프로필 수정 요청 DTO
 *
 * PUT /api/v1/players/me 에서 사용됩니다.
 * 모든 필드가 nullable이며, null인 필드는 기존 값을 유지합니다.
 * (단, throwingHand/battingHand/height/weight는 명시적으로 null 설정 가능)
 */
data class UpdatePlayerProfileRequest(
    val primaryPosition: Position? = null,
    val throwingHand: ThrowingHand? = null,
    val battingHand: BattingHand? = null,
    @field:Min(100, message = "키는 100cm 이상이어야 합니다.")
    @field:Max(250, message = "키는 250cm 이하여야 합니다.")
    val height: Int? = null,
    @field:Min(30, message = "몸무게는 30kg 이상이어야 합니다.")
    @field:Max(200, message = "몸무게는 200kg 이하여야 합니다.")
    val weight: Int? = null,
)

/**
 * 내 선수 프로필 응답 DTO
 *
 * 인증된 사용자의 선수 프로필 정보를 반환합니다.
 */
data class MyPlayerProfileResponse(
    val id: Long,
    val name: String,
    val primaryPosition: String,
    val throwingHand: String?,
    val battingHand: String?,
    val height: Int?,
    val weight: Int?,
    val profileImageUrl: String?,
)

/**
 * Player 엔티티를 MyPlayerProfileResponse로 변환합니다.
 */
fun Player.toMyProfileResponse(): MyPlayerProfileResponse =
    MyPlayerProfileResponse(
        id = this.id,
        name = this.name,
        primaryPosition = this.primaryPosition.displayName,
        throwingHand = this.throwingHand?.displayName,
        battingHand = this.battingHand?.displayName,
        height = this.height,
        weight = this.weight,
        profileImageUrl = this.profileImageUrl,
    )
