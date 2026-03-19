package com.nextup.scorer.controller.competition

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.league.League
import com.nextup.core.service.competition.CompetitionService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate

@DisplayName("CompetitionScorerController")
class CompetitionScorerControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var competitionService: CompetitionService
    private lateinit var controller: CompetitionScorerController

    @BeforeEach
    fun setUp() {
        competitionService = mockk()
        controller = CompetitionScorerController(competitionService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Nested
    @DisplayName("GET /api/scorer/competitions")
    inner class GetAllCompetitions {

        @Test
        fun `should return all competitions when no filter provided`() {
            // given
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association)
            val competitions =
                listOf(
                    createCompetition(1L, "2025 춘계대회", league, 2025, 1),
                    createCompetition(2L, "2025 추계대회", league, 2025, 2)
                )
            every { competitionService.getAll() } returns competitions

            // when & then
            mockMvc.perform(get("/api/v1/scorer/competitions"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("2025 춘계대회"))
                .andExpect(jsonPath("$.data[0].leagueName").value("1부 리그"))

            verify(exactly = 1) { competitionService.getAll() }
        }

        @Test
        fun `should return competitions filtered by leagueId`() {
            // given
            val leagueId = 1L
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(leagueId, "1부 리그", association)
            val competitions =
                listOf(
                    createCompetition(1L, "2025 춘계대회", league, 2025, 1)
                )
            every { competitionService.getByLeagueId(leagueId) } returns competitions

            // when & then
            mockMvc.perform(get("/api/v1/scorer/competitions/by-league/$leagueId"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].leagueId").value(leagueId))

            verify(exactly = 1) { competitionService.getByLeagueId(leagueId) }
        }
    }

    @Nested
    @DisplayName("GET /api/scorer/competitions/{id}")
    inner class GetCompetition {

        @Test
        fun `should return competition when found`() {
            // given
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association)
            val competition = createCompetition(1L, "2025 춘계대회", league, 2025, 1)
            every { competitionService.getByIdWithLeague(1L) } returns competition

            // when & then
            mockMvc.perform(get("/api/v1/scorer/competitions/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("2025 춘계대회"))
                .andExpect(jsonPath("$.data.leagueId").value(1))
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))

            verify(exactly = 1) { competitionService.getByIdWithLeague(1L) }
        }
    }

    private fun createAssociation(
        id: Long,
        name: String
    ): Association {
        return Association(
            name = name,
            abbreviation = null,
            region = "서울",
            description = null,
            logoUrl = null,
            websiteUrl = null
        ).apply {
            val idField = Association::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
    }

    private fun createLeague(
        id: Long,
        name: String,
        association: Association
    ): League {
        return League(
            association = association,
            name = name,
            abbreviation = null,
            foundedYear = 2020,
            divisionLevel = 1,
            description = null,
            logoUrl = null
        ).apply {
            val idField = League::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
    }

    private fun createCompetition(
        id: Long,
        name: String,
        league: League,
        year: Int,
        season: Int
    ): Competition {
        return Competition(
            league = league,
            name = name,
            year = year,
            season = season,
            type = CompetitionType.LEAGUE,
            startDate = LocalDate.of(year, 3, 1),
            endDate = LocalDate.of(year, 6, 30),
            status = CompetitionStatus.SCHEDULED,
            description = null,
            maxTeams = null
        ).apply {
            val idField = Competition::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
    }
}
