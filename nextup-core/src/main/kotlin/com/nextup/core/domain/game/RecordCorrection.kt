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

/**
 * 기록 정정 엔티티
 *
 * 관리자가 타격/투수 기록을 정정할 때의 이력을 저장합니다.
 * 경기 상태(FINISHED 포함)와 무관하게 관리자 권한으로 정정 가능합니다.
 */
@Entity
@Table(
    name = "record_corrections",
    indexes = [
        Index(name = "idx_record_corrections_game_id", columnList = "game_id"),
        Index(name = "idx_record_corrections_target", columnList = "correction_type, target_record_id"),
        Index(name = "idx_record_corrections_admin", columnList = "admin_user_id"),
    ],
)
class RecordCorrection private constructor(
    @Column(name = "game_id", nullable = false)
    val gameId: Long,
    @Column(name = "admin_user_id", nullable = false)
    val adminUserId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "correction_type", nullable = false, length = 20)
    val correctionType: CorrectionType,
    @Column(name = "target_record_id", nullable = false)
    val targetRecordId: Long,
    @Column(name = "field_name", nullable = false, length = 100)
    val fieldName: String,
    @Column(name = "old_value", nullable = false, length = 500)
    val oldValue: String,
    @Column(name = "new_value", nullable = false, length = 500)
    val newValue: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    val reason: String,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    companion object {
        /**
         * 기록 정정 이력을 생성합니다.
         */
        fun create(
            gameId: Long,
            adminUserId: Long,
            correctionType: CorrectionType,
            targetRecordId: Long,
            fieldName: String,
            oldValue: String,
            newValue: String,
            reason: String,
        ): RecordCorrection {
            require(gameId > 0) { "경기 ID는 필수입니다" }
            require(adminUserId > 0) { "관리자 ID는 필수입니다" }
            require(fieldName.isNotBlank()) { "정정 필드명은 필수입니다" }
            require(reason.isNotBlank()) { "정정 사유는 필수입니다" }

            return RecordCorrection(
                gameId = gameId,
                adminUserId = adminUserId,
                correctionType = correctionType,
                targetRecordId = targetRecordId,
                fieldName = fieldName,
                oldValue = oldValue,
                newValue = newValue,
                reason = reason,
            )
        }
    }
}
