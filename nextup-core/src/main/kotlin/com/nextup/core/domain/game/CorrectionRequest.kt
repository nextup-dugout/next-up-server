package com.nextup.core.domain.game

import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

/**
 * 기록 정정 요청 엔티티
 *
 * 기록원이 기록 정정을 요청하고, 관리자가 승인/반려하는 워크플로우를 관리합니다.
 * PENDING → APPROVED / REJECTED
 */
@Entity
@Table(
    name = "correction_requests",
    indexes = [
        Index(name = "idx_correction_requests_game_id", columnList = "game_id"),
        Index(name = "idx_correction_requests_status", columnList = "status"),
        Index(name = "idx_correction_requests_requester", columnList = "requester_user_id"),
    ],
)
class CorrectionRequest private constructor(
    @Column(name = "game_id", nullable = false)
    val gameId: Long,
    @Column(name = "requester_user_id", nullable = false)
    val requesterUserId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "correction_type", nullable = false, length = 20)
    val correctionType: CorrectionType,
    @Column(name = "target_record_id", nullable = false)
    val targetRecordId: Long,
    @Column(name = "field_name", nullable = false, length = 100)
    val fieldName: String,
    @Column(name = "new_value", nullable = false, length = 500)
    val newValue: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    val reason: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: CorrectionRequestStatus = CorrectionRequestStatus.PENDING,
    @Column(name = "reviewer_user_id")
    var reviewerUserId: Long? = null,
    @Column(name = "review_comment", columnDefinition = "TEXT")
    var reviewComment: String? = null,
    @Column(name = "reviewed_at")
    var reviewedAt: Instant? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    /**
     * 정정 요청을 승인합니다.
     */
    fun approve(
        reviewerUserId: Long,
        comment: String? = null,
    ) {
        require(status == CorrectionRequestStatus.PENDING) {
            "PENDING 상태의 요청만 승인할 수 있습니다"
        }
        this.status = CorrectionRequestStatus.APPROVED
        this.reviewerUserId = reviewerUserId
        this.reviewComment = comment
        this.reviewedAt = Instant.now()
    }

    /**
     * 정정 요청을 반려합니다.
     */
    fun reject(
        reviewerUserId: Long,
        comment: String,
    ) {
        require(status == CorrectionRequestStatus.PENDING) {
            "PENDING 상태의 요청만 반려할 수 있습니다"
        }
        require(comment.isNotBlank()) {
            "반려 사유는 필수입니다"
        }
        this.status = CorrectionRequestStatus.REJECTED
        this.reviewerUserId = reviewerUserId
        this.reviewComment = comment
        this.reviewedAt = Instant.now()
    }

    companion object {
        /**
         * 기록 정정 요청을 생성합니다.
         */
        fun create(
            gameId: Long,
            requesterUserId: Long,
            correctionType: CorrectionType,
            targetRecordId: Long,
            fieldName: String,
            newValue: String,
            reason: String,
        ): CorrectionRequest {
            require(gameId > 0) { "경기 ID는 필수입니다" }
            require(requesterUserId > 0) { "요청자 ID는 필수입니다" }
            require(fieldName.isNotBlank()) { "정정 필드명은 필수입니다" }
            require(newValue.isNotBlank()) { "정정 값은 필수입니다" }
            require(reason.isNotBlank()) { "정정 사유는 필수입니다" }

            return CorrectionRequest(
                gameId = gameId,
                requesterUserId = requesterUserId,
                correctionType = correctionType,
                targetRecordId = targetRecordId,
                fieldName = fieldName,
                newValue = newValue,
                reason = reason,
            )
        }
    }
}

/**
 * 기록 정정 요청 상태
 */
enum class CorrectionRequestStatus(
    val displayName: String,
) {
    PENDING("대기"),
    APPROVED("승인"),
    REJECTED("반려"),
}
