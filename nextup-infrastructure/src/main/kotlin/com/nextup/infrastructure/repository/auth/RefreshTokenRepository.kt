package com.nextup.infrastructure.repository.auth

import com.nextup.core.domain.auth.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * RefreshToken Repository
 */
@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
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
     * @return 유효한 RefreshToken 목록
     */
    @Query(
        """
        SELECT r FROM RefreshToken r
        WHERE r.userId = :userId
        AND r.isRevoked = false
        AND r.expiresAt > :now
    """,
    )
    fun findValidTokensByUserId(
        @Param("userId") userId: Long,
        @Param("now") now: Instant = Instant.now(),
    ): List<RefreshToken>

    /**
     * 사용자의 모든 RefreshToken을 폐기합니다.
     *
     * @param userId 사용자 ID
     */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.isRevoked = true WHERE r.userId = :userId")
    fun revokeAllByUserId(
        @Param("userId") userId: Long,
    )

    /**
     * 만료된 RefreshToken을 삭제합니다.
     *
     * @param expiredBefore 이 시간 이전에 만료된 토큰 삭제
     * @return 삭제된 토큰 수
     */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :expiredBefore")
    fun deleteExpiredTokens(
        @Param("expiredBefore") expiredBefore: Instant,
    ): Int

    /**
     * 폐기된 RefreshToken을 삭제합니다.
     *
     * @return 삭제된 토큰 수
     */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.isRevoked = true")
    fun deleteRevokedTokens(): Int
}
