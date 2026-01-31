package com.nextup.api.dto.common

/**
 * API 응답 래퍼
 *
 * 모든 API 응답을 일관된 형태로 제공합니다.
 *
 * @param T 응답 데이터 타입
 * @property success 성공 여부
 * @property data 응답 데이터 (성공 시)
 * @property error 에러 정보 (실패 시)
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ErrorDetails?
) {
    companion object {
        /**
         * 성공 응답을 생성합니다.
         */
        fun <T> success(data: T): ApiResponse<T> {
            return ApiResponse(success = true, data = data, error = null)
        }

        /**
         * 실패 응답을 생성합니다.
         */
        fun <T> error(code: String, message: String): ApiResponse<T> {
            return ApiResponse(
                success = false,
                data = null,
                error = ErrorDetails(code, message)
            )
        }
    }
}
