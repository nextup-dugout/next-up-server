package com.nextup.infrastructure.adapter.notification

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StubPushNotificationAdapterTest {
    private val adapter = StubPushNotificationAdapter()

    @Test
    fun `단일 발송은 항상 성공을 반환한다`() {
        val result = adapter.send("test-token", "제목", "내용")

        assertThat(result).isTrue()
    }

    @Test
    fun `배치 발송은 토큰 수만큼 성공 수를 반환한다`() {
        val tokens = listOf("token-1", "token-2", "token-3")

        val result = adapter.sendBatch(tokens, "제목", "내용")

        assertThat(result).isEqualTo(3)
    }

    @Test
    fun `빈 토큰 목록의 배치 발송은 0을 반환한다`() {
        val result = adapter.sendBatch(emptyList(), "제목", "내용")

        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `추가 데이터와 함께 발송할 수 있다`() {
        val data = mapOf("gameId" to "123", "type" to "GAME_START")

        val result = adapter.send("test-token", "제목", "내용", data)

        assertThat(result).isTrue()
    }

    @Test
    fun `추가 데이터와 함께 배치 발송할 수 있다`() {
        val tokens = listOf("token-1", "token-2")
        val data = mapOf("gameId" to "123")

        val result = adapter.sendBatch(tokens, "제목", "내용", data)

        assertThat(result).isEqualTo(2)
    }
}
