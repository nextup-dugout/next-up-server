package com.nextup.backoffice.controller.team

import com.fasterxml.jackson.databind.ObjectMapper
import com.nextup.backoffice.dto.team.UpdateMemberStatusRequest
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.team.TeamMemberRole
import com.nextup.core.domain.team.TeamMemberStatus
import com.nextup.core.domain.user.User
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import com.nextup.core.service.audit.AuditService
import com.nextup.core.service.team.TeamMembershipService
import com.nextup.infrastructure.security.userdetails.CustomUserDetails
import io.mockk.every
import io.mockk.justRun
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@DisplayName("TeamMemberAdminController")
class TeamMemberAdminControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var teamMembershipService: TeamMembershipService
    private lateinit var teamMemberRepository: TeamMemberRepositoryPort
    private lateinit var auditService: AuditService
    private lateinit var controller: TeamMemberAdminController
    private lateinit var objectMapper: ObjectMapper

    private lateinit var team: Team
    private lateinit var user: User
    private lateinit var player: Player
    private lateinit var member: TeamMember

    @BeforeEach
    fun setUp() {
        teamMembershipService = mockk()
        teamMemberRepository = mockk()
        auditService = mockk(relaxed = true)

        val adminUserDetails = mockk<CustomUserDetails>()
        every { adminUserDetails.id } returns 1L
        every { adminUserDetails.authorities } returns emptyList()

        val authentication = UsernamePasswordAuthenticationToken(adminUserDetails, null, emptyList())
        SecurityContextHolder.getContext().authentication = authentication

        controller = TeamMemberAdminController(teamMembershipService, teamMemberRepository, auditService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(
                    PageableHandlerMethodArgumentResolver(),
                    AuthenticationPrincipalArgumentResolver(),
                )
                .build()
        objectMapper = ObjectMapper().apply { findAndRegisterModules() }

        val association = com.nextup.core.domain.association.Association(name = "서울시야구협회", region = "서울")
        val league = com.nextup.core.domain.league.League(association = association, name = "1부 리그", foundedYear = 2020)
        team = Team(league = league, name = "타이거즈", city = "서울", foundedYear = 2015)
        setId(team, Team::class.java, 1L)

        user = User.createLocalUser("test@example.com", "password", "테스터")
        setId(user, User::class.java, 10L)

        player = Player(name = "홍길동", primaryPosition = Position.SHORTSTOP)
        setId(player, Player::class.java, 100L)

        member = TeamMember.create(team, user, player, 7, TeamMemberRole.MEMBER)
        setId(member, TeamMember::class.java, 50L)
    }

    @Nested
    @DisplayName("GET /api/backoffice/teams/{teamId}/members")
    inner class GetAllMembers {
        @Test
        fun `should return paged members without status filter`() {
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(listOf(member), pageable, 1)
            every { teamMemberRepository.findByTeamIdWithUserAndPlayer(1L, any()) } returns page

            mockMvc.perform(get("/api/backoffice/teams/1/members"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].memberId").value(50))
        }

        @Test
        fun `should return members filtered by status`() {
            every { teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE) } returns listOf(member)

            mockMvc.perform(get("/api/backoffice/teams/1/members").param("status", "ACTIVE"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray)
        }
    }

    @Nested
    @DisplayName("PATCH /api/backoffice/teams/{teamId}/members/{memberId}/status")
    inner class UpdateMemberStatus {
        @Test
        fun `should suspend member`() {
            every { teamMemberRepository.findByIdOrNull(50L) } returns member
            every { teamMemberRepository.save(any()) } answers { firstArg() }
            val request = UpdateMemberStatusRequest(status = TeamMemberStatus.SUSPENDED, reason = "회비 미납")

            mockMvc.perform(
                patch("/api/backoffice/teams/1/members/50/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk).andExpect(jsonPath("$.success").value(true))

            verify { teamMemberRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("DELETE /api/backoffice/teams/{teamId}/members/{memberId}")
    inner class DeleteMember {
        @Test
        fun `should delete member`() {
            justRun { teamMemberRepository.deleteById(50L) }

            mockMvc.perform(delete("/api/backoffice/teams/1/members/50"))
                .andExpect(status().isOk).andExpect(jsonPath("$.success").value(true))

            verify { teamMemberRepository.deleteById(50L) }
        }
    }

    private fun <T> setId(
        entity: T,
        clazz: Class<*>,
        id: Long
    ) {
        val idField = clazz.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }
}
