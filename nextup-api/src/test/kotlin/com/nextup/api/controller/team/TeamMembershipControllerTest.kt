package com.nextup.api.controller.team

import com.nextup.api.dto.team.*
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.*
import com.nextup.core.domain.user.User
import com.nextup.core.service.team.TeamMembershipService
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

@DisplayName("TeamMembershipController")
class TeamMembershipControllerTest {
    private lateinit var teamMembershipService: TeamMembershipService
    private lateinit var controller: TeamMembershipController

    private val team =
        mockk<Team> {
            every { id } returns 1L
            every { name } returns "테스트팀"
        }
    private val user =
        mockk<User> {
            every { id } returns 10L
            every { nickname } returns "testUser"
            every { email } returns "test@test.com"
        }
    private val position =
        mockk<Position> {
            every { abbreviation } returns "SS"
        }
    private val player =
        mockk<Player> {
            every { id } returns 100L
            every { name } returns "홍길동"
            every { primaryPosition } returns position
        }

    @BeforeEach
    fun setUp() {
        teamMembershipService = mockk()
        controller = TeamMembershipController(teamMembershipService)
    }

    private fun createMockJoinRequest(
        id: Long = 1L,
        status: JoinRequestStatus = JoinRequestStatus.PENDING,
    ): TeamJoinRequest =
        mockk {
            every { this@mockk.id } returns id
            every { this@mockk.team } returns this@TeamMembershipControllerTest.team
            every { this@mockk.user } returns this@TeamMembershipControllerTest.user
            every { this@mockk.player } returns this@TeamMembershipControllerTest.player
            every { desiredUniformNumber } returns 7
            every { requestMessage } returns "가입합니다"
            every { this@mockk.status } returns status
            every { requestedAt } returns LocalDateTime.of(2026, 1, 1, 12, 0)
            every { processedAt } returns null
            every { processedBy } returns null
            every { responseMessage } returns null
        }

    private fun createMockMember(
        id: Long = 50L,
        role: TeamMemberRole = TeamMemberRole.MEMBER,
    ): TeamMember =
        mockk {
            every { this@mockk.id } returns id
            every { this@mockk.team } returns this@TeamMembershipControllerTest.team
            every { this@mockk.user } returns this@TeamMembershipControllerTest.user
            every { this@mockk.player } returns this@TeamMembershipControllerTest.player
            every { this@mockk.role } returns role
            every { uniformNumber } returns 7
            every { status } returns TeamMemberStatus.ACTIVE
            every { joinedAt } returns LocalDateTime.of(2026, 1, 1, 12, 0)
            every { leftAt } returns null
        }

    @Nested
    @DisplayName("POST /api/v1/teams/{teamId}/join-requests")
    inner class RequestJoin {
        @Test
        fun `should create join request successfully`() {
            // given
            val request = JoinRequestDto(desiredUniformNumber = 7, requestMessage = "가입합니다")
            val joinRequest = createMockJoinRequest()
            every {
                teamMembershipService.requestJoin(
                    userId = 10L,
                    teamId = 1L,
                    desiredUniformNumber = 7,
                    message = "가입합니다",
                )
            } returns joinRequest

            // when
            val response = controller.requestJoin(teamId = 1L, request = request, userId = 10L)

            // then
            assertThat(response.data?.requestId).isEqualTo(1L)
            assertThat(response.data?.desiredUniformNumber).isEqualTo(7)
            assertThat(response.data?.status).isEqualTo(JoinRequestStatus.PENDING)
        }
    }

    @Nested
    @DisplayName("GET /api/v1/teams/{teamId}/join-requests")
    inner class GetJoinRequests {
        @Test
        fun `should return empty list`() {
            // when
            val response = controller.getJoinRequests(teamId = 1L, userId = 10L)

            // then
            assertThat(response.data).isEmpty()
        }
    }

    @Nested
    @DisplayName("POST /api/v1/teams/{teamId}/join-requests/{requestId}/approve")
    inner class ApproveJoinRequest {
        @Test
        fun `should approve join request and return member`() {
            // given
            val request = ApproveJoinRequestDto(uniformNumber = 7, responseMessage = "환영합니다")
            val member = createMockMember()
            every {
                teamMembershipService.approveJoinRequest(
                    requestId = 1L,
                    processorUserId = 10L,
                    finalUniformNumber = 7,
                    responseMessage = "환영합니다",
                )
            } returns member

            // when
            val response =
                controller.approveJoinRequest(
                    teamId = 1L,
                    requestId = 1L,
                    request = request,
                    processorUserId = 10L,
                )

            // then
            assertThat(response.data?.memberId).isEqualTo(50L)
            assertThat(response.data?.role).isEqualTo(TeamMemberRole.MEMBER)
        }
    }

    @Nested
    @DisplayName("POST /api/v1/teams/{teamId}/join-requests/{requestId}/reject")
    inner class RejectJoinRequest {
        @Test
        fun `should reject join request`() {
            // given
            val request = RejectJoinRequestDto(responseMessage = "정원 초과")
            val joinRequest = createMockJoinRequest(status = JoinRequestStatus.REJECTED)
            every {
                teamMembershipService.rejectJoinRequest(
                    requestId = 1L,
                    processorUserId = 10L,
                    reason = "정원 초과",
                )
            } returns joinRequest

            // when
            val response =
                controller.rejectJoinRequest(
                    teamId = 1L,
                    requestId = 1L,
                    request = request,
                    processorUserId = 10L,
                )

            // then
            assertThat(response.data?.requestId).isEqualTo(1L)
        }
    }

    @Nested
    @DisplayName("GET /api/v1/teams/{teamId}/members")
    inner class GetMembers {
        @Test
        fun `should return team members`() {
            // given
            val members = listOf(createMockMember(50L), createMockMember(51L))
            every { teamMembershipService.getRoster(1L) } returns members

            // when
            val response = controller.getMembers(teamId = 1L)

            // then
            assertThat(response.data).hasSize(2)
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/teams/{teamId}/members/{memberId}")
    inner class KickMember {
        @Test
        fun `should kick member when requester is team member`() {
            // given
            val request = KickMemberRequest(reason = "규율 위반", addToBlacklist = false)
            val kickerMember = createMockMember(id = 99L, role = TeamMemberRole.OWNER)
            every { teamMembershipService.getMember(1L, 10L) } returns kickerMember
            every { teamMembershipService.getMemberById(50L) } returns createMockMember(id = 50L)
            justRun {
                teamMembershipService.kickMember(
                    memberId = 50L,
                    kickerUserId = 10L,
                    reason = "규율 위반",
                    addToBlacklist = false,
                )
            }

            // when
            val response = controller.kickMember(teamId = 1L, memberId = 50L, request = request, kickerUserId = 10L)

            // then
            assertThat(response.success).isTrue()
            verify { teamMembershipService.kickMember(50L, 10L, "규율 위반", false) }
        }

        @Test
        fun `should throw when requester is not team member`() {
            // given
            val request = KickMemberRequest(reason = "규율 위반")
            every { teamMembershipService.getMember(1L, 10L) } returns null

            // when & then
            assertThrows<IllegalStateException> {
                controller.kickMember(teamId = 1L, memberId = 50L, request = request, kickerUserId = 10L)
            }
        }

        @Test
        fun `should throw when target member not found`() {
            // given
            val request = KickMemberRequest(reason = "규율 위반")
            val kickerMember = createMockMember(id = 99L, role = TeamMemberRole.OWNER)
            every { teamMembershipService.getMember(1L, 10L) } returns kickerMember
            every { teamMembershipService.getMemberById(50L) } returns null

            // when & then
            assertThrows<IllegalStateException> {
                controller.kickMember(teamId = 1L, memberId = 50L, request = request, kickerUserId = 10L)
            }
        }

        @Test
        fun `should throw when target member belongs to different team`() {
            // given
            val request = KickMemberRequest(reason = "규율 위반")
            val kickerMember = createMockMember(id = 99L, role = TeamMemberRole.OWNER)
            val differentTeam = mockk<Team> { every { id } returns 999L }
            val wrongTeamMember =
                mockk<TeamMember> {
                    every { id } returns 50L
                    every { team } returns differentTeam
                }
            every { teamMembershipService.getMember(1L, 10L) } returns kickerMember
            every { teamMembershipService.getMemberById(50L) } returns wrongTeamMember

            // when & then
            assertThrows<IllegalStateException> {
                controller.kickMember(teamId = 1L, memberId = 50L, request = request, kickerUserId = 10L)
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/teams/{teamId}/leave")
    inner class LeaveTeam {
        @Test
        fun `should leave team successfully`() {
            // given
            val member = createMockMember()
            every { teamMembershipService.getMember(1L, 10L) } returns member
            justRun { teamMembershipService.leaveMember(50L) }

            // when
            val response = controller.leaveTeam(teamId = 1L, userId = 10L)

            // then
            assertThat(response.success).isTrue()
            verify { teamMembershipService.leaveMember(50L) }
        }

        @Test
        fun `should throw when user is not team member`() {
            // given
            every { teamMembershipService.getMember(1L, 10L) } returns null

            // when & then
            assertThrows<IllegalStateException> {
                controller.leaveTeam(teamId = 1L, userId = 10L)
            }
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/teams/{teamId}/members/{memberId}/role")
    inner class ChangeRole {
        @Test
        fun `should change member role`() {
            // given
            val request = ChangeRoleRequest(newRole = TeamMemberRole.MANAGER)
            val changerMember = createMockMember(id = 99L, role = TeamMemberRole.OWNER)
            val updatedMember = createMockMember(id = 50L, role = TeamMemberRole.MANAGER)
            every { teamMembershipService.getMember(1L, 10L) } returns changerMember
            every { teamMembershipService.getMemberById(50L) } returns createMockMember(id = 50L)
            every {
                teamMembershipService.changeRole(
                    memberId = 50L,
                    newRole = TeamMemberRole.MANAGER,
                    changerUserId = 10L,
                )
            } returns updatedMember

            // when
            val response = controller.changeRole(teamId = 1L, memberId = 50L, request = request, changerUserId = 10L)

            // then
            assertThat(response.data?.memberId).isEqualTo(50L)
        }

        @Test
        fun `should throw when requester is not team member`() {
            // given
            val request = ChangeRoleRequest(newRole = TeamMemberRole.MANAGER)
            every { teamMembershipService.getMember(1L, 10L) } returns null

            // when & then
            assertThrows<IllegalStateException> {
                controller.changeRole(teamId = 1L, memberId = 50L, request = request, changerUserId = 10L)
            }
        }

        @Test
        fun `should throw when target member not found for role change`() {
            // given
            val request = ChangeRoleRequest(newRole = TeamMemberRole.MANAGER)
            val changerMember = createMockMember(id = 99L, role = TeamMemberRole.OWNER)
            every { teamMembershipService.getMember(1L, 10L) } returns changerMember
            every { teamMembershipService.getMemberById(50L) } returns null

            // when & then
            assertThrows<IllegalStateException> {
                controller.changeRole(teamId = 1L, memberId = 50L, request = request, changerUserId = 10L)
            }
        }

        @Test
        fun `should throw when target member belongs to different team for role change`() {
            // given
            val request = ChangeRoleRequest(newRole = TeamMemberRole.MANAGER)
            val changerMember = createMockMember(id = 99L, role = TeamMemberRole.OWNER)
            val differentTeam = mockk<Team> { every { id } returns 999L }
            val wrongTeamMember =
                mockk<TeamMember> {
                    every { id } returns 50L
                    every { team } returns differentTeam
                }
            every { teamMembershipService.getMember(1L, 10L) } returns changerMember
            every { teamMembershipService.getMemberById(50L) } returns wrongTeamMember

            // when & then
            assertThrows<IllegalStateException> {
                controller.changeRole(teamId = 1L, memberId = 50L, request = request, changerUserId = 10L)
            }
        }
    }
}
