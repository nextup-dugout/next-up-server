package com.nextup.core.domain.user

/**
 * 사용자 권한
 */
enum class Role(
    val displayName: String,
    val description: String,
) {
    /** 시스템 관리자 - 전체 시스템 관리 */
    ADMIN("관리자", "시스템 전체 관리 권한"),

    /** 협회 관리자 - 협회 및 소속 리그 관리 */
    ASSOCIATION_ADMIN("협회 관리자", "협회 및 소속 리그 관리 권한"),

    /** 리그 관리자 - 리그 및 대회 관리 */
    LEAGUE_ADMIN("리그 관리자", "리그 및 대회 관리 권한"),

    /** 팀 매니저 - 팀 및 소속 선수 관리 */
    TEAM_MANAGER("팀 매니저", "팀 및 소속 선수 관리 권한"),

    /** 기록원 - 경기 기록 입력 */
    SCORER("기록원", "경기 기록 입력 권한"),

    /** 선수 - 본인 기록 조회 및 프로필 관리 */
    PLAYER("선수", "본인 기록 조회 및 프로필 관리"),

    /** 일반 사용자 - 조회만 가능 */
    USER("사용자", "기본 조회 권한"),
}
