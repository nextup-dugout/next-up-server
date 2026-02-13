package com.nextup.core.domain.game

/**
 * 경기 결과
 */
enum class GameResult(
    val displayName: String,
) {
    /** 승리 */
    WIN("승"),

    /** 패배 */
    LOSS("패"),

    /** 무승부 */
    DRAW("무"),

    /** 미결정 (경기 진행 중 또는 예정) */
    UNDECIDED("미정"),
}
