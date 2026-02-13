package com.nextup.api.mapper.stats

import com.nextup.core.service.stats.dto.MatchupDto
import com.nextup.core.service.stats.dto.MatchupHistoryDto
import com.nextup.core.service.stats.dto.MatchupStatsDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("MatchupMapper 테스트")
class MatchupMapperTest {
    @Test
    fun `MatchupDto를 MatchupResponse로 변환한다`() {
        // given
        val dto =
            MatchupDto(
                pitcherId = 100L,
                pitcherName = "김투수",
                batterId = 200L,
                batterName = "이타자",
                year = 2026,
                stats =
                    MatchupStatsDto(
                        plateAppearances = 15,
                        atBats = 12,
                        hits = 4,
                        doubles = 1,
                        triples = 0,
                        homeRuns = 1,
                        walks = 2,
                        strikeouts = 3,
                        hitByPitch = 1,
                        sacrificeFlies = 0,
                        runsBattedIn = 3,
                        battingAverage = BigDecimal("0.333"),
                        onBasePercentage = BigDecimal("0.467"),
                        sluggingPercentage = BigDecimal("0.583"),
                    ),
                history =
                    listOf(
                        MatchupHistoryDto(
                            gameId = 1L,
                            gameDate = "2026-01-15",
                            result = "안타",
                            description = "2타수 1안타",
                        ),
                    ),
            )

        // when
        val response = dto.toResponse()

        // then
        assertThat(response.pitcherId).isEqualTo(100L)
        assertThat(response.pitcherName).isEqualTo("김투수")
        assertThat(response.batterId).isEqualTo(200L)
        assertThat(response.batterName).isEqualTo("이타자")
        assertThat(response.year).isEqualTo(2026)
        assertThat(response.history).hasSize(1)
    }

    @Test
    fun `MatchupStatsDto를 MatchupStatsResponse로 변환한다`() {
        // given
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

        // when
        val response = dto.toResponse()

        // then
        assertThat(response.plateAppearances).isEqualTo(20)
        assertThat(response.atBats).isEqualTo(16)
        assertThat(response.hits).isEqualTo(5)
        assertThat(response.doubles).isEqualTo(2)
        assertThat(response.triples).isEqualTo(1)
        assertThat(response.homeRuns).isEqualTo(2)
        assertThat(response.walks).isEqualTo(3)
        assertThat(response.strikeouts).isEqualTo(4)
        assertThat(response.hitByPitch).isEqualTo(1)
        assertThat(response.sacrificeFlies).isEqualTo(0)
        assertThat(response.runsBattedIn).isEqualTo(6)
        assertThat(response.battingAverage).isEqualTo(BigDecimal("0.313"))
        assertThat(response.onBasePercentage).isEqualTo(BigDecimal("0.450"))
        assertThat(response.sluggingPercentage).isEqualTo(BigDecimal("0.750"))
    }

    @Test
    fun `MatchupHistoryDto를 MatchupHistoryResponse로 변환한다`() {
        // given
        val dto =
            MatchupHistoryDto(
                gameId = 5L,
                gameDate = "2026-02-01",
                result = "홈런",
                description = "3타수 1안타 (솔로 홈런)",
            )

        // when
        val response = dto.toResponse()

        // then
        assertThat(response.gameId).isEqualTo(5L)
        assertThat(response.gameDate).isEqualTo("2026-02-01")
        assertThat(response.result).isEqualTo("홈런")
        assertThat(response.description).isEqualTo("3타수 1안타 (솔로 홈런)")
    }

    @Test
    fun `year가 null인 MatchupDto를 변환한다`() {
        // given
        val dto =
            MatchupDto(
                pitcherId = 100L,
                pitcherName = "김투수",
                batterId = 200L,
                batterName = "이타자",
                year = null,
                stats =
                    MatchupStatsDto(
                        plateAppearances = 10,
                        atBats = 8,
                        hits = 2,
                        doubles = 0,
                        triples = 0,
                        homeRuns = 0,
                        walks = 1,
                        strikeouts = 2,
                        hitByPitch = 1,
                        sacrificeFlies = 0,
                        runsBattedIn = 1,
                        battingAverage = BigDecimal("0.250"),
                        onBasePercentage = BigDecimal("0.400"),
                        sluggingPercentage = BigDecimal("0.250"),
                    ),
                history = emptyList(),
            )

        // when
        val response = dto.toResponse()

        // then
        assertThat(response.year).isNull()
        assertThat(response.history).isEmpty()
    }
}
