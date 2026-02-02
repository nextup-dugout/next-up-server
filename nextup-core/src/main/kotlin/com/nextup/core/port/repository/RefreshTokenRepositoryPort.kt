package com.nextup.core.port.repository

import com.nextup.core.domain.auth.RefreshToken
import java.time.Instant
import java.util.Optional

/**
 * RefreshToken Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface RefreshTokenRepositoryPort {

    fun save(refreshToken: RefreshToken): RefreshToken

    fun findAll(): List<RefreshToken>

    fun delete(refreshToken: RefreshToken)

    fun deleteById(id: Long)

    /**
     * 토큰 문자열로 RefreshToken을 조회합니다.
     *
     * @param token 토큰 문자열
     * @return RefreshToken (없으면 null)
     */
    fun findByToken(token: String): RefreshToken?

    /**
     * 사용자 ID로 모든 RefreshToken을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return RefreshToken 목록
     */
    fun findAllByUserId(userId: Long): List<RefreshToken>

    /**
     * 사용자 ID로 유효한 RefreshToken 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param now 현재 시간
     * @return 유효한 RefreshToken 목록
     */
    fun findValidTokensByUserId(userId: Long, now: Instant = Instant.now()): List<RefreshToken>

    /**
     * 사용자의 모든 RefreshToken을 폐기합니다.
     *
     * @param userId 사용자 ID
     */
    fun revokeAllByUserId(userId: Long)

    /**
     * 만료된 RefreshToken을 삭제합니다.
     *
     * @param expiredBefore 이 시간 이전에 만료된 토큰 삭제
     * @return 삭제된 토큰 수
     */
    fun deleteExpiredTokens(expiredBefore: Instant): Int

    /**
     * 폐기된 RefreshToken을 삭제합니다.
     *
     * @return 삭제된 토큰 수
     */
    fun deleteRevokedTokens(): Int
}
