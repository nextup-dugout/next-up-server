package com.nextup.scorer.dto.game

import com.nextup.core.domain.game.PlayerShortageResult

/**
 * 선수 퇴장 (교체 없음) 응답 DTO
 *
 * 퇴장 처리 결과와 인원 부족 감지 정보를 포함합니다.
 * isShortage가 true이면 기록원이 몰수패 여부를 판단해야 합니다.
 */
data class PlayerExitResponse(
    val isShortage: Boolean,
    val gameTeamId: Long,
    val teamId: Long,
    val activePlayerCount: Int,
    val minimumRequired: Int,
    val message: String,
) {
    companion object {
        fun from(result: PlayerShortageResult): PlayerExitResponse =
            PlayerExitResponse(
                isShortage = result.isShortage,
                gameTeamId = result.gameTeamId,
                teamId = result.teamId,
                activePlayerCount = result.activePlayerCount,
                minimumRequired = result.minimumRequired,
                message =
                    if (result.isShortage) {
                        "인원 부족이 감지되었습니다. 활동 선수 ${result.activePlayerCount}명 " +
                            "(최소 ${result.minimumRequired}명 필요). 몰수패 처리를 고려하세요."
                    } else {
                        "선수가 퇴장 처리되었습니다. 활동 선수 ${result.activePlayerCount}명."
                    },
            )
    }
}
