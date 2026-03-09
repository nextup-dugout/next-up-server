package com.nextup.core.service.election

import com.nextup.common.exception.CandidateNotFoundException
import com.nextup.common.exception.DuplicateVoteException
import com.nextup.common.exception.ElectionNotFoundException
import com.nextup.common.exception.InvalidStateException
import com.nextup.core.domain.election.*
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.team.TeamMemberRole
import com.nextup.core.port.repository.CandidateRepositoryPort
import com.nextup.core.port.repository.ElectionRepositoryPort
import com.nextup.core.port.repository.ElectionVoteRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import com.nextup.core.service.election.dto.CastVoteRequest
import com.nextup.core.service.election.dto.CreateElectionRequest
import com.nextup.core.service.election.dto.RegisterCandidateRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

@DisplayName("ElectionService")
class ElectionServiceTest {
    private lateinit var electionRepository: ElectionRepositoryPort
    private lateinit var candidateRepository: CandidateRepositoryPort
    private lateinit var electionVoteRepository: ElectionVoteRepositoryPort
    private lateinit var teamMemberRepository: TeamMemberRepositoryPort
    private lateinit var electionService: ElectionService

    @BeforeEach
    fun setUp() {
        electionRepository = mockk()
        candidateRepository = mockk()
        electionVoteRepository = mockk()
        teamMemberRepository = mockk()
        electionService =
            ElectionService(
                electionRepository,
                candidateRepository,
                electionVoteRepository,
                teamMemberRepository,
            )
    }

    @Nested
    @DisplayName("createElection")
    inner class CreateElection {
        @Test
        fun `선거를 생성할 수 있다`() {
            // given
            val now = Instant.now()
            val request =
                CreateElectionRequest(
                    teamId = 1L,
                    title = "2024년 주장 선출",
                    description = "새로운 주장을 선출합니다",
                    electionType = ElectionType.CAPTAIN_ELECTION,
                    startAt = now.plus(1, ChronoUnit.DAYS),
                    endAt = now.plus(7, ChronoUnit.DAYS),
                )

            every { electionRepository.save(any()) } answers { firstArg() }

            // when
            val result = electionService.createElection(request)

            // then
            assertThat(result.teamId).isEqualTo(1L)
            assertThat(result.title).isEqualTo("2024년 주장 선출")
            assertThat(result.description).isEqualTo("새로운 주장을 선출합니다")
            assertThat(result.electionType).isEqualTo(ElectionType.CAPTAIN_ELECTION)
            assertThat(result.status).isEqualTo(ElectionStatus.SCHEDULED)
            verify { electionRepository.save(any()) }
        }

        @Test
        fun `구단주 선출 선거를 생성할 수 있다`() {
            // given
            val now = Instant.now()
            val request =
                CreateElectionRequest(
                    teamId = 2L,
                    title = "구단주 선출",
                    description = null,
                    electionType = ElectionType.OWNER_ELECTION,
                    startAt = now.plus(1, ChronoUnit.DAYS),
                    endAt = now.plus(7, ChronoUnit.DAYS),
                )

            every { electionRepository.save(any()) } answers { firstArg() }

            // when
            val result = electionService.createElection(request)

            // then
            assertThat(result.electionType).isEqualTo(ElectionType.OWNER_ELECTION)
            assertThat(result.description).isNull()
        }
    }

    @Nested
    @DisplayName("startElection")
    inner class StartElection {
        @Test
        fun `예정된 선거를 시작할 수 있다`() {
            // given
            val election = createTestElection(1L, ElectionStatus.SCHEDULED)
            every { electionRepository.findById(1L) } returns election

            // when
            val result = electionService.startElection(1L)

            // then
            assertThat(result.status).isEqualTo(ElectionStatus.IN_PROGRESS)
        }

        @Test
        fun `존재하지 않는 선거를 시작하면 예외가 발생한다`() {
            // given
            every { electionRepository.findById(999L) } returns null

            // when & then
            assertThatThrownBy {
                electionService.startElection(999L)
            }.isInstanceOf(ElectionNotFoundException::class.java)
        }

        @Test
        fun `이미 진행 중인 선거를 시작하면 예외가 발생한다`() {
            // given
            val election = createTestElection(1L, ElectionStatus.IN_PROGRESS)
            every { electionRepository.findById(1L) } returns election

            // when & then
            assertThatThrownBy {
                electionService.startElection(1L)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Cannot start election")
        }

        @Test
        fun `취소된 선거를 시작하면 예외가 발생한다`() {
            // given
            val election = createTestElection(1L, ElectionStatus.CANCELLED)
            every { electionRepository.findById(1L) } returns election

            // when & then
            assertThatThrownBy {
                electionService.startElection(1L)
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("completeElection")
    inner class CompleteElection {
        @Test
        fun `진행 중인 선거를 완료할 수 있다`() {
            // given
            val election = createTestElection(1L, ElectionStatus.IN_PROGRESS)
            every { electionRepository.findById(1L) } returns election

            // when
            val result = electionService.completeElection(1L)

            // then
            assertThat(result.status).isEqualTo(ElectionStatus.COMPLETED)
        }

        @Test
        fun `예정된 선거를 완료하면 예외가 발생한다`() {
            // given
            val election = createTestElection(1L, ElectionStatus.SCHEDULED)
            every { electionRepository.findById(1L) } returns election

            // when & then
            assertThatThrownBy {
                electionService.completeElection(1L)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Cannot complete election")
        }

        @Test
        fun `존재하지 않는 선거를 완료하면 예외가 발생한다`() {
            // given
            every { electionRepository.findById(999L) } returns null

            // when & then
            assertThatThrownBy {
                electionService.completeElection(999L)
            }.isInstanceOf(ElectionNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("completeElection - 자동 OWNER 이양")
    inner class CompleteElectionOwnerTransfer {
        @Test
        fun `OWNER_ELECTION 완료 시 단독 최다 득표자에게 OWNER 이양`() {
            // given
            val election = createTestOwnerElection(1L, ElectionStatus.IN_PROGRESS)
            val winnerCandidate = createTestCandidate(10L, 1L, 100L, "김철수")
            val currentOwner =
                mockk<TeamMember>(relaxed = true) {
                    every { id } returns 200L
                    every { role } returns TeamMemberRole.OWNER
                }
            val winner =
                mockk<TeamMember>(relaxed = true) {
                    every { id } returns 100L
                    every { role } returns TeamMemberRole.MEMBER
                }

            every { electionRepository.findById(1L) } returns election
            every {
                electionVoteRepository.countByElectionIdGroupByCandidateId(1L)
            } returns mapOf(10L to 5L, 11L to 3L)
            every { candidateRepository.findById(10L) } returns winnerCandidate
            every { teamMemberRepository.findByIdOrNull(100L) } returns winner
            every { teamMemberRepository.findByTeamId(1L) } returns listOf(currentOwner, winner)
            every { teamMemberRepository.save(any()) } answers { firstArg() }

            // when
            val result = electionService.completeElection(1L)

            // then
            assertThat(result.status).isEqualTo(ElectionStatus.COMPLETED)
            verify { winner.role = TeamMemberRole.OWNER }
            verify { currentOwner.role = TeamMemberRole.MEMBER }
            verify(exactly = 2) { teamMemberRepository.save(any()) }
        }

        @Test
        fun `OWNER_ELECTION 동률이면 자동 이양하지 않음`() {
            // given
            val election = createTestOwnerElection(1L, ElectionStatus.IN_PROGRESS)

            every { electionRepository.findById(1L) } returns election
            every {
                electionVoteRepository.countByElectionIdGroupByCandidateId(1L)
            } returns mapOf(10L to 5L, 11L to 5L)

            // when
            val result = electionService.completeElection(1L)

            // then
            assertThat(result.status).isEqualTo(ElectionStatus.COMPLETED)
            verify(exactly = 0) { teamMemberRepository.save(any()) }
        }

        @Test
        fun `OWNER_ELECTION 투표가 없으면 자동 이양하지 않음`() {
            // given
            val election = createTestOwnerElection(1L, ElectionStatus.IN_PROGRESS)

            every { electionRepository.findById(1L) } returns election
            every {
                electionVoteRepository.countByElectionIdGroupByCandidateId(1L)
            } returns emptyMap()

            // when
            val result = electionService.completeElection(1L)

            // then
            assertThat(result.status).isEqualTo(ElectionStatus.COMPLETED)
            verify(exactly = 0) { teamMemberRepository.save(any()) }
        }

        @Test
        fun `당선자가 이미 OWNER이면 이양하지 않음`() {
            // given
            val election = createTestOwnerElection(1L, ElectionStatus.IN_PROGRESS)
            val winnerCandidate = createTestCandidate(10L, 1L, 100L, "김철수")
            val winner =
                mockk<TeamMember>(relaxed = true) {
                    every { id } returns 100L
                    every { role } returns TeamMemberRole.OWNER
                }

            every { electionRepository.findById(1L) } returns election
            every {
                electionVoteRepository.countByElectionIdGroupByCandidateId(1L)
            } returns mapOf(10L to 5L)
            every { candidateRepository.findById(10L) } returns winnerCandidate
            every { teamMemberRepository.findByIdOrNull(100L) } returns winner

            // when
            val result = electionService.completeElection(1L)

            // then
            assertThat(result.status).isEqualTo(ElectionStatus.COMPLETED)
            verify(exactly = 0) { teamMemberRepository.save(any()) }
        }

        @Test
        fun `CAPTAIN_ELECTION이면 자동 이양하지 않음`() {
            // given
            val election = createTestElection(1L, ElectionStatus.IN_PROGRESS)
            every { electionRepository.findById(1L) } returns election

            // when
            val result = electionService.completeElection(1L)

            // then
            assertThat(result.status).isEqualTo(ElectionStatus.COMPLETED)
            verify(exactly = 0) { teamMemberRepository.save(any()) }
        }

        @Test
        fun `기존 OWNER가 없어도 당선자에게 OWNER 부여`() {
            // given
            val election = createTestOwnerElection(1L, ElectionStatus.IN_PROGRESS)
            val winnerCandidate = createTestCandidate(10L, 1L, 100L, "김철수")
            val winner =
                mockk<TeamMember>(relaxed = true) {
                    every { id } returns 100L
                    every { role } returns TeamMemberRole.MEMBER
                }

            every { electionRepository.findById(1L) } returns election
            every {
                electionVoteRepository.countByElectionIdGroupByCandidateId(1L)
            } returns mapOf(10L to 5L)
            every { candidateRepository.findById(10L) } returns winnerCandidate
            every { teamMemberRepository.findByIdOrNull(100L) } returns winner
            every { teamMemberRepository.findByTeamId(1L) } returns listOf(winner)
            every { teamMemberRepository.save(any()) } answers { firstArg() }

            // when
            val result = electionService.completeElection(1L)

            // then
            assertThat(result.status).isEqualTo(ElectionStatus.COMPLETED)
            verify { winner.role = TeamMemberRole.OWNER }
            verify(exactly = 1) { teamMemberRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("cancelElection")
    inner class CancelElection {
        @Test
        fun `예정된 선거를 취소할 수 있다`() {
            // given
            val election = createTestElection(1L, ElectionStatus.SCHEDULED)
            every { electionRepository.findById(1L) } returns election

            // when
            val result = electionService.cancelElection(1L)

            // then
            assertThat(result.status).isEqualTo(ElectionStatus.CANCELLED)
        }

        @Test
        fun `진행 중인 선거를 취소할 수 있다`() {
            // given
            val election = createTestElection(1L, ElectionStatus.IN_PROGRESS)
            every { electionRepository.findById(1L) } returns election

            // when
            val result = electionService.cancelElection(1L)

            // then
            assertThat(result.status).isEqualTo(ElectionStatus.CANCELLED)
        }

        @Test
        fun `완료된 선거를 취소하면 예외가 발생한다`() {
            // given
            val election = createTestElection(1L, ElectionStatus.COMPLETED)
            every { electionRepository.findById(1L) } returns election

            // when & then
            assertThatThrownBy {
                electionService.cancelElection(1L)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Cannot cancel election")
        }

        @Test
        fun `이미 취소된 선거를 취소하면 예외가 발생한다`() {
            // given
            val election = createTestElection(1L, ElectionStatus.CANCELLED)
            every { electionRepository.findById(1L) } returns election

            // when & then
            assertThatThrownBy {
                electionService.cancelElection(1L)
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `존재하지 않는 선거를 취소하면 예외가 발생한다`() {
            // given
            every { electionRepository.findById(999L) } returns null

            // when & then
            assertThatThrownBy {
                electionService.cancelElection(999L)
            }.isInstanceOf(ElectionNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("registerCandidate")
    inner class RegisterCandidate {
        @Test
        fun `예정된 선거에 후보자를 등록할 수 있다`() {
            // given
            val election = createTestElection(1L, ElectionStatus.SCHEDULED)
            val request =
                RegisterCandidateRequest(
                    electionId = 1L,
                    memberId = 100L,
                    memberName = "김철수",
                    statement = "열심히 하겠습니다",
                )

            every { electionRepository.findById(1L) } returns election
            every { candidateRepository.findByElectionIdAndMemberId(1L, 100L) } returns null
            every { candidateRepository.save(any()) } answers { firstArg() }

            // when
            val result = electionService.registerCandidate(request)

            // then
            assertThat(result.electionId).isEqualTo(1L)
            assertThat(result.memberId).isEqualTo(100L)
            assertThat(result.memberName).isEqualTo("김철수")
            assertThat(result.statement).isEqualTo("열심히 하겠습니다")
            verify { candidateRepository.save(any()) }
        }

        @Test
        fun `공약 없이 후보자를 등록할 수 있다`() {
            // given
            val election = createTestElection(1L, ElectionStatus.SCHEDULED)
            val request =
                RegisterCandidateRequest(
                    electionId = 1L,
                    memberId = 100L,
                    memberName = "이영희",
                    statement = null,
                )

            every { electionRepository.findById(1L) } returns election
            every { candidateRepository.findByElectionIdAndMemberId(1L, 100L) } returns null
            every { candidateRepository.save(any()) } answers { firstArg() }

            // when
            val result = electionService.registerCandidate(request)

            // then
            assertThat(result.memberName).isEqualTo("이영희")
            assertThat(result.statement).isNull()
        }

        @Test
        fun `진행 중인 선거에 후보자를 등록하면 예외가 발생한다`() {
            // given
            val election = createTestElection(1L, ElectionStatus.IN_PROGRESS)
            val request =
                RegisterCandidateRequest(
                    electionId = 1L,
                    memberId = 100L,
                    memberName = "김철수",
                    statement = null,
                )

            every { electionRepository.findById(1L) } returns election

            // when & then
            assertThatThrownBy {
                electionService.registerCandidate(request)
            }.isInstanceOf(InvalidStateException::class.java)
                .hasMessageContaining("Cannot register candidate")
        }

        @Test
        fun `완료된 선거에 후보자를 등록하면 예외가 발생한다`() {
            // given
            val election = createTestElection(1L, ElectionStatus.COMPLETED)
            val request =
                RegisterCandidateRequest(
                    electionId = 1L,
                    memberId = 100L,
                    memberName = "김철수",
                    statement = null,
                )

            every { electionRepository.findById(1L) } returns election

            // when & then
            assertThatThrownBy {
                electionService.registerCandidate(request)
            }.isInstanceOf(InvalidStateException::class.java)
        }

        @Test
        fun `이미 등록된 후보자를 다시 등록하면 예외가 발생한다`() {
            // given
            val election = createTestElection(1L, ElectionStatus.SCHEDULED)
            val existingCandidate = createTestCandidate(1L, 1L, 100L, "김철수")
            val request =
                RegisterCandidateRequest(
                    electionId = 1L,
                    memberId = 100L,
                    memberName = "김철수",
                    statement = null,
                )

            every { electionRepository.findById(1L) } returns election
            every { candidateRepository.findByElectionIdAndMemberId(1L, 100L) } returns existingCandidate

            // when & then
            assertThatThrownBy {
                electionService.registerCandidate(request)
            }.isInstanceOf(InvalidStateException::class.java)
                .hasMessageContaining("already registered")
        }

        @Test
        fun `존재하지 않는 선거에 후보자를 등록하면 예외가 발생한다`() {
            // given
            val request =
                RegisterCandidateRequest(
                    electionId = 999L,
                    memberId = 100L,
                    memberName = "김철수",
                    statement = null,
                )

            every { electionRepository.findById(999L) } returns null

            // when & then
            assertThatThrownBy {
                electionService.registerCandidate(request)
            }.isInstanceOf(ElectionNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("vote")
    inner class Vote {
        @Test
        fun `진행 중인 선거에 투표할 수 있다`() {
            // given
            val now = Instant.now()
            val election =
                Election.create(
                    teamId = 1L,
                    title = "주장 선출",
                    description = null,
                    electionType = ElectionType.CAPTAIN_ELECTION,
                    startAt = now.minus(1, ChronoUnit.DAYS),
                    endAt = now.plus(7, ChronoUnit.DAYS),
                )
            setElectionId(election, 1L)
            election.start()

            val candidate = createTestCandidate(10L, 1L, 100L, "김철수")
            val request =
                CastVoteRequest(
                    electionId = 1L,
                    voterId = 200L,
                    candidateId = 10L,
                )

            every { electionRepository.findById(1L) } returns election
            every { electionVoteRepository.findByElectionIdAndVoterId(1L, 200L) } returns null
            every { candidateRepository.findById(10L) } returns candidate
            every { electionVoteRepository.save(any()) } answers { firstArg() }

            // when
            val result = electionService.vote(request)

            // then
            assertThat(result.id).isEqualTo(10L)
            assertThat(result.memberName).isEqualTo("김철수")
            verify { electionVoteRepository.save(any()) }
        }

        @Test
        fun `투표 시간이 아니면 투표할 수 없다`() {
            // given
            val now = Instant.now()
            val election =
                Election.create(
                    teamId = 1L,
                    title = "주장 선출",
                    description = null,
                    electionType = ElectionType.CAPTAIN_ELECTION,
                    startAt = now.plus(1, ChronoUnit.DAYS),
                    endAt = now.plus(7, ChronoUnit.DAYS),
                )
            setElectionId(election, 1L)
            election.start()

            val request =
                CastVoteRequest(
                    electionId = 1L,
                    voterId = 200L,
                    candidateId = 10L,
                )

            every { electionRepository.findById(1L) } returns election

            // when & then
            assertThatThrownBy {
                electionService.vote(request)
            }.isInstanceOf(InvalidStateException::class.java)
                .hasMessageContaining("Voting is not open")
        }

        @Test
        fun `예정된 선거에 투표하면 예외가 발생한다`() {
            // given
            val election = createTestElection(1L, ElectionStatus.SCHEDULED)
            val request =
                CastVoteRequest(
                    electionId = 1L,
                    voterId = 200L,
                    candidateId = 10L,
                )

            every { electionRepository.findById(1L) } returns election

            // when & then
            assertThatThrownBy {
                electionService.vote(request)
            }.isInstanceOf(InvalidStateException::class.java)
                .hasMessageContaining("Voting is not open")
        }

        @Test
        fun `이미 투표한 사용자가 다시 투표하면 예외가 발생한다`() {
            // given
            val now = Instant.now()
            val election =
                Election.create(
                    teamId = 1L,
                    title = "주장 선출",
                    description = null,
                    electionType = ElectionType.CAPTAIN_ELECTION,
                    startAt = now.minus(1, ChronoUnit.DAYS),
                    endAt = now.plus(7, ChronoUnit.DAYS),
                )
            setElectionId(election, 1L)
            election.start()

            val existingVote =
                ElectionVote.create(
                    electionId = 1L,
                    voterId = 200L,
                    candidateId = 10L,
                )
            val request =
                CastVoteRequest(
                    electionId = 1L,
                    voterId = 200L,
                    candidateId = 11L,
                )

            every { electionRepository.findById(1L) } returns election
            every { electionVoteRepository.findByElectionIdAndVoterId(1L, 200L) } returns existingVote

            // when & then
            assertThatThrownBy {
                electionService.vote(request)
            }.isInstanceOf(DuplicateVoteException::class.java)
        }

        @Test
        fun `존재하지 않는 후보자에게 투표하면 예외가 발생한다`() {
            // given
            val now = Instant.now()
            val election =
                Election.create(
                    teamId = 1L,
                    title = "주장 선출",
                    description = null,
                    electionType = ElectionType.CAPTAIN_ELECTION,
                    startAt = now.minus(1, ChronoUnit.DAYS),
                    endAt = now.plus(7, ChronoUnit.DAYS),
                )
            setElectionId(election, 1L)
            election.start()

            val request =
                CastVoteRequest(
                    electionId = 1L,
                    voterId = 200L,
                    candidateId = 999L,
                )

            every { electionRepository.findById(1L) } returns election
            every { electionVoteRepository.findByElectionIdAndVoterId(1L, 200L) } returns null
            every { candidateRepository.findById(999L) } returns null

            // when & then
            assertThatThrownBy {
                electionService.vote(request)
            }.isInstanceOf(CandidateNotFoundException::class.java)
        }

        @Test
        fun `다른 선거의 후보자에게 투표하면 예외가 발생한다`() {
            // given
            val now = Instant.now()
            val election =
                Election.create(
                    teamId = 1L,
                    title = "주장 선출",
                    description = null,
                    electionType = ElectionType.CAPTAIN_ELECTION,
                    startAt = now.minus(1, ChronoUnit.DAYS),
                    endAt = now.plus(7, ChronoUnit.DAYS),
                )
            setElectionId(election, 1L)
            election.start()

            val candidateFromOtherElection = createTestCandidate(10L, 2L, 100L, "김철수")
            val request =
                CastVoteRequest(
                    electionId = 1L,
                    voterId = 200L,
                    candidateId = 10L,
                )

            every { electionRepository.findById(1L) } returns election
            every { electionVoteRepository.findByElectionIdAndVoterId(1L, 200L) } returns null
            every { candidateRepository.findById(10L) } returns candidateFromOtherElection

            // when & then
            assertThatThrownBy {
                electionService.vote(request)
            }.isInstanceOf(InvalidStateException::class.java)
                .hasMessageContaining("Candidate 10 is not in election 1")
        }

        @Test
        fun `존재하지 않는 선거에 투표하면 예외가 발생한다`() {
            // given
            val request =
                CastVoteRequest(
                    electionId = 999L,
                    voterId = 200L,
                    candidateId = 10L,
                )

            every { electionRepository.findById(999L) } returns null

            // when & then
            assertThatThrownBy {
                electionService.vote(request)
            }.isInstanceOf(ElectionNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("getResults")
    inner class GetResults {
        @Test
        fun `선거 결과를 조회할 수 있다`() {
            // given
            val election = createTestElection(1L, ElectionStatus.COMPLETED)
            val candidates =
                listOf(
                    createTestCandidate(10L, 1L, 100L, "김철수"),
                    createTestCandidate(11L, 1L, 101L, "이영희"),
                    createTestCandidate(12L, 1L, 102L, "박민수"),
                )
            val voteCounts =
                mapOf(
                    10L to 5L,
                    11L to 3L,
                    12L to 7L,
                )

            every { electionRepository.findById(1L) } returns election
            every { candidateRepository.findAllByElectionId(1L) } returns candidates
            every { electionVoteRepository.countByElectionIdGroupByCandidateId(1L) } returns voteCounts

            // when
            val result = electionService.getResults(1L)

            // then
            assertThat(result.election.id).isEqualTo(1L)
            assertThat(result.totalVotes).isEqualTo(15L)
            assertThat(result.candidateVoteCounts).hasSize(3)

            // 득표순으로 정렬되어 있는지 확인
            assertThat(result.candidateVoteCounts[0].candidate.memberName).isEqualTo("박민수")
            assertThat(result.candidateVoteCounts[0].voteCount).isEqualTo(7L)
            assertThat(result.candidateVoteCounts[1].candidate.memberName).isEqualTo("김철수")
            assertThat(result.candidateVoteCounts[1].voteCount).isEqualTo(5L)
            assertThat(result.candidateVoteCounts[2].candidate.memberName).isEqualTo("이영희")
            assertThat(result.candidateVoteCounts[2].voteCount).isEqualTo(3L)
        }

        @Test
        fun `투표가 없는 선거의 결과를 조회할 수 있다`() {
            // given
            val election = createTestElection(1L, ElectionStatus.COMPLETED)
            val candidates =
                listOf(
                    createTestCandidate(10L, 1L, 100L, "김철수"),
                    createTestCandidate(11L, 1L, 101L, "이영희"),
                )
            val voteCounts = emptyMap<Long, Long>()

            every { electionRepository.findById(1L) } returns election
            every { candidateRepository.findAllByElectionId(1L) } returns candidates
            every { electionVoteRepository.countByElectionIdGroupByCandidateId(1L) } returns voteCounts

            // when
            val result = electionService.getResults(1L)

            // then
            assertThat(result.totalVotes).isEqualTo(0L)
            assertThat(result.candidateVoteCounts).hasSize(2)
            assertThat(result.candidateVoteCounts[0].voteCount).isEqualTo(0L)
            assertThat(result.candidateVoteCounts[1].voteCount).isEqualTo(0L)
        }

        @Test
        fun `일부 후보자만 득표한 선거의 결과를 조회할 수 있다`() {
            // given
            val election = createTestElection(1L, ElectionStatus.COMPLETED)
            val candidates =
                listOf(
                    createTestCandidate(10L, 1L, 100L, "김철수"),
                    createTestCandidate(11L, 1L, 101L, "이영희"),
                    createTestCandidate(12L, 1L, 102L, "박민수"),
                )
            val voteCounts =
                mapOf(
                    10L to 8L,
                    // 11L은 득표 없음
                    12L to 2L,
                )

            every { electionRepository.findById(1L) } returns election
            every { candidateRepository.findAllByElectionId(1L) } returns candidates
            every { electionVoteRepository.countByElectionIdGroupByCandidateId(1L) } returns voteCounts

            // when
            val result = electionService.getResults(1L)

            // then
            assertThat(result.totalVotes).isEqualTo(10L)
            assertThat(result.candidateVoteCounts).hasSize(3)
            assertThat(result.candidateVoteCounts.find { it.candidate.memberName == "이영희" }?.voteCount).isEqualTo(0L)
        }

        @Test
        fun `존재하지 않는 선거의 결과를 조회하면 예외가 발생한다`() {
            // given
            every { electionRepository.findById(999L) } returns null

            // when & then
            assertThatThrownBy {
                electionService.getResults(999L)
            }.isInstanceOf(ElectionNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("getElectionsByTeam")
    inner class GetElectionsByTeam {
        @Test
        fun `팀의 모든 선거를 조회할 수 있다`() {
            // given
            val elections =
                listOf(
                    createTestElection(1L, ElectionStatus.SCHEDULED),
                    createTestElection(2L, ElectionStatus.IN_PROGRESS),
                    createTestElection(3L, ElectionStatus.COMPLETED),
                )

            every { electionRepository.findAllByTeamId(1L) } returns elections

            // when
            val result = electionService.getElectionsByTeam(1L)

            // then
            assertThat(result).hasSize(3)
            assertThat(result[0].id).isEqualTo(1L)
            assertThat(result[1].id).isEqualTo(2L)
            assertThat(result[2].id).isEqualTo(3L)
        }

        @Test
        fun `선거가 없는 팀의 선거를 조회하면 빈 리스트를 반환한다`() {
            // given
            every { electionRepository.findAllByTeamId(999L) } returns emptyList()

            // when
            val result = electionService.getElectionsByTeam(999L)

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("getElectionById")
    inner class GetElectionById {
        @Test
        fun `ID로 선거를 조회할 수 있다`() {
            // given
            val election = createTestElection(1L, ElectionStatus.SCHEDULED)
            every { electionRepository.findById(1L) } returns election

            // when
            val result = electionService.getElectionById(1L)

            // then
            assertThat(result.id).isEqualTo(1L)
            assertThat(result.status).isEqualTo(ElectionStatus.SCHEDULED)
        }

        @Test
        fun `존재하지 않는 선거를 조회하면 예외가 발생한다`() {
            // given
            every { electionRepository.findById(999L) } returns null

            // when & then
            assertThatThrownBy {
                electionService.getElectionById(999L)
            }.isInstanceOf(ElectionNotFoundException::class.java)
        }
    }

    // Test helper methods
    private fun createTestOwnerElection(
        id: Long,
        status: ElectionStatus,
    ): Election {
        val now = Instant.now()
        val election =
            Election.create(
                teamId = 1L,
                title = "구단주 선출",
                description = "새 구단주를 선출합니다",
                electionType = ElectionType.OWNER_ELECTION,
                startAt = now.plus(1, ChronoUnit.DAYS),
                endAt = now.plus(7, ChronoUnit.DAYS),
            )
        setElectionId(election, id)

        when (status) {
            ElectionStatus.IN_PROGRESS -> election.start()
            ElectionStatus.COMPLETED -> {
                election.start()
                election.complete()
            }
            ElectionStatus.CANCELLED -> election.cancel()
            ElectionStatus.SCHEDULED -> {}
        }

        return election
    }

    private fun createTestElection(
        id: Long,
        status: ElectionStatus,
    ): Election {
        val now = Instant.now()
        val election =
            Election.create(
                teamId = 1L,
                title = "테스트 선거",
                description = "테스트 설명",
                electionType = ElectionType.CAPTAIN_ELECTION,
                startAt = now.plus(1, ChronoUnit.DAYS),
                endAt = now.plus(7, ChronoUnit.DAYS),
            )
        setElectionId(election, id)

        // Set status based on parameter
        when (status) {
            ElectionStatus.IN_PROGRESS -> election.start()
            ElectionStatus.COMPLETED -> {
                election.start()
                election.complete()
            }
            ElectionStatus.CANCELLED -> election.cancel()
            ElectionStatus.SCHEDULED -> {} // Do nothing, default state
        }

        return election
    }

    private fun setElectionId(
        election: Election,
        id: Long
    ) {
        val idField = Election::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(election, id)
    }

    private fun createTestCandidate(
        id: Long,
        electionId: Long,
        memberId: Long,
        memberName: String,
    ): Candidate {
        val candidate =
            Candidate.create(
                electionId = electionId,
                memberId = memberId,
                memberName = memberName,
                statement = "테스트 공약",
            )
        setCandidateId(candidate, id)
        return candidate
    }

    private fun setCandidateId(
        candidate: Candidate,
        id: Long
    ) {
        val idField = Candidate::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(candidate, id)
    }
}
