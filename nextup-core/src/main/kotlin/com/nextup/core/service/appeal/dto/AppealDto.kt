package com.nextup.core.service.appeal.dto

import com.nextup.core.domain.appeal.AppealType

/**
 * 이의 제기 생성 요청 DTO
 */
data class CreateAppealRequest(
    val gameId: Long,
    val appealerId: Long,
    val appealerName: String,
    val type: AppealType,
    val title: String,
    val description: String,
)

/**
 * 이의 제기 승인 요청 DTO
 */
data class ApproveAppealRequest(
    val reviewerId: Long,
    val comment: String? = null,
)

/**
 * 이의 제기 반려 요청 DTO
 */
data class RejectAppealRequest(
    val reviewerId: Long,
    val comment: String,
)
