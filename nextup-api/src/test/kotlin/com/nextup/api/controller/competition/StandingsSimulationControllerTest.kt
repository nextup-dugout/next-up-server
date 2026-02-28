package com.nextup.api.controller.competition

import com.fasterxml.jackson.databind.ObjectMapper
import com.nextup.api.dto.standings.SimulatedGameResultRequest
import com.nextup.api.dto.standings.SimulationApiRequest
import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.common.exception.InvalidInputException
import com.nextup.core.service.standings.StandingsSimulationService
import com.nextup.core.service.standings.dto.MagicNumber
import com.nextup.core.service.standings.dto.PlayoffScenarioResult
import com.nextup.core.service.standings.dto.RankChange
import com.nextup.core.service.standings.dto.SimulationResult
import com.nextup.core.service.standings.dto.TeamStandingDto
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.math.BigDecimal

@DisplayName("StandingsSimulationController")
class StandingsSimulationControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var standingsSimulationService: StandingsSimulationService
    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        standingsSimulationService = mockk()
        val controller = StandingsSimulationController(standingsSimulationService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    // =========================================================================
    // GET /magic-numbers
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/competitions/{competitionId}/standings/magic-numbers")
    inner class GetMagicNumbers {
        @Test
        fun `매직넘버 목록을 정상 조회한다`() {
            val competitionId = 1L
            val magicNumbers =
                listOf(
                    MagicNumber(teamId = 1L, targetRank = 1, magicNumber = 3, isClinched = false, isEliminated = false),
                    MagicNumber(teamId = 2L, targetRank = 2, magicNumber = 6, isClinched = false, isEliminated = false),
                    MagicNumber(teamId = 3L, targetRank = 3, magicNumber = 0, isClinched = true, isEliminated = false),
                )

            every { standingsSimulationService.calculateMagicNumbers(competitionId) } returns magicNumbers

            mockMvc
                .perform(get("/api/v1/competitions/$competitionId/standings/magic-numbers"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].teamId").value(1))
                .andExpect(jsonPath("$.data[0].targetRank").value(1))
                .andExpect(jsonPath("$.data[0].magicNumber").value(3))
                .andExpect(jsonPath("$.data[0].isClinched").value(false))
                .andExpect(jsonPath("$.data[0].isEliminated").value(false))
                .andExpect(jsonPath("$.data[2].isClinched").value(true))
        }

        @Test
        fun `경기가 없으면 빈 목록 반환`() {
            val competitionId = 1L
            every { standingsSimulationService.calculateMagicNumbers(competitionId) } returns emptyList()

            mockMvc
                .perform(get("/api/v1/competitions/$competitionId/standings/magic-numbers"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty)
        }

        @Test
        fun `대회가 없으면 404 반환`() {
            val competitionId = 999L
            every {
                standingsSimulationService.calculateMagicNumbers(competitionId)
            } throws CompetitionNotFoundException(competitionId)

            mockMvc
                .perform(get("/api/v1/competitions/$competitionId/standings/magic-numbers"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMPETITION_NOT_FOUND"))
        }
    }

    // =========================================================================
    // POST /simulation
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/competitions/{competitionId}/standings/simulation")
    inner class PostSimulation {
        @Test
        fun `시뮬레이션 결과를 정상 반환한다`() {
            val competitionId = 1L
            val simulationResult =
                SimulationResult(
                    standings =
                        listOf(
                            TeamStandingDto(
                                rank = 1,
                                teamId = 2L,
                                teamName = "Lions",
                                gamesPlayed = 6,
                                remainingGames = 4,
                                wins = 5,
                                losses = 1,
                                draws = 0,
                                winningPercentage = BigDecimal("0.833"),
                                gamesBehind = BigDecimal.ZERO,
                                runsScored = 30,
                                runsAllowed = 10,
                                runDifferential = 20,
                            ),
                            TeamStandingDto(
                                rank = 2,
                                teamId = 1L,
                                teamName = "Tigers",
                                gamesPlayed = 6,
                                remainingGames = 4,
                                wins = 4,
                                losses = 2,
                                draws = 0,
                                winningPercentage = BigDecimal("0.667"),
                                gamesBehind = BigDecimal("1.0"),
                                runsScored = 25,
                                runsAllowed = 18,
                                runDifferential = 7,
                            ),
                        ),
                    changes =
                        listOf(
                            RankChange(
                                teamId = 1L,
                                teamName = "Tigers",
                                previousRank = 1,
                                projectedRank = 2,
                                rankChange = -1,
                            ),
                            RankChange(
                                teamId = 2L,
                                teamName = "Lions",
                                previousRank = 2,
                                projectedRank = 1,
                                rankChange = 1,
                            ),
                        ),
                )

            every {
                standingsSimulationService.simulateStandings(eq(competitionId), any())
            } returns simulationResult

            val request =
                SimulationApiRequest(
                    gameResults =
                        listOf(
                            SimulatedGameResultRequest(gameId = 5L, homeScore = 2, awayScore = 7),
                        ),
                )

            mockMvc
                .perform(
                    post("/api/v1/competitions/$competitionId/standings/simulation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.standings.length()").value(2))
                .andExpect(jsonPath("$.data.standings[0].teamId").value(2))
                .andExpect(jsonPath("$.data.standings[0].rank").value(1))
                .andExpect(jsonPath("$.data.standings[1].teamId").value(1))
                .andExpect(jsonPath("$.data.standings[1].rank").value(2))
                .andExpect(jsonPath("$.data.changes.length()").value(2))
                .andExpect(jsonPath("$.data.changes[0].teamId").value(1))
                .andExpect(jsonPath("$.data.changes[0].rankChange").value(-1))
        }

        @Test
        fun `빈 gameResults로도 시뮬레이션이 정상 동작한다`() {
            val competitionId = 1L
            val simulationResult =
                SimulationResult(
                    standings = emptyList(),
                    changes = emptyList(),
                )

            every {
                standingsSimulationService.simulateStandings(eq(competitionId), any())
            } returns simulationResult

            val request = SimulationApiRequest(gameResults = emptyList())

            mockMvc
                .perform(
                    post("/api/v1/competitions/$competitionId/standings/simulation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.standings").isEmpty)
                .andExpect(jsonPath("$.data.changes").isEmpty)
        }

        @Test
        fun `대회가 없으면 404 반환`() {
            val competitionId = 999L
            every {
                standingsSimulationService.simulateStandings(eq(competitionId), any())
            } throws CompetitionNotFoundException(competitionId)

            val request = SimulationApiRequest(gameResults = emptyList())

            mockMvc
                .perform(
                    post("/api/v1/competitions/$competitionId/standings/simulation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        fun `InvalidInputException 발생 시 400 반환`() {
            val competitionId = 1L
            every {
                standingsSimulationService.simulateStandings(eq(competitionId), any())
            } throws InvalidInputException("INVALID_SIMULATION_SCORE", "점수는 0 이상이어야 합니다.")

            val request =
                SimulationApiRequest(
                    gameResults =
                        listOf(
                            SimulatedGameResultRequest(gameId = 1L, homeScore = 0, awayScore = 0),
                        ),
                )

            mockMvc
                .perform(
                    post("/api/v1/competitions/$competitionId/standings/simulation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_SIMULATION_SCORE"))
        }
    }

    // =========================================================================
    // GET /playoff-scenarios
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/competitions/{competitionId}/standings/playoff-scenarios")
    inner class GetPlayoffScenarios {
        @Test
        fun `플레이오프 시나리오를 정상 조회한다`() {
            val competitionId = 1L
            val teamId = 1L
            val playoffTeams = 4
            val scenarioResult =
                PlayoffScenarioResult(
                    totalScenarios = 27L,
                    qualifyingScenarios = 20L,
                    probability = 0.741,
                    magicNumber = 3,
                )

            every {
                standingsSimulationService.calculatePlayoffScenarios(competitionId, teamId, playoffTeams)
            } returns scenarioResult

            mockMvc
                .perform(
                    get("/api/v1/competitions/$competitionId/standings/playoff-scenarios")
                        .param("teamId", teamId.toString())
                        .param("playoffTeams", playoffTeams.toString()),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalScenarios").value(27))
                .andExpect(jsonPath("$.data.qualifyingScenarios").value(20))
                .andExpect(jsonPath("$.data.probability").value(0.741))
                .andExpect(jsonPath("$.data.magicNumber").value(3))
        }

        @Test
        fun `확률 100%인 경우 정상 반환`() {
            val competitionId = 1L
            val scenarioResult =
                PlayoffScenarioResult(
                    totalScenarios = 1L,
                    qualifyingScenarios = 1L,
                    probability = 1.0,
                    magicNumber = null,
                )

            every {
                standingsSimulationService.calculatePlayoffScenarios(competitionId, 1L, 4)
            } returns scenarioResult

            mockMvc
                .perform(
                    get("/api/v1/competitions/$competitionId/standings/playoff-scenarios")
                        .param("teamId", "1")
                        .param("playoffTeams", "4"),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.probability").value(1.0))
        }

        @Test
        fun `대회가 없으면 404 반환`() {
            val competitionId = 999L
            every {
                standingsSimulationService.calculatePlayoffScenarios(competitionId, 1L, 4)
            } throws CompetitionNotFoundException(competitionId)

            mockMvc
                .perform(
                    get("/api/v1/competitions/$competitionId/standings/playoff-scenarios")
                        .param("teamId", "1")
                        .param("playoffTeams", "4"),
                )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        fun `팀이 대회에 없으면 400 반환`() {
            val competitionId = 1L
            every {
                standingsSimulationService.calculatePlayoffScenarios(competitionId, 999L, 4)
            } throws InvalidInputException("TEAM_NOT_IN_COMPETITION", "해당 팀은 이 대회에 참여하지 않습니다.")

            mockMvc
                .perform(
                    get("/api/v1/competitions/$competitionId/standings/playoff-scenarios")
                        .param("teamId", "999")
                        .param("playoffTeams", "4"),
                )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TEAM_NOT_IN_COMPETITION"))
        }

        @Test
        fun `playoffTeams 파라미터 누락 시 400 반환`() {
            mockMvc
                .perform(
                    get("/api/v1/competitions/1/standings/playoff-scenarios")
                        .param("teamId", "1"),
                )
                .andExpect(status().isBadRequest)
        }

        @Test
        fun `teamId 파라미터 누락 시 400 반환`() {
            mockMvc
                .perform(
                    get("/api/v1/competitions/1/standings/playoff-scenarios")
                        .param("playoffTeams", "4"),
                )
                .andExpect(status().isBadRequest)
        }
    }
}
