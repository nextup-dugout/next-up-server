package com.nextup.core.service.mercenary

import com.nextup.common.exception.MercenaryAlreadyAppliedException
import com.nextup.common.exception.MercenaryApplicationNotFoundException
import com.nextup.common.exception.MercenaryMaxCountReachedException
import com.nextup.common.exception.MercenaryRequestClosedException
import com.nextup.common.exception.MercenaryRequestNotFoundException
import com.nextup.core.domain.mercenary.MercenaryApplication
import com.nextup.core.domain.mercenary.MercenaryApplicationStatus
import com.nextup.core.domain.mercenary.MercenaryParticipation
import com.nextup.core.domain.mercenary.MercenaryRequest
import com.nextup.core.domain.mercenary.MercenaryRequestStatus
import com.nextup.core.domain.player.Position
import com.nextup.core.port.repository.MercenaryApplicationRepositoryPort
import com.nextup.core.port.repository.MercenaryParticipationRepositoryPort
import com.nextup.core.port.repository.MercenaryRequestRepositoryPort
import com.nextup.core.service.mercenary.dto.ApplyMercenaryDto
import com.nextup.core.service.mercenary.dto.CreateMercenaryRequestDto
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.temporal.ChronoUnit

class MercenaryServiceTest {
    private lateinit var mercenaryRequestRepository: MercenaryRequestRepositoryPort
    private lateinit var mercenaryApplicationRepository: MercenaryApplicationRepositoryPort
    private lateinit var mercenaryParticipationRepository: MercenaryParticipationRepositoryPort
    private lateinit var mercenaryService: MercenaryService

    private val futureDeadline = Instant.now().plus(7, ChronoUnit.DAYS)

    @BeforeEach
    fun setUp() {
        mercenaryRequestRepository = mockk()
        mercenaryApplicationRepository = mockk()
        mercenaryParticipationRepository = mockk()
        mercenaryService =
            MercenaryService(
                mercenaryRequestRepository = mercenaryRequestRepository,
                mercenaryApplicationRepository = mercenaryApplicationRepository,
                mercenaryParticipationRepository = mercenaryParticipationRepository,
            )
    }

    // ========== createRequest 테스트 ==========

    @Test
    fun `용병 요청을 생성할 수 있다`() {
        // given
        val dto =
            CreateMercenaryRequestDto(
                requestingTeamId = 1L,
                gameId = 10L,
                positions = setOf(Position.CATCHER, Position.SHORTSTOP),
                maxCount = 2,
                deadline = futureDeadline,
                description = "포수, 유격수 구합니다",
            )

        every { mercenaryRequestRepository.save(any()) } answers { firstArg() }

        // when
        val result = mercenaryService.createRequest(dto)

        // then
        assertThat(result.requestingTeamId).isEqualTo(1L)
        assertThat(result.gameId).isEqualTo(10L)
        assertThat(result.positions).containsExactlyInAnyOrder(Position.CATCHER, Position.SHORTSTOP)
        assertThat(result.maxCount).isEqualTo(2)
        assertThat(result.status).isEqualTo(MercenaryRequestStatus.OPEN)

        verify { mercenaryRequestRepository.save(any()) }
    }

    // ========== getOpenRequests 테스트 ==========

    @Test
    fun `OPEN 상태의 용병 요청 목록을 조회할 수 있다`() {
        // given
        val request1 =
            MercenaryRequest.create(
                requestingTeamId = 1L,
                gameId = 10L,
                positions = setOf(Position.CATCHER),
                maxCount = 1,
                deadline = futureDeadline,
            )
        val request2 =
            MercenaryRequest.create(
                requestingTeamId = 2L,
                gameId = 20L,
                positions = setOf(Position.SHORTSTOP),
                maxCount = 1,
                deadline = futureDeadline,
            )

        every {
            mercenaryRequestRepository.findByStatus(MercenaryRequestStatus.OPEN)
        } returns listOf(request1, request2)

        // when
        val result = mercenaryService.getOpenRequests()

        // then
        assertThat(result).hasSize(2)

        verify { mercenaryRequestRepository.findByStatus(MercenaryRequestStatus.OPEN) }
    }

    @Test
    fun `OPEN 상태의 용병 요청이 없으면 빈 리스트를 반환한다`() {
        // given
        every {
            mercenaryRequestRepository.findByStatus(MercenaryRequestStatus.OPEN)
        } returns emptyList()

        // when
        val result = mercenaryService.getOpenRequests()

        // then
        assertThat(result).isEmpty()
    }

    // ========== getRequestById 테스트 ==========

    @Test
    fun `ID로 용병 요청을 조회할 수 있다`() {
        // given
        val request =
            MercenaryRequest.create(
                requestingTeamId = 1L,
                gameId = 10L,
                positions = setOf(Position.CATCHER),
                maxCount = 1,
                deadline = futureDeadline,
            )

        every { mercenaryRequestRepository.findByIdOrNull(1L) } returns request

        // when
        val result = mercenaryService.getRequestById(1L)

        // then
        assertThat(result).isEqualTo(request)

        verify { mercenaryRequestRepository.findByIdOrNull(1L) }
    }

    @Test
    fun `존재하지 않는 ID로 용병 요청 조회 시 예외가 발생한다`() {
        // given
        every { mercenaryRequestRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThrows<MercenaryRequestNotFoundException> {
            mercenaryService.getRequestById(999L)
        }

        verify { mercenaryRequestRepository.findByIdOrNull(999L) }
    }

    // ========== apply 테스트 ==========

    @Test
    fun `용병 요청에 지원할 수 있다`() {
        // given
        val request =
            MercenaryRequest.create(
                requestingTeamId = 1L,
                gameId = 10L,
                positions = setOf(Position.CATCHER),
                maxCount = 2,
                deadline = futureDeadline,
            )

        val dto =
            ApplyMercenaryDto(
                requestId = 1L,
                playerId = 100L,
                preferredPositions = setOf(Position.CATCHER),
                message = "포수 가능합니다",
            )

        every { mercenaryRequestRepository.findByIdOrNull(1L) } returns request
        every {
            mercenaryApplicationRepository.existsByRequestIdAndPlayerId(1L, 100L)
        } returns false
        every { mercenaryApplicationRepository.save(any()) } answers { firstArg() }

        // when
        val result = mercenaryService.apply(dto)

        // then
        assertThat(result.requestId).isEqualTo(1L)
        assertThat(result.playerId).isEqualTo(100L)
        assertThat(result.preferredPositions).containsExactly(Position.CATCHER)
        assertThat(result.status).isEqualTo(MercenaryApplicationStatus.PENDING)
        assertThat(result.message).isEqualTo("포수 가능합니다")

        verify { mercenaryRequestRepository.findByIdOrNull(1L) }
        verify { mercenaryApplicationRepository.existsByRequestIdAndPlayerId(1L, 100L) }
        verify { mercenaryApplicationRepository.save(any()) }
    }

    @Test
    fun `존재하지 않는 용병 요청에 지원 시 예외가 발생한다`() {
        // given
        val dto =
            ApplyMercenaryDto(
                requestId = 999L,
                playerId = 100L,
                preferredPositions = setOf(Position.CATCHER),
            )

        every { mercenaryRequestRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThrows<MercenaryRequestNotFoundException> {
            mercenaryService.apply(dto)
        }

        verify { mercenaryRequestRepository.findByIdOrNull(999L) }
    }

    @Test
    fun `마감된 용병 요청에 지원 시 예외가 발생한다`() {
        // given
        val request =
            MercenaryRequest.create(
                requestingTeamId = 1L,
                gameId = 10L,
                positions = setOf(Position.CATCHER),
                maxCount = 1,
                deadline = futureDeadline,
            )
        request.close()

        val dto =
            ApplyMercenaryDto(
                requestId = 1L,
                playerId = 100L,
                preferredPositions = setOf(Position.CATCHER),
            )

        every { mercenaryRequestRepository.findByIdOrNull(1L) } returns request

        // when & then
        assertThrows<MercenaryRequestClosedException> {
            mercenaryService.apply(dto)
        }

        verify { mercenaryRequestRepository.findByIdOrNull(1L) }
    }

    @Test
    fun `이미 지원한 요청에 재지원 시 예외가 발생한다`() {
        // given
        val request =
            MercenaryRequest.create(
                requestingTeamId = 1L,
                gameId = 10L,
                positions = setOf(Position.CATCHER),
                maxCount = 2,
                deadline = futureDeadline,
            )

        val dto =
            ApplyMercenaryDto(
                requestId = 1L,
                playerId = 100L,
                preferredPositions = setOf(Position.CATCHER),
            )

        every { mercenaryRequestRepository.findByIdOrNull(1L) } returns request
        every {
            mercenaryApplicationRepository.existsByRequestIdAndPlayerId(1L, 100L)
        } returns true

        // when & then
        assertThrows<MercenaryAlreadyAppliedException> {
            mercenaryService.apply(dto)
        }

        verify { mercenaryRequestRepository.findByIdOrNull(1L) }
        verify { mercenaryApplicationRepository.existsByRequestIdAndPlayerId(1L, 100L) }
    }

    // ========== acceptApplication 테스트 ==========

    @Test
    fun `용병 지원을 수락할 수 있다`() {
        // given
        val request =
            MercenaryRequest.create(
                requestingTeamId = 1L,
                gameId = 10L,
                positions = setOf(Position.CATCHER),
                maxCount = 2,
                deadline = futureDeadline,
            )

        val application =
            MercenaryApplication.create(
                requestId = 1L,
                playerId = 100L,
                preferredPositions = setOf(Position.CATCHER),
            )

        every { mercenaryRequestRepository.findByIdOrNull(1L) } returns request
        every { mercenaryApplicationRepository.findByIdOrNull(1L) } returns application
        every { mercenaryApplicationRepository.countAcceptedByRequestId(1L) } returns 0L
        every { mercenaryParticipationRepository.save(any()) } answers { firstArg() }

        // when
        val result = mercenaryService.acceptApplication(1L, 1L)

        // then
        assertThat(result.status).isEqualTo(MercenaryApplicationStatus.ACCEPTED)

        verify { mercenaryParticipationRepository.save(any()) }
    }

    @Test
    fun `최대 인원 도달 시 수락하면 요청이 자동 마감된다`() {
        // given
        val request =
            MercenaryRequest.create(
                requestingTeamId = 1L,
                gameId = 10L,
                positions = setOf(Position.CATCHER),
                maxCount = 1,
                deadline = futureDeadline,
            )

        val application =
            MercenaryApplication.create(
                requestId = 1L,
                playerId = 100L,
                preferredPositions = setOf(Position.CATCHER),
            )

        every { mercenaryRequestRepository.findByIdOrNull(1L) } returns request
        every { mercenaryApplicationRepository.findByIdOrNull(1L) } returns application
        every { mercenaryApplicationRepository.countAcceptedByRequestId(1L) } returns 0L
        every { mercenaryParticipationRepository.save(any()) } answers { firstArg() }

        // when
        mercenaryService.acceptApplication(1L, 1L)

        // then
        assertThat(request.status).isEqualTo(MercenaryRequestStatus.CLOSED)
    }

    @Test
    fun `존재하지 않는 용병 요청에 대한 수락 시 예외가 발생한다`() {
        // given
        every { mercenaryRequestRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThrows<MercenaryRequestNotFoundException> {
            mercenaryService.acceptApplication(999L, 1L)
        }

        verify { mercenaryRequestRepository.findByIdOrNull(999L) }
    }

    @Test
    fun `존재하지 않는 지원에 대한 수락 시 예외가 발생한다`() {
        // given
        val request =
            MercenaryRequest.create(
                requestingTeamId = 1L,
                gameId = 10L,
                positions = setOf(Position.CATCHER),
                maxCount = 1,
                deadline = futureDeadline,
            )

        every { mercenaryRequestRepository.findByIdOrNull(1L) } returns request
        every { mercenaryApplicationRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThrows<MercenaryApplicationNotFoundException> {
            mercenaryService.acceptApplication(1L, 999L)
        }

        verify { mercenaryRequestRepository.findByIdOrNull(1L) }
        verify { mercenaryApplicationRepository.findByIdOrNull(999L) }
    }

    @Test
    fun `최대 인원이 이미 도달한 경우 수락 시 예외가 발생한다`() {
        // given
        val request =
            MercenaryRequest.create(
                requestingTeamId = 1L,
                gameId = 10L,
                positions = setOf(Position.CATCHER),
                maxCount = 1,
                deadline = futureDeadline,
            )

        val application =
            MercenaryApplication.create(
                requestId = 1L,
                playerId = 200L,
                preferredPositions = setOf(Position.CATCHER),
            )

        every { mercenaryRequestRepository.findByIdOrNull(1L) } returns request
        every { mercenaryApplicationRepository.findByIdOrNull(2L) } returns application
        every { mercenaryApplicationRepository.countAcceptedByRequestId(1L) } returns 1L

        // when & then
        assertThrows<MercenaryMaxCountReachedException> {
            mercenaryService.acceptApplication(1L, 2L)
        }

        verify { mercenaryRequestRepository.findByIdOrNull(1L) }
        verify { mercenaryApplicationRepository.findByIdOrNull(2L) }
        verify { mercenaryApplicationRepository.countAcceptedByRequestId(1L) }
    }

    // ========== rejectApplication 테스트 ==========

    @Test
    fun `용병 지원을 거절할 수 있다`() {
        // given
        val request =
            MercenaryRequest.create(
                requestingTeamId = 1L,
                gameId = 10L,
                positions = setOf(Position.CATCHER),
                maxCount = 1,
                deadline = futureDeadline,
            )

        val application =
            MercenaryApplication.create(
                requestId = 1L,
                playerId = 100L,
                preferredPositions = setOf(Position.CATCHER),
            )

        every { mercenaryRequestRepository.findByIdOrNull(1L) } returns request
        every { mercenaryApplicationRepository.findByIdOrNull(1L) } returns application

        // when
        val result = mercenaryService.rejectApplication(1L, 1L)

        // then
        assertThat(result.status).isEqualTo(MercenaryApplicationStatus.REJECTED)

        verify { mercenaryRequestRepository.findByIdOrNull(1L) }
        verify { mercenaryApplicationRepository.findByIdOrNull(1L) }
    }

    @Test
    fun `존재하지 않는 용병 요청에 대한 거절 시 예외가 발생한다`() {
        // given
        every { mercenaryRequestRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThrows<MercenaryRequestNotFoundException> {
            mercenaryService.rejectApplication(999L, 1L)
        }
    }

    @Test
    fun `존재하지 않는 지원에 대한 거절 시 예외가 발생한다`() {
        // given
        val request =
            MercenaryRequest.create(
                requestingTeamId = 1L,
                gameId = 10L,
                positions = setOf(Position.CATCHER),
                maxCount = 1,
                deadline = futureDeadline,
            )

        every { mercenaryRequestRepository.findByIdOrNull(1L) } returns request
        every { mercenaryApplicationRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThrows<MercenaryApplicationNotFoundException> {
            mercenaryService.rejectApplication(1L, 999L)
        }
    }

    // ========== getApplicationsByRequest 테스트 ==========

    @Test
    fun `용병 요청에 대한 지원 목록을 조회할 수 있다`() {
        // given
        val request =
            MercenaryRequest.create(
                requestingTeamId = 1L,
                gameId = 10L,
                positions = setOf(Position.CATCHER),
                maxCount = 2,
                deadline = futureDeadline,
            )

        val app1 =
            MercenaryApplication.create(
                requestId = 1L,
                playerId = 100L,
                preferredPositions = setOf(Position.CATCHER),
            )
        val app2 =
            MercenaryApplication.create(
                requestId = 1L,
                playerId = 200L,
                preferredPositions = setOf(Position.CATCHER),
            )

        every { mercenaryRequestRepository.findByIdOrNull(1L) } returns request
        every { mercenaryApplicationRepository.findByRequestId(1L) } returns listOf(app1, app2)

        // when
        val result = mercenaryService.getApplicationsByRequest(1L)

        // then
        assertThat(result).hasSize(2)

        verify { mercenaryRequestRepository.findByIdOrNull(1L) }
        verify { mercenaryApplicationRepository.findByRequestId(1L) }
    }

    @Test
    fun `존재하지 않는 용병 요청의 지원 목록 조회 시 예외가 발생한다`() {
        // given
        every { mercenaryRequestRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThrows<MercenaryRequestNotFoundException> {
            mercenaryService.getApplicationsByRequest(999L)
        }
    }

    // ========== getParticipationsByPlayer 테스트 ==========

    @Test
    fun `선수의 용병 참가 이력을 조회할 수 있다`() {
        // given
        val p1 = MercenaryParticipation.create(gameId = 10L, playerId = 100L, teamId = 1L)
        val p2 = MercenaryParticipation.create(gameId = 20L, playerId = 100L, teamId = 2L)

        every { mercenaryParticipationRepository.findByPlayerId(100L) } returns listOf(p1, p2)

        // when
        val result = mercenaryService.getParticipationsByPlayer(100L)

        // then
        assertThat(result).hasSize(2)

        verify { mercenaryParticipationRepository.findByPlayerId(100L) }
    }

    @Test
    fun `용병 참가 이력이 없으면 빈 리스트를 반환한다`() {
        // given
        every { mercenaryParticipationRepository.findByPlayerId(100L) } returns emptyList()

        // when
        val result = mercenaryService.getParticipationsByPlayer(100L)

        // then
        assertThat(result).isEmpty()
    }

    // ========== cancelRequest 테스트 ==========

    @Test
    fun `용병 요청을 취소할 수 있다`() {
        // given
        val request =
            MercenaryRequest.create(
                requestingTeamId = 1L,
                gameId = 10L,
                positions = setOf(Position.CATCHER),
                maxCount = 1,
                deadline = futureDeadline,
            )

        every { mercenaryRequestRepository.findByIdOrNull(1L) } returns request

        // when
        val result = mercenaryService.cancelRequest(1L)

        // then
        assertThat(result.status).isEqualTo(MercenaryRequestStatus.CANCELLED)

        verify { mercenaryRequestRepository.findByIdOrNull(1L) }
    }

    @Test
    fun `존재하지 않는 용병 요청 취소 시 예외가 발생한다`() {
        // given
        every { mercenaryRequestRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThrows<MercenaryRequestNotFoundException> {
            mercenaryService.cancelRequest(999L)
        }

        verify { mercenaryRequestRepository.findByIdOrNull(999L) }
    }

    @Test
    fun `OPEN 상태가 아닌 용병 요청 취소 시 예외가 발생한다`() {
        // given
        val request =
            MercenaryRequest.create(
                requestingTeamId = 1L,
                gameId = 10L,
                positions = setOf(Position.CATCHER),
                maxCount = 1,
                deadline = futureDeadline,
            )
        request.close()

        every { mercenaryRequestRepository.findByIdOrNull(1L) } returns request

        // when & then
        assertThrows<com.nextup.common.exception.InvalidStateException> {
            mercenaryService.cancelRequest(1L)
        }

        verify { mercenaryRequestRepository.findByIdOrNull(1L) }
    }
}
