package com.nextup.core.domain.event

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("GamePostponedEvent 테스트")
class GamePostponedEventTest {
    @Test
    @DisplayName("모든 필드가 올바르게 설정된다")
    fun allFieldsAreSetCorrectly() {
        // given
        val newScheduledAt = LocalDateTime.of(2026, 4, 1, 14, 0)

        // when
        val event =
            GamePostponedEvent(
                gameId = 42L,
                homeTeamId = 10L,
                awayTeamId = 20L,
                newScheduledAt = newScheduledAt,
            )

        // then
        assertThat(event.gameId).isEqualTo(42L)
        assertThat(event.homeTeamId).isEqualTo(10L)
        assertThat(event.awayTeamId).isEqualTo(20L)
        assertThat(event.newScheduledAt).isEqualTo(newScheduledAt)
    }

    @Test
    @DisplayName("data class equals와 copy 동작 확인")
    fun equalsAndCopyWork() {
        // given
        val newScheduledAt = LocalDateTime.of(2026, 4, 1, 14, 0)
        val event1 =
            GamePostponedEvent(
                gameId = 1L,
                homeTeamId = 10L,
                awayTeamId = 20L,
                newScheduledAt = newScheduledAt,
            )
        val event2 =
            GamePostponedEvent(
                gameId = 1L,
                homeTeamId = 10L,
                awayTeamId = 20L,
                newScheduledAt = newScheduledAt,
            )

        // then
        assertThat(event1).isEqualTo(event2)
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode())

        val copied = event1.copy(gameId = 2L)
        assertThat(copied.gameId).isEqualTo(2L)
        assertThat(copied.homeTeamId).isEqualTo(10L)
    }
}
