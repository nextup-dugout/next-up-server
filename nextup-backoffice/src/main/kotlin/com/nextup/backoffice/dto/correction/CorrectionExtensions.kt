package com.nextup.backoffice.dto.correction

import com.nextup.core.domain.game.CorrectionRequest

/**
 * CorrectionRequest Entity를 CorrectionRequestAdminResponse DTO로 변환하는 Extension Function
 */
fun CorrectionRequest.toAdminResponse(): CorrectionRequestAdminResponse =
    CorrectionRequestAdminResponse(
        id = this.id,
        gameId = this.gameId,
        requesterUserId = this.requesterUserId,
        correctionType = this.correctionType,
        targetRecordId = this.targetRecordId,
        fieldName = this.fieldName,
        newValue = this.newValue,
        reason = this.reason,
        status = this.status,
        statusDisplayName = this.status.displayName,
        reviewerUserId = this.reviewerUserId,
        reviewComment = this.reviewComment,
        reviewedAt = this.reviewedAt,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )
