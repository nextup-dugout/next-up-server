package com.nextup.api.controller.game

import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.core.service.game.BoxScoreService
import com.nextup.core.service.game.dto.*
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

@DisplayName("BoxScoreController 테스트")
class BoxScoreControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var boxScoreService: BoxScoreService

    @BeforeEach
    fun setUp() {
        boxScoreService = mockk()

        val controller = BoxScoreController(boxScoreService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Nested
    @DisplayName("GET /api/games/{gameId}/boxscore")
    inner class GetBoxScore {
        @Test
        fun `경기의 박스스코어를 정상적으로 조회한다`() {
            // given
            val gameId = 1L
            val boxScoreDto =
                BoxScoreDto(
                    gameId = gameId,
                    homeTeam =
                        TeamBoxScoreDto(
                            teamId = 10L,
                            teamName = "홈팀",
                            logoUrl = null,
                            inningScores = listOf(1, 0, 2, 0, 0, 0, 3, 0, 0),
                            runs = 6,
                            hits = 10,
                            errors = 1,
                            batters =
                                listOf(
                                    BatterLineDto(
                                        playerId = 100L,
                                        name = "홈타자1",
                                        position = "1B",
                                        battingOrder = 1,
                                        plateAppearances = 4,
                                        atBats = 3,
                                        runs = 2,
                                        hits = 2,
                                        rbis = 1,
                                        walks = 1,
                                        strikeouts = 0,
                                        avg = ".333",
                                    ),
                                ),
                            pitchers =
                                listOf(
                                    PitcherLineDto(
                                        playerId = 101L,
                                        name = "홈투수1",
                                        inningsPitched = "7.0",
                                        hits = 5,
                                        runs = 2,
                                        earnedRuns = 2,
                                        walks = 2,
                                        strikeouts = 8,
                                        homeRuns = 0,
                                        decision = "W",
                                        era = "2.57",
                                    ),
                                ),
                        ),
                    awayTeam =
                        TeamBoxScoreDto(
                            teamId = 20L,
                            teamName = "원정팀",
                            logoUrl = null,
                            inningScores = listOf(0, 1, 0, 1, 0, 0, 0, 0, 0),
                            runs = 2,
                            hits = 5,
                            errors = 2,
                            batters =
                                listOf(
                                    BatterLineDto(
                                        playerId = 200L,
                                        name = "원정타자1",
                                        position = "CF",
                                        battingOrder = 1,
                                        plateAppearances = 4,
                                        atBats = 4,
                                        runs = 1,
                                        hits = 1,
                                        rbis = 0,
                                        walks = 0,
                                        strikeouts = 2,
                                        avg = ".250",
                                    ),
                                ),
                            pitchers =
                                listOf(
                                    PitcherLineDto(
                                        playerId = 201L,
                                        name = "원정투수1",
                                        inningsPitched = "6.2",
                                        hits = 8,
                                        runs = 5,
                                        earnedRuns = 4,
                                        walks = 3,
                                        strikeouts = 5,
                                        homeRuns = 1,
                                        decision = "L",
                                        era = "5.40",
                                    ),
                                ),
                        ),
                    currentInning = "9회말",
                    gameStatus = "경기 종료",
                )

            every { boxScoreService.getBoxScore(gameId) } returns boxScoreDto

            // when & then
            mockMvc
                .perform(get("/api/v1/games/$gameId/boxscore"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.gameId").value(gameId))
                .andExpect(jsonPath("$.data.homeTeam.teamName").value("홈팀"))
                .andExpect(jsonPath("$.data.homeTeam.runs").value(6))
                .andExpect(jsonPath("$.data.homeTeam.hits").value(10))
                .andExpect(jsonPath("$.data.homeTeam.errors").value(1))
                .andExpect(jsonPath("$.data.awayTeam.teamName").value("원정팀"))
                .andExpect(jsonPath("$.data.awayTeam.runs").value(2))
                .andExpect(jsonPath("$.data.awayTeam.hits").value(5))
                .andExpect(jsonPath("$.data.awayTeam.errors").value(2))
                .andExpect(jsonPath("$.data.currentInning").value("9회말"))
                .andExpect(jsonPath("$.data.gameStatus").value("경기 종료"))
        }

        @Test
        fun `타자 정보를 정확하게 반환한다`() {
            // given
            val gameId = 1L
            val boxScoreDto =
                BoxScoreDto(
                    gameId = gameId,
                    homeTeam =
                        TeamBoxScoreDto(
                            teamId = 10L,
                            teamName = "홈팀",
                            logoUrl = null,
                            inningScores = emptyList(),
                            runs = 0,
                            hits = 0,
                            errors = 0,
                            batters =
                                listOf(
                                    BatterLineDto(
                                        playerId = 100L,
                                        name = "타자A",
                                        position = "SS",
                                        battingOrder = 1,
                                        plateAppearances = 5,
                                        atBats = 4,
                                        runs = 2,
                                        hits = 3,
                                        rbis = 2,
                                        walks = 1,
                                        strikeouts = 1,
                                        avg = ".750",
                                    ),
                                ),
                            pitchers = emptyList(),
                        ),
                    awayTeam =
                        TeamBoxScoreDto(
                            teamId = 20L,
                            teamName = "원정팀",
                            logoUrl = null,
                            inningScores = emptyList(),
                            runs = 0,
                            hits = 0,
                            errors = 0,
                            batters = emptyList(),
                            pitchers = emptyList(),
                        ),
                    currentInning = "5회초",
                    gameStatus = "경기 중",
                )

            every { boxScoreService.getBoxScore(gameId) } returns boxScoreDto

            // when & then
            mockMvc
                .perform(get("/api/v1/games/$gameId/boxscore"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.homeTeam.batters[0].playerId").value(100))
                .andExpect(jsonPath("$.data.homeTeam.batters[0].name").value("타자A"))
                .andExpect(jsonPath("$.data.homeTeam.batters[0].position").value("SS"))
                .andExpect(jsonPath("$.data.homeTeam.batters[0].battingOrder").value(1))
                .andExpect(jsonPath("$.data.homeTeam.batters[0].plateAppearances").value(5))
                .andExpect(jsonPath("$.data.homeTeam.batters[0].atBats").value(4))
                .andExpect(jsonPath("$.data.homeTeam.batters[0].runs").value(2))
                .andExpect(jsonPath("$.data.homeTeam.batters[0].hits").value(3))
                .andExpect(jsonPath("$.data.homeTeam.batters[0].rbis").value(2))
                .andExpect(jsonPath("$.data.homeTeam.batters[0].walks").value(1))
                .andExpect(jsonPath("$.data.homeTeam.batters[0].strikeouts").value(1))
                .andExpect(jsonPath("$.data.homeTeam.batters[0].avg").value(".750"))
        }

        @Test
        fun `투수 정보를 정확하게 반환한다`() {
            // given
            val gameId = 1L
            val boxScoreDto =
                BoxScoreDto(
                    gameId = gameId,
                    homeTeam =
                        TeamBoxScoreDto(
                            teamId = 10L,
                            teamName = "홈팀",
                            logoUrl = null,
                            inningScores = emptyList(),
                            runs = 0,
                            hits = 0,
                            errors = 0,
                            batters = emptyList(),
                            pitchers =
                                listOf(
                                    PitcherLineDto(
                                        playerId = 101L,
                                        name = "투수B",
                                        inningsPitched = "6.1",
                                        hits = 7,
                                        runs = 3,
                                        earnedRuns = 2,
                                        walks = 2,
                                        strikeouts = 10,
                                        homeRuns = 1,
                                        decision = "W",
                                        era = "2.84",
                                    ),
                                ),
                        ),
                    awayTeam =
                        TeamBoxScoreDto(
                            teamId = 20L,
                            teamName = "원정팀",
                            logoUrl = null,
                            inningScores = emptyList(),
                            runs = 0,
                            hits = 0,
                            errors = 0,
                            batters = emptyList(),
                            pitchers = emptyList(),
                        ),
                    currentInning = "7회초",
                    gameStatus = "경기 중",
                )

            every { boxScoreService.getBoxScore(gameId) } returns boxScoreDto

            // when & then
            mockMvc
                .perform(get("/api/v1/games/$gameId/boxscore"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.homeTeam.pitchers[0].playerId").value(101))
                .andExpect(jsonPath("$.data.homeTeam.pitchers[0].name").value("투수B"))
                .andExpect(jsonPath("$.data.homeTeam.pitchers[0].inningsPitched").value("6.1"))
                .andExpect(jsonPath("$.data.homeTeam.pitchers[0].hits").value(7))
                .andExpect(jsonPath("$.data.homeTeam.pitchers[0].runs").value(3))
                .andExpect(jsonPath("$.data.homeTeam.pitchers[0].earnedRuns").value(2))
                .andExpect(jsonPath("$.data.homeTeam.pitchers[0].walks").value(2))
                .andExpect(jsonPath("$.data.homeTeam.pitchers[0].strikeouts").value(10))
                .andExpect(jsonPath("$.data.homeTeam.pitchers[0].homeRuns").value(1))
                .andExpect(jsonPath("$.data.homeTeam.pitchers[0].decision").value("W"))
                .andExpect(jsonPath("$.data.homeTeam.pitchers[0].era").value("2.84"))
        }

        @Test
        fun `이닝별 점수를 정확하게 반환한다`() {
            // given
            val gameId = 1L
            val boxScoreDto =
                BoxScoreDto(
                    gameId = gameId,
                    homeTeam =
                        TeamBoxScoreDto(
                            teamId = 10L,
                            teamName = "홈팀",
                            logoUrl = null,
                            inningScores = listOf(2, 0, 1, 0, 0, 3, 0, 1, 0),
                            runs = 7,
                            hits = 12,
                            errors = 0,
                            batters = emptyList(),
                            pitchers = emptyList(),
                        ),
                    awayTeam =
                        TeamBoxScoreDto(
                            teamId = 20L,
                            teamName = "원정팀",
                            logoUrl = null,
                            inningScores = listOf(0, 1, 0, 0, 2, 0, 0, 0, 1),
                            runs = 4,
                            hits = 8,
                            errors = 1,
                            batters = emptyList(),
                            pitchers = emptyList(),
                        ),
                    currentInning = "9회말",
                    gameStatus = "경기 종료",
                )

            every { boxScoreService.getBoxScore(gameId) } returns boxScoreDto

            // when & then
            mockMvc
                .perform(get("/api/v1/games/$gameId/boxscore"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.homeTeam.inningScores").isArray)
                .andExpect(jsonPath("$.data.homeTeam.inningScores[0]").value(2))
                .andExpect(jsonPath("$.data.homeTeam.inningScores[5]").value(3))
                .andExpect(jsonPath("$.data.awayTeam.inningScores").isArray)
                .andExpect(jsonPath("$.data.awayTeam.inningScores[1]").value(1))
                .andExpect(jsonPath("$.data.awayTeam.inningScores[4]").value(2))
        }

        @Test
        fun `예외 발생 시 에러 응답을 반환한다`() {
            // given
            val gameId = 999L
            every { boxScoreService.getBoxScore(gameId) } throws IllegalArgumentException("경기를 찾을 수 없습니다")

            // when & then
            mockMvc
                .perform(get("/api/v1/games/$gameId/boxscore"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }
    }
}
