package com.nextup.core.domain.game

/**
 * 라인업 제출 상태
 *
 * 감독의 라인업 작성부터 기록원 확인까지의 워크플로우를 나타냅니다.
 *
 * 상태 전이:
 * DRAFT → SUBMITTED → EXCHANGE_PENDING → EXCHANGED → CONFIRMED
 *                   ↓                  ↓
 *               REJECTED        EXCHANGE_REJECTED (→ SUBMITTED)
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

    /**
     * 교환 대기 중 - 양 팀이 모두 제출하여 상대팀 감독의 승인을 기다리는 상태
     */
    EXCHANGE_PENDING("교환 대기 중"),

    /**
     * 교환 거부됨 - 상대팀 감독이 라인업 교환을 거부한 상태 (재제출 필요)
     */
    EXCHANGE_REJECTED("교환 거부됨"),

    /**
     * 교환됨 - 양 팀 감독 모두 승인하여 상호 공개된 상태
     */
    EXCHANGED("교환됨"),
    ;

    /**
     * 제출 가능한 상태인지 확인합니다.
     */
    fun canSubmit(): Boolean = this == DRAFT || this == REJECTED || this == EXCHANGE_REJECTED

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
    fun canEdit(): Boolean = this == DRAFT || this == REJECTED || this == EXCHANGE_REJECTED

    /**
     * 교환 대기 상태로 전환 가능한지 확인합니다.
     */
    fun canMarkExchangePending(): Boolean = this == SUBMITTED

    /**
     * 교환 승인 가능한 상태인지 확인합니다.
     */
    fun canApproveExchange(): Boolean = this == EXCHANGE_PENDING

    /**
     * 교환 거부 가능한 상태인지 확인합니다.
     */
    fun canRejectExchange(): Boolean = this == EXCHANGE_PENDING

    /**
     * SUBMITTED 상태로 복원 가능한지 확인합니다.
     * 교환 거부로 인해 상대팀이 EXCHANGE_REJECTED가 되었을 때 내 라인업을 SUBMITTED로 복원합니다.
     */
    fun canRevertToSubmitted(): Boolean = this == EXCHANGE_PENDING

    /**
     * 상대팀이 조회 가능한 상태인지 확인합니다.
     */
    fun isVisibleToOpponent(): Boolean = this == EXCHANGED
}
