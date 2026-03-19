package com.nextup.core.service.stats.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("Stats DTO 테스트")
class StatsDtoTest {
    @Nested
    @DisplayName("PlayerRecordDto")
    inner class PlayerRecordDtoTest {
        @Test
        fun `PlayerRecordDto 생성 및 속성 접근`() {
            val dto =
                PlayerRecordDto(
                    playerId = 1L,
                    playerName = "홍길동",
                    scope = RecordScope.CAREER,
                    type = RecordType.ALL,
                    year = 2026,
                    competitionId = 10L,
                    competitionName = "봄리그",
                    battingStats = null,
                    pitchingStats = null,
                )

            assertThat(dto.playerId).isEqualTo(1L)
            assertThat(dto.playerName).isEqualTo("홍길동")
            assertThat(dto.scope).isEqualTo(RecordScope.CAREER)
            assertThat(dto.type).isEqualTo(RecordType.ALL)
            assertThat(dto.year).isEqualTo(2026)
            assertThat(dto.competitionId).isEqualTo(10L)
            assertThat(dto.competitionName).isEqualTo("봄리그")
        }

        @Test
        fun `PlayerRecordDto copy 테스트`() {
            val original =
                PlayerRecordDto(
                    playerId = 1L,
                    playerName = "홍길동",
                    scope = RecordScope.CAREER,
                    type = RecordType.ALL,
                    year = null,
                    competitionId = null,
                    competitionName = null,
                    battingStats = null,
                    pitchingStats = null,
                )

            val copied = original.copy(year = 2026)

            assertThat(copied.year).isEqualTo(2026)
            assertThat(copied.playerId).isEqualTo(original.playerId)
        }
    }

    @Nested
    @DisplayName("BattingStatsDto")
    inner class BattingStatsDtoTest {
        @Test
        fun `BattingStatsDto 생성 및 속성 접근`() {
            val dto =
                BattingStatsDto(
                    gamesPlayed = 100,
                    plateAppearances = 450,
                    atBats = 400,
                    hits = 120,
                    doubles = 25,
                    triples = 3,
                    homeRuns = 15,
                    runs = 60,
                    runsBattedIn = 55,
                    walks = 40,
                    strikeouts = 80,
                    stolenBases = 10,
                    battingAverage = BigDecimal("0.300"),
                    onBasePercentage = BigDecimal("0.378"),
                    sluggingPercentage = BigDecimal("0.485"),
                    ops = BigDecimal("0.863"),
                )

            assertThat(dto.gamesPlayed).isEqualTo(100)
            assertThat(dto.hits).isEqualTo(120)
            assertThat(dto.battingAverage).isEqualTo(BigDecimal("0.300"))
            assertThat(dto.ops).isEqualTo(BigDecimal("0.863"))
        }
    }

    @Nested
    @DisplayName("PitchingStatsDto")
    inner class PitchingStatsDtoTest {
        @Test
        fun `PitchingStatsDto 생성 및 속성 접근`() {
            val dto =
                PitchingStatsDto(
                    gamesPlayed = 30,
                    gamesStarted = 25,
                    inningsPitched = "160.2",
                    wins = 10,
                    losses = 5,
                    saves = 0,
                    holds = 0,
                    earnedRuns = 45,
                    hitsAllowed = 140,
                    walksAllowed = 40,
                    strikeouts = 130,
                    homeRunsAllowed = 12,
                    era = BigDecimal("2.52"),
                    whip = BigDecimal("1.12"),
                )

            assertThat(dto.gamesPlayed).isEqualTo(30)
            assertThat(dto.wins).isEqualTo(10)
            assertThat(dto.era).isEqualTo(BigDecimal("2.52"))
            assertThat(dto.inningsPitched).isEqualTo("160.2")
        }
    }

    @Nested
    @DisplayName("RecordScope & RecordType")
    inner class EnumTest {
        @Test
        fun `RecordScope enum 값 확인`() {
            assertThat(RecordScope.values()).containsExactly(
                RecordScope.SEASON,
                RecordScope.CAREER,
                RecordScope.COMPETITION,
            )
        }

        @Test
        fun `RecordType enum 값 확인`() {
            assertThat(RecordType.values()).containsExactly(
                RecordType.BATTING,
                RecordType.PITCHING,
                RecordType.ALL,
            )
        }
    }

    @Nested
    @DisplayName("RecentFormDto")
    inner class RecentFormDtoTest {
        @Test
        fun `RecentFormDto 생성 및 속성 접근`() {
            val dto =
                RecentFormDto(
                    playerId = 1L,
                    playerName = "홍길동",
                    type = FormType.BATTING,
                    gamesRequested = 5,
                    gamesFound = 5,
                    trend = FormTrend.UP,
                    trendDescription = "상승세",
                    batting = null,
                    pitching = null,
                )

            assertThat(dto.playerId).isEqualTo(1L)
            assertThat(dto.type).isEqualTo(FormType.BATTING)
            assertThat(dto.trend).isEqualTo(FormTrend.UP)
        }

        @Test
        fun `FormType enum 값 확인`() {
            assertThat(FormType.values()).containsExactly(
                FormType.BATTING,
                FormType.PITCHING,
            )
        }

        @Test
        fun `FormTrend enum 값 확인`() {
            assertThat(FormTrend.values()).containsExactly(
                FormTrend.UP,
                FormTrend.DOWN,
                FormTrend.STABLE,
            )
        }
    }

    @Nested
    @DisplayName("RecentBattingFormDto")
    inner class RecentBattingFormDtoTest {
        @Test
        fun `RecentBattingFormDto 생성 및 속성 접근`() {
            val dto =
                RecentBattingFormDto(
                    games = emptyList(),
                    totalAtBats = 20,
                    totalHits = 8,
                    totalHomeRuns = 2,
                    totalRbis = 5,
                    totalRuns = 4,
                    recentAverage = BigDecimal("0.400"),
                    overallAverage = BigDecimal("0.300"),
                )

            assertThat(dto.totalAtBats).isEqualTo(20)
            assertThat(dto.totalHits).isEqualTo(8)
            assertThat(dto.recentAverage).isEqualTo(BigDecimal("0.400"))
        }
    }

    @Nested
    @DisplayName("GameBattingDto")
    inner class GameBattingDtoTest {
        @Test
        fun `GameBattingDto 생성 및 속성 접근`() {
            val dto =
                GameBattingDto(
                    gameId = 1L,
                    gameDate = "2026-01-20",
                    opponentName = "상대팀",
                    atBats = 4,
                    hits = 2,
                    homeRuns = 1,
                    rbis = 2,
                    runs = 1,
                    walks = 0,
                    strikeouts = 1,
                )

            assertThat(dto.gameId).isEqualTo(1L)
            assertThat(dto.opponentName).isEqualTo("상대팀")
            assertThat(dto.hits).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("RecentPitchingFormDto")
    inner class RecentPitchingFormDtoTest {
        @Test
        fun `RecentPitchingFormDto 생성 및 속성 접근`() {
            val dto =
                RecentPitchingFormDto(
                    games = emptyList(),
                    totalInningsPitchedOuts = 63,
                    inningsPitchedDisplay = "21.0",
                    totalEarnedRuns = 5,
                    totalStrikeouts = 20,
                    recentEra = BigDecimal("2.14"),
                    overallEra = BigDecimal("3.00"),
                )

            assertThat(dto.totalInningsPitchedOuts).isEqualTo(63)
            assertThat(dto.inningsPitchedDisplay).isEqualTo("21.0")
            assertThat(dto.recentEra).isEqualTo(BigDecimal("2.14"))
        }
    }

    @Nested
    @DisplayName("GamePitchingDto")
    inner class GamePitchingDtoTest {
        @Test
        fun `GamePitchingDto 생성 및 속성 접근`() {
            val dto =
                GamePitchingDto(
                    gameId = 10L,
                    gameDate = "2026-01-25",
                    opponentName = "상대팀",
                    inningsPitched = "7.0",
                    earnedRuns = 2,
                    strikeouts = 8,
                    walksAllowed = 2,
                    hitsAllowed = 5,
                    decision = "W",
                )

            assertThat(dto.gameId).isEqualTo(10L)
            assertThat(dto.inningsPitched).isEqualTo("7.0")
            assertThat(dto.decision).isEqualTo("W")
        }

        @Test
        fun `GamePitchingDto with null decision`() {
            val dto =
                GamePitchingDto(
                    gameId = 11L,
                    gameDate = "2026-01-26",
                    opponentName = "상대팀",
                    inningsPitched = "5.0",
                    earnedRuns = 3,
                    strikeouts = 5,
                    walksAllowed = 2,
                    hitsAllowed = 6,
                    decision = null,
                )

            assertThat(dto.decision).isNull()
        }
    }

    @Nested
    @DisplayName("MatchupDto")
    inner class MatchupDtoTest {
        @Test
        fun `MatchupDto 생성 및 속성 접근`() {
            val dto =
                MatchupDto(
                    pitcherId = 100L,
                    pitcherName = "김투수",
                    batterId = 200L,
                    batterName = "이타자",
                    year = 2026,
                    stats =
                        MatchupStatsDto(
                            plateAppearances = 10,
                            atBats = 8,
                            hits = 3,
                            doubles = 1,
                            triples = 0,
                            homeRuns = 1,
                            walks = 1,
                            strikeouts = 2,
                            hitByPitch = 1,
                            sacrificeFlies = 0,
                            runsBattedIn = 2,
                            battingAverage = BigDecimal("0.375"),
                            onBasePercentage = BigDecimal("0.500"),
                            sluggingPercentage = BigDecimal("0.750"),
                        ),
                    history = emptyList(),
                )

            assertThat(dto.pitcherId).isEqualTo(100L)
            assertThat(dto.batterName).isEqualTo("이타자")
            assertThat(dto.stats.hits).isEqualTo(3)
        }
    }

    @Nested
    @DisplayName("MatchupStatsDto")
    inner class MatchupStatsDtoTest {
        @Test
        fun `MatchupStatsDto 생성 및 속성 접근`() {
            val dto =
                MatchupStatsDto(
                    plateAppearances = 20,
                    atBats = 16,
                    hits = 5,
                    doubles = 2,
                    triples = 1,
                    homeRuns = 2,
                    walks = 3,
                    strikeouts = 4,
                    hitByPitch = 1,
                    sacrificeFlies = 0,
                    runsBattedIn = 6,
                    battingAverage = BigDecimal("0.313"),
                    onBasePercentage = BigDecimal("0.450"),
                    sluggingPercentage = BigDecimal("0.750"),
                )

            assertThat(dto.plateAppearances).isEqualTo(20)
            assertThat(dto.homeRuns).isEqualTo(2)
            assertThat(dto.battingAverage).isEqualTo(BigDecimal("0.313"))
        }
    }

    @Nested
    @DisplayName("MatchupHistoryDto")
    inner class MatchupHistoryDtoTest {
        @Test
        fun `MatchupHistoryDto 생성 및 속성 접근`() {
            val dto =
                MatchupHistoryDto(
                    gameId = 1L,
                    gameDate = "2026-01-15",
                    result = "안타",
                    description = "2타수 1안타",
                )

            assertThat(dto.gameId).isEqualTo(1L)
            assertThat(dto.result).isEqualTo("안타")
            assertThat(dto.description).isEqualTo("2타수 1안타")
        }
    }

    @Nested
    @DisplayName("TeamStatsDto")
    inner class TeamStatsDtoTest {
        @Test
        fun `TeamStatsDto 생성 및 속성 접근`() {
            val dto =
                TeamStatsDto(
                    teamId = 1L,
                    teamName = "테스트팀",
                    logoUrl = null,
                    year = 2026,
                    competitionId = 10L,
                    competitionName = "봄리그",
                    record =
                        TeamRecordDto(
                            gamesPlayed = 20,
                            wins = 12,
                            losses = 6,
                            draws = 2,
                            winningPercentage = BigDecimal("0.667"),
                        ),
                    batting =
                        TeamBattingStatsDto(
                            totalAtBats = 700,
                            totalHits = 210,
                            totalHomeRuns = 25,
                            totalRunsBattedIn = 95,
                            totalRuns = 100,
                            teamBattingAverage = BigDecimal("0.300"),
                            teamOnBasePercentage = BigDecimal("0.380"),
                            teamSluggingPercentage = BigDecimal("0.450"),
                        ),
                    pitching =
                        TeamPitchingStatsDto(
                            totalInningsPitchedOuts = 540,
                            inningsPitchedDisplay = "180.0",
                            totalEarnedRuns = 60,
                            totalStrikeouts = 150,
                            totalWalksAllowed = 55,
                            teamEra = BigDecimal("3.00"),
                            teamWhip = BigDecimal("1.20"),
                        ),
                )

            assertThat(dto.teamId).isEqualTo(1L)
            assertThat(dto.teamName).isEqualTo("테스트팀")
            assertThat(dto.record.wins).isEqualTo(12)
            assertThat(dto.batting.totalHits).isEqualTo(210)
            assertThat(dto.pitching.teamEra).isEqualTo(BigDecimal("3.00"))
        }
    }

    @Nested
    @DisplayName("TeamRecordDto")
    inner class TeamRecordDtoTest {
        @Test
        fun `TeamRecordDto 생성 및 속성 접근`() {
            val dto =
                TeamRecordDto(
                    gamesPlayed = 30,
                    wins = 18,
                    losses = 10,
                    draws = 2,
                    winningPercentage = BigDecimal("0.643"),
                )

            assertThat(dto.gamesPlayed).isEqualTo(30)
            assertThat(dto.wins).isEqualTo(18)
            assertThat(dto.winningPercentage).isEqualTo(BigDecimal("0.643"))
        }
    }

    @Nested
    @DisplayName("TeamBattingStatsDto")
    inner class TeamBattingStatsDtoTest {
        @Test
        fun `TeamBattingStatsDto 생성 및 속성 접근`() {
            val dto =
                TeamBattingStatsDto(
                    totalAtBats = 500,
                    totalHits = 150,
                    totalHomeRuns = 20,
                    totalRunsBattedIn = 70,
                    totalRuns = 80,
                    teamBattingAverage = BigDecimal("0.300"),
                    teamOnBasePercentage = BigDecimal("0.370"),
                    teamSluggingPercentage = BigDecimal("0.460"),
                )

            assertThat(dto.totalAtBats).isEqualTo(500)
            assertThat(dto.teamBattingAverage).isEqualTo(BigDecimal("0.300"))
        }
    }

    @Nested
    @DisplayName("TeamPitchingStatsDto")
    inner class TeamPitchingStatsDtoTest {
        @Test
        fun `TeamPitchingStatsDto 생성 및 속성 접근`() {
            val dto =
                TeamPitchingStatsDto(
                    totalInningsPitchedOuts = 450,
                    inningsPitchedDisplay = "150.0",
                    totalEarnedRuns = 50,
                    totalStrikeouts = 120,
                    totalWalksAllowed = 45,
                    teamEra = BigDecimal("3.00"),
                    teamWhip = BigDecimal("1.15"),
                )

            assertThat(dto.totalInningsPitchedOuts).isEqualTo(450)
            assertThat(dto.teamEra).isEqualTo(BigDecimal("3.00"))
            assertThat(dto.teamWhip).isEqualTo(BigDecimal("1.15"))
        }
    }
}
