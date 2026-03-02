package com.nextup.core.domain.game

/**
 * 경기 이벤트 타입
 */
enum class GameEventType(
    val displayName: String,
) {
    PLATE_APPEARANCE("타석 결과"),
    BASE_RUNNING("주루 플레이"),
    SUBSTITUTION("선수 교체"),
    PITCHING_CHANGE("투수 교체"),
    INNING_CHANGE("이닝 전환"),
    GAME_STATUS("경기 상태 변경"),
    POSITION_CHANGE("포지션 변경"),
}
