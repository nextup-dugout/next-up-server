package com.nextup.api.dto.player

import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.PlayerTeamHistory

/**
 * 선수 검색/목록 응답 DTO
 *
 * GET /api/v1/players 에서 사용됩니다.
 */
data class PlayerSearchResponse(
    val id: Long,
    val name: String,
    val backNumber: Int?,
    val position: String?,
    val teamName: String?,
    val profileImageUrl: String?,
)

/**
 * Player 엔티티를 PlayerSearchResponse로 변환합니다.
 * currentHistory가 있으면 팀 정보와 등번호/포지션을 포함합니다.
 */
fun Player.toSearchResponse(currentHistory: PlayerTeamHistory? = null): PlayerSearchResponse =
    PlayerSearchResponse(
        id = this.id,
        name = this.name,
        backNumber = currentHistory?.uniformNumber,
        position = currentHistory?.position?.displayName ?: this.primaryPosition.displayName,
        teamName = currentHistory?.team?.name,
        profileImageUrl = this.profileImageUrl,
    )
