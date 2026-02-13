package com.nextup.backoffice.controller.association

import com.fasterxml.jackson.databind.ObjectMapper
import com.nextup.backoffice.dto.association.CreateAssociationRequest
import com.nextup.backoffice.dto.association.UpdateAssociationRequest
import com.nextup.core.domain.association.Association
import com.nextup.core.service.association.AssociationService
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

@DisplayName("AssociationAdminController")
class AssociationAdminControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var associationService: AssociationService
    private lateinit var controller: AssociationAdminController
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        associationService = mockk()
        controller = AssociationAdminController(associationService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
        objectMapper = ObjectMapper()
    }

    @Nested
    @DisplayName("GET /api/backoffice/associations")
    inner class GetAllAssociations {
        @Test
        fun `should return all associations including inactive`() {
            // given
            val associations =
                listOf(
                    createAssociation(1L, "서울시야구협회", true),
                    createAssociation(2L, "경기도야구협회", false),
                )
            every { associationService.getAll() } returns associations

            // when & then
            mockMvc
                .perform(get("/api/backoffice/associations"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("서울시야구협회"))
                .andExpect(jsonPath("$.data[0].isActive").value(true))
                .andExpect(jsonPath("$.data[1].isActive").value(false))

            verify(exactly = 1) { associationService.getAll() }
        }
    }

    @Nested
    @DisplayName("GET /api/backoffice/associations/{id}")
    inner class GetAssociation {
        @Test
        fun `should return association when found`() {
            // given
            val association = createAssociation(1L, "서울시야구협회", true)
            every { associationService.getById(1L) } returns association

            // when & then
            mockMvc
                .perform(get("/api/backoffice/associations/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("서울시야구협회"))
                .andExpect(jsonPath("$.data.isActive").value(true))

            verify(exactly = 1) { associationService.getById(1L) }
        }
    }

    @Nested
    @DisplayName("POST /api/backoffice/associations")
    inner class CreateAssociation {
        @Test
        fun `should create association with valid request`() {
            // given
            val request =
                CreateAssociationRequest(
                    name = "서울시야구협회",
                    abbreviation = "SBA",
                    region = "서울",
                    description = "서울시 사회인 야구 협회",
                    logoUrl = "https://example.com/logo.png",
                    websiteUrl = "https://example.com",
                )
            val association = createAssociation(1L, "서울시야구협회", true)

            every {
                associationService.create(
                    name = "서울시야구협회",
                    abbreviation = "SBA",
                    region = "서울",
                    description = "서울시 사회인 야구 협회",
                    logoUrl = "https://example.com/logo.png",
                    websiteUrl = "https://example.com",
                )
            } returns association

            // when & then
            mockMvc
                .perform(
                    post("/api/backoffice/associations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("서울시야구협회"))

            verify(exactly = 1) {
                associationService.create(
                    name = "서울시야구협회",
                    abbreviation = "SBA",
                    region = "서울",
                    description = "서울시 사회인 야구 협회",
                    logoUrl = "https://example.com/logo.png",
                    websiteUrl = "https://example.com",
                )
            }
        }
    }

    @Nested
    @DisplayName("PUT /api/backoffice/associations/{id}")
    inner class UpdateAssociation {
        @Test
        fun `should update association with valid request`() {
            // given
            val request =
                UpdateAssociationRequest(
                    description = "수정된 설명",
                    logoUrl = "https://example.com/new-logo.png",
                    websiteUrl = "https://example.com/new",
                )
            val association = createAssociation(1L, "서울시야구협회", true)

            every {
                associationService.update(
                    id = 1L,
                    description = "수정된 설명",
                    logoUrl = "https://example.com/new-logo.png",
                    websiteUrl = "https://example.com/new",
                )
            } returns association

            // when & then
            mockMvc
                .perform(
                    put("/api/backoffice/associations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))

            verify(exactly = 1) {
                associationService.update(
                    id = 1L,
                    description = "수정된 설명",
                    logoUrl = "https://example.com/new-logo.png",
                    websiteUrl = "https://example.com/new",
                )
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/backoffice/associations/{id}")
    inner class DeactivateAssociation {
        @Test
        fun `should deactivate association`() {
            // given
            val association = createAssociation(1L, "서울시야구협회", false)
            every { associationService.deactivate(1L) } returns association

            // when & then
            mockMvc
                .perform(delete("/api/backoffice/associations/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.isActive").value(false))

            verify(exactly = 1) { associationService.deactivate(1L) }
        }
    }

    @Nested
    @DisplayName("POST /api/backoffice/associations/{id}/activate")
    inner class ActivateAssociation {
        @Test
        fun `should activate association`() {
            // given
            val association = createAssociation(1L, "서울시야구협회", true)
            every { associationService.activate(1L) } returns association

            // when & then
            mockMvc
                .perform(post("/api/backoffice/associations/1/activate"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.isActive").value(true))

            verify(exactly = 1) { associationService.activate(1L) }
        }
    }

    private fun createAssociation(
        id: Long,
        name: String,
        isActive: Boolean,
    ): Association =
        Association(
            name = name,
            abbreviation = "SBA",
            region = "서울",
            description = "협회 설명",
            logoUrl = "https://example.com/logo.png",
            websiteUrl = "https://example.com",
        ).apply {
            val idField = Association::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)

            if (!isActive) {
                this.deactivate()
            }
        }
}
