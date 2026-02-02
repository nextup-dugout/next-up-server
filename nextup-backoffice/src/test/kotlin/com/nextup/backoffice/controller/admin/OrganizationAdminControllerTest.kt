package com.nextup.backoffice.controller.admin

import com.fasterxml.jackson.databind.ObjectMapper
import com.nextup.backoffice.dto.admin.AssignAdminRequest
import com.nextup.backoffice.dto.admin.ChangeRoleRequest
import com.nextup.core.domain.admin.OrganizationAdmin
import com.nextup.core.domain.admin.OrganizationRole
import com.nextup.core.domain.admin.OrganizationType
import com.nextup.core.domain.user.User
import com.nextup.infrastructure.service.admin.OrganizationAdminService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@DisplayName("OrganizationAdminController")
class OrganizationAdminControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var organizationAdminService: OrganizationAdminService
    private lateinit var controller: OrganizationAdminController
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        organizationAdminService = mockk()
        controller = OrganizationAdminController(organizationAdminService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
        objectMapper = ObjectMapper()
    }

    @Nested
    @DisplayName("POST /api/backoffice/organizations/{type}/{id}/admins")
    inner class AssignAdmin {

        @Test
        fun `관리자를 할당할 수 있다`() {
            // given
            val userId = 1L
            val user = createUser(userId, "test@example.com")
            val organizationType = OrganizationType.ASSOCIATION
            val organizationId = 1L
            val role = OrganizationRole.ADMIN
            val admin = createOrganizationAdmin(1L, user, organizationType, organizationId, role)

            val request = AssignAdminRequest(userId = userId, role = role)

            every {
                organizationAdminService.assignAdmin(
                    userId = userId,
                    organizationType = organizationType,
                    organizationId = organizationId,
                    role = role,
                    assignedBy = null
                )
            } returns admin

            // when & then
            mockMvc.perform(
                post("/api/backoffice/organizations/ASSOCIATION/1/admins")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.organizationType").value("ASSOCIATION"))
                .andExpect(jsonPath("$.data.organizationId").value(organizationId))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.isActive").value(true))

            verify(exactly = 1) {
                organizationAdminService.assignAdmin(
                    userId = userId,
                    organizationType = organizationType,
                    organizationId = organizationId,
                    role = role,
                    assignedBy = null
                )
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/backoffice/organizations/{type}/{id}/admins/{userId}")
    inner class RemoveAdmin {

        @Test
        fun `관리자 권한을 해제할 수 있다`() {
            // given
            val userId = 1L
            val organizationType = OrganizationType.ASSOCIATION
            val organizationId = 1L

            every {
                organizationAdminService.removeAdmin(
                    userId = userId,
                    organizationType = organizationType,
                    organizationId = organizationId
                )
            } returns Unit

            // when & then
            mockMvc.perform(
                delete("/api/backoffice/organizations/ASSOCIATION/1/admins/$userId")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))

            verify(exactly = 1) {
                organizationAdminService.removeAdmin(
                    userId = userId,
                    organizationType = organizationType,
                    organizationId = organizationId
                )
            }
        }
    }

    @Nested
    @DisplayName("GET /api/backoffice/organizations/{type}/{id}/admins")
    inner class GetAdminsByOrganization {

        @Test
        fun `특정 조직의 관리자 목록을 조회할 수 있다`() {
            // given
            val organizationType = OrganizationType.ASSOCIATION
            val organizationId = 1L
            val user1 = createUser(1L, "user1@example.com")
            val user2 = createUser(2L, "user2@example.com")
            val admin1 = createOrganizationAdmin(1L, user1, organizationType, organizationId, OrganizationRole.ADMIN)
            val admin2 = createOrganizationAdmin(2L, user2, organizationType, organizationId, OrganizationRole.MANAGER)

            every {
                organizationAdminService.getAdminsByOrganization(organizationType, organizationId)
            } returns listOf(admin1, admin2)

            // when & then
            mockMvc.perform(
                get("/api/backoffice/organizations/ASSOCIATION/1/admins")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))

            verify(exactly = 1) {
                organizationAdminService.getAdminsByOrganization(organizationType, organizationId)
            }
        }
    }

    @Nested
    @DisplayName("GET /api/backoffice/organizations/by-user/{userId}")
    inner class GetOrganizationsByUser {

        @Test
        fun `사용자가 관리하는 모든 조직을 조회할 수 있다`() {
            // given
            val userId = 1L
            val user = createUser(userId, "test@example.com")
            val admin1 = createOrganizationAdmin(1L, user, OrganizationType.ASSOCIATION, 1L, OrganizationRole.ADMIN)
            val admin2 = createOrganizationAdmin(2L, user, OrganizationType.LEAGUE, 2L, OrganizationRole.MANAGER)

            every { organizationAdminService.getOrganizationsByUser(userId) } returns listOf(admin1, admin2)

            // when & then
            mockMvc.perform(
                get("/api/backoffice/organizations/by-user/$userId")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))

            verify(exactly = 1) { organizationAdminService.getOrganizationsByUser(userId) }
        }
    }

    @Nested
    @DisplayName("PUT /api/backoffice/organizations/{type}/{id}/admins/{userId}/role")
    inner class ChangeRole {

        @Test
        fun `관리자 역할을 변경할 수 있다`() {
            // given
            val userId = 1L
            val user = createUser(userId, "test@example.com")
            val organizationType = OrganizationType.ASSOCIATION
            val organizationId = 1L
            val newRole = OrganizationRole.MANAGER
            val admin = createOrganizationAdmin(1L, user, organizationType, organizationId, newRole)

            val request = ChangeRoleRequest(role = newRole)

            every {
                organizationAdminService.changeRole(
                    userId = userId,
                    organizationType = organizationType,
                    organizationId = organizationId,
                    newRole = newRole
                )
            } returns admin

            // when & then
            mockMvc.perform(
                put("/api/backoffice/organizations/ASSOCIATION/1/admins/$userId/role")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("MANAGER"))

            verify(exactly = 1) {
                organizationAdminService.changeRole(
                    userId = userId,
                    organizationType = organizationType,
                    organizationId = organizationId,
                    newRole = newRole
                )
            }
        }
    }

    // Helper methods

    private fun createUser(id: Long, email: String): User {
        val user = User.createLocalUser(
            email = email,
            encodedPassword = "password",
            nickname = "Test User"
        )
        setEntityId(user, id)
        return user
    }

    private fun createOrganizationAdmin(
        id: Long,
        user: User,
        organizationType: OrganizationType,
        organizationId: Long,
        role: OrganizationRole = OrganizationRole.ADMIN
    ): OrganizationAdmin {
        val admin = OrganizationAdmin.create(
            user = user,
            organizationType = organizationType,
            organizationId = organizationId,
            role = role
        )
        setEntityId(admin, id)
        return admin
    }

    private fun setEntityId(entity: Any, id: Long) {
        val idField = entity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }
}
