package com.nextup.api.controller.league

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import com.nextup.infrastructure.service.league.LeagueService
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

@DisplayName("LeagueController")
class LeagueControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var leagueService: LeagueService
    private lateinit var controller: LeagueController

    @BeforeEach
    fun setUp() {
        leagueService = mockk()
        controller = LeagueController(leagueService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Nested
    @DisplayName("GET /api/leagues")
    inner class GetLeagues {

        @Test
        fun `should return all active leagues`() {
            // given
            val association = createAssociation(1L, "서울시야구협회")
            val leagues = listOf(
                createLeague(1L, "1부 리그", association),
                createLeague(2L, "2부 리그", association)
            )
            every { leagueService.getAllActive() } returns leagues

            // when & then
            mockMvc.perform(get("/api/leagues"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("1부 리그"))
        }
    }

    @Nested
    @DisplayName("GET /api/leagues/{id}")
    inner class GetLeague {

        @Test
        fun `should return league when found`() {
            // given
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association)
            every { leagueService.getById(1L) } returns league

            // when & then
            mockMvc.perform(get("/api/leagues/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("1부 리그"))
                .andExpect(jsonPath("$.data.associationId").value(1))
                .andExpect(jsonPath("$.data.associationName").value("서울시야구협회"))
        }
    }

    @Nested
    @DisplayName("GET /api/associations/{associationId}/leagues")
    inner class GetLeaguesByAssociation {

        @Test
        fun `should return leagues by association`() {
            // given
            val associationId = 1L
            val association = createAssociation(associationId, "서울시야구협회")
            val leagues = listOf(
                createLeague(1L, "1부 리그", association),
                createLeague(2L, "2부 리그", association)
            )
            every { leagueService.getActiveByAssociationId(associationId) } returns leagues

            // when & then
            mockMvc.perform(get("/api/associations/$associationId/leagues"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].associationId").value(associationId))
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
}
