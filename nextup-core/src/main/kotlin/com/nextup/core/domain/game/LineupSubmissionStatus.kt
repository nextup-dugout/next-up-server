package com.nextup.core.domain.game

/**
 * 라인업 제출 상태
 *
 * 감독의 라인업 작성부터 기록원 확인까지의 워크플로우를 나타냅니다.
 */
enum class LineupSubmissionStatus(
    val displayName: String,
) {
    /**
     * 작성 중 - 감독이 라인업을 작성 중인 상태
     */
    DRAFT("작성 중"),

    /**
     * 제출됨 - 감독이 기록원에게 제출한 상태
     */
    SUBMITTED("제출됨"),

    /**
     * 확인 완료 - 기록원이 라인업을 확인하고 승인한 상태
     */
    CONFIRMED("확인 완료"),

    /**
     * 반려됨 - 기록원이 라인업을 반려한 상태
     */
    REJECTED("반려됨"),
    ;

    /**
     * 제출 가능한 상태인지 확인합니다.
     */
    fun canSubmit(): Boolean = this == DRAFT || this == REJECTED

    /**
     * 확인 가능한 상태인지 확인합니다.
     */
    fun canConfirm(): Boolean = this == SUBMITTED

    /**
     * 반려 가능한 상태인지 확인합니다.
     */
    fun canReject(): Boolean = this == SUBMITTED

    /**
     * 수정 가능한 상태인지 확인합니다.
     */
    fun canEdit(): Boolean = this == DRAFT || this == REJECTED
}
