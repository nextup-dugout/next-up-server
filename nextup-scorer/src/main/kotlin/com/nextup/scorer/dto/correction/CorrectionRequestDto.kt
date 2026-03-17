package com.nextup.scorer.dto.correction

import com.nextup.core.domain.game.CorrectionRequest
import com.nextup.core.domain.game.CorrectionRequestStatus
import com.nextup.core.domain.game.CorrectionType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant

/**
 * 기록 정정 요청 생성 DTO (Scorer)
 */
data class CreateCorrectionRequestDto(
    @field:NotNull(message = "경기 ID는 필수입니다")
    val gameId: Long,
    @field:NotNull(message = "정정 유형은 필수입니다")
    val correctionType: CorrectionType,
    @field:NotNull(message = "대상 기록 ID는 필수입니다")
    val targetRecordId: Long,
    @field:NotBlank(message = "정정 필드명은 필수입니다")
    val fieldName: String,
    @field:NotBlank(message = "정정 값은 필수입니다")
    val newValue: String,
    @field:NotBlank(message = "정정 사유는 필수입니다")
    val reason: String,
)

/**
 * 기록 정정 요청 응답 DTO (Scorer)
 */
data class CorrectionRequestResponse(
    val id: Long,
    val gameId: Long,
    val requesterUserId: Long,
    val correctionType: CorrectionType,
    val targetRecordId: Long,
    val fieldName: String,
    val newValue: String,
    val reason: String,
    val status: CorrectionRequestStatus,
    val statusDisplayName: String,
    val reviewerUserId: Long?,
    val reviewComment: String?,
    val reviewedAt: Instant?,
    val createdAt: Instant,
) {
    companion object {
        fun from(request: CorrectionRequest): CorrectionRequestResponse =
            CorrectionRequestResponse(
                id = request.id,
                gameId = request.gameId,
                requesterUserId = request.requesterUserId,
                correctionType = request.correctionType,
                targetRecordId = request.targetRecordId,
                fieldName = request.fieldName,
                newValue = request.newValue,
                reason = request.reason,
                status = request.status,
                statusDisplayName = request.status.displayName,
                reviewerUserId = request.reviewerUserId,
                reviewComment = request.reviewComment,
                reviewedAt = request.reviewedAt,
                createdAt = request.createdAt,
            )
    }
}
