package com.nextup.backoffice.dto.common

/**
 * API 응답 래퍼
 *
 * 모든 API 응답을 일관된 형태로 제공합니다.
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ErrorDetails?
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> {
            return ApiResponse(success = true, data = data, error = null)
        }

        fun <T> error(code: String, message: String): ApiResponse<T> {
            return ApiResponse(
                success = false,
                data = null,
                error = ErrorDetails(code, message)
            )
        }
    }
}

data class ErrorDetails(
    val code: String,
    val message: String
)
