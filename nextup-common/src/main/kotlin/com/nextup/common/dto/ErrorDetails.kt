package com.nextup.common.dto

/**
 * 에러 상세 정보
 *
 * @property code 에러 코드 (예: "GAME_NOT_FOUND", "INVALID_GAME_STATE")
 * @property message 에러 메시지
 */
data class ErrorDetails(
    val code: String,
    val message: String,
)
