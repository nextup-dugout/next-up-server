package com.nextup.api.controller.competition

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.league.League
import com.nextup.infrastructure.service.competition.CompetitionService
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
import java.time.LocalDate

@DisplayName("CompetitionController")
class CompetitionControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var competitionService: CompetitionService
    private lateinit var controller: CompetitionController

    @BeforeEach
    fun setUp() {
        competitionService = mockk()
        controller = CompetitionController(competitionService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Nested
    @DisplayName("GET /api/v1/competitions")
    inner class GetCompetitions {

        @Test
        fun `should return all in-progress competitions`() {
            // given
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association)
            val competitions = listOf(
                createCompetition(1L, league, "2025 춘계대회", 2025, 1).apply { start() },
                createCompetition(2L, league, "2025 하계대회", 2025, 2).apply { start() }
            )
            every { competitionService.getInProgress() } returns competitions

            // when & then
            mockMvc.perform(get("/api/v1/competitions"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("2025 춘계대회"))
                .andExpect(jsonPath("$.data[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data[1].name").value("2025 하계대회"))
        }

        @Test
        fun `should return empty list when no in-progress competitions`() {
            // given
            every { competitionService.getInProgress() } returns emptyList()

            // when & then
            mockMvc.perform(get("/api/v1/competitions"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(0))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/competitions/{id}")
    inner class GetCompetition {

        @Test
        fun `should return competition detail when found`() {
            // given
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association)
            val competition = createCompetition(1L, league, "2025 춘계대회", 2025, 1)
            every { competitionService.getByIdWithLeague(1L) } returns competition

            // when & then
            mockMvc.perform(get("/api/v1/competitions/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("2025 춘계대회"))
                .andExpect(jsonPath("$.data.leagueId").value(1))
                .andExpect(jsonPath("$.data.leagueName").value("1부 리그"))
                .andExpect(jsonPath("$.data.year").value(2025))
                .andExpect(jsonPath("$.data.season").value(1))
                .andExpect(jsonPath("$.data.type").value("LEAGUE"))
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/competitions/by-league/{leagueId}")
    inner class GetCompetitionsByLeague {

        @Test
        fun `should return competitions by league`() {
            // given
            val leagueId = 1L
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(leagueId, "1부 리그", association)
            val competitions = listOf(
                createCompetition(1L, league, "2025 춘계대회", 2025, 1),
                createCompetition(2L, league, "2025 하계대회", 2025, 2),
                createCompetition(3L, league, "2024 추계대회", 2024, 2)
            )
            every { competitionService.getByLeagueId(leagueId) } returns competitions

            // when & then
            mockMvc.perform(get("/api/v1/competitions/by-league/$leagueId"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].leagueId").value(leagueId))
                .andExpect(jsonPath("$.data[0].name").value("2025 춘계대회"))
                .andExpect(jsonPath("$.data[1].name").value("2025 하계대회"))
                .andExpect(jsonPath("$.data[2].name").value("2024 추계대회"))
        }

        @Test
        fun `should return empty list when league has no competitions`() {
            // given
            val leagueId = 1L
            every { competitionService.getByLeagueId(leagueId) } returns emptyList()

            // when & then
            mockMvc.perform(get("/api/v1/competitions/by-league/$leagueId"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(0))
        }
    }

    private fun createAssociation(id: Long, name: String): Association {
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

    private fun createLeague(id: Long, name: String, association: Association): League {
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
        league: League,
        name: String,
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
            endDate = null,
            description = null,
            maxTeams = null
        ).apply {
            val idField = Competition::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
    }
}
