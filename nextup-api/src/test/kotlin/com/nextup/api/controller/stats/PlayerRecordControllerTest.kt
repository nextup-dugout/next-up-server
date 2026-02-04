package com.nextup.api.controller.stats

import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.core.service.stats.PlayerRecordService
import com.nextup.core.service.stats.RecentFormService
import com.nextup.core.service.stats.dto.*
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.math.BigDecimal

@DisplayName("PlayerRecordController 테스트")
class PlayerRecordControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var playerRecordService: PlayerRecordService
    private lateinit var recentFormService: RecentFormService

    @BeforeEach
    fun setUp() {
        playerRecordService = mockk()
        recentFormService = mockk()

        val controller = PlayerRecordController(playerRecordService, recentFormService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Nested
    @DisplayName("GET /api/v1/players/{playerId}/records")
    inner class GetPlayerRecord {
        @Test
        fun `선수 기록을 정상적으로 조회한다`() {
            // given
            val playerId = 1L
            val recordDto =
                PlayerRecordDto(
                    playerId = playerId,
                    playerName = "홍길동",
                    scope = RecordScope.CAREER,
                    type = RecordType.ALL,
                    year = null,
                    competitionId = null,
                    competitionName = null,
                    battingStats =
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
                        ),
                    pitchingStats = null,
                )

            every { playerRecordService.getPlayerRecord(playerId, RecordScope.CAREER, RecordType.ALL, null, null) } returns recordDto

            // when & then
            mockMvc
                .perform(get("/api/v1/players/$playerId/records"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.playerId").value(playerId))
                .andExpect(jsonPath("$.data.playerName").value("홍길동"))
                .andExpect(jsonPath("$.data.battingStats.gamesPlayed").value(100))
                .andExpect(jsonPath("$.data.battingStats.hits").value(120))
        }

        @Test
        fun `시즌 범위로 선수 기록을 조회한다`() {
            // given
            val playerId = 1L
            val year = 2026
            val recordDto =
                PlayerRecordDto(
                    playerId = playerId,
                    playerName = "홍길동",
                    scope = RecordScope.SEASON,
                    type = RecordType.BATTING,
                    year = year,
                    competitionId = null,
                    competitionName = null,
                    battingStats =
                        BattingStatsDto(
                            gamesPlayed = 30,
                            plateAppearances = 130,
                            atBats = 115,
                            hits = 35,
                            doubles = 8,
                            triples = 1,
                            homeRuns = 5,
                            runs = 20,
                            runsBattedIn = 18,
                            walks = 12,
                            strikeouts = 25,
                            stolenBases = 3,
                            battingAverage = BigDecimal("0.304"),
                            onBasePercentage = BigDecimal("0.369"),
                            sluggingPercentage = BigDecimal("0.496"),
                            ops = BigDecimal("0.865"),
                        ),
                    pitchingStats = null,
                )

            every {
                playerRecordService.getPlayerRecord(playerId, RecordScope.SEASON, RecordType.BATTING, year, null)
            } returns recordDto

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/players/$playerId/records")
                        .param("scope", "SEASON")
                        .param("type", "BATTING")
                        .param("year", year.toString()),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scope").value("SEASON"))
                .andExpect(jsonPath("$.data.year").value(year))
        }

        @Test
        fun `투수 기록을 조회한다`() {
            // given
            val playerId = 2L
            val recordDto =
                PlayerRecordDto(
                    playerId = playerId,
                    playerName = "김투수",
                    scope = RecordScope.CAREER,
                    type = RecordType.PITCHING,
                    year = null,
                    competitionId = null,
                    competitionName = null,
                    battingStats = null,
                    pitchingStats =
                        PitchingStatsDto(
                            gamesPlayed = 50,
                            gamesStarted = 40,
                            inningsPitched = "250.1",
                            wins = 15,
                            losses = 8,
                            saves = 0,
                            holds = 0,
                            earnedRuns = 70,
                            hitsAllowed = 220,
                            walksAllowed = 60,
                            strikeouts = 200,
                            homeRunsAllowed = 20,
                            era = BigDecimal("2.52"),
                            whip = BigDecimal("1.12"),
                        ),
                )

            every {
                playerRecordService.getPlayerRecord(playerId, RecordScope.CAREER, RecordType.PITCHING, null, null)
            } returns recordDto

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/players/$playerId/records")
                        .param("type", "PITCHING"),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pitchingStats.wins").value(15))
                .andExpect(jsonPath("$.data.pitchingStats.strikeouts").value(200))
                .andExpect(jsonPath("$.data.pitchingStats.inningsPitched").value("250.1"))
        }

        @Test
        fun `예외 발생 시 에러 응답을 반환한다`() {
            // given
            val playerId = 999L
            every {
                playerRecordService.getPlayerRecord(playerId, RecordScope.CAREER, RecordType.ALL, null, null)
            } throws IllegalArgumentException("선수를 찾을 수 없습니다")

            // when & then
            mockMvc
                .perform(get("/api/v1/players/$playerId/records"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/players/{playerId}/records/form")
    inner class GetRecentForm {
        @Test
        fun `최근 타격 폼을 정상적으로 조회한다`() {
            // given
            val playerId = 1L
            val games = 5
            val formDto =
                RecentFormDto(
                    playerId = playerId,
                    playerName = "홍길동",
                    type = FormType.BATTING,
                    gamesRequested = games,
                    gamesFound = games,
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

            every { recentFormService.getRecentForm(playerId, games, FormType.BATTING) } returns formDto

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/players/$playerId/records/form")
                        .param("games", games.toString())
                        .param("type", "BATTING"),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.playerId").value(playerId))
                .andExpect(jsonPath("$.data.trend").value("UP"))
                .andExpect(jsonPath("$.data.batting.totalHits").value(8))
        }

        @Test
        fun `최근 투수 폼을 조회한다`() {
            // given
            val playerId = 2L
            val games = 3
            val formDto =
                RecentFormDto(
                    playerId = playerId,
                    playerName = "김투수",
                    type = FormType.PITCHING,
                    gamesRequested = games,
                    gamesFound = games,
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

            every { recentFormService.getRecentForm(playerId, games, FormType.PITCHING) } returns formDto

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/players/$playerId/records/form")
                        .param("games", games.toString())
                        .param("type", "PITCHING"),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("PITCHING"))
                .andExpect(jsonPath("$.data.pitching.totalStrikeouts").value(20))
                .andExpect(jsonPath("$.data.pitching.inningsPitchedDisplay").value("21.0"))
        }

        @Test
        fun `기본값으로 최근 폼을 조회한다`() {
            // given
            val playerId = 1L
            val formDto =
                RecentFormDto(
                    playerId = playerId,
                    playerName = "테스트선수",
                    type = FormType.BATTING,
                    gamesRequested = 5,
                    gamesFound = 5,
                    trend = FormTrend.DOWN,
                    trendDescription = "최근 폼 하락",
                    batting =
                        RecentBattingFormDto(
                            games = emptyList(),
                            totalAtBats = 15,
                            totalHits = 3,
                            totalHomeRuns = 0,
                            totalRbis = 1,
                            totalRuns = 1,
                            recentAverage = BigDecimal("0.200"),
                            overallAverage = BigDecimal("0.280"),
                        ),
                    pitching = null,
                )

            every { recentFormService.getRecentForm(playerId, 5, FormType.BATTING) } returns formDto

            // when & then
            mockMvc
                .perform(get("/api/v1/players/$playerId/records/form"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.gamesRequested").value(5))
                .andExpect(jsonPath("$.data.trend").value("DOWN"))
        }

        @Test
        fun `예외 발생 시 에러 응답을 반환한다`() {
            // given
            val playerId = 999L
            every { recentFormService.getRecentForm(playerId, 5, FormType.BATTING) } throws
                IllegalArgumentException("선수를 찾을 수 없습니다")

            // when & then
            mockMvc
                .perform(get("/api/v1/players/$playerId/records/form"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }
    }
}
