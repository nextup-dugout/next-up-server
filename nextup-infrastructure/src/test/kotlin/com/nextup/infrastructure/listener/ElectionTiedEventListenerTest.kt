package com.nextup.infrastructure.listener

import com.nextup.core.domain.election.Election
import com.nextup.core.domain.election.ElectionType
import com.nextup.core.domain.event.ElectionTiedEvent
import com.nextup.core.port.repository.ElectionVoteRepositoryPort
import com.nextup.core.service.election.ElectionService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

@DisplayName("ElectionTiedEventListener 테스트")
class ElectionTiedEventListenerTest {
    private val electionService: ElectionService = mockk(relaxed = true)
    private val electionVoteRepository: ElectionVoteRepositoryPort = mockk()

    private lateinit var listener: ElectionTiedEventListener

    @BeforeEach
    fun setUp() {
        listener =
            ElectionTiedEventListener(
                electionService = electionService,
                electionVoteRepository = electionVoteRepository,
            )
    }

    @Test
    fun `동률 이벤트 수신 시 재선거를 자동 생성한다`() {
        // given
        val event =
            ElectionTiedEvent(
                electionId = 1L,
                teamId = 1L,
                electionType = ElectionType.OWNER_ELECTION,
                tiedCandidateCount = 2,
                tiedVoteCount = 5L,
            )
        every {
            electionVoteRepository.countByElectionIdGroupByCandidateId(1L)
        } returns mapOf(10L to 5L, 20L to 5L)

        val runoffElection = createTestRunoffElection(50L)
        every {
            electionService.createRunoffElection(1L, listOf(10L, 20L))
        } returns runoffElection

        // when
        listener.onElectionTied(event)

        // then
        verify {
            electionService.createRunoffElection(1L, listOf(10L, 20L))
        }
    }

    @Test
    fun `투표 결과가 없으면 재선거를 생성하지 않는다`() {
        // given
        val event =
            ElectionTiedEvent(
                electionId = 1L,
                teamId = 1L,
                electionType = ElectionType.OWNER_ELECTION,
                tiedCandidateCount = 2,
                tiedVoteCount = 5L,
            )
        every {
            electionVoteRepository.countByElectionIdGroupByCandidateId(1L)
        } returns emptyMap()

        // when
        listener.onElectionTied(event)

        // then
        verify(exactly = 0) {
            electionService.createRunoffElection(any(), any())
        }
    }

    @Test
    fun `최대 재선거 횟수 초과 시 예외를 잡고 로그만 남긴다`() {
        // given
        val event =
            ElectionTiedEvent(
                electionId = 1L,
                teamId = 1L,
                electionType = ElectionType.OWNER_ELECTION,
                tiedCandidateCount = 2,
                tiedVoteCount = 5L,
            )
        every {
            electionVoteRepository.countByElectionIdGroupByCandidateId(1L)
        } returns mapOf(10L to 5L, 20L to 5L)

        every {
            electionService.createRunoffElection(1L, listOf(10L, 20L))
        } throws IllegalArgumentException("최대 재선거 횟수(2)를 초과했습니다.")

        // when - should not throw
        listener.onElectionTied(event)

        // then
        verify {
            electionService.createRunoffElection(1L, listOf(10L, 20L))
        }
    }

    @Test
    fun `3명 동률인 경우 모든 동률 후보를 포함하여 재선거를 생성한다`() {
        // given
        val event =
            ElectionTiedEvent(
                electionId = 1L,
                teamId = 1L,
                electionType = ElectionType.OWNER_ELECTION,
                tiedCandidateCount = 3,
                tiedVoteCount = 3L,
            )
        every {
            electionVoteRepository.countByElectionIdGroupByCandidateId(1L)
        } returns mapOf(10L to 3L, 20L to 3L, 30L to 3L, 40L to 1L)

        val runoffElection = createTestRunoffElection(50L)
        every {
            electionService.createRunoffElection(1L, listOf(10L, 20L, 30L))
        } returns runoffElection

        // when
        listener.onElectionTied(event)

        // then
        verify {
            electionService.createRunoffElection(1L, listOf(10L, 20L, 30L))
        }
    }

    @Test
    fun `예기치 않은 예외 발생 시에도 리스너가 실패하지 않는다`() {
        // given
        val event =
            ElectionTiedEvent(
                electionId = 1L,
                teamId = 1L,
                electionType = ElectionType.OWNER_ELECTION,
                tiedCandidateCount = 2,
                tiedVoteCount = 5L,
            )
        every {
            electionVoteRepository.countByElectionIdGroupByCandidateId(1L)
        } returns mapOf(10L to 5L, 20L to 5L)

        every {
            electionService.createRunoffElection(1L, listOf(10L, 20L))
        } throws RuntimeException("DB connection error")

        // when - should not throw
        listener.onElectionTied(event)

        // then
        verify {
            electionService.createRunoffElection(1L, listOf(10L, 20L))
        }
    }

    private fun createTestRunoffElection(id: Long): Election {
        val now = Instant.now()
        val election =
            Election.createRunoff(
                parentElection = createParentElection(),
                currentRunoffCount = 0,
            )
        val idField = Election::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(election, id)
        return election
    }

    private fun createParentElection(): Election {
        val now = Instant.now()
        val election =
            Election.create(
                teamId = 1L,
                title = "구단주 선출",
                description = null,
                electionType = ElectionType.OWNER_ELECTION,
                startAt = now.minus(7, ChronoUnit.DAYS),
                endAt = now.minus(1, ChronoUnit.DAYS),
            )
        val idField = Election::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(election, 1L)
        return election
    }
}
