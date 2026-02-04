package com.nextup.api.mapper.stats

import com.nextup.core.service.stats.dto.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("RecentFormMapper 테스트")
class RecentFormMapperTest {
    @Test
    fun `RecentFormDto를 RecentFormResponse로 변환한다 - 타격`() {
        // given
        val dto =
            RecentFormDto(
                playerId = 1L,
                playerName = "홍길동",
                type = FormType.BATTING,
                gamesRequested = 5,
                gamesFound = 5,
                trend = FormTrend.UP,
                trendDescription = "최근 5경기 타율 상승세",
                batting =
                    RecentBattingFormDto(
                        games =
                            listOf(
                                GameBattingDto(
                                    gameId = 1L,
                                    gameDate = "2026-01-20",
                                    opponentName = "상대팀A",
                                    atBats = 4,
                                    hits = 2,
                                    homeRuns = 1,
                                    rbis = 2,
                                    runs = 1,
                                    walks = 0,
                                    strikeouts = 1,
                                ),
                            ),
                        totalAtBats = 20,
                        totalHits = 8,
                        totalHomeRuns = 2,
                        totalRbis = 5,
                        totalRuns = 4,
                        recentAverage = BigDecimal("0.400"),
                        overallAverage = BigDecimal("0.300"),
                    ),
                pitching = null,
            )

        // when
        val response = dto.toResponse()

        // then
        assertThat(response.playerId).isEqualTo(1L)
        assertThat(response.playerName).isEqualTo("홍길동")
        assertThat(response.type.name).isEqualTo("BATTING")
        assertThat(response.gamesRequested).isEqualTo(5)
        assertThat(response.gamesFound).isEqualTo(5)
        assertThat(response.trend.name).isEqualTo("UP")
        assertThat(response.trendDescription).isEqualTo("최근 5경기 타율 상승세")
        assertThat(response.batting).isNotNull
        assertThat(response.pitching).isNull()
    }

    @Test
    fun `RecentFormDto를 RecentFormResponse로 변환한다 - 투수`() {
        // given
        val dto =
            RecentFormDto(
                playerId = 2L,
                playerName = "김투수",
                type = FormType.PITCHING,
                gamesRequested = 3,
                gamesFound = 3,
                trend = FormTrend.STABLE,
                trendDescription = "최근 3경기 안정적인 투구",
                batting = null,
                pitching =
                    RecentPitchingFormDto(
                        games =
                            listOf(
                                GamePitchingDto(
                                    gameId = 10L,
                                    gameDate = "2026-01-25",
                                    opponentName = "상대팀B",
                                    inningsPitched = "7.0",
                                    earnedRuns = 2,
                                    strikeouts = 8,
                                    walksAllowed = 2,
                                    hitsAllowed = 5,
                                    decision = "W",
                                ),
                            ),
                        totalInningsPitchedOuts = 63,
                        inningsPitchedDisplay = "21.0",
                        totalEarnedRuns = 5,
                        totalStrikeouts = 20,
                        recentEra = BigDecimal("2.14"),
                        overallEra = BigDecimal("3.00"),
                    ),
            )

        // when
        val response = dto.toResponse()

        // then
        assertThat(response.playerId).isEqualTo(2L)
        assertThat(response.type.name).isEqualTo("PITCHING")
        assertThat(response.trend.name).isEqualTo("STABLE")
        assertThat(response.batting).isNull()
        assertThat(response.pitching).isNotNull
    }

    @Test
    fun `FormType을 FormTypeResponse로 변환한다`() {
        assertThat(FormType.BATTING.toResponse().name).isEqualTo("BATTING")
        assertThat(FormType.PITCHING.toResponse().name).isEqualTo("PITCHING")
    }

    @Test
    fun `FormTrend를 FormTrendResponse로 변환한다`() {
        assertThat(FormTrend.UP.toResponse().name).isEqualTo("UP")
        assertThat(FormTrend.DOWN.toResponse().name).isEqualTo("DOWN")
        assertThat(FormTrend.STABLE.toResponse().name).isEqualTo("STABLE")
    }

    @Test
    fun `RecentBattingFormDto를 RecentBattingFormResponse로 변환한다`() {
        // given
        val dto =
            RecentBattingFormDto(
                games =
                    listOf(
                        GameBattingDto(
                            gameId = 1L,
                            gameDate = "2026-01-20",
                            opponentName = "상대팀A",
                            atBats = 4,
                            hits = 2,
                            homeRuns = 1,
                            rbis = 2,
                            runs = 1,
                            walks = 0,
                            strikeouts = 1,
                        ),
                        GameBattingDto(
                            gameId = 2L,
                            gameDate = "2026-01-21",
                            opponentName = "상대팀B",
                            atBats = 3,
                            hits = 1,
                            homeRuns = 0,
                            rbis = 0,
                            runs = 0,
                            walks = 1,
                            strikeouts = 0,
                        ),
                    ),
                totalAtBats = 7,
                totalHits = 3,
                totalHomeRuns = 1,
                totalRbis = 2,
                totalRuns = 1,
                recentAverage = BigDecimal("0.429"),
                overallAverage = BigDecimal("0.300"),
            )

        // when
        val response = dto.toResponse()

        // then
        assertThat(response.games).hasSize(2)
        assertThat(response.totalAtBats).isEqualTo(7)
        assertThat(response.totalHits).isEqualTo(3)
        assertThat(response.totalHomeRuns).isEqualTo(1)
        assertThat(response.totalRbis).isEqualTo(2)
        assertThat(response.totalRuns).isEqualTo(1)
        assertThat(response.recentAverage).isEqualTo(BigDecimal("0.429"))
        assertThat(response.overallAverage).isEqualTo(BigDecimal("0.300"))
    }

    @Test
    fun `GameBattingDto를 GameBattingResponse로 변환한다`() {
        // given
        val dto =
            GameBattingDto(
                gameId = 5L,
                gameDate = "2026-02-01",
                opponentName = "강팀",
                atBats = 5,
                hits = 3,
                homeRuns = 2,
                rbis = 4,
                runs = 2,
                walks = 0,
                strikeouts = 1,
            )

        // when
        val response = dto.toResponse()

        // then
        assertThat(response.gameId).isEqualTo(5L)
        assertThat(response.gameDate).isEqualTo("2026-02-01")
        assertThat(response.opponentName).isEqualTo("강팀")
        assertThat(response.atBats).isEqualTo(5)
        assertThat(response.hits).isEqualTo(3)
        assertThat(response.homeRuns).isEqualTo(2)
        assertThat(response.rbis).isEqualTo(4)
        assertThat(response.runs).isEqualTo(2)
        assertThat(response.walks).isEqualTo(0)
        assertThat(response.strikeouts).isEqualTo(1)
    }

    @Test
    fun `RecentPitchingFormDto를 RecentPitchingFormResponse로 변환한다`() {
        // given
        val dto =
            RecentPitchingFormDto(
                games =
                    listOf(
                        GamePitchingDto(
                            gameId = 10L,
                            gameDate = "2026-01-25",
                            opponentName = "상대팀",
                            inningsPitched = "6.0",
                            earnedRuns = 2,
                            strikeouts = 7,
                            walksAllowed = 1,
                            hitsAllowed = 4,
                            decision = "W",
                        ),
                    ),
                totalInningsPitchedOuts = 18,
                inningsPitchedDisplay = "6.0",
                totalEarnedRuns = 2,
                totalStrikeouts = 7,
                recentEra = BigDecimal("3.00"),
                overallEra = BigDecimal("3.50"),
            )

        // when
        val response = dto.toResponse()

        // then
        assertThat(response.games).hasSize(1)
        assertThat(response.totalInningsPitchedOuts).isEqualTo(18)
        assertThat(response.inningsPitchedDisplay).isEqualTo("6.0")
        assertThat(response.totalEarnedRuns).isEqualTo(2)
        assertThat(response.totalStrikeouts).isEqualTo(7)
        assertThat(response.recentEra).isEqualTo(BigDecimal("3.00"))
        assertThat(response.overallEra).isEqualTo(BigDecimal("3.50"))
    }

    @Test
    fun `GamePitchingDto를 GamePitchingResponse로 변환한다`() {
        // given
        val dto =
            GamePitchingDto(
                gameId = 20L,
                gameDate = "2026-02-05",
                opponentName = "강팀",
                inningsPitched = "7.2",
                earnedRuns = 1,
                strikeouts = 10,
                walksAllowed = 2,
                hitsAllowed = 5,
                decision = "W",
            )

        // when
        val response = dto.toResponse()

        // then
        assertThat(response.gameId).isEqualTo(20L)
        assertThat(response.gameDate).isEqualTo("2026-02-05")
        assertThat(response.opponentName).isEqualTo("강팀")
        assertThat(response.inningsPitched).isEqualTo("7.2")
        assertThat(response.earnedRuns).isEqualTo(1)
        assertThat(response.strikeouts).isEqualTo(10)
        assertThat(response.walksAllowed).isEqualTo(2)
        assertThat(response.hitsAllowed).isEqualTo(5)
        assertThat(response.decision).isEqualTo("W")
    }

    @Test
    fun `하락세 폼을 변환한다`() {
        // given
        val dto =
            RecentFormDto(
                playerId = 3L,
                playerName = "슬럼프",
                type = FormType.BATTING,
                gamesRequested = 5,
                gamesFound = 5,
                trend = FormTrend.DOWN,
                trendDescription = "최근 5경기 타율 하락세",
                batting =
                    RecentBattingFormDto(
                        games = emptyList(),
                        totalAtBats = 20,
                        totalHits = 3,
                        totalHomeRuns = 0,
                        totalRbis = 1,
                        totalRuns = 0,
                        recentAverage = BigDecimal("0.150"),
                        overallAverage = BigDecimal("0.280"),
                    ),
                pitching = null,
            )

        // when
        val response = dto.toResponse()

        // then
        assertThat(response.trend.name).isEqualTo("DOWN")
        assertThat(response.batting?.recentAverage).isEqualTo(BigDecimal("0.150"))
    }
}
