package com.nextup.core.domain.player

enum class Position(val displayName: String, val abbreviation: String, val category: PositionCategory) {
    // Pitcher
    STARTING_PITCHER("선발투수", "SP", PositionCategory.PITCHER),
    RELIEF_PITCHER("중간계투", "RP", PositionCategory.PITCHER),
    CLOSER("마무리투수", "CP", PositionCategory.PITCHER),

    // Catcher
    CATCHER("포수", "C", PositionCategory.CATCHER),

    // Infield
    FIRST_BASE("1루수", "1B", PositionCategory.INFIELD),
    SECOND_BASE("2루수", "2B", PositionCategory.INFIELD),
    SHORTSTOP("유격수", "SS", PositionCategory.INFIELD),
    THIRD_BASE("3루수", "3B", PositionCategory.INFIELD),

    // Outfield
    LEFT_FIELD("좌익수", "LF", PositionCategory.OUTFIELD),
    CENTER_FIELD("중견수", "CF", PositionCategory.OUTFIELD),
    RIGHT_FIELD("우익수", "RF", PositionCategory.OUTFIELD),

    // Designated Hitter
    DESIGNATED_HITTER("지명타자", "DH", PositionCategory.DESIGNATED_HITTER);
}

enum class PositionCategory(val displayName: String) {
    PITCHER("투수"),
    CATCHER("포수"),
    INFIELD("내야수"),
    OUTFIELD("외야수"),
    DESIGNATED_HITTER("지명타자")
}
