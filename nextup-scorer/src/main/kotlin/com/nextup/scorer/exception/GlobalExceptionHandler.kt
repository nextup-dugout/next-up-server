package com.nextup.scorer.exception

import com.nextup.common.exception.BusinessException
import com.nextup.common.exception.InvalidStateException
import com.nextup.common.exception.NotFoundException
import com.nextup.scorer.dto.common.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 전역 예외 핸들러
 *
 * 모든 컨트롤러에서 발생하는 예외를 처리합니다.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 처리
     */
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ResponseEntity<ApiResponse<Unit>> {
        val status = when (ex) {
            is NotFoundException -> HttpStatus.NOT_FOUND
            is InvalidStateException -> HttpStatus.BAD_REQUEST
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }
        return ResponseEntity
            .status(status)
            .body(ApiResponse.error(ex.code, ex.message))
    }

    /**
     * 입력 검증 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Unit>> {
        val message = ex.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("VALIDATION_ERROR", message))
    }

    /**
     * IllegalArgumentException 처리 (도메인 로직 검증 실패)
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ApiResponse<Unit>> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("INVALID_ARGUMENT", ex.message ?: "Invalid argument"))
    }

    /**
     * 기타 예외 처리
     */
    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ApiResponse<Unit>> {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "An unexpected error occurred"))
    }
}
