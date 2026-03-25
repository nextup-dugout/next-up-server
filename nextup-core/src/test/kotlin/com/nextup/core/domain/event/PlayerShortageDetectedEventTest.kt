package com.nextup.core.domain.event

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PlayerShortageDetectedEvent 테스트")
class PlayerShortageDetectedEventTest {
    @Test
    fun `이벤트 필드가 올바르게 설정됨`() {
        // given & when
        val event =
            PlayerShortageDetectedEvent(
                gameId = 1L,
                gameTeamId = 10L,
                teamId = 100L,
                activePlayerCount = 8,
                minimumRequired = 9,
            )

        // then
        assertThat(event.gameId).isEqualTo(1L)
        assertThat(event.gameTeamId).isEqualTo(10L)
        assertThat(event.teamId).isEqualTo(100L)
        assertThat(event.activePlayerCount).isEqualTo(8)
        assertThat(event.minimumRequired).isEqualTo(9)
    }

    @Test
    fun `data class equals와 copy 동작 확인`() {
        // given
        val event1 =
            PlayerShortageDetectedEvent(
                gameId = 1L,
                gameTeamId = 10L,
                teamId = 100L,
                activePlayerCount = 8,
                minimumRequired = 9,
            )
        val event2 =
            PlayerShortageDetectedEvent(
                gameId = 1L,
                gameTeamId = 10L,
                teamId = 100L,
                activePlayerCount = 8,
                minimumRequired = 9,
            )

        // then
        assertThat(event1).isEqualTo(event2)
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode())

        val copied = event1.copy(activePlayerCount = 7)
        assertThat(copied.activePlayerCount).isEqualTo(7)
        assertThat(copied.gameId).isEqualTo(1L)
    }

    @Test
    fun `toString 포함 확인`() {
        val event =
            PlayerShortageDetectedEvent(
                gameId = 42L,
                gameTeamId = 5L,
                teamId = 200L,
                activePlayerCount = 8,
                minimumRequired = 9,
            )

        val str = event.toString()
        assertThat(str).contains("42")
        assertThat(str).contains("8")
        assertThat(str).contains("9")
    }
}
