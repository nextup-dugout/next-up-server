package com.nextup.common.exception

/**
 * 비즈니스 로직 예외 기본 클래스
 */
open class BusinessException(
    val code: String,
    override val message: String,
) : RuntimeException(message)

/**
 * 엔티티를 찾을 수 없을 때 발생하는 예외
 */
open class NotFoundException(
    code: String,
    message: String,
) : BusinessException(code, message)

/**
 * 유효하지 않은 상태일 때 발생하는 예외
 */
open class InvalidStateException(
    code: String,
    message: String,
) : BusinessException(code, message)

/**
 * 유효하지 않은 입력일 때 발생하는 예외
 */
open class InvalidInputException(
    code: String,
    message: String,
) : BusinessException(code, message)

/**
 * 접근 권한이 없을 때 발생하는 예외
 */
open class ForbiddenException(
    code: String,
    message: String,
) : BusinessException(code, message)

/**
 * 리소스 충돌(동시성 경쟁)이 발생했을 때 사용하는 예외 (HTTP 409)
 */
open class ConflictException(
    code: String,
    message: String,
) : BusinessException(code, message)
