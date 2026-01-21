package com.nextup.core.domain.player

enum class Position(val displayName: String, val category: PositionCategory) {
    // Pitcher
    STARTING_PITCHER("선발투수", PositionCategory.PITCHER),
    RELIEF_PITCHER("중간계투", PositionCategory.PITCHER),
    CLOSER("마무리투수", PositionCategory.PITCHER),

    // Catcher
    CATCHER("포수", PositionCategory.CATCHER),

    // Infield
    FIRST_BASE("1루수", PositionCategory.INFIELD),
    SECOND_BASE("2루수", PositionCategory.INFIELD),
    SHORTSTOP("유격수", PositionCategory.INFIELD),
    THIRD_BASE("3루수", PositionCategory.INFIELD),

    // Outfield
    LEFT_FIELD("좌익수", PositionCategory.OUTFIELD),
    CENTER_FIELD("중견수", PositionCategory.OUTFIELD),
    RIGHT_FIELD("우익수", PositionCategory.OUTFIELD),

    // Designated Hitter
    DESIGNATED_HITTER("지명타자", PositionCategory.DESIGNATED_HITTER);
}

enum class PositionCategory(val displayName: String) {
    PITCHER("투수"),
    CATCHER("포수"),
    INFIELD("내야수"),
    OUTFIELD("외야수"),
    DESIGNATED_HITTER("지명타자")
}
