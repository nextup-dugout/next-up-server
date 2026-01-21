package com.nextup.core.domain.competition

/**
 * 대회 유형
 */
enum class CompetitionType(val displayName: String) {
    LEAGUE("리그"),
    TOURNAMENT("토너먼트"),
    PLAYOFF("플레이오프"),
    CHAMPIONSHIP("챔피언십"),
    FRIENDLY("친선")
}
