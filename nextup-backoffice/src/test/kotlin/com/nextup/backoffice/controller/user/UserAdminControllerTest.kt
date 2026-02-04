package com.nextup.backoffice.controller.user

import com.fasterxml.jackson.databind.ObjectMapper
import com.nextup.backoffice.dto.user.CreateUserRequest
import com.nextup.backoffice.dto.user.RoleChangeRequest
import com.nextup.backoffice.dto.user.UpdateUserRequest
import com.nextup.core.domain.user.Role
import com.nextup.core.domain.user.User
import com.nextup.core.service.user.UserService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@DisplayName("UserAdminController")
class UserAdminControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var userService: UserService
    private lateinit var controller: UserAdminController
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        userService = mockk()
        controller = UserAdminController(userService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(PageableHandlerMethodArgumentResolver())
                .build()
        objectMapper =
            ObjectMapper().apply {
                findAndRegisterModules()
            }
    }

    @Nested
    @DisplayName("GET /api/backoffice/users")
    inner class GetAllUsers {
        @Test
        fun `should return all users`() {
            // given
            val users =
                listOf(
                    createTestUser(1L, "user1@example.com", "사용자1", true),
                    createTestUser(2L, "user2@example.com", "사용자2", false),
                )
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(users, pageable, 2)
            every { userService.getAll(any()) } returns page

            // when & then
            mockMvc
                .perform(get("/api/backoffice/users"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray)
                .andExpect(jsonPath("$.data.content.length()").value(2))

            verify(exactly = 1) { userService.getAll(any()) }
        }

        @Test
        fun `should filter by active status`() {
            // given
            val users =
                listOf(
                    createTestUser(1L, "user1@example.com", "사용자1", true),
                )
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(users, pageable, 1)
            every { userService.getAllByStatus(true, any()) } returns page

            // when & then
            mockMvc
                .perform(get("/api/backoffice/users").param("isActive", "true"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1))

            verify(exactly = 1) { userService.getAllByStatus(true, any()) }
        }
    }

    @Nested
    @DisplayName("GET /api/backoffice/users/search")
    inner class SearchUsers {
        @Test
        fun `should search users by keyword`() {
            // given
            val users =
                listOf(
                    createTestUser(1L, "user1@example.com", "검색결과", true),
                )
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(users, pageable, 1)
            every { userService.search("검색", any()) } returns page

            // when & then
            mockMvc
                .perform(get("/api/backoffice/users/search").param("keyword", "검색"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].nickname").value("검색결과"))

            verify(exactly = 1) { userService.search("검색", any()) }
        }
    }

    @Nested
    @DisplayName("GET /api/backoffice/users/by-role/{role}")
    inner class GetUsersByRole {
        @Test
        fun `should get users by role`() {
            // given
            val users =
                listOf(
                    createTestUser(1L, "admin@example.com", "관리자", true),
                )
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(users, pageable, 1)
            every { userService.getAllByRole(Role.ADMIN, any()) } returns page

            // when & then
            mockMvc
                .perform(get("/api/backoffice/users/by-role/ADMIN"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1))

            verify(exactly = 1) { userService.getAllByRole(Role.ADMIN, any()) }
        }
    }

    @Nested
    @DisplayName("GET /api/backoffice/users/{id}")
    inner class GetUser {
        @Test
        fun `should return user when found`() {
            // given
            val user = createTestUser(1L, "user@example.com", "사용자", true)
            every { userService.getById(1L) } returns user

            // when & then
            mockMvc
                .perform(get("/api/backoffice/users/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("사용자"))

            verify(exactly = 1) { userService.getById(1L) }
        }
    }

    @Nested
    @DisplayName("POST /api/backoffice/users")
    inner class CreateUser {
        @Test
        fun `should create user with valid request`() {
            // given
            val request =
                CreateUserRequest(
                    email = "newuser@example.com",
                    password = "password123",
                    nickname = "새사용자",
                    roles = setOf("USER"),
                )
            val user = createTestUser(1L, "newuser@example.com", "새사용자", true)

            every {
                userService.createLocalUser(
                    email = "newuser@example.com",
                    encodedPassword = "password123",
                    nickname = "새사용자",
                )
            } returns user
            every { userService.getById(1L) } returns user

            // when & then
            mockMvc
                .perform(
                    post("/api/backoffice/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("newuser@example.com"))

            verify(exactly = 1) {
                userService.createLocalUser(
                    email = "newuser@example.com",
                    encodedPassword = "password123",
                    nickname = "새사용자",
                )
            }
        }

        @Test
        fun `should create user with admin role`() {
            // given
            val request =
                CreateUserRequest(
                    email = "admin@example.com",
                    password = "password123",
                    nickname = "관리자",
                    roles = setOf("USER", "ADMIN"),
                )
            val user = createTestUser(1L, "admin@example.com", "관리자", true)

            every {
                userService.createLocalUser(
                    email = "admin@example.com",
                    encodedPassword = "password123",
                    nickname = "관리자",
                )
            } returns user
            every { userService.addRole(1L, Role.ADMIN) } returns user
            every { userService.getById(1L) } returns user

            // when & then
            mockMvc
                .perform(
                    post("/api/backoffice/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))

            verify(exactly = 1) { userService.addRole(1L, Role.ADMIN) }
        }
    }

    @Nested
    @DisplayName("PUT /api/backoffice/users/{id}")
    inner class UpdateUser {
        @Test
        fun `should update user with valid request`() {
            // given
            val request =
                UpdateUserRequest(
                    nickname = "수정된닉네임",
                    profileImageUrl = "https://example.com/image.jpg",
                )
            val user = createTestUser(1L, "user@example.com", "수정된닉네임", true)

            every {
                userService.updateProfile(
                    userId = 1L,
                    nickname = "수정된닉네임",
                    profileImageUrl = "https://example.com/image.jpg",
                )
            } returns user
            every { userService.getById(1L) } returns user

            // when & then
            mockMvc
                .perform(
                    put("/api/backoffice/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))

            verify(exactly = 1) {
                userService.updateProfile(
                    userId = 1L,
                    nickname = "수정된닉네임",
                    profileImageUrl = "https://example.com/image.jpg",
                )
            }
        }

        @Test
        fun `should update user with role changes`() {
            // given
            val request =
                UpdateUserRequest(
                    nickname = "수정된닉네임",
                    roles = setOf("USER", "ADMIN"),
                )
            val userWithRoles = createTestUserWithRoles(1L, "user@example.com", "수정된닉네임", setOf(Role.USER, Role.SCORER))

            every {
                userService.updateProfile(
                    userId = 1L,
                    nickname = "수정된닉네임",
                    profileImageUrl = null,
                )
            } returns userWithRoles
            every { userService.removeRole(1L, Role.SCORER) } returns userWithRoles
            every { userService.addRole(1L, Role.ADMIN) } returns userWithRoles
            every { userService.getById(1L) } returns userWithRoles

            // when & then
            mockMvc
                .perform(
                    put("/api/backoffice/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))

            verify(exactly = 1) { userService.removeRole(1L, Role.SCORER) }
            verify(exactly = 1) { userService.addRole(1L, Role.ADMIN) }
        }
    }

    @Nested
    @DisplayName("POST /api/backoffice/users/{id}/roles")
    inner class AddRole {
        @Test
        fun `should add role to user`() {
            // given
            val request = RoleChangeRequest(role = "ADMIN")
            val user = createTestUser(1L, "user@example.com", "사용자", true)

            every { userService.addRole(1L, Role.ADMIN) } returns user

            // when & then
            mockMvc
                .perform(
                    post("/api/backoffice/users/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))

            verify(exactly = 1) { userService.addRole(1L, Role.ADMIN) }
        }
    }

    @Nested
    @DisplayName("DELETE /api/backoffice/users/{id}/roles/{role}")
    inner class RemoveRole {
        @Test
        fun `should remove role from user`() {
            // given
            val user = createTestUser(1L, "user@example.com", "사용자", true)
            every { userService.removeRole(1L, Role.ADMIN) } returns user

            // when & then
            mockMvc
                .perform(delete("/api/backoffice/users/1/roles/ADMIN"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))

            verify(exactly = 1) { userService.removeRole(1L, Role.ADMIN) }
        }
    }

    @Nested
    @DisplayName("DELETE /api/backoffice/users/{id}")
    inner class DeactivateUser {
        @Test
        fun `should deactivate user`() {
            // given
            val user = createTestUser(1L, "user@example.com", "사용자", false)
            every { userService.deactivate(1L) } returns user

            // when & then
            mockMvc
                .perform(delete("/api/backoffice/users/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.isActive").value(false))

            verify(exactly = 1) { userService.deactivate(1L) }
        }
    }

    @Nested
    @DisplayName("POST /api/backoffice/users/{id}/activate")
    inner class ActivateUser {
        @Test
        fun `should activate user`() {
            // given
            val user = createTestUser(1L, "user@example.com", "사용자", true)
            every { userService.activate(1L) } returns user

            // when & then
            mockMvc
                .perform(post("/api/backoffice/users/1/activate"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.isActive").value(true))

            verify(exactly = 1) { userService.activate(1L) }
        }
    }

    private fun createTestUser(
        id: Long,
        email: String,
        nickname: String,
        isActive: Boolean,
    ): User {
        val user =
            User.createLocalUser(
                email = email,
                encodedPassword = "encoded",
                nickname = nickname,
            )
        val idField = User::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(user, id)

        if (!isActive) {
            user.deactivate()
        }

        return user
    }

    private fun createTestUserWithRoles(
        id: Long,
        email: String,
        nickname: String,
        roles: Set<Role>,
    ): User {
        val user =
            User.createLocalUser(
                email = email,
                encodedPassword = "encoded",
                nickname = nickname,
            )
        val idField = User::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(user, id)

        // Add additional roles using the private _roles field
        val rolesField = User::class.java.getDeclaredField("_roles")
        rolesField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val userRoles = rolesField.get(user) as MutableSet<Role>
        userRoles.addAll(roles)

        return user
    }
}
