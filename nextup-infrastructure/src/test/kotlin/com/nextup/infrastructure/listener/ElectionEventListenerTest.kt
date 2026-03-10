package com.nextup.infrastructure.listener

import com.nextup.core.domain.election.Candidate
import com.nextup.core.domain.election.ElectionType
import com.nextup.core.domain.event.ElectionCompletedEvent
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.team.TeamMemberRole
import com.nextup.core.port.repository.CandidateRepositoryPort
import com.nextup.core.port.repository.ElectionVoteRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ElectionEventListener 테스트")
class ElectionEventListenerTest {
    private val electionVoteRepository: ElectionVoteRepositoryPort = mockk()
    private val candidateRepository: CandidateRepositoryPort = mockk()
    private val teamMemberRepository: TeamMemberRepositoryPort = mockk()

    private lateinit var listener: ElectionEventListener

    @BeforeEach
    fun setUp() {
        listener =
            ElectionEventListener(
                electionVoteRepository = electionVoteRepository,
                candidateRepository = candidateRepository,
                teamMemberRepository = teamMemberRepository,
            )
    }

    @Test
    fun `OWNER_ELECTION이 아닌 선거 완료 이벤트는 무시한다`() {
        // given
        val event =
            ElectionCompletedEvent(
                electionId = 1L,
                teamId = 1L,
                electionType = ElectionType.CAPTAIN_ELECTION,
            )

        // when
        listener.onElectionCompleted(event)

        // then
        verify(exactly = 0) { electionVoteRepository.countByElectionIdGroupByCandidateId(any()) }
    }

    @Test
    fun `투표가 없으면 OWNER 이양을 생략한다`() {
        // given
        val event =
            ElectionCompletedEvent(
                electionId = 1L,
                teamId = 1L,
                electionType = ElectionType.OWNER_ELECTION,
            )
        every { electionVoteRepository.countByElectionIdGroupByCandidateId(1L) } returns emptyMap()

        // when
        listener.onElectionCompleted(event)

        // then
        verify { electionVoteRepository.countByElectionIdGroupByCandidateId(1L) }
        verify(exactly = 0) { candidateRepository.findById(any()) }
    }

    @Test
    fun `동률이면 OWNER 이양을 생략한다`() {
        // given
        val event =
            ElectionCompletedEvent(
                electionId = 1L,
                teamId = 1L,
                electionType = ElectionType.OWNER_ELECTION,
            )
        every {
            electionVoteRepository.countByElectionIdGroupByCandidateId(1L)
        } returns
            mapOf(
                10L to 5L,
                20L to 5L,
            )

        // when
        listener.onElectionCompleted(event)

        // then
        verify(exactly = 0) { candidateRepository.findById(any()) }
    }

    @Test
    fun `당선 후보자를 찾을 수 없으면 이양을 생략한다`() {
        // given
        val event =
            ElectionCompletedEvent(
                electionId = 1L,
                teamId = 1L,
                electionType = ElectionType.OWNER_ELECTION,
            )
        every {
            electionVoteRepository.countByElectionIdGroupByCandidateId(1L)
        } returns
            mapOf(
                10L to 5L,
                20L to 3L,
            )
        every { candidateRepository.findById(10L) } returns null

        // when
        listener.onElectionCompleted(event)

        // then
        verify { candidateRepository.findById(10L) }
        verify(exactly = 0) { teamMemberRepository.findByIdOrNull(any()) }
    }

    @Test
    fun `당선자의 TeamMember를 찾을 수 없으면 이양을 생략한다`() {
        // given
        val event =
            ElectionCompletedEvent(
                electionId = 1L,
                teamId = 1L,
                electionType = ElectionType.OWNER_ELECTION,
            )
        val candidate =
            Candidate.create(
                electionId = 1L,
                memberId = 100L,
                memberName = "홍길동",
            )
        every {
            electionVoteRepository.countByElectionIdGroupByCandidateId(1L)
        } returns mapOf(10L to 5L)
        every { candidateRepository.findById(10L) } returns candidate
        every { teamMemberRepository.findByIdOrNull(100L) } returns null

        // when
        listener.onElectionCompleted(event)

        // then
        verify { teamMemberRepository.findByIdOrNull(100L) }
        verify(exactly = 0) { teamMemberRepository.findByTeamId(any()) }
    }

    @Test
    fun `당선자가 이미 OWNER이면 이양을 생략한다`() {
        // given
        val event =
            ElectionCompletedEvent(
                electionId = 1L,
                teamId = 1L,
                electionType = ElectionType.OWNER_ELECTION,
            )
        val candidate =
            Candidate.create(
                electionId = 1L,
                memberId = 100L,
                memberName = "홍길동",
            )
        val winner: TeamMember =
            mockk {
                every { role } returns TeamMemberRole.OWNER
            }
        every {
            electionVoteRepository.countByElectionIdGroupByCandidateId(1L)
        } returns mapOf(10L to 5L)
        every { candidateRepository.findById(10L) } returns candidate
        every { teamMemberRepository.findByIdOrNull(100L) } returns winner

        // when
        listener.onElectionCompleted(event)

        // then
        verify(exactly = 0) { teamMemberRepository.findByTeamId(any()) }
    }

    @Test
    fun `OWNER 선거 완료 시 기존 OWNER를 강등하고 당선자를 승격한다`() {
        // given
        val event =
            ElectionCompletedEvent(
                electionId = 1L,
                teamId = 1L,
                electionType = ElectionType.OWNER_ELECTION,
            )
        val candidate =
            Candidate.create(
                electionId = 1L,
                memberId = 100L,
                memberName = "홍길동",
            )
        val winner: TeamMember =
            mockk {
                every { role } returns TeamMemberRole.MEMBER
                every { role = any() } just Runs
            }
        val currentOwner: TeamMember =
            mockk {
                every { role } returns TeamMemberRole.OWNER
                every { role = any() } just Runs
                every { id } returns 200L
            }
        val otherMember: TeamMember =
            mockk {
                every { role } returns TeamMemberRole.MEMBER
            }
        every {
            electionVoteRepository.countByElectionIdGroupByCandidateId(1L)
        } returns
            mapOf(
                10L to 5L,
                20L to 3L,
            )
        every { candidateRepository.findById(10L) } returns candidate
        every { teamMemberRepository.findByIdOrNull(100L) } returns winner
        every { teamMemberRepository.findByTeamId(1L) } returns listOf(currentOwner, winner, otherMember)
        every { teamMemberRepository.save(any()) } returnsArgument 0

        // when
        listener.onElectionCompleted(event)

        // then
        verify { currentOwner.role = TeamMemberRole.MEMBER }
        verify { winner.role = TeamMemberRole.OWNER }
        verify(exactly = 2) { teamMemberRepository.save(any()) }
    }

    @Test
    fun `기존 OWNER가 없는 경우에도 당선자를 OWNER로 승격한다`() {
        // given
        val event =
            ElectionCompletedEvent(
                electionId = 1L,
                teamId = 1L,
                electionType = ElectionType.OWNER_ELECTION,
            )
        val candidate =
            Candidate.create(
                electionId = 1L,
                memberId = 100L,
                memberName = "홍길동",
            )
        val winner: TeamMember =
            mockk {
                every { role } returns TeamMemberRole.MEMBER
                every { role = any() } just Runs
            }
        every {
            electionVoteRepository.countByElectionIdGroupByCandidateId(1L)
        } returns mapOf(10L to 5L)
        every { candidateRepository.findById(10L) } returns candidate
        every { teamMemberRepository.findByIdOrNull(100L) } returns winner
        every { teamMemberRepository.findByTeamId(1L) } returns listOf(winner)
        every { teamMemberRepository.save(any()) } returnsArgument 0

        // when
        listener.onElectionCompleted(event)

        // then
        verify { winner.role = TeamMemberRole.OWNER }
        verify(exactly = 1) { teamMemberRepository.save(any()) }
    }
}
