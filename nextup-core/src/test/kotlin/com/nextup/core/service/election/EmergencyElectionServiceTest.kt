package com.nextup.core.service.election

import com.nextup.common.exception.ActiveElectionAlreadyExistsException
import com.nextup.common.exception.ElectionNotFoundException
import com.nextup.common.exception.InvalidActingOwnerException
import com.nextup.common.exception.InvalidStateException
import com.nextup.common.exception.TeamMemberNotFoundException
import com.nextup.common.exception.UnauthorizedEmergencyElectionException
import com.nextup.core.domain.election.Election
import com.nextup.core.domain.election.ElectionStatus
import com.nextup.core.domain.election.ElectionType
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.team.TeamMemberRole
import com.nextup.core.port.repository.ElectionRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import com.nextup.core.service.election.dto.DesignateActingOwnerRequest
import com.nextup.core.service.election.dto.TriggerEmergencyElectionRequest
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

@DisplayName("EmergencyElectionService")
class EmergencyElectionServiceTest {
    private lateinit var electionRepository: ElectionRepositoryPort
    private lateinit var teamMemberRepository: TeamMemberRepositoryPort
    private lateinit var emergencyElectionService: EmergencyElectionService

    @BeforeEach
    fun setUp() {
        electionRepository = mockk()
        teamMemberRepository = mockk()
        emergencyElectionService =
            EmergencyElectionService(
                electionRepository,
                teamMemberRepository,
            )
    }

    @Nested
    @DisplayName("triggerEmergencyElection")
    inner class TriggerEmergencyElection {
        @Test
        fun `MANAGER가 긴급 선거를 발동할 수 있다`() {
            // given
            val now = Instant.now()
            val request =
                TriggerEmergencyElectionRequest(
                    teamId = 1L,
                    requesterId = 10L,
                    title = "비상대책위원회 긴급 선거",
                    description = "구단주 부재로 인한 긴급 선거",
                    startAt = now,
                    endAt = now.plus(14, ChronoUnit.DAYS),
                )

            val managerMember =
                mockk<TeamMember> {
                    every { role } returns TeamMemberRole.MANAGER
                }

            every { teamMemberRepository.findByIdOrNull(10L) } returns managerMember
            every { electionRepository.findAllByTeamId(1L) } returns emptyList()
            every { electionRepository.save(any()) } answers { firstArg() }

            // when
            val result = emergencyElectionService.triggerEmergencyElection(request)

            // then
            assertThat(result.electionType).isEqualTo(ElectionType.EMERGENCY)
            assertThat(result.status).isEqualTo(ElectionStatus.IN_PROGRESS)
            assertThat(result.triggeredByMemberId).isEqualTo(10L)
            assertThat(result.regularElectionDeadline).isNotNull
            verify { electionRepository.save(any()) }
        }

        @Test
        fun `MANAGER가 아닌 멤버가 긴급 선거를 발동하면 예외가 발생한다`() {
            // given
            val now = Instant.now()
            val request =
                TriggerEmergencyElectionRequest(
                    teamId = 1L,
                    requesterId = 10L,
                    title = "비상대책위원회 긴급 선거",
                    startAt = now,
                    endAt = now.plus(14, ChronoUnit.DAYS),
                )

            val memberRole =
                mockk<TeamMember> {
                    every { role } returns TeamMemberRole.MEMBER
                }

            every { teamMemberRepository.findByIdOrNull(10L) } returns memberRole

            // when & then
            assertThatThrownBy {
                emergencyElectionService.triggerEmergencyElection(request)
            }.isInstanceOf(UnauthorizedEmergencyElectionException::class.java)
        }

        @Test
        fun `OWNER가 긴급 선거를 발동하면 예외가 발생한다`() {
            // given
            val now = Instant.now()
            val request =
                TriggerEmergencyElectionRequest(
                    teamId = 1L,
                    requesterId = 10L,
                    title = "비상대책위원회 긴급 선거",
                    startAt = now,
                    endAt = now.plus(14, ChronoUnit.DAYS),
                )

            val ownerMember =
                mockk<TeamMember> {
                    every { role } returns TeamMemberRole.OWNER
                }

            every { teamMemberRepository.findByIdOrNull(10L) } returns ownerMember

            // when & then
            assertThatThrownBy {
                emergencyElectionService.triggerEmergencyElection(request)
            }.isInstanceOf(UnauthorizedEmergencyElectionException::class.java)
        }

        @Test
        fun `존재하지 않는 멤버가 긴급 선거를 발동하면 예외가 발생한다`() {
            // given
            val now = Instant.now()
            val request =
                TriggerEmergencyElectionRequest(
                    teamId = 1L,
                    requesterId = 999L,
                    title = "비상대책위원회 긴급 선거",
                    startAt = now,
                    endAt = now.plus(14, ChronoUnit.DAYS),
                )

            every { teamMemberRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                emergencyElectionService.triggerEmergencyElection(request)
            }.isInstanceOf(TeamMemberNotFoundException::class.java)
        }

        @Test
        fun `이미 진행 중인 선거가 있으면 긴급 선거 발동 시 예외가 발생한다`() {
            // given
            val now = Instant.now()
            val request =
                TriggerEmergencyElectionRequest(
                    teamId = 1L,
                    requesterId = 10L,
                    title = "비상대책위원회 긴급 선거",
                    startAt = now,
                    endAt = now.plus(14, ChronoUnit.DAYS),
                )

            val managerMember =
                mockk<TeamMember> {
                    every { role } returns TeamMemberRole.MANAGER
                }
            val activeElection = createTestElection(1L, ElectionStatus.IN_PROGRESS)

            every { teamMemberRepository.findByIdOrNull(10L) } returns managerMember
            every { electionRepository.findAllByTeamId(1L) } returns listOf(activeElection)

            // when & then
            assertThatThrownBy {
                emergencyElectionService.triggerEmergencyElection(request)
            }.isInstanceOf(ActiveElectionAlreadyExistsException::class.java)
        }

        @Test
        fun `이미 예정된 선거가 있으면 긴급 선거 발동 시 예외가 발생한다`() {
            // given
            val now = Instant.now()
            val request =
                TriggerEmergencyElectionRequest(
                    teamId = 1L,
                    requesterId = 10L,
                    title = "비상대책위원회 긴급 선거",
                    startAt = now,
                    endAt = now.plus(14, ChronoUnit.DAYS),
                )

            val managerMember =
                mockk<TeamMember> {
                    every { role } returns TeamMemberRole.MANAGER
                }
            val scheduledElection = createTestElection(1L, ElectionStatus.SCHEDULED)

            every { teamMemberRepository.findByIdOrNull(10L) } returns managerMember
            every { electionRepository.findAllByTeamId(1L) } returns listOf(scheduledElection)

            // when & then
            assertThatThrownBy {
                emergencyElectionService.triggerEmergencyElection(request)
            }.isInstanceOf(ActiveElectionAlreadyExistsException::class.java)
        }

        @Test
        fun `완료된 선거만 있을 때 긴급 선거를 발동할 수 있다`() {
            // given
            val now = Instant.now()
            val request =
                TriggerEmergencyElectionRequest(
                    teamId = 1L,
                    requesterId = 10L,
                    title = "비상대책위원회 긴급 선거",
                    startAt = now,
                    endAt = now.plus(14, ChronoUnit.DAYS),
                )

            val managerMember =
                mockk<TeamMember> {
                    every { role } returns TeamMemberRole.MANAGER
                }
            val completedElection = createTestElection(1L, ElectionStatus.COMPLETED)

            every { teamMemberRepository.findByIdOrNull(10L) } returns managerMember
            every { electionRepository.findAllByTeamId(1L) } returns listOf(completedElection)
            every { electionRepository.save(any()) } answers { firstArg() }

            // when
            val result = emergencyElectionService.triggerEmergencyElection(request)

            // then
            assertThat(result.electionType).isEqualTo(ElectionType.EMERGENCY)
            assertThat(result.status).isEqualTo(ElectionStatus.IN_PROGRESS)
        }
    }

    @Nested
    @DisplayName("designateActingOwner")
    inner class DesignateActingOwner {
        @Test
        fun `긴급 선거에서 MANAGER 멤버를 임시 구단주로 지정할 수 있다`() {
            // given
            val emergencyElection = createTestEmergencyElection(1L)
            val managerMember =
                mockk<TeamMember> {
                    every { role } returns TeamMemberRole.MANAGER
                }

            every { electionRepository.findById(1L) } returns emergencyElection
            every { teamMemberRepository.findByIdOrNull(20L) } returns managerMember
            every { electionRepository.save(any()) } answers { firstArg() }

            val request = DesignateActingOwnerRequest(electionId = 1L, actingOwnerMemberId = 20L)

            // when
            val result = emergencyElectionService.designateActingOwner(request)

            // then
            assertThat(result.actingOwnerMemberId).isEqualTo(20L)
            assertThat(result.actingOwnerPermissions).isNotNull
            assertThat(result.actingOwnerPermissions!!.canManageLineup).isTrue()
            assertThat(result.actingOwnerPermissions!!.canManageSchedule).isTrue()
            assertThat(result.actingOwnerPermissions!!.canKickMember).isFalse()
            assertThat(result.actingOwnerPermissions!!.canDissolveTeam).isFalse()
            assertThat(result.actingOwnerPermissions!!.canTransferOwnership).isFalse()
            verify { electionRepository.save(any()) }
        }

        @Test
        fun `긴급 선거가 아닌 선거에서 임시 구단주를 지정하면 예외가 발생한다`() {
            // given
            val normalElection = createTestElection(1L, ElectionStatus.IN_PROGRESS)

            every { electionRepository.findById(1L) } returns normalElection

            val request = DesignateActingOwnerRequest(electionId = 1L, actingOwnerMemberId = 20L)

            // when & then
            assertThatThrownBy {
                emergencyElectionService.designateActingOwner(request)
            }.isInstanceOf(InvalidStateException::class.java)
                .hasMessageContaining("not an emergency election")
        }

        @Test
        fun `존재하지 않는 선거에서 임시 구단주를 지정하면 예외가 발생한다`() {
            // given
            every { electionRepository.findById(999L) } returns null

            val request = DesignateActingOwnerRequest(electionId = 999L, actingOwnerMemberId = 20L)

            // when & then
            assertThatThrownBy {
                emergencyElectionService.designateActingOwner(request)
            }.isInstanceOf(ElectionNotFoundException::class.java)
        }

        @Test
        fun `MANAGER가 아닌 멤버를 임시 구단주로 지정하면 예외가 발생한다`() {
            // given
            val emergencyElection = createTestEmergencyElection(1L)
            val regularMember =
                mockk<TeamMember> {
                    every { role } returns TeamMemberRole.MEMBER
                }

            every { electionRepository.findById(1L) } returns emergencyElection
            every { teamMemberRepository.findByIdOrNull(20L) } returns regularMember

            val request = DesignateActingOwnerRequest(electionId = 1L, actingOwnerMemberId = 20L)

            // when & then
            assertThatThrownBy {
                emergencyElectionService.designateActingOwner(request)
            }.isInstanceOf(InvalidActingOwnerException::class.java)
        }

        @Test
        fun `존재하지 않는 멤버를 임시 구단주로 지정하면 예외가 발생한다`() {
            // given
            val emergencyElection = createTestEmergencyElection(1L)

            every { electionRepository.findById(1L) } returns emergencyElection
            every { teamMemberRepository.findByIdOrNull(999L) } returns null

            val request = DesignateActingOwnerRequest(electionId = 1L, actingOwnerMemberId = 999L)

            // when & then
            assertThatThrownBy {
                emergencyElectionService.designateActingOwner(request)
            }.isInstanceOf(TeamMemberNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("createRegularElectionAfterEmergency")
    inner class CreateRegularElectionAfterEmergency {
        @Test
        fun `긴급 선거 완료 후 정규 선거를 자동 생성할 수 있다`() {
            // given
            val emergencyElection = createTestEmergencyElection(1L)

            every { electionRepository.save(any()) } answers { firstArg() }

            // when
            val result =
                emergencyElectionService.createRegularElectionAfterEmergency(emergencyElection)

            // then
            assertThat(result.electionType).isEqualTo(ElectionType.OWNER_ELECTION)
            assertThat(result.teamId).isEqualTo(emergencyElection.teamId)
            assertThat(result.title).contains("정규")
            verify { electionRepository.save(any()) }
        }

        @Test
        fun `일반 선거로 정규 선거 자동 생성을 호출하면 예외가 발생한다`() {
            // given
            val normalElection = createTestElection(1L, ElectionStatus.COMPLETED)

            // when & then
            assertThatThrownBy {
                emergencyElectionService.createRegularElectionAfterEmergency(normalElection)
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    // --- helper methods ---

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

    private fun createTestEmergencyElection(id: Long): Election {
        val now = Instant.now()
        val election =
            Election.createEmergency(
                teamId = 1L,
                triggeredByMemberId = 10L,
                title = "비상대책위원회 긴급 선거",
                description = "테스트 긴급 선거",
                startAt = now,
                endAt = now.plus(14, ChronoUnit.DAYS),
            )
        setElectionId(election, id)
        return election
    }

    private fun setElectionId(
        election: Election,
        id: Long,
    ) {
        val idField = Election::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(election, id)
    }
}
