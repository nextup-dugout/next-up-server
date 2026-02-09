package com.nextup.backoffice.dto.appeal

import com.nextup.core.domain.appeal.Appeal
import com.nextup.core.domain.appeal.AppealStatus
import com.nextup.core.domain.appeal.AppealType
import java.time.Instant
import java.time.LocalDateTime

/**
 * 이의 제기 관리자 응답 DTO
 *
 * backoffice 모듈에 독립적으로 존재
 */
data class AppealAdminResponse(
    val id: Long,
    val gameId: Long,
    val appealerId: Long,
    val appealerName: String,
    val type: AppealType,
    val title: String,
    val description: String,
    val status: AppealStatus,
    val reviewerId: Long?,
    val reviewerComment: String?,
    val reviewedAt: LocalDateTime?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(appeal: Appeal): AppealAdminResponse =
            AppealAdminResponse(
                id = appeal.id,
                gameId = appeal.game.id,
                appealerId = appeal.appealerId,
                appealerName = appeal.appealerName,
                type = appeal.type,
                title = appeal.title,
                description = appeal.description,
                status = appeal.status,
                reviewerId = appeal.reviewerId,
                reviewerComment = appeal.reviewerComment,
                reviewedAt = appeal.reviewedAt,
                createdAt = appeal.createdAt,
                updatedAt = appeal.updatedAt,
            )
    }
}
