package com.nextup.core.service.stats

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("CompetitionStatsDto 테스트")
class CompetitionStatsDtoTest {
    @Nested
    @DisplayName("CompetitionBattingStatsDto")
    inner class CompetitionBattingStatsDtoTests {
        @Test
        fun `should create batting stats dto with all fields`() {
            // given & when
            val dto =
                CompetitionBattingStatsDto(
                    playerId = 1L,
                    competitionId = 100L,
                    gamesPlayed = 10,
                    plateAppearances = 45,
                    atBats = 40,
                    hits = 12,
                    doubles = 3,
                    triples = 1,
                    homeRuns = 2,
                    runs = 7,
                    runsBattedIn = 9,
                    walks = 4,
                    strikeouts = 8,
                    stolenBases = 3,
                    battingAverage = "0.300",
                    onBasePercentage = "0.364",
                    sluggingPercentage = "0.525",
                    ops = "0.889",
                )

            // then
            assertThat(dto.playerId).isEqualTo(1L)
            assertThat(dto.competitionId).isEqualTo(100L)
            assertThat(dto.gamesPlayed).isEqualTo(10)
            assertThat(dto.plateAppearances).isEqualTo(45)
            assertThat(dto.atBats).isEqualTo(40)
            assertThat(dto.hits).isEqualTo(12)
            assertThat(dto.doubles).isEqualTo(3)
            assertThat(dto.triples).isEqualTo(1)
            assertThat(dto.homeRuns).isEqualTo(2)
            assertThat(dto.runs).isEqualTo(7)
            assertThat(dto.runsBattedIn).isEqualTo(9)
            assertThat(dto.walks).isEqualTo(4)
            assertThat(dto.strikeouts).isEqualTo(8)
            assertThat(dto.stolenBases).isEqualTo(3)
            assertThat(dto.battingAverage).isEqualTo("0.300")
            assertThat(dto.onBasePercentage).isEqualTo("0.364")
            assertThat(dto.sluggingPercentage).isEqualTo("0.525")
            assertThat(dto.ops).isEqualTo("0.889")
        }

        @Test
        fun `should create batting stats dto with zero stats`() {
            // given & when
            val dto =
                CompetitionBattingStatsDto(
                    playerId = 1L,
                    competitionId = 100L,
                    gamesPlayed = 0,
                    plateAppearances = 0,
                    atBats = 0,
                    hits = 0,
                    doubles = 0,
                    triples = 0,
                    homeRuns = 0,
                    runs = 0,
                    runsBattedIn = 0,
                    walks = 0,
                    strikeouts = 0,
                    stolenBases = 0,
                    battingAverage = "0",
                    onBasePercentage = "0",
                    sluggingPercentage = "0",
                    ops = "0",
                )

            // then
            assertThat(dto.gamesPlayed).isZero
            assertThat(dto.hits).isZero
            assertThat(dto.battingAverage).isEqualTo("0")
            assertThat(dto.ops).isEqualTo("0")
        }

        @Test
        fun `should support equality check as data class`() {
            // given
            val dto1 =
                CompetitionBattingStatsDto(
                    playerId = 1L,
                    competitionId = 100L,
                    gamesPlayed = 5,
                    plateAppearances = 20,
                    atBats = 18,
                    hits = 6,
                    doubles = 1,
                    triples = 0,
                    homeRuns = 1,
                    runs = 3,
                    runsBattedIn = 4,
                    walks = 2,
                    strikeouts = 3,
                    stolenBases = 1,
                    battingAverage = "0.333",
                    onBasePercentage = "0.400",
                    sluggingPercentage = "0.556",
                    ops = "0.956",
                )
            val dto2 = dto1.copy()

            // then
            assertThat(dto1).isEqualTo(dto2)
            assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode())
        }

        @Test
        fun `should support copy with modified fields`() {
            // given
            val original =
                CompetitionBattingStatsDto(
                    playerId = 1L,
                    competitionId = 100L,
                    gamesPlayed = 5,
                    plateAppearances = 20,
                    atBats = 18,
                    hits = 6,
                    doubles = 1,
                    triples = 0,
                    homeRuns = 1,
                    runs = 3,
                    runsBattedIn = 4,
                    walks = 2,
                    strikeouts = 3,
                    stolenBases = 1,
                    battingAverage = "0.333",
                    onBasePercentage = "0.400",
                    sluggingPercentage = "0.556",
                    ops = "0.956",
                )

            // when
            val modified = original.copy(hits = 8, battingAverage = "0.444")

            // then
            assertThat(modified.hits).isEqualTo(8)
            assertThat(modified.battingAverage).isEqualTo("0.444")
            assertThat(modified.playerId).isEqualTo(original.playerId)
            assertThat(modified.competitionId).isEqualTo(original.competitionId)
        }
    }

    @Nested
    @DisplayName("CompetitionPitchingStatsDto")
    inner class CompetitionPitchingStatsDtoTests {
        @Test
        fun `should create pitching stats dto with all fields`() {
            // given & when
            val dto =
                CompetitionPitchingStatsDto(
                    playerId = 1L,
                    competitionId = 100L,
                    gamesPlayed = 5,
                    gamesStarted = 4,
                    inningsPitchedDisplay = "25.2",
                    wins = 3,
                    losses = 1,
                    saves = 0,
                    holds = 1,
                    earnedRuns = 10,
                    hitsAllowed = 20,
                    walksAllowed = 8,
                    strikeouts = 25,
                    homeRunsAllowed = 3,
                    earnedRunAverage = "3.51",
                    whip = "1.09",
                )

            // then
            assertThat(dto.playerId).isEqualTo(1L)
            assertThat(dto.competitionId).isEqualTo(100L)
            assertThat(dto.gamesPlayed).isEqualTo(5)
            assertThat(dto.gamesStarted).isEqualTo(4)
            assertThat(dto.inningsPitchedDisplay).isEqualTo("25.2")
            assertThat(dto.wins).isEqualTo(3)
            assertThat(dto.losses).isEqualTo(1)
            assertThat(dto.saves).isEqualTo(0)
            assertThat(dto.holds).isEqualTo(1)
            assertThat(dto.earnedRuns).isEqualTo(10)
            assertThat(dto.hitsAllowed).isEqualTo(20)
            assertThat(dto.walksAllowed).isEqualTo(8)
            assertThat(dto.strikeouts).isEqualTo(25)
            assertThat(dto.homeRunsAllowed).isEqualTo(3)
            assertThat(dto.earnedRunAverage).isEqualTo("3.51")
            assertThat(dto.whip).isEqualTo("1.09")
        }

        @Test
        fun `should allow null ERA`() {
            // given & when
            val dto =
                CompetitionPitchingStatsDto(
                    playerId = 1L,
                    competitionId = 100L,
                    gamesPlayed = 0,
                    gamesStarted = 0,
                    inningsPitchedDisplay = "0",
                    wins = 0,
                    losses = 0,
                    saves = 0,
                    holds = 0,
                    earnedRuns = 0,
                    hitsAllowed = 0,
                    walksAllowed = 0,
                    strikeouts = 0,
                    homeRunsAllowed = 0,
                    earnedRunAverage = null,
                    whip = "0",
                )

            // then
            assertThat(dto.earnedRunAverage).isNull()
            assertThat(dto.gamesPlayed).isZero
        }

        @Test
        fun `should support equality check as data class`() {
            // given
            val dto1 =
                CompetitionPitchingStatsDto(
                    playerId = 1L,
                    competitionId = 100L,
                    gamesPlayed = 3,
                    gamesStarted = 2,
                    inningsPitchedDisplay = "15.0",
                    wins = 2,
                    losses = 1,
                    saves = 0,
                    holds = 0,
                    earnedRuns = 5,
                    hitsAllowed = 12,
                    walksAllowed = 4,
                    strikeouts = 15,
                    homeRunsAllowed = 2,
                    earnedRunAverage = "3.00",
                    whip = "1.07",
                )
            val dto2 = dto1.copy()

            // then
            assertThat(dto1).isEqualTo(dto2)
            assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode())
        }

        @Test
        fun `should support copy with modified fields`() {
            // given
            val original =
                CompetitionPitchingStatsDto(
                    playerId = 1L,
                    competitionId = 100L,
                    gamesPlayed = 3,
                    gamesStarted = 2,
                    inningsPitchedDisplay = "15.0",
                    wins = 2,
                    losses = 1,
                    saves = 0,
                    holds = 0,
                    earnedRuns = 5,
                    hitsAllowed = 12,
                    walksAllowed = 4,
                    strikeouts = 15,
                    homeRunsAllowed = 2,
                    earnedRunAverage = "3.00",
                    whip = "1.07",
                )

            // when
            val modified =
                original.copy(wins = 3, earnedRunAverage = "2.50")

            // then
            assertThat(modified.wins).isEqualTo(3)
            assertThat(modified.earnedRunAverage).isEqualTo("2.50")
            assertThat(modified.playerId).isEqualTo(original.playerId)
        }
    }
}
