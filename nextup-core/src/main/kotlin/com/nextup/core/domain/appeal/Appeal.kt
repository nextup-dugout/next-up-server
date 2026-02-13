package com.nextup.core.domain.appeal

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.game.Game
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 이의 제기/정정 신청 엔티티
 *
 * 경기 기록에 대한 선수/감독의 이의 제기를 관리합니다.
 */
@Entity
@Table(
    name = "appeals",
    indexes = [
        Index(name = "idx_appeals_game_id", columnList = "game_id"),
        Index(name = "idx_appeals_appealer_id", columnList = "appealer_id"),
        Index(name = "idx_appeals_status", columnList = "status"),
        Index(
            name = "idx_appeals_game_status",
            columnList = "game_id, status",
        ),
    ],
)
class Appeal private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    val game: Game,
    @Column(name = "appealer_id", nullable = false)
    val appealerId: Long,
    @Column(name = "appealer_name", nullable = false, length = 100)
    val appealerName: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val type: AppealType,
    @Column(nullable = false, length = 200)
    val title: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: AppealStatus = AppealStatus.PENDING,
    @Column(name = "reviewer_id")
    var reviewerId: Long? = null,
    @Column(name = "reviewer_comment", columnDefinition = "TEXT")
    var reviewerComment: String? = null,
    @Column(name = "reviewed_at")
    var reviewedAt: LocalDateTime? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    /**
     * 이의 제기를 승인합니다.
     */
    fun approve(
        reviewerId: Long,
        comment: String?,
    ) {
        require(status == AppealStatus.PENDING) {
            "대기 중인 이의 제기만 승인할 수 있습니다"
        }
        this.status = AppealStatus.APPROVED
        this.reviewerId = reviewerId
        this.reviewerComment = comment
        this.reviewedAt = LocalDateTime.now()
    }

    /**
     * 이의 제기를 반려합니다.
     */
    fun reject(
        reviewerId: Long,
        comment: String,
    ) {
        require(status == AppealStatus.PENDING) {
            "대기 중인 이의 제기만 반려할 수 있습니다"
        }
        require(comment.isNotBlank()) {
            "반려 사유는 필수입니다"
        }
        this.status = AppealStatus.REJECTED
        this.reviewerId = reviewerId
        this.reviewerComment = comment
        this.reviewedAt = LocalDateTime.now()
    }

    companion object {
        /**
         * 이의 제기를 생성합니다.
         */
        fun create(
            game: Game,
            appealerId: Long,
            appealerName: String,
            type: AppealType,
            title: String,
            description: String,
        ): Appeal {
            require(appealerId > 0) { "신청자 ID는 필수입니다" }
            require(appealerName.isNotBlank()) { "신청자 이름은 필수입니다" }
            require(title.isNotBlank()) { "제목은 필수입니다" }
            require(description.isNotBlank()) { "상세 설명은 필수입니다" }

            return Appeal(
                game = game,
                appealerId = appealerId,
                appealerName = appealerName,
                type = type,
                title = title,
                description = description,
            )
        }
    }
}
