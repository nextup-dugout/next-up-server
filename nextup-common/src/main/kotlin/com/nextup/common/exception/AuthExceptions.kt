package com.nextup.common.exception

/**
 * 인증 관련 기본 예외 클래스
 */
open class AuthenticationException(
    code: String,
    message: String,
) : BusinessException(code, message)

/**
 * 유효하지 않은 토큰일 때 발생하는 예외
 */
class InvalidTokenException(
    message: String = "Invalid token",
) : AuthenticationException(
        "INVALID_TOKEN",
        message,
    )

/**
 * 토큰이 만료되었을 때 발생하는 예외
 */
class TokenExpiredException(
    message: String = "Token has expired",
) : AuthenticationException(
        "TOKEN_EXPIRED",
        message,
    )

/**
 * Refresh Token을 찾을 수 없을 때 발생하는 예외
 */
class RefreshTokenNotFoundException(
    message: String = "Refresh token not found",
) : NotFoundException(
        "REFRESH_TOKEN_NOT_FOUND",
        message,
    )

/**
 * 잘못된 인증 정보일 때 발생하는 예외
 */
class InvalidCredentialsException(
    message: String = "Invalid email or password",
) : AuthenticationException(
        "INVALID_CREDENTIALS",
        message,
    )

/**
 * Refresh Token이 만료되었을 때 발생하는 예외
 */
class RefreshTokenExpiredException(
    message: String = "Refresh token has expired",
) : AuthenticationException(
        "REFRESH_TOKEN_EXPIRED",
        message,
    )

/**
 * Refresh Token이 폐기되었을 때 발생하는 예외
 */
class RefreshTokenRevokedException(
    message: String = "Refresh token has been revoked",
) : AuthenticationException(
        "REFRESH_TOKEN_REVOKED",
        message,
    )

/**
 * 지원하지 않는 OAuth2 Provider일 때 발생하는 예외
 */
class UnsupportedOAuth2ProviderException(
    provider: String,
) : AuthenticationException(
        "UNSUPPORTED_OAUTH2_PROVIDER",
        "Unsupported OAuth2 provider: $provider",
    )

/**
 * OAuth2 인증 처리 중 오류가 발생했을 때 발생하는 예외
 */
class OAuth2AuthenticationProcessingException(
    message: String,
) : AuthenticationException(
        "OAUTH2_PROCESSING_ERROR",
        message,
    )
