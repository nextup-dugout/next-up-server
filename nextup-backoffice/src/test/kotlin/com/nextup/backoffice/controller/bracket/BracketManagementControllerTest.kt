package com.nextup.backoffice.controller.bracket

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.nextup.backoffice.exception.GlobalExceptionHandler
import com.nextup.common.exception.BracketEntryNotFoundException
import com.nextup.common.exception.InvalidInputException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.BracketEntry
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import com.nextup.core.service.bracket.BracketGeneratorService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate

@DisplayName("BracketManagementController")
class BracketManagementControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var bracketGeneratorService: BracketGeneratorService
    private lateinit var objectMapper: ObjectMapper

    private lateinit var competition: Competition
    private lateinit var team1: Team
    private lateinit var team2: Team

    @BeforeEach
    fun setup() {
        bracketGeneratorService = mockk()
        objectMapper = ObjectMapper().registerModule(JavaTimeModule())

        mockMvc =
            MockMvcBuilders
                .standaloneSetup(BracketManagementController(bracketGeneratorService))
                .setControllerAdvice(GlobalExceptionHandler())
                .build()

        // Test data
        val association = Association(name = "테스트 협회", id = 1L)
        val league = League(association = association, name = "테스트 리그", foundedYear = 2020, id = 1L)
        competition =
            Competition(
                league = league,
                name = "2025 춘계 토너먼트",
                year = 2025,
                season = 1,
                type = CompetitionType.TOURNAMENT,
                startDate = LocalDate.of(2025, 3, 1),
                id = 1L,
            )
        team1 = Team(league = league, name = "팀A", city = "서울", foundedYear = 2020, id = 1L)
        team2 = Team(league = league, name = "팀B", city = "서울", foundedYear = 2020, id = 2L)
    }

    @Nested
    @DisplayName("POST /backoffice/v1/competitions/{competitionId}/bracket/generate")
    inner class GenerateBracket {
        @Test
        fun `should generate single elimination bracket`() {
            // given
            val competitionId = 1L
            val requestBody =
                """
                {
                    "tournamentType": "SINGLE_ELIMINATION",
                    "seededTeamIds": [1, 2, 3, 4]
                }
                """.trimIndent()

            val bracketEntries =
                listOf(
                    BracketEntry(
                        competition = competition,
                        roundNumber = 1,
                        matchNumber = 1,
                        team1 = team1,
                        team2 = team2,
                        bracketType = "WINNERS",
                        seed1 = 1,
                        seed2 = 4,
                        id = 1L,
                    ),
                )

            every {
                bracketGeneratorService.generateSingleElimination(competitionId, listOf(1L, 2L, 3L, 4L))
            } returns bracketEntries

            // when & then
            mockMvc
                .perform(
                    post("/backoffice/v1/competitions/{competitionId}/bracket/generate", competitionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].roundNumber").value(1))
                .andExpect(jsonPath("$.data[0].bracketType").value("WINNERS"))

            verify(exactly = 1) {
                bracketGeneratorService.generateSingleElimination(competitionId, listOf(1L, 2L, 3L, 4L))
            }
        }

        @Test
        fun `should generate double elimination bracket`() {
            // given
            val competitionId = 1L
            val requestBody =
                """
                {
                    "tournamentType": "DOUBLE_ELIMINATION",
                    "seededTeamIds": [1, 2, 3, 4]
                }
                """.trimIndent()

            val bracketEntries =
                listOf(
                    BracketEntry(
                        competition = competition,
                        roundNumber = 1,
                        matchNumber = 1,
                        team1 = team1,
                        team2 = team2,
                        bracketType = "WINNERS",
                        seed1 = 1,
                        seed2 = 4,
                        id = 1L,
                    ),
                )

            every {
                bracketGeneratorService.generateDoubleElimination(competitionId, listOf(1L, 2L, 3L, 4L))
            } returns bracketEntries

            // when & then
            mockMvc
                .perform(
                    post("/backoffice/v1/competitions/{competitionId}/bracket/generate", competitionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)

            verify(exactly = 1) {
                bracketGeneratorService.generateDoubleElimination(competitionId, listOf(1L, 2L, 3L, 4L))
            }
        }

        @Test
        fun `should return 400 when team list is empty`() {
            // given
            val competitionId = 1L
            val requestBody =
                """
                {
                    "tournamentType": "SINGLE_ELIMINATION",
                    "seededTeamIds": []
                }
                """.trimIndent()

            // when & then
            mockMvc
                .perform(
                    post("/backoffice/v1/competitions/{competitionId}/bracket/generate", competitionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody),
                ).andExpect(status().isBadRequest)
        }

        @Test
        fun `should return 400 when duplicate teams exist`() {
            // given
            val competitionId = 1L
            val requestBody =
                """
                {
                    "tournamentType": "SINGLE_ELIMINATION",
                    "seededTeamIds": [1, 2, 1]
                }
                """.trimIndent()

            every {
                bracketGeneratorService.generateSingleElimination(competitionId, listOf(1L, 2L, 1L))
            } throws
                InvalidInputException(
                    code = "DUPLICATE_TEAMS",
                    message = "중복된 팀이 존재합니다",
                )

            // when & then
            mockMvc
                .perform(
                    post("/backoffice/v1/competitions/{competitionId}/bracket/generate", competitionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody),
                ).andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("PUT /backoffice/v1/competitions/{competitionId}/bracket/{entryId}/advance")
    inner class AdvanceWinner {
        @Test
        fun `should advance winner successfully`() {
            // given
            val competitionId = 1L
            val entryId = 1L
            val winnerTeamId = 1L
            val requestBody =
                """
                {
                    "winnerTeamId": $winnerTeamId
                }
                """.trimIndent()

            val updatedEntry =
                BracketEntry(
                    competition = competition,
                    roundNumber = 1,
                    matchNumber = 1,
                    team1 = team1,
                    team2 = team2,
                    winner = team1,
                    bracketType = "WINNERS",
                    seed1 = 1,
                    seed2 = 2,
                    id = 1L,
                )

            every { bracketGeneratorService.advanceWinner(entryId, winnerTeamId) } returns updatedEntry

            // when & then
            mockMvc
                .perform(
                    put(
                        "/backoffice/v1/competitions/{competitionId}/bracket/{entryId}/advance",
                        competitionId,
                        entryId,
                    ).contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.winner.id").value(1))
                .andExpect(jsonPath("$.data.isCompleted").value(true))

            verify(exactly = 1) { bracketGeneratorService.advanceWinner(entryId, winnerTeamId) }
        }

        @Test
        fun `should return 404 when bracket entry not found`() {
            // given
            val competitionId = 1L
            val entryId = 999L
            val winnerTeamId = 1L
            val requestBody =
                """
                {
                    "winnerTeamId": $winnerTeamId
                }
                """.trimIndent()

            every {
                bracketGeneratorService.advanceWinner(entryId, winnerTeamId)
            } throws BracketEntryNotFoundException(entryId)

            // when & then
            mockMvc
                .perform(
                    put(
                        "/backoffice/v1/competitions/{competitionId}/bracket/{entryId}/advance",
                        competitionId,
                        entryId,
                    ).contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody),
                ).andExpect(status().isNotFound)
        }

        @Test
        fun `should return 400 when winner team not found`() {
            // given
            val competitionId = 1L
            val entryId = 1L
            val winnerTeamId = 999L
            val requestBody =
                """
                {
                    "winnerTeamId": $winnerTeamId
                }
                """.trimIndent()

            every {
                bracketGeneratorService.advanceWinner(entryId, winnerTeamId)
            } throws
                InvalidInputException(
                    code = "TEAM_NOT_FOUND",
                    message = "팀을 찾을 수 없습니다: $winnerTeamId",
                )

            // when & then
            mockMvc
                .perform(
                    put(
                        "/backoffice/v1/competitions/{competitionId}/bracket/{entryId}/advance",
                        competitionId,
                        entryId,
                    ).contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody),
                ).andExpect(status().isBadRequest)
        }
    }
}
