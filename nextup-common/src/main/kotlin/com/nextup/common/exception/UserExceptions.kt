package com.nextup.common.exception

/**
 * 사용자를 찾을 수 없을 때 발생하는 예외
 */
class UserNotFoundException(userId: Long) :
    NotFoundException(
        "USER_NOT_FOUND",
        "User not found: $userId"
    )

/**
 * 이메일로 사용자를 찾을 수 없을 때 발생하는 예외
 */
class UserNotFoundByEmailException(email: String) :
    NotFoundException(
        "USER_NOT_FOUND",
        "User not found with email: $email"
    )

/**
 * 이메일이 중복될 때 발생하는 예외
 */
class EmailDuplicateException(email: String) :
    BusinessException(
        "EMAIL_DUPLICATE",
        "Email already exists: $email"
    )

/**
 * OAuth 계정이 이미 연결되어 있을 때 발생하는 예외
 */
class OAuthAccountAlreadyLinkedException(provider: String) :
    BusinessException(
        "OAUTH_ALREADY_LINKED",
        "OAuth account already linked: $provider"
    )

/**
 * OAuth 계정을 찾을 수 없을 때 발생하는 예외
 */
class OAuthAccountNotFoundException(provider: String, oauthId: String) :
    NotFoundException(
        "OAUTH_ACCOUNT_NOT_FOUND",
        "OAuth account not found: $provider, $oauthId"
    )

/**
 * 인증 수단이 부족할 때 발생하는 예외
 */
class InsufficientAuthMethodException :
    BusinessException(
        "INSUFFICIENT_AUTH_METHOD",
        "At least one authentication method is required"
    )

/**
 * 비활성화된 사용자일 때 발생하는 예외
 */
class UserDeactivatedException(userId: Long) :
    InvalidStateException(
        "USER_DEACTIVATED",
        "User is deactivated: $userId"
    )
