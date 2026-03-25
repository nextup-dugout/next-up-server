package com.nextup.core.domain.game

/**
 * 인원 부족 감지 결과 Value Object
 *
 * 선수 퇴장 후 인원 부족 상태를 나타냅니다.
 * 사회인 야구에서는 최소 9명이 필요하며, 9명 미만이면 몰수패 대상입니다.
 */
data class PlayerShortageResult(
    val isShortage: Boolean,
    val gameTeamId: Long,
    val teamId: Long,
    val activePlayerCount: Int,
    val minimumRequired: Int,
) {
    companion object {
        /**
         * 사회인 야구 기본 최소 인원 (9명)
         *
         * KBO 및 사회인 야구 연맹 규정에 따라 최소 9명이 필요합니다.
         * 교체 선수 없이 9명 미만이 되면 몰수패 대상이 됩니다.
         */
        const val DEFAULT_MINIMUM_PLAYERS = 9

        fun noShortage(
            gameTeamId: Long,
            teamId: Long,
            activePlayerCount: Int,
            minimumRequired: Int = DEFAULT_MINIMUM_PLAYERS,
        ): PlayerShortageResult =
            PlayerShortageResult(
                isShortage = false,
                gameTeamId = gameTeamId,
                teamId = teamId,
                activePlayerCount = activePlayerCount,
                minimumRequired = minimumRequired,
            )

        fun shortage(
            gameTeamId: Long,
            teamId: Long,
            activePlayerCount: Int,
            minimumRequired: Int = DEFAULT_MINIMUM_PLAYERS,
        ): PlayerShortageResult =
            PlayerShortageResult(
                isShortage = true,
                gameTeamId = gameTeamId,
                teamId = teamId,
                activePlayerCount = activePlayerCount,
                minimumRequired = minimumRequired,
            )
    }
}
