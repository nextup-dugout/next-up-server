package com.nextup.api.controller.association

import com.nextup.core.domain.association.Association
import com.nextup.core.service.association.AssociationService
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

@DisplayName("AssociationController")
class AssociationControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var associationService: AssociationService
    private lateinit var controller: AssociationController

    @BeforeEach
    fun setUp() {
        associationService = mockk()
        controller = AssociationController(associationService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Nested
    @DisplayName("GET /api/associations")
    inner class GetAssociations {
        @Test
        fun `should return all active associations`() {
            // given
            val associations =
                listOf(
                    createAssociation(1L, "서울시야구협회", "서울"),
                    createAssociation(2L, "경기도야구협회", "경기"),
                )
            every { associationService.getAllActive() } returns associations

            // when & then
            mockMvc
                .perform(get("/api/associations"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("서울시야구협회"))
        }

        @Test
        fun `should filter by region when region parameter is provided`() {
            // given
            val associations =
                listOf(
                    createAssociation(1L, "서울시야구협회", "서울"),
                )
            every { associationService.getActiveByRegion("서울") } returns associations

            // when & then
            mockMvc
                .perform(get("/api/associations").param("region", "서울"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].region").value("서울"))
        }
    }

    @Nested
    @DisplayName("GET /api/associations/{id}")
    inner class GetAssociation {
        @Test
        fun `should return association when found`() {
            // given
            val association = createAssociation(1L, "서울시야구협회", "서울")
            every { associationService.getById(1L) } returns association

            // when & then
            mockMvc
                .perform(get("/api/associations/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("서울시야구협회"))
                .andExpect(jsonPath("$.data.region").value("서울"))
        }
    }

    private fun createAssociation(
        id: Long,
        name: String,
        region: String,
    ): Association =
        Association(
            name = name,
            abbreviation = null,
            region = region,
            description = null,
            logoUrl = null,
            websiteUrl = null,
        ).apply {
            val idField = Association::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
}
