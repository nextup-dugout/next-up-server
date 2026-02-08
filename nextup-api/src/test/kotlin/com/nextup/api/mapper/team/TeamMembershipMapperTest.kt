package com.nextup.api.mapper.team

import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.*
import com.nextup.core.domain.user.User
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TeamMembershipMapperTest {
    private val team = mockk<Team> { every { id } returns 1L }
    private val user =
        mockk<User> {
            every { id } returns 10L
            every { nickname } returns "testUser"
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

    @Test
    fun `should map TeamJoinRequest to JoinRequestResponse`() {
        // given
        val joinRequest =
            mockk<TeamJoinRequest> {
                every { id } returns 5L
                every { this@mockk.team } returns this@TeamMembershipMapperTest.team
                every { this@mockk.user } returns this@TeamMembershipMapperTest.user
                every { this@mockk.player } returns this@TeamMembershipMapperTest.player
                every { desiredUniformNumber } returns 7
                every { requestMessage } returns "가입 신청합니다"
                every { status } returns JoinRequestStatus.PENDING
                every { requestedAt } returns LocalDateTime.of(2026, 1, 1, 12, 0)
                every { processedAt } returns null
                every { processedBy } returns null
                every { responseMessage } returns null
            }

        // when
        val response = joinRequest.toResponse()

        // then
        assertThat(response.requestId).isEqualTo(5L)
        assertThat(response.teamId).isEqualTo(1L)
        assertThat(response.userId).isEqualTo(10L)
        assertThat(response.playerId).isEqualTo(100L)
        assertThat(response.desiredUniformNumber).isEqualTo(7)
        assertThat(response.requestMessage).isEqualTo("가입 신청합니다")
        assertThat(response.status).isEqualTo(JoinRequestStatus.PENDING)
        assertThat(response.processedAt).isNull()
        assertThat(response.processedBy).isNull()
    }

    @Test
    fun `should map approved TeamJoinRequest with processor`() {
        // given
        val processor =
            mockk<User> {
                every { id } returns 20L
            }
        val processedTime = LocalDateTime.of(2026, 1, 2, 10, 0)
        val joinRequest =
            mockk<TeamJoinRequest> {
                every { id } returns 6L
                every { this@mockk.team } returns this@TeamMembershipMapperTest.team
                every { this@mockk.user } returns this@TeamMembershipMapperTest.user
                every { this@mockk.player } returns this@TeamMembershipMapperTest.player
                every { desiredUniformNumber } returns 11
                every { requestMessage } returns null
                every { status } returns JoinRequestStatus.APPROVED
                every { requestedAt } returns LocalDateTime.of(2026, 1, 1, 12, 0)
                every { processedAt } returns processedTime
                every { processedBy } returns processor
                every { responseMessage } returns "환영합니다"
            }

        // when
        val response = joinRequest.toResponse()

        // then
        assertThat(response.status).isEqualTo(JoinRequestStatus.APPROVED)
        assertThat(response.processedBy).isEqualTo(20L)
        assertThat(response.processedAt).isEqualTo(processedTime)
        assertThat(response.responseMessage).isEqualTo("환영합니다")
    }

    @Test
    fun `should map TeamMember to TeamMemberResponse`() {
        // given
        val joinedAt = LocalDateTime.of(2026, 1, 1, 12, 0)
        val member =
            mockk<TeamMember> {
                every { id } returns 50L
                every { this@mockk.team } returns this@TeamMembershipMapperTest.team
                every { this@mockk.user } returns this@TeamMembershipMapperTest.user
                every { this@mockk.player } returns this@TeamMembershipMapperTest.player
                every { role } returns TeamMemberRole.MEMBER
                every { uniformNumber } returns 7
                every { status } returns TeamMemberStatus.ACTIVE
                every { this@mockk.joinedAt } returns joinedAt
                every { leftAt } returns null
            }

        // when
        val response = member.toResponse()

        // then
        assertThat(response.memberId).isEqualTo(50L)
        assertThat(response.teamId).isEqualTo(1L)
        assertThat(response.userId).isEqualTo(10L)
        assertThat(response.playerId).isEqualTo(100L)
        assertThat(response.playerName).isEqualTo("홍길동")
        assertThat(response.role).isEqualTo(TeamMemberRole.MEMBER)
        assertThat(response.uniformNumber).isEqualTo(7)
        assertThat(response.status).isEqualTo(TeamMemberStatus.ACTIVE)
        assertThat(response.joinedAt).isEqualTo(joinedAt)
        assertThat(response.leftAt).isNull()
    }

    @Test
    fun `should map TeamMember to TeamMemberDetailResponse`() {
        // given
        val joinedAt = LocalDateTime.of(2026, 1, 1, 12, 0)
        val member =
            mockk<TeamMember> {
                every { id } returns 50L
                every { this@mockk.user } returns this@TeamMembershipMapperTest.user
                every { this@mockk.player } returns this@TeamMembershipMapperTest.player
                every { role } returns TeamMemberRole.OWNER
                every { uniformNumber } returns 1
                every { status } returns TeamMemberStatus.ACTIVE
                every { this@mockk.joinedAt } returns joinedAt
            }

        // when
        val response = member.toDetailResponse()

        // then
        assertThat(response.memberId).isEqualTo(50L)
        assertThat(response.user.userId).isEqualTo(10L)
        assertThat(response.user.nickname).isEqualTo("testUser")
        assertThat(response.player.playerId).isEqualTo(100L)
        assertThat(response.player.name).isEqualTo("홍길동")
        assertThat(response.player.primaryPosition).isEqualTo("SS")
        assertThat(response.role).isEqualTo(TeamMemberRole.OWNER)
        assertThat(response.uniformNumber).isEqualTo(1)
    }

    @Test
    fun `should map List of TeamMember to List of TeamMemberResponse`() {
        // given
        val joinedAt = LocalDateTime.of(2026, 1, 1, 12, 0)
        val member1 =
            mockk<TeamMember> {
                every { id } returns 1L
                every { this@mockk.team } returns this@TeamMembershipMapperTest.team
                every { this@mockk.user } returns this@TeamMembershipMapperTest.user
                every { this@mockk.player } returns this@TeamMembershipMapperTest.player
                every { role } returns TeamMemberRole.MEMBER
                every { uniformNumber } returns 7
                every { status } returns TeamMemberStatus.ACTIVE
                every { this@mockk.joinedAt } returns joinedAt
                every { leftAt } returns null
            }
        val member2 =
            mockk<TeamMember> {
                every { id } returns 2L
                every { this@mockk.team } returns this@TeamMembershipMapperTest.team
                every { this@mockk.user } returns this@TeamMembershipMapperTest.user
                every { this@mockk.player } returns this@TeamMembershipMapperTest.player
                every { role } returns TeamMemberRole.OWNER
                every { uniformNumber } returns 1
                every { status } returns TeamMemberStatus.ACTIVE
                every { this@mockk.joinedAt } returns joinedAt
                every { leftAt } returns null
            }

        // when
        val responses = listOf(member1, member2).toResponse()

        // then
        assertThat(responses).hasSize(2)
        assertThat(responses[0].memberId).isEqualTo(1L)
        assertThat(responses[1].memberId).isEqualTo(2L)
    }

    @Test
    fun `should map List of TeamMember to List of TeamMemberDetailResponse`() {
        // given
        val joinedAt = LocalDateTime.of(2026, 1, 1, 12, 0)
        val member =
            mockk<TeamMember> {
                every { id } returns 1L
                every { this@mockk.user } returns this@TeamMembershipMapperTest.user
                every { this@mockk.player } returns this@TeamMembershipMapperTest.player
                every { role } returns TeamMemberRole.MEMBER
                every { uniformNumber } returns 7
                every { status } returns TeamMemberStatus.ACTIVE
                every { this@mockk.joinedAt } returns joinedAt
            }

        // when
        val responses = listOf(member).toDetailResponse()

        // then
        assertThat(responses).hasSize(1)
        assertThat(responses[0].memberId).isEqualTo(1L)
    }
}
