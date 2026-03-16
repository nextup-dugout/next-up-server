package com.nextup.core.domain.stats

import com.nextup.core.domain.player.Player
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("시즌 통계 팀 ID 지원 테스트")
class SeasonStatsTeamIdTest {
    private val player: Player = mockk(relaxed = true)

    @Nested
    @DisplayName("SeasonBattingStats teamId")
    inner class BattingStatsTeamId {
        @Test
        fun `teamId 없이 생성하면 null이다`() {
            val stats = SeasonBattingStats.create(player = player, year = 2026)
            assertThat(stats.teamId).isNull()
        }

        @Test
        fun `teamId를 지정하여 생성할 수 있다`() {
            val stats = SeasonBattingStats.create(player = player, year = 2026, teamId = 10L)
            assertThat(stats.teamId).isEqualTo(10L)
        }

        @Test
        fun `같은 선수의 다른 팀별 통계를 구분할 수 있다`() {
            val statsTeamA = SeasonBattingStats.create(player = player, year = 2026, teamId = 10L)
            val statsTeamB = SeasonBattingStats.create(player = player, year = 2026, teamId = 20L)

            assertThat(statsTeamA.teamId).isNotEqualTo(statsTeamB.teamId)
        }
    }

    @Nested
    @DisplayName("SeasonPitchingStats teamId")
    inner class PitchingStatsTeamId {
        @Test
        fun `teamId 없이 생성하면 null이다`() {
            val stats = SeasonPitchingStats.create(player = player, year = 2026)
            assertThat(stats.teamId).isNull()
        }

        @Test
        fun `teamId를 지정하여 생성할 수 있다`() {
            val stats = SeasonPitchingStats.create(player = player, year = 2026, teamId = 10L)
            assertThat(stats.teamId).isEqualTo(10L)
        }
    }

    @Nested
    @DisplayName("SeasonFieldingStats teamId")
    inner class FieldingStatsTeamId {
        @Test
        fun `teamId 없이 생성하면 null이다`() {
            val stats = SeasonFieldingStats.create(player = player, year = 2026)
            assertThat(stats.teamId).isNull()
        }

        @Test
        fun `teamId를 지정하여 생성할 수 있다`() {
            val stats = SeasonFieldingStats.create(player = player, year = 2026, teamId = 10L)
            assertThat(stats.teamId).isEqualTo(10L)
        }
    }
}
