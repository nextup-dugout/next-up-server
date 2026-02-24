package com.nextup.api.exception

import com.nextup.common.dto.ApiResponse
import com.nextup.common.exception.AuthenticationException
import com.nextup.common.exception.ForbiddenException
import com.nextup.common.exception.InvalidStateException
import com.nextup.infrastructure.exception.BaseExceptionHandler
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler : BaseExceptionHandler() {

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException): ResponseEntity<ApiResponse<Nothing>> {
        log.warn("AuthenticationException: code={}, message={}", ex.code, ex.message)
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(ex.code, ex.message))
    }

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbiddenException(ex: ForbiddenException): ResponseEntity<ApiResponse<Nothing>> {
        log.warn("ForbiddenException: code={}, message={}", ex.code, ex.message)
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(ex.code, ex.message))
    }

    @ExceptionHandler(InvalidStateException::class)
    fun handleInvalidStateException(ex: InvalidStateException): ResponseEntity<ApiResponse<Nothing>> {
        log.warn("InvalidStateException: code={}, message={}", ex.code, ex.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ex.code, ex.message))
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(
        ex: MissingServletRequestParameterException,
    ): ResponseEntity<ApiResponse<Nothing>> {
        log.warn("MissingServletRequestParameterException: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("MISSING_PARAMETER", ex.message))
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(ex: IllegalStateException): ResponseEntity<ApiResponse<Nothing>> {
        log.warn("IllegalStateException: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("INVALID_STATE", ex.message ?: "Invalid state"))
    }
}
