package com.nextup.core.domain.game

/**
 * 퇴장 사유
 *
 * 부상 퇴장과 심판 판정에 의한 퇴장, 기타 사유를 구분합니다.
 * 사회인 야구에서 부상 퇴장은 빈번하며, 긴급 교체와 연계됩니다.
 */
enum class EjectionReason(
    val displayName: String,
) {
    /** 부상으로 인한 퇴장 */
    INJURY("부상"),

    /** 심판 판정에 의한 퇴장 (퇴장 명령) */
    EJECTION_BY_UMPIRE("심판 퇴장"),

    /** 기타 사유 */
    OTHER("기타"),
}
