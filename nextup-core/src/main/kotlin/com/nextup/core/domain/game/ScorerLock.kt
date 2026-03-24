package com.nextup.core.domain.game

import com.nextup.common.exception.GameAlreadyLockedException
import com.nextup.common.exception.GameNotLockedByCurrentScorerException
import com.nextup.common.exception.GameNotLockedForRecordingException
import com.nextup.common.exception.ScorerMismatchException
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.time.LocalDateTime

/**
 * 기록원 잠금 시스템 (Embeddable Value Object)
 *
 * 경기를 기록하는 기록원(scorer)의 독점 잠금 상태를 관리합니다.
 * 한 번에 하나의 기록원만 경기를 기록할 수 있도록 보장합니다.
 *
 * @property scorerId 현재 잠금을 소유한 기록원 ID (null이면 잠금 없음)
 * @property lockedAt 잠금 시각 (null이면 잠금 없음)
 */
@Embeddable
class ScorerLock(
    @Column(name = "scorer_id")
    var scorerId: Long? = null,
    @Column(name = "locked_at")
    var lockedAt: LocalDateTime? = null,
) {
    /**
     * 잠금 상태인지 확인합니다.
     */
    val isLocked: Boolean
        get() = scorerId != null

    /**
     * 기록원이 경기를 독점 잠금합니다.
     *
     * 이미 다른 기록원이 잠금한 경우 [GameAlreadyLockedException]이 발생합니다.
     * 동일 기록원이 중복 잠금 시도하면 멱등하게 무시합니다.
     *
     * @param gameId 경기 ID (예외 메시지용)
     * @param scorerId 잠금을 요청하는 기록원 ID
     * @throws GameAlreadyLockedException 다른 기록원이 이미 잠금한 경우
     */
    fun lock(
        gameId: Long,
        scorerId: Long,
    ) {
        if (this.scorerId != null && this.scorerId != scorerId) {
            throw GameAlreadyLockedException(gameId, this.scorerId!!)
        }
        this.scorerId = scorerId
        this.lockedAt = LocalDateTime.now()
    }

    /**
     * 기록원의 경기 잠금을 해제합니다.
     *
     * 잠금한 기록원 본인만 해제할 수 있습니다.
     *
     * @param gameId 경기 ID (예외 메시지용)
     * @param scorerId 잠금 해제를 요청하는 기록원 ID
     * @throws GameNotLockedByCurrentScorerException 해당 기록원이 잠금하지 않은 경우
     */
    fun unlock(
        gameId: Long,
        scorerId: Long,
    ) {
        if (this.scorerId == null || this.scorerId != scorerId) {
            throw GameNotLockedByCurrentScorerException(gameId, scorerId)
        }
        this.scorerId = null
        this.lockedAt = null
    }

    /**
     * 강제로 기록원 잠금을 해제합니다 (관리자용 / 경기 종료 시).
     */
    fun forceUnlock() {
        this.scorerId = null
        this.lockedAt = null
    }

    /**
     * 잠금이 만료되었는지 확인합니다.
     *
     * @param timeoutMinutes 타임아웃 시간 (분)
     * @return 잠금이 만료되었으면 true
     */
    fun isExpired(timeoutMinutes: Long): Boolean {
        val lockedTime = lockedAt ?: return false
        return lockedTime.plusMinutes(timeoutMinutes).isBefore(LocalDateTime.now())
    }

    /**
     * 만료된 잠금을 자동 해제합니다 (스케줄러용).
     */
    fun expire() {
        this.scorerId = null
        this.lockedAt = null
    }

    /**
     * 현재 기록원이 경기를 잠금하고 있는지 확인합니다.
     */
    fun isLockedBy(scorerId: Long): Boolean = this.scorerId == scorerId

    /**
     * 기록 API 호출 시 기록원 잠금 검증을 수행합니다.
     *
     * 경기가 잠금되지 않은 상태이면 [GameNotLockedForRecordingException]을 발생시키고,
     * 잠금한 기록원과 요청 기록원이 다르면 [ScorerMismatchException]을 발생시킵니다.
     *
     * @param gameId 경기 ID (예외 메시지용)
     * @param requestScorerId 기록을 요청하는 기록원 ID
     * @throws GameNotLockedForRecordingException 경기가 잠금되지 않은 경우
     * @throws ScorerMismatchException 잠금한 기록원과 요청 기록원이 다른 경우
     */
    fun validate(
        gameId: Long,
        requestScorerId: Long,
    ) {
        if (this.scorerId == null) {
            throw GameNotLockedForRecordingException(gameId)
        }
        if (this.scorerId != requestScorerId) {
            throw ScorerMismatchException(gameId, requestScorerId, this.scorerId!!)
        }
    }
}
