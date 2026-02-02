package com.nextup.backoffice.controller.league

import com.fasterxml.jackson.databind.ObjectMapper
import com.nextup.backoffice.dto.league.CreateLeagueRequest
import com.nextup.backoffice.dto.league.UpdateLeagueRequest
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import com.nextup.core.service.league.LeagueService
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

@DisplayName("LeagueAdminController")
class LeagueAdminControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var leagueService: LeagueService
    private lateinit var controller: LeagueAdminController
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        leagueService = mockk()
        controller = LeagueAdminController(leagueService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
        objectMapper = ObjectMapper()
    }

    @Nested
    @DisplayName("GET /api/backoffice/leagues")
    inner class GetAllLeagues {

        @Test
        fun `should return all leagues including inactive`() {
            // given
            val association = createAssociation(1L, "서울시야구협회")
            val leagues = listOf(
                createLeague(1L, "1부 리그", association, true),
                createLeague(2L, "2부 리그", association, false)
            )
            every { leagueService.getAll() } returns leagues

            // when & then
            mockMvc.perform(get("/api/backoffice/leagues"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("1부 리그"))
                .andExpect(jsonPath("$.data[0].isActive").value(true))
                .andExpect(jsonPath("$.data[1].isActive").value(false))

            verify(exactly = 1) { leagueService.getAll() }
        }
    }

    @Nested
    @DisplayName("GET /api/backoffice/leagues/{id}")
    inner class GetLeague {

        @Test
        fun `should return league when found`() {
            // given
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association, true)
            every { leagueService.getById(1L) } returns league

            // when & then
            mockMvc.perform(get("/api/backoffice/leagues/1"))
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
    @DisplayName("POST /api/backoffice/leagues")
    inner class CreateLeague {

        @Test
        fun `should create league with valid request`() {
            // given
            val request = CreateLeagueRequest(
                associationId = 1L,
                name = "1부 리그",
                abbreviation = "1st",
                foundedYear = 2020,
                divisionLevel = 1,
                description = "최상위 리그",
                logoUrl = "https://example.com/logo.png"
            )
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association, true)

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
                post("/api/backoffice/leagues")
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
    @DisplayName("PUT /api/backoffice/leagues/{id}")
    inner class UpdateLeague {

        @Test
        fun `should update league with valid request`() {
            // given
            val request = UpdateLeagueRequest(
                description = "수정된 설명",
                logoUrl = "https://example.com/new-logo.png"
            )
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association, true)

            every {
                leagueService.update(
                    id = 1L,
                    description = "수정된 설명",
                    logoUrl = "https://example.com/new-logo.png"
                )
            } returns league

            // when & then
            mockMvc.perform(
                put("/api/backoffice/leagues/1")
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
    @DisplayName("DELETE /api/backoffice/leagues/{id}")
    inner class DeactivateLeague {

        @Test
        fun `should deactivate league`() {
            // given
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association, false)
            every { leagueService.deactivate(1L) } returns league

            // when & then
            mockMvc.perform(delete("/api/backoffice/leagues/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.isActive").value(false))

            verify(exactly = 1) { leagueService.deactivate(1L) }
        }
    }

    @Nested
    @DisplayName("POST /api/backoffice/leagues/{id}/activate")
    inner class ActivateLeague {

        @Test
        fun `should activate league`() {
            // given
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association, true)
            every { leagueService.activate(1L) } returns league

            // when & then
            mockMvc.perform(post("/api/backoffice/leagues/1/activate"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.isActive").value(true))

            verify(exactly = 1) { leagueService.activate(1L) }
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

    private fun createLeague(id: Long, name: String, association: Association, isActive: Boolean): League {
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

            if (!isActive) {
                this.deactivate()
            }
        }
    }
}
