package com.nextup.scorer.controller.league

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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@DisplayName("LeagueController (Scorer)")
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
            mockMvc.perform(get("/api/v1/scorer/leagues"))
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
                get("/api/v1/scorer/leagues")
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
            mockMvc.perform(get("/api/v1/scorer/leagues/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("1부 리그"))
                .andExpect(jsonPath("$.data.associationId").value(1))
                .andExpect(jsonPath("$.data.isActive").value(true))

            verify(exactly = 1) { leagueService.getById(1L) }
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
