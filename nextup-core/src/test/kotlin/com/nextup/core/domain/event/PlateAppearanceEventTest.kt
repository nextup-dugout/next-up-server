package com.nextup.core.domain.event

import com.nextup.core.domain.game.PlateAppearanceResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("PlateAppearance 도메인 이벤트 테스트")
class PlateAppearanceEventTest {
    @Nested
    @DisplayName("PlateAppearanceRecordedEvent")
    inner class PlateAppearanceRecordedEventTest {
        @Test
        fun `should create event with required fields`() {
            // given & when
            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = 1L,
                    pitcherId = 2L,
                    result = PlateAppearanceResult.SINGLE,
                )

            // then
            assertThat(event.gameId).isEqualTo(10L)
            assertThat(event.playerId).isEqualTo(1L)
            assertThat(event.result).isEqualTo(PlateAppearanceResult.SINGLE)
            assertThat(event.timestamp).isNotNull()
        }

        @Test
        fun `should use provided timestamp when given`() {
            // given
            val fixedTime = Instant.parse("2024-05-15T10:00:00Z")

            // when
            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = 1L,
                    pitcherId = 2L,
                    result = PlateAppearanceResult.HOME_RUN,
                    timestamp = fixedTime,
                )

            // then
            assertThat(event.timestamp).isEqualTo(fixedTime)
        }

        @Test
        fun `should support equality comparison as data class`() {
            // given
            val fixedTime = Instant.parse("2024-05-15T10:00:00Z")
            val event1 =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = 1L,
                    pitcherId = 2L,
                    result = PlateAppearanceResult.WALK,
                    timestamp = fixedTime,
                )
            val event2 =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = 1L,
                    pitcherId = 2L,
                    result = PlateAppearanceResult.WALK,
                    timestamp = fixedTime,
                )

            // then
            assertThat(event1).isEqualTo(event2)
            assertThat(event1.hashCode()).isEqualTo(event2.hashCode())
        }

        @Test
        fun `should support copy with modified fields`() {
            // given
            val original =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = 1L,
                    pitcherId = 2L,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when
            val copied = original.copy(result = PlateAppearanceResult.DOUBLE)

            // then
            assertThat(copied.gameId).isEqualTo(10L)
            assertThat(copied.playerId).isEqualTo(1L)
            assertThat(copied.result).isEqualTo(PlateAppearanceResult.DOUBLE)
        }

        @Test
        fun `should have distinct events for different results`() {
            // given
            val fixedTime = Instant.parse("2024-05-15T10:00:00Z")
            val singleEvent =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = 1L,
                    pitcherId = 2L,
                    result = PlateAppearanceResult.SINGLE,
                    timestamp = fixedTime,
                )
            val homeRunEvent =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = 1L,
                    pitcherId = 2L,
                    result = PlateAppearanceResult.HOME_RUN,
                    timestamp = fixedTime,
                )

            // then
            assertThat(singleEvent).isNotEqualTo(homeRunEvent)
        }
    }

    @Nested
    @DisplayName("PlateAppearanceUndoneEvent")
    inner class PlateAppearanceUndoneEventTest {
        @Test
        fun `should create event with required fields`() {
            // given & when
            val event =
                PlateAppearanceUndoneEvent(
                    gameId = 20L,
                    playerId = 5L,
                    pitcherId = 3L,
                    result = PlateAppearanceResult.STRIKEOUT,
                )

            // then
            assertThat(event.gameId).isEqualTo(20L)
            assertThat(event.playerId).isEqualTo(5L)
            assertThat(event.result).isEqualTo(PlateAppearanceResult.STRIKEOUT)
            assertThat(event.timestamp).isNotNull()
        }

        @Test
        fun `should use provided timestamp when given`() {
            // given
            val fixedTime = Instant.parse("2024-09-01T14:30:00Z")

            // when
            val event =
                PlateAppearanceUndoneEvent(
                    gameId = 20L,
                    playerId = 5L,
                    pitcherId = 3L,
                    result = PlateAppearanceResult.DOUBLE,
                    timestamp = fixedTime,
                )

            // then
            assertThat(event.timestamp).isEqualTo(fixedTime)
        }

        @Test
        fun `should support equality comparison as data class`() {
            // given
            val fixedTime = Instant.parse("2024-09-01T14:30:00Z")
            val event1 =
                PlateAppearanceUndoneEvent(
                    gameId = 20L,
                    playerId = 5L,
                    pitcherId = 3L,
                    result = PlateAppearanceResult.TRIPLE,
                    timestamp = fixedTime,
                )
            val event2 =
                PlateAppearanceUndoneEvent(
                    gameId = 20L,
                    playerId = 5L,
                    pitcherId = 3L,
                    result = PlateAppearanceResult.TRIPLE,
                    timestamp = fixedTime,
                )

            // then
            assertThat(event1).isEqualTo(event2)
            assertThat(event1.hashCode()).isEqualTo(event2.hashCode())
        }

        @Test
        fun `should support copy with modified fields`() {
            // given
            val original =
                PlateAppearanceUndoneEvent(
                    gameId = 20L,
                    playerId = 5L,
                    pitcherId = 3L,
                    result = PlateAppearanceResult.HOME_RUN,
                )

            // when
            val copied = original.copy(gameId = 99L)

            // then
            assertThat(copied.gameId).isEqualTo(99L)
            assertThat(copied.playerId).isEqualTo(5L)
            assertThat(copied.result).isEqualTo(PlateAppearanceResult.HOME_RUN)
        }

        @Test
        fun `should have toString with meaningful content`() {
            // given
            val event =
                PlateAppearanceUndoneEvent(
                    gameId = 20L,
                    playerId = 5L,
                    pitcherId = 3L,
                    result = PlateAppearanceResult.WALK,
                )

            // when
            val str = event.toString()

            // then
            assertThat(str).contains("PlateAppearanceUndoneEvent")
            assertThat(str).contains("20")
            assertThat(str).contains("5")
        }
    }
}
