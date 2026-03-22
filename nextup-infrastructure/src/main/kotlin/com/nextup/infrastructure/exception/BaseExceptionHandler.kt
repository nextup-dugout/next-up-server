package com.nextup.infrastructure.exception

import com.nextup.common.dto.ApiResponse
import com.nextup.common.exception.BusinessException
import com.nextup.common.exception.ConflictException
import com.nextup.common.exception.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler

/**
 * 공통 예외 처리 기본 클래스
 *
 * 3개 API 모듈(api, backoffice, scorer)에서 공통으로 사용되는
 * 예외 처리 로직을 중앙화합니다.
 * 각 모듈의 GlobalExceptionHandler는 이 클래스를 상속하여 사용하며,
 * 모듈별 고유 예외가 있는 경우 override로 확장합니다.
 */
abstract class BaseExceptionHandler {

    protected val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(ex: NotFoundException): ResponseEntity<ApiResponse<Nothing>> {
        log.warn("NotFoundException: code={}, message={}", ex.code, ex.message)
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.code, ex.message))
    }

    @ExceptionHandler(ConflictException::class)
    fun handleConflictException(ex: ConflictException): ResponseEntity<ApiResponse<Nothing>> {
        log.warn("ConflictException: code={}, message={}", ex.code, ex.message)
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(ex.code, ex.message))
    }

    @ExceptionHandler(BusinessException::class)
    open fun handleBusinessException(ex: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        log.warn("BusinessException: code={}, message={}", ex.code, ex.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ex.code, ex.message))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        ex: HttpMessageNotReadableException,
    ): ResponseEntity<ApiResponse<Nothing>> {
        log.warn("HttpMessageNotReadableException: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("INVALID_REQUEST", "요청 본문을 읽을 수 없습니다"))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val errors =
            ex.bindingResult.fieldErrors
                .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        log.warn("ValidationException: {}", errors)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("VALIDATION_ERROR", errors))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ApiResponse<Nothing>> {
        log.warn("IllegalArgumentException: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("INVALID_ARGUMENT", ex.message ?: "Invalid argument"))
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException::class)
    fun handleOptimisticLockingFailureException(
        ex: ObjectOptimisticLockingFailureException,
    ): ResponseEntity<ApiResponse<Nothing>> {
        log.warn("OptimisticLockingFailureException: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse.error("CONCURRENT_BOOKING", "다른 사용자가 먼저 예약했습니다. 다시 시도해주세요."))
    }

    @ExceptionHandler(Exception::class)
    open fun handleException(ex: Exception): ResponseEntity<ApiResponse<Nothing>> {
        log.error("UnexpectedException: {}", ex.message, ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred"))
    }
}
