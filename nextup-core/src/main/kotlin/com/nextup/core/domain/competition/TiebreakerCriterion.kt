package com.nextup.core.domain.competition

/**
 * 리그 순위 동률 타이브레이커 기준
 *
 * 순위표에서 승률이 동일한 팀 간의 순위를 결정할 때 사용하는 기준입니다.
 * 대회별로 타이브레이커 기준의 우선순위를 커스터마이징할 수 있습니다.
 */
enum class TiebreakerCriterion(
    val displayName: String,
) {
    /** 상대 전적 (직접 대결 승률) */
    HEAD_TO_HEAD("상대 전적"),

    /** 득실점차 (득점 - 실점) */
    RUN_DIFFERENTIAL("득실점차"),

    /** 다득점 (총 득점 수) */
    RUNS_SCORED("다득점"),
}
