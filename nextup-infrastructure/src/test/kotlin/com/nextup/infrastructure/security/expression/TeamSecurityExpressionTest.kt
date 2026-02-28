package com.nextup.infrastructure.security.expression

import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.team.TeamMemberRole
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("TeamSecurityExpression")
class TeamSecurityExpressionTest {
    private lateinit var teamMemberRepository: TeamMemberRepositoryPort
    private lateinit var teamSecurityExpression: TeamSecurityExpression

    @BeforeEach
    fun setUp() {
        teamMemberRepository = mockk()
        teamSecurityExpression = TeamSecurityExpression(teamMemberRepository)
    }

    private fun mockMember(role: TeamMemberRole): TeamMember =
        mockk {
            every { this@mockk.role } returns role
        }

    @Nested
    @DisplayName("isOwner()")
    inner class IsOwner {
        @Test
        fun `returns true when user is OWNER`() {
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns mockMember(TeamMemberRole.OWNER)
            assertThat(teamSecurityExpression.isOwner(1L, 10L)).isTrue()
        }

        @Test
        fun `returns false when user is MANAGER`() {
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns mockMember(TeamMemberRole.MANAGER)
            assertThat(teamSecurityExpression.isOwner(1L, 10L)).isFalse()
        }

        @Test
        fun `returns false when user is MEMBER`() {
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns mockMember(TeamMemberRole.MEMBER)
            assertThat(teamSecurityExpression.isOwner(1L, 10L)).isFalse()
        }

        @Test
        fun `returns false when user is not a team member`() {
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns null
            assertThat(teamSecurityExpression.isOwner(1L, 10L)).isFalse()
        }

        @Test
        fun `returns false when principal is null`() {
            assertThat(teamSecurityExpression.isOwner(1L, null)).isFalse()
        }

        @Test
        fun `accepts String principal`() {
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns mockMember(TeamMemberRole.OWNER)
            assertThat(teamSecurityExpression.isOwner(1L, "10")).isTrue()
        }
    }

    @Nested
    @DisplayName("isOwnerOrManager()")
    inner class IsOwnerOrManager {
        @Test
        fun `returns true when user is OWNER`() {
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns mockMember(TeamMemberRole.OWNER)
            assertThat(teamSecurityExpression.isOwnerOrManager(1L, 10L)).isTrue()
        }

        @Test
        fun `returns true when user is MANAGER`() {
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns mockMember(TeamMemberRole.MANAGER)
            assertThat(teamSecurityExpression.isOwnerOrManager(1L, 10L)).isTrue()
        }

        @Test
        fun `returns false when user is MEMBER`() {
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns mockMember(TeamMemberRole.MEMBER)
            assertThat(teamSecurityExpression.isOwnerOrManager(1L, 10L)).isFalse()
        }

        @Test
        fun `returns false when user is GUEST`() {
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns mockMember(TeamMemberRole.GUEST)
            assertThat(teamSecurityExpression.isOwnerOrManager(1L, 10L)).isFalse()
        }

        @Test
        fun `returns false when user is not a team member`() {
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns null
            assertThat(teamSecurityExpression.isOwnerOrManager(1L, 10L)).isFalse()
        }

        @Test
        fun `returns false when principal is null`() {
            assertThat(teamSecurityExpression.isOwnerOrManager(1L, null)).isFalse()
        }
    }

    @Nested
    @DisplayName("isMember()")
    inner class IsMember {
        @Test
        fun `returns true when user is any team member`() {
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns mockMember(TeamMemberRole.MEMBER)
            assertThat(teamSecurityExpression.isMember(1L, 10L)).isTrue()
        }

        @Test
        fun `returns false when user is not a team member`() {
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns null
            assertThat(teamSecurityExpression.isMember(1L, 10L)).isFalse()
        }

        @Test
        fun `returns false when principal is null`() {
            assertThat(teamSecurityExpression.isMember(1L, null)).isFalse()
        }
    }

    @Nested
    @DisplayName("principal type handling")
    inner class PrincipalTypeHandling {
        @Test
        fun `accepts Long principal`() {
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns mockMember(TeamMemberRole.OWNER)
            assertThat(teamSecurityExpression.isOwner(1L, 10L)).isTrue()
        }

        @Test
        fun `accepts Int principal as Number`() {
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns mockMember(TeamMemberRole.OWNER)
            assertThat(teamSecurityExpression.isOwner(1L, 10)).isTrue()
        }

        @Test
        fun `accepts String principal`() {
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns mockMember(TeamMemberRole.OWNER)
            assertThat(teamSecurityExpression.isOwner(1L, "10")).isTrue()
        }

        @Test
        fun `returns false for invalid String principal`() {
            assertThat(teamSecurityExpression.isOwner(1L, "not-a-number")).isFalse()
        }

        @Test
        fun `returns false for unknown principal type`() {
            assertThat(teamSecurityExpression.isOwner(1L, Any())).isFalse()
        }
    }
}
