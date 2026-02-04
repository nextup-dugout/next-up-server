package com.nextup.core.domain.player

/**
 * 선수 경력 유형
 *
 * 선출(선수 출신) 여부 판별의 기초 데이터로 사용됩니다.
 */
enum class CareerType(
    val displayName: String,
    val isProfessional: Boolean,
) {
    /** KBO 프로야구 */
    KBO("KBO 프로", true),

    /** MLB 메이저리그 */
    MLB("MLB", true),

    /** NPB 일본프로야구 */
    NPB("NPB", true),

    /** 기타 해외 프로리그 */
    FOREIGN_PRO("해외 프로", true),

    /** 독립리그 */
    INDEPENDENT("독립리그", true),

    /** 대학교 야구부 */
    UNIVERSITY("대학교", false),

    /** 고등학교 야구부 */
    HIGH_SCHOOL("고등학교", false),

    /** 중학교 야구부 */
    MIDDLE_SCHOOL("중학교", false),

    /** 리틀야구/유소년 */
    YOUTH("유소년", false),

    /** 사회인 야구 */
    AMATEUR("사회인", false),

    /** 군 야구단 */
    MILITARY("군", false),
}
