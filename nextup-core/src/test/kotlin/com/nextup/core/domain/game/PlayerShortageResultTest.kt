package com.nextup.core.domain.game

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PlayerShortageResult 테스트")
class PlayerShortageResultTest {
    @Nested
    @DisplayName("noShortage()")
    inner class NoShortage {
        @Test
        @DisplayName("인원이 충분하면 isShortage = false")
        fun `인원이 충분하면 부족 아님`() {
            val result =
                PlayerShortageResult.noShortage(
                    gameTeamId = 1L,
                    teamId = 10L,
                    activePlayerCount = 9,
                )

            assertThat(result.isShortage).isFalse()
            assertThat(result.activePlayerCount).isEqualTo(9)
            assertThat(result.minimumRequired).isEqualTo(9)
        }
    }

    @Nested
    @DisplayName("shortage()")
    inner class Shortage {
        @Test
        @DisplayName("인원이 부족하면 isShortage = true")
        fun `인원이 부족하면 부족 감지`() {
            val result =
                PlayerShortageResult.shortage(
                    gameTeamId = 1L,
                    teamId = 10L,
                    activePlayerCount = 8,
                )

            assertThat(result.isShortage).isTrue()
            assertThat(result.activePlayerCount).isEqualTo(8)
            assertThat(result.minimumRequired).isEqualTo(9)
            assertThat(result.teamId).isEqualTo(10L)
            assertThat(result.gameTeamId).isEqualTo(1L)
        }

        @Test
        @DisplayName("최소 인원을 커스텀으로 설정할 수 있다")
        fun `커스텀 최소 인원으로 부족 감지`() {
            val result =
                PlayerShortageResult.shortage(
                    gameTeamId = 1L,
                    teamId = 10L,
                    activePlayerCount = 7,
                    minimumRequired = 8,
                )

            assertThat(result.isShortage).isTrue()
            assertThat(result.minimumRequired).isEqualTo(8)
        }
    }

    @Nested
    @DisplayName("DEFAULT_MINIMUM_PLAYERS")
    inner class DefaultMinimumPlayers {
        @Test
        @DisplayName("기본 최소 인원은 9명이다")
        fun `기본 최소 인원은 9명`() {
            assertThat(PlayerShortageResult.DEFAULT_MINIMUM_PLAYERS).isEqualTo(9)
        }
    }
}
