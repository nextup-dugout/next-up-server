package com.nextup.infrastructure.scheduler

import com.nextup.core.domain.match.MatchRequest
import com.nextup.core.domain.match.MatchResponse
import com.nextup.core.domain.match.MatchResponseStatus
import com.nextup.core.port.repository.MatchRequestRepositoryPort
import com.nextup.core.port.repository.MatchResponseRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("MatchRequestExpiryScheduler 테스트")
class MatchRequestExpirySchedulerTest {
    private val matchRequestRepository = mockk<MatchRequestRepositoryPort>()
    private val matchResponseRepository = mockk<MatchResponseRepositoryPort>()

    private val scheduler =
        MatchRequestExpiryScheduler(
            matchRequestRepository = matchRequestRepository,
            matchResponseRepository = matchResponseRepository,
        )

    @Nested
    @DisplayName("만료 처리")
    inner class ExpireMatchRequests {
        @Test
        fun `만료된 OPEN 요청이 EXPIRED로 전환된다`() {
            // given
            val expiredRequest = mockk<MatchRequest>(relaxed = true)
            every { expiredRequest.isExpired() } returns true
            every { expiredRequest.id } returns 1L
            every { matchRequestRepository.findAllOpen() } returns listOf(expiredRequest)
            every { matchRequestRepository.save(any()) } answers { firstArg() }
            every { matchResponseRepository.findByMatchRequestId(1L) } returns emptyList()

            // when
            scheduler.expireMatchRequests()

            // then
            verify(exactly = 1) { expiredRequest.expire() }
            verify(exactly = 1) { matchRequestRepository.save(expiredRequest) }
        }

        @Test
        fun `만료되지 않은 요청은 건너뛴다`() {
            // given
            val activeRequest = mockk<MatchRequest>(relaxed = true)
            every { activeRequest.isExpired() } returns false
            every { matchRequestRepository.findAllOpen() } returns listOf(activeRequest)

            // when
            scheduler.expireMatchRequests()

            // then
            verify(exactly = 0) { activeRequest.expire() }
            verify(exactly = 0) { matchRequestRepository.save(any()) }
        }

        @Test
        fun `OPEN 요청이 없으면 아무 작업도 하지 않는다`() {
            // given
            every { matchRequestRepository.findAllOpen() } returns emptyList()

            // when
            scheduler.expireMatchRequests()

            // then
            verify(exactly = 0) { matchRequestRepository.save(any()) }
            verify(exactly = 0) { matchResponseRepository.findByMatchRequestId(any()) }
        }

        @Test
        fun `만료 시 관련 PENDING 응답이 REJECTED로 전환된다`() {
            // given
            val expiredRequest = mockk<MatchRequest>(relaxed = true)
            every { expiredRequest.isExpired() } returns true
            every { expiredRequest.id } returns 1L
            every { matchRequestRepository.findAllOpen() } returns listOf(expiredRequest)
            every { matchRequestRepository.save(any()) } answers { firstArg() }

            val pendingResponse = mockk<MatchResponse>(relaxed = true)
            every { pendingResponse.status } returns MatchResponseStatus.PENDING
            every { matchResponseRepository.findByMatchRequestId(1L) } returns listOf(pendingResponse)
            every { matchResponseRepository.save(any()) } answers { firstArg() }

            // when
            scheduler.expireMatchRequests()

            // then
            verify(exactly = 1) { pendingResponse.reject() }
            verify(exactly = 1) { matchResponseRepository.save(pendingResponse) }
        }

        @Test
        fun `만료 시 이미 ACCEPTED 또는 REJECTED 상태의 응답은 건너뛴다`() {
            // given
            val expiredRequest = mockk<MatchRequest>(relaxed = true)
            every { expiredRequest.isExpired() } returns true
            every { expiredRequest.id } returns 1L
            every { matchRequestRepository.findAllOpen() } returns listOf(expiredRequest)
            every { matchRequestRepository.save(any()) } answers { firstArg() }

            val acceptedResponse = mockk<MatchResponse>(relaxed = true)
            every { acceptedResponse.status } returns MatchResponseStatus.ACCEPTED
            val rejectedResponse = mockk<MatchResponse>(relaxed = true)
            every { rejectedResponse.status } returns MatchResponseStatus.REJECTED

            every {
                matchResponseRepository.findByMatchRequestId(1L)
            } returns listOf(acceptedResponse, rejectedResponse)

            // when
            scheduler.expireMatchRequests()

            // then
            verify(exactly = 0) { acceptedResponse.reject() }
            verify(exactly = 0) { rejectedResponse.reject() }
            verify(exactly = 0) { matchResponseRepository.save(any()) }
        }

        @Test
        fun `여러 만료 요청과 응답이 모두 처리된다`() {
            // given
            val request1 = mockk<MatchRequest>(relaxed = true)
            every { request1.isExpired() } returns true
            every { request1.id } returns 1L

            val request2 = mockk<MatchRequest>(relaxed = true)
            every { request2.isExpired() } returns true
            every { request2.id } returns 2L

            val activeRequest = mockk<MatchRequest>(relaxed = true)
            every { activeRequest.isExpired() } returns false

            every {
                matchRequestRepository.findAllOpen()
            } returns listOf(request1, request2, activeRequest)
            every { matchRequestRepository.save(any()) } answers { firstArg() }

            val response1 = mockk<MatchResponse>(relaxed = true)
            every { response1.status } returns MatchResponseStatus.PENDING
            val response2 = mockk<MatchResponse>(relaxed = true)
            every { response2.status } returns MatchResponseStatus.PENDING

            every { matchResponseRepository.findByMatchRequestId(1L) } returns listOf(response1)
            every { matchResponseRepository.findByMatchRequestId(2L) } returns listOf(response2)
            every { matchResponseRepository.save(any()) } answers { firstArg() }

            // when
            scheduler.expireMatchRequests()

            // then
            verify(exactly = 1) { request1.expire() }
            verify(exactly = 1) { request2.expire() }
            verify(exactly = 0) { activeRequest.expire() }
            verify(exactly = 2) { matchRequestRepository.save(any()) }
            verify(exactly = 1) { response1.reject() }
            verify(exactly = 1) { response2.reject() }
            verify(exactly = 2) { matchResponseRepository.save(any()) }
        }
    }
}
