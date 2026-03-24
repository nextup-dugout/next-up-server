package com.nextup.core.domain.competition

/**
 * 대회 유형
 */
enum class CompetitionType(
    val displayName: String,
) {
    LEAGUE("리그"),
    TOURNAMENT("토너먼트"),
    PLAYOFF("플레이오프"),
    CHAMPIONSHIP("챔피언십"),
    FRIENDLY("친선"),
    ;

    /**
     * 공식 대회 여부를 반환합니다.
     *
     * FRIENDLY(친선)을 제외한 모든 대회 유형이 공식 대회입니다.
     * 공식 대회의 기록만 시즌 순위/리더보드에 반영됩니다.
     */
    val isOfficial: Boolean
        get() = this != FRIENDLY

    companion object {
        /** 공식 대회 유형 목록 (순위/리더보드 필터링용) */
        val OFFICIAL_TYPES: List<CompetitionType> =
            entries.filter { it.isOfficial }
    }
}
