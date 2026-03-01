package com.nextup.backoffice.controller.user

import com.fasterxml.jackson.databind.ObjectMapper
import com.nextup.backoffice.dto.user.CreateUserRequest
import com.nextup.backoffice.dto.user.RoleChangeRequest
import com.nextup.backoffice.dto.user.UpdateUserRequest
import com.nextup.core.domain.user.Role
import com.nextup.core.domain.user.User
import com.nextup.core.service.audit.AuditService
import com.nextup.core.service.user.UserService
import com.nextup.infrastructure.security.userdetails.CustomUserDetails
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
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
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var auditService: AuditService
    private lateinit var controller: UserAdminController
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        userService = mockk()
        passwordEncoder = mockk()
        auditService = mockk(relaxed = true)
        every { passwordEncoder.encode(any()) } answers { "encoded_${firstArg<String>()}" }

        val adminUserDetails = mockk<CustomUserDetails>()
        every { adminUserDetails.id } returns 1L
        every { adminUserDetails.authorities } returns emptyList()

        val authentication = UsernamePasswordAuthenticationToken(adminUserDetails, null, emptyList())
        SecurityContextHolder.getContext().authentication = authentication

        controller = UserAdminController(userService, passwordEncoder, auditService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(
                    PageableHandlerMethodArgumentResolver(),
                    AuthenticationPrincipalArgumentResolver(),
                )
                .build()
        objectMapper = ObjectMapper().apply { findAndRegisterModules() }
    }

    @Nested
    @DisplayName("GET /api/backoffice/users")
    inner class GetAllUsers {
        @Test
        fun `모든 사용자를 페이징 조회할 수 있다`() {
            val user = createUser(1L, "test@example.com", "테스터")
            val page = PageImpl(listOf(user), PageRequest.of(0, 20), 1)
            every { userService.getAll(any()) } returns page

            mockMvc.perform(get("/api/backoffice/users"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].email").value("test@example.com"))
        }

        @Test
        fun `활성 상태로 필터링하여 조회할 수 있다`() {
            val user = createUser(1L, "test@example.com", "테스터")
            val page = PageImpl(listOf(user), PageRequest.of(0, 20), 1)
            every { userService.getAllByStatus(true, any()) } returns page

            mockMvc.perform(get("/api/backoffice/users").param("isActive", "true"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
        }
    }

    @Nested
    @DisplayName("GET /api/backoffice/users/search")
    inner class SearchUsers {
        @Test
        fun `키워드로 사용자를 검색할 수 있다`() {
            val user = createUser(1L, "test@example.com", "테스터")
            val page = PageImpl(listOf(user), PageRequest.of(0, 20), 1)
            every { userService.search("test", any()) } returns page

            mockMvc.perform(get("/api/backoffice/users/search").param("keyword", "test"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
        }
    }

    @Nested
    @DisplayName("GET /api/backoffice/users/{id}")
    inner class GetUser {
        @Test
        fun `특정 사용자를 조회할 수 있다`() {
            val user = createUser(1L, "test@example.com", "테스터")
            every { userService.getById(1L) } returns user

            mockMvc.perform(get("/api/backoffice/users/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
        }
    }

    @Nested
    @DisplayName("POST /api/backoffice/users")
    inner class CreateUser {
        @Test
        fun `새 사용자를 생성할 수 있다`() {
            val user = createUser(1L, "new@example.com", "신규유저")
            val request =
                CreateUserRequest(
                    email = "new@example.com",
                    password = "password123",
                    nickname = "신규유저",
                    roles = setOf("USER"),
                )
            every { userService.createLocalUser(any(), any(), any()) } returns user
            every { userService.getById(any()) } returns user

            mockMvc.perform(
                post("/api/backoffice/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
        }

        @Test
        fun `ADMIN 역할로 사용자를 생성하면 addRole이 호출된다`() {
            val user = createUser(1L, "admin@example.com", "관리자")
            val request =
                CreateUserRequest(
                    email = "admin@example.com",
                    password = "password123",
                    nickname = "관리자",
                    roles = setOf("USER", "ADMIN"),
                )
            every { userService.createLocalUser(any(), any(), any()) } returns user
            every { userService.addRole(1L, Role.ADMIN) } returns user
            every { userService.getById(any()) } returns user

            mockMvc.perform(
                post("/api/backoffice/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))

            io.mockk.verify(exactly = 1) { userService.addRole(1L, Role.ADMIN) }
        }
    }

    @Nested
    @DisplayName("PUT /api/backoffice/users/{id}")
    inner class UpdateUser {
        @Test
        fun `사용자 정보를 수정할 수 있다`() {
            val user = createUser(1L, "test@example.com", "수정된이름")
            val request = UpdateUserRequest(nickname = "수정된이름", profileImageUrl = null, roles = null)
            every { userService.updateProfile(userId = 1L, nickname = "수정된이름", profileImageUrl = null) } returns user
            every { userService.getById(1L) } returns user

            mockMvc.perform(
                put("/api/backoffice/users/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk).andExpect(jsonPath("$.success").value(true))
        }

        @Test
        fun `역할 변경을 포함하여 사용자 정보를 수정할 수 있다`() {
            val user = createUserWithRoles(1L, "test@example.com", "수정된이름", setOf(Role.USER, Role.SCORER))
            val request =
                UpdateUserRequest(
                    nickname = "수정된이름",
                    profileImageUrl = null,
                    roles = setOf("USER", "ADMIN"),
                )
            every {
                userService.updateProfile(userId = 1L, nickname = "수정된이름", profileImageUrl = null)
            } returns user
            every { userService.removeRole(1L, Role.SCORER) } returns user
            every { userService.addRole(1L, Role.ADMIN) } returns user
            every { userService.getById(1L) } returns user

            mockMvc.perform(
                put("/api/backoffice/users/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk).andExpect(jsonPath("$.success").value(true))

            io.mockk.verify(exactly = 1) { userService.removeRole(1L, Role.SCORER) }
            io.mockk.verify(exactly = 1) { userService.addRole(1L, Role.ADMIN) }
        }
    }

    @Nested
    @DisplayName("POST /api/backoffice/users/{id}/roles")
    inner class AddRole {
        @Test
        fun `사용자에게 역할을 부여할 수 있다`() {
            val user = createUser(1L, "test@example.com", "테스터")
            val request = RoleChangeRequest(role = "ADMIN")
            every { userService.addRole(1L, Role.ADMIN) } returns user

            mockMvc.perform(
                post("/api/backoffice/users/1/roles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk).andExpect(jsonPath("$.success").value(true))
        }
    }

    @Nested
    @DisplayName("DELETE /api/backoffice/users/{id}/roles/{role}")
    inner class RemoveRole {
        @Test
        fun `사용자의 역할을 제거할 수 있다`() {
            val user = createUser(1L, "test@example.com", "테스터")
            every { userService.removeRole(1L, Role.ADMIN) } returns user

            mockMvc.perform(delete("/api/backoffice/users/1/roles/ADMIN"))
                .andExpect(status().isOk).andExpect(jsonPath("$.success").value(true))
        }
    }

    @Nested
    @DisplayName("DELETE /api/backoffice/users/{id}")
    inner class DeactivateUser {
        @Test
        fun `사용자를 비활성화할 수 있다`() {
            val user = createUser(1L, "test@example.com", "테스터")
            every { userService.deactivate(1L) } returns user

            mockMvc.perform(delete("/api/backoffice/users/1"))
                .andExpect(status().isOk).andExpect(jsonPath("$.success").value(true))
        }
    }

    @Nested
    @DisplayName("POST /api/backoffice/users/{id}/activate")
    inner class ActivateUser {
        @Test
        fun `사용자를 활성화할 수 있다`() {
            val user = createUser(1L, "test@example.com", "테스터")
            every { userService.activate(1L) } returns user

            mockMvc.perform(post("/api/backoffice/users/1/activate"))
                .andExpect(status().isOk).andExpect(jsonPath("$.success").value(true))
        }
    }

    private fun createUser(
        id: Long,
        email: String,
        nickname: String,
    ): User {
        val user = User.createLocalUser(email = email, encodedPassword = "encoded_password", nickname = nickname)
        val idField = User::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(user, id)
        return user
    }

    private fun createUserWithRoles(
        id: Long,
        email: String,
        nickname: String,
        roles: Set<Role>,
    ): User {
        val user = User.createLocalUser(email = email, encodedPassword = "encoded_password", nickname = nickname)
        val idField = User::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(user, id)
        val rolesField = User::class.java.getDeclaredField("_roles")
        rolesField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val userRoles = rolesField.get(user) as MutableSet<Role>
        userRoles.addAll(roles)
        return user
    }
}
