package com.nextup.api.dto.player

import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.PlayerTeamHistory
import java.time.LocalDate

/**
 * 선수 단건 상세 조회 응답 DTO
 *
 * GET /api/v1/players/{playerId} 에서 사용됩니다.
 */
data class PlayerDetailResponse(
    val id: Long,
    val name: String,
    val backNumber: Int?,
    val position: String?,
    val teamName: String?,
    val profileImageUrl: String?,
    val birthDate: LocalDate?,
    val birthPlace: String?,
    val nationality: String?,
    val height: Int?,
    val weight: Int?,
    val throwingHand: String?,
    val battingHand: String?,
    val debutYear: Int?,
    val isActive: Boolean,
)

/**
 * Player 엔티티를 PlayerDetailResponse로 변환합니다.
 * currentHistory가 있으면 팀 정보와 등번호/포지션을 포함합니다.
 */
fun Player.toDetailResponse(currentHistory: PlayerTeamHistory? = null): PlayerDetailResponse =
    PlayerDetailResponse(
        id = this.id,
        name = this.name,
        backNumber = currentHistory?.uniformNumber,
        position = currentHistory?.position?.displayName ?: this.primaryPosition.displayName,
        teamName = currentHistory?.team?.name,
        profileImageUrl = this.profileImageUrl,
        birthDate = this.birthDate,
        birthPlace = this.birthPlace,
        nationality = this.nationality,
        height = this.height,
        weight = this.weight,
        throwingHand = this.throwingHand?.name,
        battingHand = this.battingHand?.name,
        debutYear = this.debutYear,
        isActive = this.isActive,
    )
