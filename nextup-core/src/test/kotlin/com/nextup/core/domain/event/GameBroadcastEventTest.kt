package com.nextup.core.domain.event

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("브로드캐스트 도메인 이벤트 테스트")
class GameBroadcastEventTest {

    @Nested
    @DisplayName("GameStartedEvent")
    inner class GameStartedEventTest {

        @Test
        fun `gameId가 올바르게 설정됨`() {
            val event = GameStartedEvent(gameId = 42L)
            assertThat(event.gameId).isEqualTo(42L)
        }

        @Test
        fun `data class equals와 copy 동작 확인`() {
            val event1 = GameStartedEvent(gameId = 1L)
            val event2 = GameStartedEvent(gameId = 1L)

            assertThat(event1).isEqualTo(event2)
            assertThat(event1.hashCode()).isEqualTo(event2.hashCode())

            val copied = event1.copy(gameId = 2L)
            assertThat(copied.gameId).isEqualTo(2L)
        }
    }

    @Nested
    @DisplayName("GameEndedEvent")
    inner class GameEndedEventTest {

        @Test
        fun `gameId와 finalStatus가 올바르게 설정됨`() {
            val event = GameEndedEvent(gameId = 42L, finalStatus = "FINISHED")
            assertThat(event.gameId).isEqualTo(42L)
            assertThat(event.finalStatus).isEqualTo("FINISHED")
        }

        @Test
        fun `다양한 finalStatus 값 지원`() {
            assertThat(GameEndedEvent(1L, "CANCELLED").finalStatus).isEqualTo("CANCELLED")
            assertThat(GameEndedEvent(1L, "FORFEITED").finalStatus).isEqualTo("FORFEITED")
            assertThat(GameEndedEvent(1L, "CALLED").finalStatus).isEqualTo("CALLED")
        }

        @Test
        fun `data class equals와 copy 동작 확인`() {
            val event1 = GameEndedEvent(gameId = 1L, finalStatus = "FINISHED")
            val event2 = GameEndedEvent(gameId = 1L, finalStatus = "FINISHED")

            assertThat(event1).isEqualTo(event2)
            assertThat(event1.hashCode()).isEqualTo(event2.hashCode())

            val copied = event1.copy(finalStatus = "CANCELLED")
            assertThat(copied.gameId).isEqualTo(1L)
            assertThat(copied.finalStatus).isEqualTo("CANCELLED")
        }
    }

    @Nested
    @DisplayName("HalfInningAdvancedEvent")
    inner class HalfInningAdvancedEventTest {

        @Test
        fun `모든 필드가 올바르게 설정됨`() {
            val event = HalfInningAdvancedEvent(gameId = 42L, newInning = 5, newIsTopInning = false)
            assertThat(event.gameId).isEqualTo(42L)
            assertThat(event.newInning).isEqualTo(5)
            assertThat(event.newIsTopInning).isFalse()
        }

        @Test
        fun `초와 말 구분`() {
            val topEvent = HalfInningAdvancedEvent(1L, 3, true)
            val bottomEvent = HalfInningAdvancedEvent(1L, 3, false)

            assertThat(topEvent.newIsTopInning).isTrue()
            assertThat(bottomEvent.newIsTopInning).isFalse()
            assertThat(topEvent).isNotEqualTo(bottomEvent)
        }

        @Test
        fun `data class equals와 copy 동작 확인`() {
            val event1 = HalfInningAdvancedEvent(gameId = 1L, newInning = 4, newIsTopInning = true)
            val event2 = HalfInningAdvancedEvent(gameId = 1L, newInning = 4, newIsTopInning = true)

            assertThat(event1).isEqualTo(event2)
            assertThat(event1.hashCode()).isEqualTo(event2.hashCode())

            val copied = event1.copy(newInning = 5, newIsTopInning = false)
            assertThat(copied.newInning).isEqualTo(5)
            assertThat(copied.newIsTopInning).isFalse()
        }
    }

    @Nested
    @DisplayName("PlayerSubstitutedEvent")
    inner class PlayerSubstitutedEventTest {

        @Test
        fun `gameId와 gameEventId가 올바르게 설정됨`() {
            val event = PlayerSubstitutedEvent(gameId = 42L, gameEventId = 99L)
            assertThat(event.gameId).isEqualTo(42L)
            assertThat(event.gameEventId).isEqualTo(99L)
        }

        @Test
        fun `data class equals와 copy 동작 확인`() {
            val event1 = PlayerSubstitutedEvent(gameId = 1L, gameEventId = 10L)
            val event2 = PlayerSubstitutedEvent(gameId = 1L, gameEventId = 10L)

            assertThat(event1).isEqualTo(event2)
            assertThat(event1.hashCode()).isEqualTo(event2.hashCode())

            val copied = event1.copy(gameEventId = 20L)
            assertThat(copied.gameId).isEqualTo(1L)
            assertThat(copied.gameEventId).isEqualTo(20L)
        }
    }
}
