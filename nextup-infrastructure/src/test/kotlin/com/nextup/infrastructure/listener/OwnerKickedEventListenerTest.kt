package com.nextup.infrastructure.listener

import com.nextup.core.domain.election.Election
import com.nextup.core.domain.event.OwnerKickedEvent
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.team.TeamMemberRole
import com.nextup.core.port.repository.ElectionRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("OwnerKickedEventListener 테스트")
class OwnerKickedEventListenerTest {
    private val electionRepository: ElectionRepositoryPort = mockk()
    private val teamMemberRepository: TeamMemberRepositoryPort = mockk()

    private lateinit var listener: OwnerKickedEventListener

    @BeforeEach
    fun setUp() {
        listener =
            OwnerKickedEventListener(
                electionRepository = electionRepository,
                teamMemberRepository = teamMemberRepository,
            )
    }

    @Test
    fun `OWNER가 이미 존재하면 선거를 생성하지 않는다`() {
        // given
        val event = OwnerKickedEvent(teamId = 1L, kickedPlayerId = 100L, kickedMemberId = 200L)
        every { teamMemberRepository.countOwnersByTeamId(1L) } returns 1L

        // when
        listener.handleOwnerKicked(event)

        // then
        verify(exactly = 0) { electionRepository.save(any()) }
    }

    @Test
    fun `OWNER 부재 시 MANAGER가 있으면 긴급 선거를 생성한다`() {
        // given
        val event = OwnerKickedEvent(teamId = 1L, kickedPlayerId = 100L, kickedMemberId = 200L)
        val manager: TeamMember =
            mockk {
                every { id } returns 300L
                every { role } returns TeamMemberRole.MANAGER
                every { isActive } returns true
            }
        every { teamMemberRepository.countOwnersByTeamId(1L) } returns 0L
        every { teamMemberRepository.findByTeamId(1L) } returns listOf(manager)

        val electionSlot = slot<Election>()
        every { electionRepository.save(capture(electionSlot)) } answers { firstArg() }

        // when
        listener.handleOwnerKicked(event)

        // then
        verify(exactly = 1) { electionRepository.save(any()) }
        val savedElection = electionSlot.captured
        assertThat(savedElection.teamId).isEqualTo(1L)
        assertThat(savedElection.electionType.name).isEqualTo("EMERGENCY")
        assertThat(savedElection.title).contains("긴급")
    }

    @Test
    fun `OWNER 부재 시 MANAGER가 없으면 일반 OWNER_ELECTION을 생성한다`() {
        // given
        val event = OwnerKickedEvent(teamId = 1L, kickedPlayerId = 100L, kickedMemberId = 200L)
        val member: TeamMember =
            mockk {
                every { id } returns 300L
                every { role } returns TeamMemberRole.MEMBER
                every { isActive } returns true
            }
        every { teamMemberRepository.countOwnersByTeamId(1L) } returns 0L
        every { teamMemberRepository.findByTeamId(1L) } returns listOf(member)

        val electionSlot = slot<Election>()
        every { electionRepository.save(capture(electionSlot)) } answers { firstArg() }

        // when
        listener.handleOwnerKicked(event)

        // then
        verify(exactly = 1) { electionRepository.save(any()) }
        val savedElection = electionSlot.captured
        assertThat(savedElection.teamId).isEqualTo(1L)
        assertThat(savedElection.electionType.name).isEqualTo("OWNER_ELECTION")
    }

    @Test
    fun `OWNER 부재 시 팀원이 아무도 없어도 일반 선거를 생성한다`() {
        // given
        val event = OwnerKickedEvent(teamId = 1L, kickedPlayerId = 100L, kickedMemberId = 200L)
        every { teamMemberRepository.countOwnersByTeamId(1L) } returns 0L
        every { teamMemberRepository.findByTeamId(1L) } returns emptyList()

        val electionSlot = slot<Election>()
        every { electionRepository.save(capture(electionSlot)) } answers { firstArg() }

        // when
        listener.handleOwnerKicked(event)

        // then
        verify(exactly = 1) { electionRepository.save(any()) }
        val savedElection = electionSlot.captured
        assertThat(savedElection.electionType.name).isEqualTo("OWNER_ELECTION")
    }
}
