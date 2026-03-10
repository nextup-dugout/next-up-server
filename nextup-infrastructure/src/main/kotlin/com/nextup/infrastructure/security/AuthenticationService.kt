package com.nextup.infrastructure.security

import com.nextup.common.exception.*
import com.nextup.core.domain.auth.RefreshToken
import com.nextup.core.domain.user.User
import com.nextup.infrastructure.repository.UserJpaRepository
import com.nextup.infrastructure.repository.auth.RefreshTokenRepository
import com.nextup.infrastructure.security.jwt.JwtTokenProvider
import com.nextup.infrastructure.security.oauth2.AuthCodeStore
import com.nextup.infrastructure.security.userdetails.CustomUserDetails
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 인증 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@Transactional
class AuthenticationService(
    private val userJpaRepository: UserJpaRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val passwordEncoder: PasswordEncoder,
    private val authCodeStore: AuthCodeStore,
) {
    /**
     * 로그인 처리
     *
     * @param email 사용자 이메일
     * @param password 비밀번호
     * @param deviceInfo 디바이스 정보 (선택)
     * @param ipAddress IP 주소 (선택)
     * @return TokenPair (accessToken, refreshToken)
     * @throws InvalidCredentialsException 이메일 또는 비밀번호가 잘못된 경우
     * @throws UserDeactivatedException 비활성화된 사용자인 경우
     */
    fun login(
        email: String,
        password: String,
        deviceInfo: String? = null,
        ipAddress: String? = null,
    ): TokenPair {
        val user =
            userJpaRepository.findByEmail(email)
                ?: throw InvalidCredentialsException()

        if (!user.isActive) {
            throw UserDeactivatedException(user.id)
        }

        if (user.password == null || !passwordEncoder.matches(password, user.password)) {
            throw InvalidCredentialsException()
        }

        return generateTokenPair(user, deviceInfo, ipAddress)
    }

    /**
     * 토큰 갱신
     *
     * @param refreshTokenString Refresh Token 문자열
     * @param deviceInfo 디바이스 정보 (선택)
     * @param ipAddress IP 주소 (선택)
     * @return 새로운 TokenPair
     * @throws RefreshTokenNotFoundException Refresh Token을 찾을 수 없는 경우
     * @throws RefreshTokenExpiredException Refresh Token이 만료된 경우
     * @throws RefreshTokenRevokedException Refresh Token이 폐기된 경우
     */
    fun refresh(
        refreshTokenString: String,
        deviceInfo: String? = null,
        ipAddress: String? = null,
    ): TokenPair {
        val refreshToken =
            refreshTokenRepository.findByToken(refreshTokenString)
                ?: throw RefreshTokenNotFoundException()

        if (refreshToken.isExpired) {
            throw RefreshTokenExpiredException()
        }

        if (refreshToken.isRevoked) {
            throw RefreshTokenRevokedException()
        }

        // 기존 Refresh Token 폐기
        refreshToken.revoke()
        refreshTokenRepository.save(refreshToken)

        val user =
            userJpaRepository
                .findById(refreshToken.userId)
                .orElseThrow { UserNotFoundException(refreshToken.userId) }

        if (!user.isActive) {
            throw UserDeactivatedException(user.id)
        }

        return generateTokenPair(user, deviceInfo, ipAddress)
    }

    /**
     * 로그아웃 처리 (현재 Refresh Token만 폐기)
     *
     * @param refreshTokenString Refresh Token 문자열
     */
    fun logout(refreshTokenString: String) {
        val refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
        refreshToken?.revoke()
        refreshToken?.let { refreshTokenRepository.save(it) }
    }

    /**
     * 전체 로그아웃 처리 (모든 Refresh Token 폐기)
     *
     * @param userId 사용자 ID
     */
    fun logoutAll(userId: Long) {
        refreshTokenRepository.revokeAllByUserId(userId)
    }

    /**
     * 사용자 ID로 CustomUserDetails를 조회합니다.
     *
     * @param userId 사용자 ID
     * @return CustomUserDetails
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    fun getUserDetails(userId: Long): CustomUserDetails {
        val user =
            userJpaRepository
                .findById(userId)
                .orElseThrow { UserNotFoundException(userId) }
        return CustomUserDetails.from(user)
    }

    /**
     * OAuth2 인가 코드를 JWT 토큰으로 교환합니다.
     *
     * @param code 일회용 인가 코드
     * @param deviceInfo 디바이스 정보 (선택)
     * @param ipAddress IP 주소 (선택)
     * @return OAuth2TokenResult (accessToken, refreshToken, isNewUser)
     * @throws InvalidInputException 유효하지 않거나 만료된 인가 코드인 경우
     */
    fun exchangeOAuth2Code(
        code: String,
        deviceInfo: String? = null,
        ipAddress: String? = null,
    ): OAuth2TokenResult {
        val authCodeResult =
            authCodeStore.consume(code)
                ?: throw InvalidInputException("INVALID_AUTH_CODE", "유효하지 않거나 만료된 인가 코드입니다")

        val user =
            userJpaRepository
                .findById(authCodeResult.userId)
                .orElseThrow { UserNotFoundException(authCodeResult.userId) }

        val tokenPair = generateTokenPair(user, deviceInfo, ipAddress)

        return OAuth2TokenResult(
            accessToken = tokenPair.accessToken,
            refreshToken = tokenPair.refreshToken,
            isNewUser = authCodeResult.isNewUser,
        )
    }

    private fun generateTokenPair(
        user: User,
        deviceInfo: String?,
        ipAddress: String?,
    ): TokenPair {
        val roles = user.roles.map { it.name }.toSet()

        val accessToken =
            jwtTokenProvider.createAccessToken(
                userId = user.id,
                email = user.email,
                roles = roles,
            )

        val refreshTokenString = jwtTokenProvider.createRefreshToken(user.id)
        val refreshToken =
            RefreshToken.create(
                userId = user.id,
                token = refreshTokenString,
                expiresAt = jwtTokenProvider.getRefreshTokenExpiration(),
                deviceInfo = deviceInfo,
                ipAddress = ipAddress,
            )
        refreshTokenRepository.save(refreshToken)

        return TokenPair(
            accessToken = accessToken,
            refreshToken = refreshTokenString,
        )
    }
}

/**
 * Access Token과 Refresh Token을 담는 데이터 클래스
 */
data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
)

/**
 * OAuth2 토큰 교환 결과
 */
data class OAuth2TokenResult(
    val accessToken: String,
    val refreshToken: String,
    val isNewUser: Boolean,
)
