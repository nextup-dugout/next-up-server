package com.nextup.backoffice.dto.correction

import com.nextup.core.domain.game.CorrectionRequestStatus
import com.nextup.core.domain.game.CorrectionType
import java.time.Instant

/**
 * 기록 정정 요청 승인 요청 DTO (Backoffice)
 */
data class ApproveCorrectionRequestDto(
    val comment: String? = null,
)

/**
 * 기록 정정 요청 반려 요청 DTO (Backoffice)
 */
data class RejectCorrectionRequestDto(
    val comment: String,
)

/**
 * 기록 정정 요청 응답 DTO (Backoffice)
 *
 * 변환 로직은 CorrectionExtensions.kt의 Extension Function을 사용합니다.
 */
data class CorrectionRequestAdminResponse(
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
    val updatedAt: Instant,
)
