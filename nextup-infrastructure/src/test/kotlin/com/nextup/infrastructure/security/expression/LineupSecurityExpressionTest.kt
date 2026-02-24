package com.nextup.infrastructure.security.expression

import com.nextup.core.domain.game.LineupSubmission
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.port.repository.LineupSubmissionRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("LineupSecurityExpression")
class LineupSecurityExpressionTest {
    private lateinit var lineupSubmissionRepository: LineupSubmissionRepositoryPort
    private lateinit var teamMemberRepository: TeamMemberRepositoryPort
    private lateinit var lineupSecurityExpression: LineupSecurityExpression

    private val teamId = 1L
    private val userId = 10L
    private val submissionId = 100L

    private val mockTeam =
        mockk<Team> {
            every { id } returns teamId
        }

    private val mockSubmission =
        mockk<LineupSubmission> {
            every { team } returns mockTeam
        }

    @BeforeEach
    fun setUp() {
        lineupSubmissionRepository = mockk()
        teamMemberRepository = mockk()
        lineupSecurityExpression = LineupSecurityExpression(lineupSubmissionRepository, teamMemberRepository)
    }

    private fun mockMember(canManage: Boolean): TeamMember =
        mockk {
            every { canManageMembers() } returns canManage
        }

    @Nested
    @DisplayName("canSubmit()")
    inner class CanSubmit {
        @Test
        fun `returns true when user is OWNER of the submission team`() {
            every { lineupSubmissionRepository.findByIdOrNull(submissionId) } returns mockSubmission
            every { teamMemberRepository.findByTeamIdAndUserId(teamId, userId) } returns mockMember(true)

            assertThat(lineupSecurityExpression.canSubmit(submissionId, userId)).isTrue()
        }

        @Test
        fun `returns true when user is MANAGER of the submission team`() {
            every { lineupSubmissionRepository.findByIdOrNull(submissionId) } returns mockSubmission
            every { teamMemberRepository.findByTeamIdAndUserId(teamId, userId) } returns mockMember(true)

            assertThat(lineupSecurityExpression.canSubmit(submissionId, userId)).isTrue()
        }

        @Test
        fun `returns false when user is MEMBER of the submission team`() {
            every { lineupSubmissionRepository.findByIdOrNull(submissionId) } returns mockSubmission
            every { teamMemberRepository.findByTeamIdAndUserId(teamId, userId) } returns mockMember(false)

            assertThat(lineupSecurityExpression.canSubmit(submissionId, userId)).isFalse()
        }

        @Test
        fun `returns false when submission is not found`() {
            every { lineupSubmissionRepository.findByIdOrNull(submissionId) } returns null

            assertThat(lineupSecurityExpression.canSubmit(submissionId, userId)).isFalse()
        }

        @Test
        fun `returns false when user is not a member of the submission team`() {
            every { lineupSubmissionRepository.findByIdOrNull(submissionId) } returns mockSubmission
            every { teamMemberRepository.findByTeamIdAndUserId(teamId, userId) } returns null

            assertThat(lineupSecurityExpression.canSubmit(submissionId, userId)).isFalse()
        }

        @Test
        fun `returns false when principal is null`() {
            assertThat(lineupSecurityExpression.canSubmit(submissionId, null)).isFalse()
        }
    }
}
