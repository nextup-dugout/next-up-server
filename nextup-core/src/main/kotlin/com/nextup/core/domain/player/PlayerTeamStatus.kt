package com.nextup.core.domain.player

/**
 * 선수의 팀 소속 상태를 나타내는 열거형
 *
 * - ACTIVE: 현재 해당 팀에서 활동 중
 * - INACTIVE: 비활동 상태 (부상, 휴식 등)
 * - TRANSFERRED: 다른 팀으로 이적
 */
enum class PlayerTeamStatus(val displayName: String) {
    ACTIVE("활동중"),
    INACTIVE("비활동"),
    TRANSFERRED("이적")
}
