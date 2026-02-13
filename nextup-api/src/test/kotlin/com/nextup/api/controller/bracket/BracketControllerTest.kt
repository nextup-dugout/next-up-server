package com.nextup.api.controller.bracket

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.BracketEntry
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import com.nextup.core.service.bracket.BracketGeneratorService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate

class BracketControllerTest {
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
                .standaloneSetup(BracketController(bracketGeneratorService))
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

    @Test
    fun `should return bracket when competition exists`() {
        // given
        val competitionId = 1L
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
                    seed2 = 2,
                    id = 1L,
                ),
            )

        every { bracketGeneratorService.getBracket(competitionId) } returns bracketEntries

        // when & then
        mockMvc
            .perform(
                get("/api/v1/competitions/{competitionId}/bracket", competitionId)
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.competitionId").value(competitionId))
            .andExpect(jsonPath("$.data.entries").isArray)
            .andExpect(jsonPath("$.data.entries[0].id").value(1))
            .andExpect(jsonPath("$.data.entries[0].roundNumber").value(1))
            .andExpect(jsonPath("$.data.entries[0].matchNumber").value(1))
            .andExpect(jsonPath("$.data.entries[0].team1.id").value(1))
            .andExpect(jsonPath("$.data.entries[0].team1.name").value("팀A"))
            .andExpect(jsonPath("$.data.entries[0].team2.id").value(2))
            .andExpect(jsonPath("$.data.entries[0].team2.name").value("팀B"))
            .andExpect(jsonPath("$.data.entries[0].bracketType").value("WINNERS"))
    }

    @Test
    fun `should return empty bracket when no entries exist`() {
        // given
        val competitionId = 1L
        every { bracketGeneratorService.getBracket(competitionId) } returns emptyList()

        // when & then
        mockMvc
            .perform(
                get("/api/v1/competitions/{competitionId}/bracket", competitionId)
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.competitionId").value(competitionId))
            .andExpect(jsonPath("$.data.entries").isEmpty)
    }

    @Test
    fun `should return 404 when competition not found`() {
        // given
        val competitionId = 999L
        every { bracketGeneratorService.getBracket(competitionId) } throws CompetitionNotFoundException(competitionId)

        // when & then
        mockMvc
            .perform(
                get("/api/v1/competitions/{competitionId}/bracket", competitionId)
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("COMPETITION_NOT_FOUND"))
    }

    @Test
    fun `should return bracket with bye match`() {
        // given
        val competitionId = 1L
        val bracketEntries =
            listOf(
                BracketEntry(
                    competition = competition,
                    roundNumber = 1,
                    matchNumber = 1,
                    team1 = team1,
                    team2 = null,
                    bracketType = "WINNERS",
                    seed1 = 1,
                    seed2 = 2,
                    id = 1L,
                ),
            )

        every { bracketGeneratorService.getBracket(competitionId) } returns bracketEntries

        // when & then
        mockMvc
            .perform(
                get("/api/v1/competitions/{competitionId}/bracket", competitionId)
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.entries[0].team1.id").value(1))
            .andExpect(jsonPath("$.data.entries[0].team2").isEmpty)
            .andExpect(jsonPath("$.data.entries[0].isBye").value(true))
    }
}
