package com.nextup.core.domain.event

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("GameCancelledEvent 테스트")
class GameCancelledEventTest {
    @Test
    fun `gameId가 올바르게 설정됨`() {
        // given & when
        val event = GameCancelledEvent(gameId = 42L)

        // then
        assertThat(event.gameId).isEqualTo(42L)
    }

    @Test
    fun `timestamp가 자동 생성됨`() {
        // given
        val before = Instant.now()

        // when
        val event = GameCancelledEvent(gameId = 1L)

        // then
        val after = Instant.now()
        assertThat(event.timestamp).isBetween(before, after)
    }

    @Test
    fun `명시적 timestamp가 설정됨`() {
        // given
        val fixedTimestamp = Instant.parse("2024-06-15T10:00:00Z")

        // when
        val event = GameCancelledEvent(gameId = 1L, timestamp = fixedTimestamp)

        // then
        assertThat(event.timestamp).isEqualTo(fixedTimestamp)
    }

    @Test
    fun `data class equals와 copy 동작 확인`() {
        // given
        val timestamp = Instant.now()
        val event1 = GameCancelledEvent(gameId = 1L, timestamp = timestamp)
        val event2 = GameCancelledEvent(gameId = 1L, timestamp = timestamp)

        // then
        assertThat(event1).isEqualTo(event2)
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode())

        val copied = event1.copy(gameId = 2L)
        assertThat(copied.gameId).isEqualTo(2L)
        assertThat(copied.timestamp).isEqualTo(timestamp)
    }
}
