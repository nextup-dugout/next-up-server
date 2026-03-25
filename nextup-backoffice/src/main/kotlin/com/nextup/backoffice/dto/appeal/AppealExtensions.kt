package com.nextup.backoffice.dto.appeal

import com.nextup.core.domain.appeal.Appeal

/**
 * Appeal Entity를 AppealAdminResponse DTO로 변환하는 Extension Function
 */
fun Appeal.toAdminResponse(): AppealAdminResponse =
    AppealAdminResponse(
        id = this.id,
        gameId = this.game.id,
        appealerId = this.appealerId,
        appealerName = this.appealerName,
        type = this.type,
        title = this.title,
        description = this.description,
        status = this.status,
        reviewerId = this.reviewerId,
        reviewerComment = this.reviewerComment,
        reviewedAt = this.reviewedAt,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )
