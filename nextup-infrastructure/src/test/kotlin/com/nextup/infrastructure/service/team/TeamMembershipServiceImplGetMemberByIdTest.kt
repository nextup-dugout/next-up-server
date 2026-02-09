package com.nextup.infrastructure.service.team

import com.nextup.core.domain.team.TeamMember
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("TeamMembershipServiceImpl.getMemberById")
class TeamMembershipServiceImplGetMemberByIdTest {
    private lateinit var teamMemberRepository: TeamMemberRepositoryPort
    private lateinit var service: TeamMembershipServiceImpl

    @BeforeEach
    fun setUp() {
        teamMemberRepository = mockk()
        service =
            TeamMembershipServiceImpl(
                teamMemberRepository,
                mockk(),
                mockk(),
                mockk(),
                mockk(),
                mockk(),
            )
    }

    @Test
    fun `should return member when found`() {
        // given
        val memberId = 50L
        val member = mockk<TeamMember>()
        every { teamMemberRepository.findByIdOrNull(memberId) } returns member

        // when
        val result = service.getMemberById(memberId)

        // then
        assertThat(result).isEqualTo(member)
        verify(exactly = 1) { teamMemberRepository.findByIdOrNull(memberId) }
    }

    @Test
    fun `should return null when not found`() {
        // given
        val memberId = 999L
        every { teamMemberRepository.findByIdOrNull(memberId) } returns null

        // when
        val result = service.getMemberById(memberId)

        // then
        assertThat(result).isNull()
        verify(exactly = 1) { teamMemberRepository.findByIdOrNull(memberId) }
    }
}
