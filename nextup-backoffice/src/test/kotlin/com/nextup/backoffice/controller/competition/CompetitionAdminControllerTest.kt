package com.nextup.backoffice.controller.competition

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.nextup.backoffice.dto.competition.CreateCompetitionRequest
import com.nextup.backoffice.dto.competition.PrepareNextSeasonRequest
import com.nextup.backoffice.dto.competition.UpdateCompetitionRequest
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.league.League
import com.nextup.core.service.competition.CompetitionService
import com.nextup.core.service.competition.SeasonTransitionService
import com.nextup.core.service.competition.dto.NextSeasonPreparationResult
import com.nextup.core.service.competition.dto.SeasonSummaryDto
import com.nextup.core.service.standings.dto.TeamStandingDto
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("CompetitionAdminController")
class CompetitionAdminControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var competitionService: CompetitionService
    private lateinit var seasonTransitionService: SeasonTransitionService
    private lateinit var controller: CompetitionAdminController
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        competitionService = mockk()
        seasonTransitionService = mockk()
        controller = CompetitionAdminController(competitionService, seasonTransitionService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
        objectMapper = ObjectMapper().registerModule(JavaTimeModule())
    }

    @Nested
    @DisplayName("GET /api/backoffice/competitions")
    inner class GetAllCompetitions {
        @Test
        fun `should return all competitions`() {
            // given
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association)
            val competitions =
                listOf(
                    createCompetition(1L, "2025 춘계대회", league, 2025, 1),
                    createCompetition(2L, "2025 추계대회", league, 2025, 2),
                )
            every { competitionService.getAll() } returns competitions

            // when & then
            mockMvc
                .perform(get("/api/backoffice/competitions"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("2025 춘계대회"))
                .andExpect(jsonPath("$.data[0].year").value(2025))
                .andExpect(jsonPath("$.data[0].season").value(1))

            verify(exactly = 1) { competitionService.getAll() }
        }
    }

    @Nested
    @DisplayName("GET /api/backoffice/competitions/{id}")
    inner class GetCompetition {
        @Test
        fun `should return competition when found`() {
            // given
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association)
            val competition = createCompetition(1L, "2025 춘계대회", league, 2025, 1)
            every { competitionService.getByIdWithLeague(1L) } returns competition

            // when & then
            mockMvc
                .perform(get("/api/backoffice/competitions/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("2025 춘계대회"))
                .andExpect(jsonPath("$.data.leagueId").value(1))
                .andExpect(jsonPath("$.data.year").value(2025))
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))

            verify(exactly = 1) { competitionService.getByIdWithLeague(1L) }
        }
    }

    @Nested
    @DisplayName("POST /api/backoffice/competitions")
    inner class CreateCompetition {
        @Test
        fun `should create competition with valid request`() {
            // given
            val request =
                CreateCompetitionRequest(
                    leagueId = 1L,
                    name = "2025 춘계대회",
                    year = 2025,
                    season = 1,
                    type = CompetitionType.LEAGUE,
                    startDate = LocalDate.of(2025, 3, 1),
                    endDate = LocalDate.of(2025, 6, 30),
                    description = "2025년 춘계 시즌",
                    maxTeams = 8,
                )
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association)
            val competition = createCompetition(1L, "2025 춘계대회", league, 2025, 1)

            every {
                competitionService.create(
                    leagueId = 1L,
                    name = "2025 춘계대회",
                    year = 2025,
                    season = 1,
                    type = CompetitionType.LEAGUE,
                    startDate = LocalDate.of(2025, 3, 1),
                    endDate = LocalDate.of(2025, 6, 30),
                    description = "2025년 춘계 시즌",
                    maxTeams = 8,
                )
            } returns competition

            // when & then
            mockMvc
                .perform(
                    post("/api/backoffice/competitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("2025 춘계대회"))

            verify(exactly = 1) {
                competitionService.create(
                    leagueId = 1L,
                    name = "2025 춘계대회",
                    year = 2025,
                    season = 1,
                    type = CompetitionType.LEAGUE,
                    startDate = LocalDate.of(2025, 3, 1),
                    endDate = LocalDate.of(2025, 6, 30),
                    description = "2025년 춘계 시즌",
                    maxTeams = 8,
                )
            }
        }
    }

    @Nested
    @DisplayName("PUT /api/backoffice/competitions/{id}")
    inner class UpdateCompetition {
        @Test
        fun `should update competition with valid request`() {
            // given
            val request =
                UpdateCompetitionRequest(
                    description = "수정된 설명",
                    endDate = LocalDate.of(2025, 7, 15),
                )
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association)
            val competition = createCompetition(1L, "2025 춘계대회", league, 2025, 1)

            every {
                competitionService.update(
                    id = 1L,
                    description = "수정된 설명",
                    endDate = LocalDate.of(2025, 7, 15),
                )
            } returns competition

            // when & then
            mockMvc
                .perform(
                    put("/api/backoffice/competitions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))

            verify(exactly = 1) {
                competitionService.update(
                    id = 1L,
                    description = "수정된 설명",
                    endDate = LocalDate.of(2025, 7, 15),
                )
            }
        }
    }

    @Nested
    @DisplayName("POST /api/backoffice/competitions/{id}/start")
    inner class StartCompetition {
        @Test
        fun `should start competition`() {
            // given
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association)
            val competition =
                createCompetition(1L, "2025 춘계대회", league, 2025, 1).apply {
                    start()
                }
            every { competitionService.start(1L) } returns competition

            // when & then
            mockMvc
                .perform(post("/api/backoffice/competitions/1/start"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))

            verify(exactly = 1) { competitionService.start(1L) }
        }
    }

    @Nested
    @DisplayName("POST /api/backoffice/competitions/{id}/complete")
    inner class CompleteCompetition {
        @Test
        fun `should complete competition`() {
            // given
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association)
            val competition =
                createCompetition(1L, "2025 춘계대회", league, 2025, 1).apply {
                    start()
                    complete(LocalDate.now())
                }
            every { competitionService.complete(1L, any()) } returns competition

            // when & then
            mockMvc
                .perform(post("/api/backoffice/competitions/1/complete"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))

            verify(exactly = 1) { competitionService.complete(1L, any()) }
        }
    }

    @Nested
    @DisplayName("GET /api/backoffice/competitions/{id}/season-summary")
    inner class GetSeasonSummary {
        @Test
        fun `should return season summary for completed competition`() {
            // given
            val summary =
                SeasonSummaryDto(
                    competitionId = 1L,
                    competitionName = "2025 춘계대회",
                    year = 2025,
                    season = 1,
                    startDate = LocalDate.of(2025, 3, 1),
                    endDate = LocalDate.of(2025, 6, 30),
                    totalTeams = 2,
                    totalPlayers = 30,
                    finalStandings =
                        listOf(
                            TeamStandingDto(
                                rank = 1,
                                teamId = 1L,
                                teamName = "한강 타이거즈",
                                gamesPlayed = 10,
                                remainingGames = 0,
                                wins = 8,
                                losses = 2,
                                draws = 0,
                                winningPercentage = BigDecimal("0.800"),
                                gamesBehind = BigDecimal.ZERO,
                                runsScored = 50,
                                runsAllowed = 30,
                                runDifferential = 20,
                            ),
                        ),
                )

            every { seasonTransitionService.getSeasonSummary(1L) } returns summary

            // when & then
            mockMvc
                .perform(get("/api/backoffice/competitions/1/season-summary"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.competitionId").value(1))
                .andExpect(jsonPath("$.data.competitionName").value("2025 춘계대회"))
                .andExpect(jsonPath("$.data.totalTeams").value(2))
                .andExpect(jsonPath("$.data.totalPlayers").value(30))
                .andExpect(jsonPath("$.data.finalStandings[0].rank").value(1))

            verify(exactly = 1) { seasonTransitionService.getSeasonSummary(1L) }
        }
    }

    @Nested
    @DisplayName("POST /api/backoffice/competitions/{id}/prepare-next-season")
    inner class PrepareNextSeason {
        @Test
        fun `should prepare next season successfully`() {
            // given
            val request =
                PrepareNextSeasonRequest(
                    name = "2025 추계대회",
                    startDate = LocalDate.of(2025, 9, 1),
                    endDate = LocalDate.of(2025, 11, 30),
                    description = "2025년 추계 시즌",
                )
            val result =
                NextSeasonPreparationResult(
                    newCompetitionId = 2L,
                    newCompetitionName = "2025 추계대회",
                    year = 2025,
                    season = 2,
                    previousCompetitionId = 1L,
                    registeredTeamCount = 4,
                    registeredPlayerCount = 60,
                    skippedPlayerCount = 5,
                )

            every {
                seasonTransitionService.prepareNextSeason(
                    previousCompetitionId = 1L,
                    name = "2025 추계대회",
                    startDate = LocalDate.of(2025, 9, 1),
                    endDate = LocalDate.of(2025, 11, 30),
                    description = "2025년 추계 시즌",
                    maxTeams = null,
                )
            } returns result

            // when & then
            mockMvc
                .perform(
                    post("/api/backoffice/competitions/1/prepare-next-season")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.newCompetitionId").value(2))
                .andExpect(jsonPath("$.data.newCompetitionName").value("2025 추계대회"))
                .andExpect(jsonPath("$.data.year").value(2025))
                .andExpect(jsonPath("$.data.season").value(2))
                .andExpect(jsonPath("$.data.registeredTeamCount").value(4))
                .andExpect(jsonPath("$.data.registeredPlayerCount").value(60))
                .andExpect(jsonPath("$.data.skippedPlayerCount").value(5))

            verify(exactly = 1) {
                seasonTransitionService.prepareNextSeason(
                    previousCompetitionId = 1L,
                    name = "2025 추계대회",
                    startDate = LocalDate.of(2025, 9, 1),
                    endDate = LocalDate.of(2025, 11, 30),
                    description = "2025년 추계 시즌",
                    maxTeams = null,
                )
            }
        }
    }

    private fun createAssociation(
        id: Long,
        name: String,
    ): Association =
        Association(
            name = name,
            abbreviation = null,
            region = "서울",
            description = null,
            logoUrl = null,
            websiteUrl = null,
        ).apply {
            val idField = Association::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    private fun createLeague(
        id: Long,
        name: String,
        association: Association,
    ): League =
        League(
            association = association,
            name = name,
            abbreviation = null,
            foundedYear = 2020,
            divisionLevel = 1,
            description = null,
            logoUrl = null,
        ).apply {
            val idField = League::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    private fun createCompetition(
        id: Long,
        name: String,
        league: League,
        year: Int,
        season: Int,
    ): Competition =
        Competition(
            league = league,
            name = name,
            year = year,
            season = season,
            type = CompetitionType.LEAGUE,
            startDate = LocalDate.of(year, 3, 1),
            endDate = LocalDate.of(year, 6, 30),
            status = CompetitionStatus.SCHEDULED,
            description = null,
            maxTeams = null,
        ).apply {
            val idField = Competition::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
}
