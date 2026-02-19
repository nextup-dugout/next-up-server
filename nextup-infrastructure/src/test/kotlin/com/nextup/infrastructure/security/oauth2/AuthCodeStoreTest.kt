package com.nextup.infrastructure.security.oauth2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@DisplayName("AuthCodeStore 테스트")
class AuthCodeStoreTest {
    private lateinit var authCodeStore: AuthCodeStore

    @BeforeEach
    fun setUp() {
        authCodeStore = AuthCodeStore()
    }

    @Test
    fun `인가 코드를 생성하고 소비할 수 있다`() {
        // given
        val code = authCodeStore.generate(userId = 1L, isNewUser = false)

        // when
        val result = authCodeStore.consume(code)

        // then
        assertThat(result).isNotNull
        assertThat(result!!.userId).isEqualTo(1L)
        assertThat(result.isNewUser).isFalse()
    }

    @Test
    fun `인가 코드는 1회만 사용할 수 있다`() {
        // given
        val code = authCodeStore.generate(userId = 1L, isNewUser = false)

        // when
        val firstConsume = authCodeStore.consume(code)
        val secondConsume = authCodeStore.consume(code)

        // then
        assertThat(firstConsume).isNotNull
        assertThat(secondConsume).isNull()
    }

    @Test
    fun `존재하지 않는 코드는 null을 반환한다`() {
        // when
        val result = authCodeStore.consume("non-existent-code")

        // then
        assertThat(result).isNull()
    }

    @Test
    fun `isNewUser 플래그가 올바르게 전달된다`() {
        // given
        val code = authCodeStore.generate(userId = 2L, isNewUser = true)

        // when
        val result = authCodeStore.consume(code)

        // then
        assertThat(result).isNotNull
        assertThat(result!!.userId).isEqualTo(2L)
        assertThat(result.isNewUser).isTrue()
    }

    @Test
    fun `서로 다른 코드는 독립적으로 동작한다`() {
        // given
        val code1 = authCodeStore.generate(userId = 1L, isNewUser = false)
        val code2 = authCodeStore.generate(userId = 2L, isNewUser = true)

        // when
        val result1 = authCodeStore.consume(code1)
        val result2 = authCodeStore.consume(code2)

        // then
        assertThat(result1!!.userId).isEqualTo(1L)
        assertThat(result2!!.userId).isEqualTo(2L)
    }

    @Test
    fun `만료된 인가 코드는 null을 반환한다`() {
        // given - 리플렉션으로 만료된 엔트리를 직접 삽입
        val storeField = AuthCodeStore::class.java.getDeclaredField("store")
        storeField.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val store = storeField.get(authCodeStore) as ConcurrentHashMap<String, Any>

        val entryClass =
            Class.forName(
                "com.nextup.infrastructure.security.oauth2.AuthCodeStore\$AuthCodeEntry",
            )
        val entryConstructor =
            entryClass.getDeclaredConstructor(
                Long::class.java,
                Boolean::class.java,
                Instant::class.java,
            )
        entryConstructor.isAccessible = true
        val expiredEntry =
            entryConstructor.newInstance(
                1L,
                false,
                Instant.now().minusSeconds(60),
            )

        store["expired-code"] = expiredEntry

        // when
        val result = authCodeStore.consume("expired-code")

        // then
        assertThat(result).isNull()
    }
}
