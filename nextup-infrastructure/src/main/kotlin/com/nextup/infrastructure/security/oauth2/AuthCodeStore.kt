package com.nextup.infrastructure.security.oauth2

import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * OAuth2 일회용 인가 코드 저장소
 *
 * OAuth2 인증 성공 시 생성된 일회용 코드를 저장하고,
 * 토큰 교환 시 코드를 소비(1회 사용 후 즉시 삭제)합니다.
 * TTL 30초 이내에 사용되지 않은 코드는 자동 만료됩니다.
 */
@Component
class AuthCodeStore {
    private val store = ConcurrentHashMap<String, AuthCodeEntry>()

    companion object {
        private const val CODE_TTL_SECONDS = 30L
    }

    /**
     * 일회용 인가 코드를 생성하고 저장합니다.
     *
     * @param userId 사용자 ID
     * @param isNewUser 신규 사용자 여부
     * @return 생성된 인가 코드
     */
    fun generate(
        userId: Long,
        isNewUser: Boolean,
    ): String {
        cleanExpired()
        val code = UUID.randomUUID().toString()
        store[code] =
            AuthCodeEntry(
                userId = userId,
                isNewUser = isNewUser,
                expiresAt = Instant.now().plusSeconds(CODE_TTL_SECONDS),
            )
        return code
    }

    /**
     * 인가 코드를 소비하고 삭제합니다 (1회 사용).
     *
     * @param code 인가 코드
     * @return 사용자 정보 (userId, isNewUser), 유효하지 않거나 만료된 경우 null
     */
    fun consume(code: String): AuthCodeResult? {
        val entry = store.remove(code) ?: return null
        if (entry.expiresAt.isBefore(Instant.now())) {
            return null
        }
        return AuthCodeResult(userId = entry.userId, isNewUser = entry.isNewUser)
    }

    private fun cleanExpired() {
        val now = Instant.now()
        store.entries.removeIf { it.value.expiresAt.isBefore(now) }
    }

    private data class AuthCodeEntry(
        val userId: Long,
        val isNewUser: Boolean,
        val expiresAt: Instant,
    )
}

data class AuthCodeResult(
    val userId: Long,
    val isNewUser: Boolean,
)
