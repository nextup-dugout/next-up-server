package com.nextup.api.exception

import com.nextup.api.dto.common.ApiResponse
import com.nextup.common.exception.AuthenticationException
import com.nextup.common.exception.BusinessException
import com.nextup.common.exception.InvalidStateException
import com.nextup.common.exception.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException): ResponseEntity<ApiResponse<Nothing>> {
        log.warn("AuthenticationException: code={}, message={}", ex.code, ex.message)
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(ex.code, ex.message))
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(ex: NotFoundException): ResponseEntity<ApiResponse<Nothing>> {
        log.warn("NotFoundException: code={}, message={}", ex.code, ex.message)
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.code, ex.message))
    }

    @ExceptionHandler(InvalidStateException::class)
    fun handleInvalidStateException(ex: InvalidStateException): ResponseEntity<ApiResponse<Nothing>> {
        log.warn("InvalidStateException: code={}, message={}", ex.code, ex.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ex.code, ex.message))
    }

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        log.warn("BusinessException: code={}, message={}", ex.code, ex.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ex.code, ex.message))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val errors = ex.bindingResult.fieldErrors
            .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        log.warn("ValidationException: {}", errors)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("VALIDATION_ERROR", errors))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ApiResponse<Nothing>> {
        log.error("UnexpectedException: {}", ex.message, ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred"))
    }
}
