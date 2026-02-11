package com.nextup.scorer.controller.league

import com.fasterxml.jackson.databind.ObjectMapper
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import com.nextup.core.service.league.LeagueService
import com.nextup.scorer.dto.league.CreateLeagueRequest
import com.nextup.scorer.dto.league.UpdateLeagueRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@DisplayName("LeagueController (Scorer)")
class LeagueControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var leagueService: LeagueService
    private lateinit var controller: LeagueController
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        leagueService = mockk()
        controller = LeagueController(leagueService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
        objectMapper = ObjectMapper()
    }

    @Nested
    @DisplayName("GET /api/scorer/leagues")
    inner class GetAllLeagues {

        @Test
        fun `should return all leagues when no filter provided`() {
            // given
            val association = createAssociation(1L, "서울시야구협회")
            val leagues =
                listOf(
                    createLeague(1L, "1부 리그", association),
                    createLeague(2L, "2부 리그", association)
                )
            every { leagueService.getAll() } returns leagues

            // when & then
            mockMvc.perform(get("/api/scorer/leagues"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("1부 리그"))
                .andExpect(jsonPath("$.data[0].associationName").value("서울시야구협회"))

            verify(exactly = 1) { leagueService.getAll() }
        }

        @Test
        fun `should return leagues filtered by associationId`() {
            // given
            val associationId = 1L
            val association = createAssociation(associationId, "서울시야구협회")
            val leagues =
                listOf(
                    createLeague(1L, "1부 리그", association)
                )
            every { leagueService.getByAssociationId(associationId) } returns leagues

            // when & then
            mockMvc.perform(
                get("/api/scorer/leagues")
                    .param("associationId", associationId.toString())
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].associationId").value(associationId))

            verify(exactly = 1) { leagueService.getByAssociationId(associationId) }
        }
    }

    @Nested
    @DisplayName("GET /api/scorer/leagues/{id}")
    inner class GetLeague {

        @Test
        fun `should return league when found`() {
            // given
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association)
            every { leagueService.getById(1L) } returns league

            // when & then
            mockMvc.perform(get("/api/scorer/leagues/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("1부 리그"))
                .andExpect(jsonPath("$.data.associationId").value(1))
                .andExpect(jsonPath("$.data.isActive").value(true))

            verify(exactly = 1) { leagueService.getById(1L) }
        }
    }

    @Nested
    @DisplayName("POST /api/scorer/leagues")
    inner class CreateLeague {

        @Test
        fun `should create league with valid request`() {
            // given
            val request =
                CreateLeagueRequest(
                    associationId = 1L,
                    name = "1부 리그",
                    abbreviation = "1st",
                    foundedYear = 2020,
                    divisionLevel = 1,
                    description = "최상위 리그",
                    logoUrl = "https://example.com/logo.png"
                )
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association)

            every {
                leagueService.create(
                    associationId = 1L,
                    name = "1부 리그",
                    abbreviation = "1st",
                    foundedYear = 2020,
                    divisionLevel = 1,
                    description = "최상위 리그",
                    logoUrl = "https://example.com/logo.png"
                )
            } returns league

            // when & then
            mockMvc.perform(
                post("/api/scorer/leagues")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("1부 리그"))

            verify(exactly = 1) {
                leagueService.create(
                    associationId = 1L,
                    name = "1부 리그",
                    abbreviation = "1st",
                    foundedYear = 2020,
                    divisionLevel = 1,
                    description = "최상위 리그",
                    logoUrl = "https://example.com/logo.png"
                )
            }
        }
    }

    @Nested
    @DisplayName("PUT /api/scorer/leagues/{id}")
    inner class UpdateLeague {

        @Test
        fun `should update league with valid request`() {
            // given
            val request =
                UpdateLeagueRequest(
                    description = "수정된 설명",
                    logoUrl = "https://example.com/new-logo.png"
                )
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association)

            every {
                leagueService.update(
                    id = 1L,
                    description = "수정된 설명",
                    logoUrl = "https://example.com/new-logo.png"
                )
            } returns league

            // when & then
            mockMvc.perform(
                put("/api/scorer/leagues/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))

            verify(exactly = 1) {
                leagueService.update(
                    id = 1L,
                    description = "수정된 설명",
                    logoUrl = "https://example.com/new-logo.png"
                )
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/scorer/leagues/{id}")
    inner class DeactivateLeague {

        @Test
        fun `should deactivate league`() {
            // given
            val association = createAssociation(1L, "서울시야구협회")
            val league =
                createLeague(1L, "1부 리그", association).apply {
                    deactivate()
                }
            every { leagueService.deactivate(1L) } returns league

            // when & then
            mockMvc.perform(delete("/api/scorer/leagues/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.isActive").value(false))

            verify(exactly = 1) { leagueService.deactivate(1L) }
        }
    }

    @Nested
    @DisplayName("POST /api/scorer/leagues/{id}/activate")
    inner class ActivateLeague {

        @Test
        fun `should activate league`() {
            // given
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association)
            every { leagueService.activate(1L) } returns league

            // when & then
            mockMvc.perform(post("/api/scorer/leagues/1/activate"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.isActive").value(true))

            verify(exactly = 1) { leagueService.activate(1L) }
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
}
