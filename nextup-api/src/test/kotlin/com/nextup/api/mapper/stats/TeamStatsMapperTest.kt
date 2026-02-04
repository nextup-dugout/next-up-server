package com.nextup.api.mapper.stats

import com.nextup.core.service.stats.dto.TeamBattingStatsDto
import com.nextup.core.service.stats.dto.TeamPitchingStatsDto
import com.nextup.core.service.stats.dto.TeamRecordDto
import com.nextup.core.service.stats.dto.TeamStatsDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("TeamStatsMapper 테스트")
class TeamStatsMapperTest {
    @Test
    fun `TeamStatsDto를 TeamStatsResponse로 변환한다`() {
        // given
        val dto =
            TeamStatsDto(
                teamId = 1L,
                teamName = "테스트팀",
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

        // when
        val response = dto.toResponse()

        // then
        assertThat(response.teamId).isEqualTo(1L)
        assertThat(response.teamName).isEqualTo("테스트팀")
        assertThat(response.year).isEqualTo(2026)
        assertThat(response.competitionId).isEqualTo(10L)
        assertThat(response.competitionName).isEqualTo("봄리그")
    }

    @Test
    fun `TeamRecordDto를 TeamRecordResponse로 변환한다`() {
        // given
        val dto =
            TeamRecordDto(
                gamesPlayed = 30,
                wins = 18,
                losses = 10,
                draws = 2,
                winningPercentage = BigDecimal("0.643"),
            )

        // when
        val response = dto.toResponse()

        // then
        assertThat(response.gamesPlayed).isEqualTo(30)
        assertThat(response.wins).isEqualTo(18)
        assertThat(response.losses).isEqualTo(10)
        assertThat(response.draws).isEqualTo(2)
        assertThat(response.winningPercentage).isEqualTo(BigDecimal("0.643"))
    }

    @Test
    fun `TeamBattingStatsDto를 TeamBattingStatsResponse로 변환한다`() {
        // given
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

        // when
        val response = dto.toResponse()

        // then
        assertThat(response.totalAtBats).isEqualTo(500)
        assertThat(response.totalHits).isEqualTo(150)
        assertThat(response.totalHomeRuns).isEqualTo(20)
        assertThat(response.totalRunsBattedIn).isEqualTo(70)
        assertThat(response.totalRuns).isEqualTo(80)
        assertThat(response.teamBattingAverage).isEqualTo(BigDecimal("0.300"))
    }

    @Test
    fun `TeamPitchingStatsDto를 TeamPitchingStatsResponse로 변환한다`() {
        // given
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

        // when
        val response = dto.toResponse()

        // then
        assertThat(response.totalInningsPitchedOuts).isEqualTo(450)
        assertThat(response.inningsPitchedDisplay).isEqualTo("150.0")
        assertThat(response.totalEarnedRuns).isEqualTo(50)
        assertThat(response.totalStrikeouts).isEqualTo(120)
        assertThat(response.totalWalksAllowed).isEqualTo(45)
        assertThat(response.teamEra).isEqualTo(BigDecimal("3.00"))
        assertThat(response.teamWhip).isEqualTo(BigDecimal("1.15"))
    }
}
