package com.nextup.core.domain.game

/**
 * 경기 이벤트 유형
 *
 * 경기 중 발생할 수 있는 이벤트의 대분류를 정의합니다.
 */
enum class GameEventType(
    val displayName: String,
    val description: String
) {
    // 타석 관련
    PLATE_APPEARANCE("타석 결과", "타자의 타석 결과 (안타, 아웃, 볼넷 등)"),

    // 주루 관련
    BASE_RUNNING("주루 이벤트", "도루, 보크, 폭투 등 주루 관련 이벤트"),

    // 선수 교체
    SUBSTITUTION("선수 교체", "타자, 투수, 수비 교체"),

    // 이닝/경기 진행
    INNING_START("이닝 시작", "새 이닝 시작"),
    INNING_END("이닝 종료", "이닝 종료 (3아웃)"),
    HALF_INNING_CHANGE("공수 교대", "초/말 전환"),
    GAME_START("경기 시작", "경기 시작"),
    GAME_END("경기 종료", "경기 정상 종료"),
    GAME_CALLED("콜드게임", "우천, 일몰 등으로 경기 중단"),

    // 기타
    TIMEOUT("타임아웃", "타임아웃 요청"),
    MOUND_VISIT("마운드 방문", "감독/코치 마운드 방문"),
    REVIEW("비디오 판독", "비디오 판독 요청"),
    INJURY("부상", "선수 부상 발생"),
    EJECTION("퇴장", "퇴장 처분"),
    DELAY("지연", "경기 지연"),
    OTHER("기타", "기타 이벤트");

    /**
     * 타석 결과 이벤트인지 확인합니다.
     */
    val isPlateAppearance: Boolean
        get() = this == PLATE_APPEARANCE

    /**
     * 주루 이벤트인지 확인합니다.
     */
    val isBaseRunning: Boolean
        get() = this == BASE_RUNNING

    /**
     * 선수 교체 이벤트인지 확인합니다.
     */
    val isSubstitution: Boolean
        get() = this == SUBSTITUTION

    /**
     * 경기 진행 관련 이벤트인지 확인합니다.
     */
    val isGameProgress: Boolean
        get() = this in listOf(
            INNING_START, INNING_END, HALF_INNING_CHANGE,
            GAME_START, GAME_END, GAME_CALLED
        )
}
