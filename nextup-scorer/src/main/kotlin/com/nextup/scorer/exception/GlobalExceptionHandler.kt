package com.nextup.scorer.exception

import com.nextup.common.dto.ApiResponse
import com.nextup.common.exception.BusinessException
import com.nextup.common.exception.InvalidInputException
import com.nextup.common.exception.InvalidStateException
import com.nextup.common.exception.NotFoundException
import com.nextup.infrastructure.exception.BaseExceptionHandler
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 전역 예외 핸들러
 *
 * 모든 컨트롤러에서 발생하는 예외를 처리합니다.
 * 공통 예외 처리 로직은 BaseExceptionHandler에서 상속받으며,
 * scorer 모듈 전용 동작을 override합니다.
 */
@RestControllerAdvice
class GlobalExceptionHandler : BaseExceptionHandler() {

    /**
     * 비즈니스 예외 처리
     *
     * 예외 타입에 따라 HTTP 상태 코드를 결정합니다.
     * - NotFoundException → 404
     * - InvalidStateException / InvalidInputException → 400
     * - 기타 BusinessException → 500
     */
    @ExceptionHandler(BusinessException::class)
    override fun handleBusinessException(ex: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        val status =
            when (ex) {
                is NotFoundException -> HttpStatus.NOT_FOUND
                is InvalidStateException -> HttpStatus.BAD_REQUEST
                is InvalidInputException -> HttpStatus.BAD_REQUEST
                else -> HttpStatus.INTERNAL_SERVER_ERROR
            }
        return ResponseEntity
            .status(status)
            .body(ApiResponse.error(ex.code, ex.message))
    }

    /**
     * 기타 예외 처리
     */
    @ExceptionHandler(Exception::class)
    override fun handleException(ex: Exception): ResponseEntity<ApiResponse<Nothing>> {
        log.error("UnexpectedException: {}", ex.message, ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "An unexpected error occurred"))
    }
}
