package com.nextup.api.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nextup.api.dto.election.CastVoteApiRequest
import com.nextup.api.dto.election.CreateElectionApiRequest
import com.nextup.api.dto.election.RegisterCandidateApiRequest
import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.CandidateNotFoundException
import com.nextup.common.exception.DuplicateVoteException
import com.nextup.common.exception.ElectionNotFoundException
import com.nextup.common.exception.InvalidStateException
import com.nextup.core.domain.election.Candidate
import com.nextup.core.domain.election.Election
import com.nextup.core.domain.election.ElectionStatus
import com.nextup.core.domain.election.ElectionType
import com.nextup.core.service.election.ElectionService
import com.nextup.core.service.election.dto.ElectionResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant
import java.time.temporal.ChronoUnit

class ElectionControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var electionService: ElectionService
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        electionService = mockk()
        val controller = ElectionController(electionService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(GlobalExceptionHandler()).build()
        objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    }

    private fun createMockElection(
        id: Long = 1L,
        teamId: Long = 1L,
        title: String = "구단주 선출",
        description: String? = null,
        electionType: ElectionType = ElectionType.OWNER_ELECTION,
        startAt: Instant = Instant.now().plus(1, ChronoUnit.DAYS),
        endAt: Instant = Instant.now().plus(8, ChronoUnit.DAYS),
        status: ElectionStatus = ElectionStatus.SCHEDULED,
        isVotingOpen: Boolean = false,
        createdAt: Instant = Instant.now(),
    ): Election =
        mockk {
            every { this@mockk.id } returns id
            every { this@mockk.teamId } returns teamId
            every { this@mockk.title } returns title
            every { this@mockk.description } returns description
            every { this@mockk.electionType } returns electionType
            every { this@mockk.startAt } returns startAt
            every { this@mockk.endAt } returns endAt
            every { this@mockk.status } returns status
            every { isVotingOpen() } returns isVotingOpen
            every { this@mockk.createdAt } returns createdAt
            every { this@mockk.triggeredByMemberId } returns null
            every { this@mockk.actingOwnerMemberId } returns null
            every { this@mockk.actingOwnerPermissions } returns null
            every { this@mockk.regularElectionDeadline } returns null
        }

    private fun createMockCandidate(
        id: Long,
        electionId: Long,
        memberId: Long,
        memberName: String,
        statement: String? = null,
        createdAt: Instant = Instant.now(),
    ): Candidate =
        mockk {
            every { this@mockk.id } returns id
            every { this@mockk.electionId } returns electionId
            every { this@mockk.memberId } returns memberId
            every { this@mockk.memberName } returns memberName
            every { this@mockk.statement } returns statement
            every { this@mockk.createdAt } returns createdAt
        }

    @Test
    fun `should create election`() {
        // given
        val teamId = 1L
        val startAt = Instant.now().plus(1, ChronoUnit.DAYS)
        val endAt = startAt.plus(7, ChronoUnit.DAYS)
        val request =
            CreateElectionApiRequest(
                title = "구단주 선출",
                description = "2024년 구단주 선출",
                electionType = ElectionType.OWNER_ELECTION,
                startAt = startAt,
                endAt = endAt,
            )
        val election =
            createMockElection(
                id = 1L,
                teamId = teamId,
                title = request.title,
                description = request.description,
                electionType = request.electionType,
                startAt = request.startAt,
                endAt = request.endAt,
            )

        every { electionService.createElection(any()) } returns election

        // when & then
        mockMvc
            .perform(
                post("/api/v1/teams/$teamId/elections")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.teamId").value(teamId))
            .andExpect(jsonPath("$.data.title").value(request.title))
            .andExpect(jsonPath("$.data.electionType").value("OWNER_ELECTION"))
            .andExpect(jsonPath("$.data.status").value("SCHEDULED"))

        verify(exactly = 1) { electionService.createElection(any()) }
    }

    @Test
    fun `should get elections by team`() {
        // given
        val teamId = 1L
        val election =
            createMockElection(
                id = 1L,
                teamId = teamId,
                title = "구단주 선출",
            )

        every { electionService.getElectionsByTeam(teamId) } returns listOf(election)

        // when & then
        mockMvc
            .perform(get("/api/v1/teams/$teamId/elections"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].teamId").value(teamId))

        verify(exactly = 1) { electionService.getElectionsByTeam(teamId) }
    }

    @Test
    fun `should get election by id`() {
        // given
        val teamId = 1L
        val electionId = 1L
        val election = createMockElection(id = electionId, teamId = teamId)

        every { electionService.getElectionById(electionId) } returns election

        // when & then
        mockMvc
            .perform(get("/api/v1/teams/$teamId/elections/$electionId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(electionId))

        verify(exactly = 1) { electionService.getElectionById(electionId) }
    }

    @Test
    fun `should return 404 when election not found`() {
        // given
        val teamId = 1L
        val electionId = 999L

        every { electionService.getElectionById(electionId) } throws ElectionNotFoundException(electionId)

        // when & then
        mockMvc
            .perform(get("/api/v1/teams/$teamId/elections/$electionId"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("ELECTION_NOT_FOUND"))

        verify(exactly = 1) { electionService.getElectionById(electionId) }
    }

    @Test
    fun `should start election`() {
        // given
        val teamId = 1L
        val electionId = 1L
        val election =
            createMockElection(
                id = electionId,
                teamId = teamId,
                status = ElectionStatus.IN_PROGRESS,
            )

        every { electionService.startElection(electionId) } returns election

        // when & then
        mockMvc
            .perform(put("/api/v1/teams/$teamId/elections/$electionId/start"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))

        verify(exactly = 1) { electionService.startElection(electionId) }
    }

    @Test
    fun `should complete election`() {
        // given
        val teamId = 1L
        val electionId = 1L
        val election =
            createMockElection(
                id = electionId,
                teamId = teamId,
                status = ElectionStatus.COMPLETED,
            )

        every { electionService.completeElection(electionId) } returns election

        // when & then
        mockMvc
            .perform(put("/api/v1/teams/$teamId/elections/$electionId/complete"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("COMPLETED"))

        verify(exactly = 1) { electionService.completeElection(electionId) }
    }

    @Test
    fun `should cancel election`() {
        // given
        val teamId = 1L
        val electionId = 1L
        val election =
            createMockElection(
                id = electionId,
                teamId = teamId,
                status = ElectionStatus.CANCELLED,
            )

        every { electionService.cancelElection(electionId) } returns election

        // when & then
        mockMvc
            .perform(put("/api/v1/teams/$teamId/elections/$electionId/cancel"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("CANCELLED"))

        verify(exactly = 1) { electionService.cancelElection(electionId) }
    }

    @Test
    fun `should register candidate`() {
        // given
        val teamId = 1L
        val electionId = 1L
        val request =
            RegisterCandidateApiRequest(
                memberId = 100L,
                memberName = "홍길동",
                statement = "열심히 하겠습니다",
            )
        val candidate =
            createMockCandidate(
                id = 1L,
                electionId = electionId,
                memberId = request.memberId,
                memberName = request.memberName,
                statement = request.statement,
            )

        every { electionService.registerCandidate(any()) } returns candidate

        // when & then
        mockMvc
            .perform(
                post("/api/v1/teams/$teamId/elections/$electionId/candidates")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.memberId").value(request.memberId))
            .andExpect(jsonPath("$.data.memberName").value(request.memberName))

        verify(exactly = 1) { electionService.registerCandidate(any()) }
    }

    @Test
    fun `should return 400 when registering candidate to non-scheduled election`() {
        // given
        val teamId = 1L
        val electionId = 1L
        val request =
            RegisterCandidateApiRequest(
                memberId = 100L,
                memberName = "홍길동",
                statement = null,
            )

        every { electionService.registerCandidate(any()) } throws
            InvalidStateException(
                code = "CANNOT_REGISTER_CANDIDATE",
                message = "Cannot register candidate: election status is IN_PROGRESS",
            )

        // when & then
        mockMvc
            .perform(
                post("/api/v1/teams/$teamId/elections/$electionId/candidates")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("CANNOT_REGISTER_CANDIDATE"))

        verify(exactly = 1) { electionService.registerCandidate(any()) }
    }

    @Test
    fun `should cast vote`() {
        // given
        val teamId = 1L
        val electionId = 1L
        val request =
            CastVoteApiRequest(
                voterId = 100L,
                candidateId = 10L,
            )
        val candidate =
            createMockCandidate(
                id = request.candidateId,
                electionId = electionId,
                memberId = 200L,
                memberName = "홍길동",
            )

        every { electionService.vote(any()) } returns candidate

        // when & then
        mockMvc
            .perform(
                post("/api/v1/teams/$teamId/elections/$electionId/votes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(request.candidateId))

        verify(exactly = 1) { electionService.vote(any()) }
    }

    @Test
    fun `should return 400 when voting twice`() {
        // given
        val teamId = 1L
        val electionId = 1L
        val request =
            CastVoteApiRequest(
                voterId = 100L,
                candidateId = 10L,
            )

        every { electionService.vote(any()) } throws DuplicateVoteException()

        // when & then
        mockMvc
            .perform(
                post("/api/v1/teams/$teamId/elections/$electionId/votes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("DUPLICATE_VOTE"))

        verify(exactly = 1) { electionService.vote(any()) }
    }

    @Test
    fun `should return 404 when voting for non-existent candidate`() {
        // given
        val teamId = 1L
        val electionId = 1L
        val candidateId = 999L
        val request =
            CastVoteApiRequest(
                voterId = 100L,
                candidateId = candidateId,
            )

        every { electionService.vote(any()) } throws CandidateNotFoundException(candidateId)

        // when & then
        mockMvc
            .perform(
                post("/api/v1/teams/$teamId/elections/$electionId/votes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("CANDIDATE_NOT_FOUND"))

        verify(exactly = 1) { electionService.vote(any()) }
    }

    @Test
    fun `should get election results`() {
        // given
        val teamId = 1L
        val electionId = 1L
        val election =
            createMockElection(
                id = electionId,
                teamId = teamId,
                status = ElectionStatus.COMPLETED,
                startAt = Instant.now().minus(2, ChronoUnit.DAYS),
                endAt = Instant.now().minus(1, ChronoUnit.DAYS),
            )
        val result =
            ElectionResult(
                election = election,
                candidateResults = emptyList(),
                totalVotes = 0,
            )

        every { electionService.getResults(electionId) } returns result

        // when & then
        mockMvc
            .perform(get("/api/v1/teams/$teamId/elections/$electionId/results"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.election.id").value(electionId))
            .andExpect(jsonPath("$.data.totalVotes").value(0))

        verify(exactly = 1) { electionService.getResults(electionId) }
    }
}
