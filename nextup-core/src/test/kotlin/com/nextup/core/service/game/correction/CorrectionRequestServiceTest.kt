package com.nextup.core.service.game.correction

import com.nextup.common.exception.CorrectionRequestNotFoundException
import com.nextup.core.domain.game.CorrectionRequest
import com.nextup.core.domain.game.CorrectionRequestStatus
import com.nextup.core.domain.game.CorrectionType
import com.nextup.core.port.repository.CorrectionRequestRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("CorrectionRequestService")
class CorrectionRequestServiceTest {
    private lateinit var correctionRequestRepository: CorrectionRequestRepositoryPort
    private lateinit var recordCorrectionService: RecordCorrectionService
    private lateinit var service: CorrectionRequestService

    @BeforeEach
    fun setUp() {
        correctionRequestRepository = mockk()
        recordCorrectionService = mockk(relaxed = true)
        service =
            CorrectionRequestService(
                correctionRequestRepository = correctionRequestRepository,
                recordCorrectionService = recordCorrectionService,
            )
    }

    private fun createBattingRequest(): CorrectionRequest =
        CorrectionRequest.create(
            gameId = 1L,
            requesterUserId = 1L,
            correctionType = CorrectionType.BATTING,
            targetRecordId = 1L,
            fieldName = "hits",
            newValue = "3",
            reason = "기록 오류",
        )

    private fun createPitchingRequest(): CorrectionRequest =
        CorrectionRequest.create(
            gameId = 1L,
            requesterUserId = 1L,
            correctionType = CorrectionType.PITCHING,
            targetRecordId = 2L,
            fieldName = "strikeOuts",
            newValue = "5",
            reason = "기록 오류",
        )

    @Nested
    @DisplayName("createRequest - 정정 요청 생성")
    inner class CreateRequestTest {
        @Test
        fun `정정 요청을 생성하고 저장한다`() {
            // given
            val saved = createBattingRequest()
            every { correctionRequestRepository.save(any()) } returns saved

            // when
            val result =
                service.createRequest(
                    gameId = 1L,
                    requesterUserId = 1L,
                    correctionType = CorrectionType.BATTING,
                    targetRecordId = 1L,
                    fieldName = "hits",
                    newValue = "3",
                    reason = "기록 오류",
                )

            // then
            assertThat(result).isNotNull
            assertThat(result.correctionType).isEqualTo(CorrectionType.BATTING)
            assertThat(result.status).isEqualTo(CorrectionRequestStatus.PENDING)
            verify(exactly = 1) { correctionRequestRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("approve - 정정 요청 승인")
    inner class ApproveTest {
        @Test
        fun `BATTING 타입 정정 요청을 승인하고 타격 기록을 정정한다`() {
            // given
            val requestId = 1L
            val reviewerUserId = 10L
            val request = createBattingRequest()
            every { correctionRequestRepository.findByIdOrNull(requestId) } returns request
            every {
                recordCorrectionService.correctBattingRecord(any(), any(), any())
            } returns mockk(relaxed = true)

            // when
            val result =
                service.approve(
                    requestId = requestId,
                    reviewerUserId = reviewerUserId,
                    comment = "승인합니다",
                )

            // then
            assertThat(result.status).isEqualTo(CorrectionRequestStatus.APPROVED)
            assertThat(result.reviewerUserId).isEqualTo(reviewerUserId)
            verify(exactly = 1) {
                recordCorrectionService.correctBattingRecord(
                    gameId = 1L,
                    recordId = 1L,
                    request =
                        BattingCorrectionRequest(
                            adminUserId = reviewerUserId,
                            fieldName = "hits",
                            newValue = "3",
                            reason = "기록 오류",
                        ),
                )
            }
        }

        @Test
        fun `PITCHING 타입 정정 요청을 승인하고 투수 기록을 정정한다`() {
            // given
            val requestId = 2L
            val reviewerUserId = 10L
            val request = createPitchingRequest()
            every { correctionRequestRepository.findByIdOrNull(requestId) } returns request
            every {
                recordCorrectionService.correctPitchingRecord(any(), any(), any())
            } returns mockk(relaxed = true)

            // when
            val result =
                service.approve(
                    requestId = requestId,
                    reviewerUserId = reviewerUserId,
                    comment = "승인합니다",
                )

            // then
            assertThat(result.status).isEqualTo(CorrectionRequestStatus.APPROVED)
            assertThat(result.reviewerUserId).isEqualTo(reviewerUserId)
            verify(exactly = 1) {
                recordCorrectionService.correctPitchingRecord(
                    gameId = 1L,
                    recordId = 2L,
                    request =
                        PitchingCorrectionRequest(
                            adminUserId = reviewerUserId,
                            fieldName = "strikeOuts",
                            newValue = "5",
                            reason = "기록 오류",
                        ),
                )
            }
        }
    }

    @Nested
    @DisplayName("reject - 정정 요청 반려")
    inner class RejectTest {
        @Test
        fun `PENDING 상태의 정정 요청을 반려한다`() {
            // given
            val requestId = 1L
            val reviewerUserId = 10L
            val request = createBattingRequest()
            every { correctionRequestRepository.findByIdOrNull(requestId) } returns request

            // when
            val result =
                service.reject(
                    requestId = requestId,
                    reviewerUserId = reviewerUserId,
                    comment = "근거가 부족합니다",
                )

            // then
            assertThat(result.status).isEqualTo(CorrectionRequestStatus.REJECTED)
            assertThat(result.reviewerUserId).isEqualTo(reviewerUserId)
            assertThat(result.reviewComment).isEqualTo("근거가 부족합니다")
        }
    }

    @Nested
    @DisplayName("getById - ID로 정정 요청 조회")
    inner class GetByIdTest {
        @Test
        fun `존재하지 않는 ID이면 CorrectionRequestNotFoundException을 던진다`() {
            // given
            val requestId = 999L
            every { correctionRequestRepository.findByIdOrNull(requestId) } returns null

            // when & then
            assertThrows<CorrectionRequestNotFoundException> {
                service.getById(requestId)
            }
        }
    }

    @Nested
    @DisplayName("getPendingRequests - 대기 중인 정정 요청 목록 조회")
    inner class GetPendingRequestsTest {
        @Test
        fun `PENDING 상태인 정정 요청 목록을 반환한다`() {
            // given
            val pending = listOf(createBattingRequest(), createPitchingRequest())
            every {
                correctionRequestRepository.findByStatus(CorrectionRequestStatus.PENDING)
            } returns pending

            // when
            val result = service.getPendingRequests()

            // then
            assertThat(result).hasSize(2)
            assertThat(result).allMatch { it.status == CorrectionRequestStatus.PENDING }
        }
    }
}
