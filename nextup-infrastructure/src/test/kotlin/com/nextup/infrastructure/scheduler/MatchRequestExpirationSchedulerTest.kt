package com.nextup.infrastructure.scheduler

import com.nextup.core.domain.match.MatchRequest
import com.nextup.core.domain.match.MatchRequestStatus
import com.nextup.core.domain.match.SkillLevel
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.MatchRequestRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@DisplayName("MatchRequestExpirationScheduler 테스트")
class MatchRequestExpirationSchedulerTest {
    private val matchRequestRepository = mockk<MatchRequestRepositoryPort>()
    private val scheduler = MatchRequestExpirationScheduler(matchRequestRepository)

    private val testTeam = mockk<Team>()

    @BeforeEach
    fun setUp() {
        every { matchRequestRepository.save(any()) } answers { firstArg() }
    }

    @Test
    fun `30일 이상 경과한 OPEN 요청이 EXPIRED로 전환됨`() {
        // given
        val expiredRequest = createMatchRequestWithAge(31)
        every { matchRequestRepository.findAllOpen() } returns listOf(expiredRequest)

        // when
        scheduler.expireOldMatchRequests()

        // then
        assertThat(expiredRequest.status).isEqualTo(MatchRequestStatus.EXPIRED)
        verify(exactly = 1) { matchRequestRepository.save(expiredRequest) }
    }

    @Test
    fun `30일 미만인 OPEN 요청은 만료되지 않음`() {
        // given
        val recentRequest = createMatchRequestWithAge(10)
        every { matchRequestRepository.findAllOpen() } returns listOf(recentRequest)

        // when
        scheduler.expireOldMatchRequests()

        // then
        assertThat(recentRequest.status).isEqualTo(MatchRequestStatus.OPEN)
        verify(exactly = 0) { matchRequestRepository.save(any()) }
    }

    @Test
    fun `만료 대상이 없으면 저장하지 않음`() {
        // given
        every { matchRequestRepository.findAllOpen() } returns emptyList()

        // when
        scheduler.expireOldMatchRequests()

        // then
        verify(exactly = 0) { matchRequestRepository.save(any()) }
    }

    @Test
    fun `복수 만료 요청이 모두 처리됨`() {
        // given
        val expired1 = createMatchRequestWithAge(31)
        val expired2 = createMatchRequestWithAge(60)
        val recent = createMatchRequestWithAge(5)
        every { matchRequestRepository.findAllOpen() } returns listOf(expired1, expired2, recent)

        // when
        scheduler.expireOldMatchRequests()

        // then
        assertThat(expired1.status).isEqualTo(MatchRequestStatus.EXPIRED)
        assertThat(expired2.status).isEqualTo(MatchRequestStatus.EXPIRED)
        assertThat(recent.status).isEqualTo(MatchRequestStatus.OPEN)
        verify(exactly = 2) { matchRequestRepository.save(any()) }
    }

    private fun createMatchRequestWithAge(daysOld: Long): MatchRequest {
        val request =
            MatchRequest.create(
                team = testTeam,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = "14:00",
                preferredLocation = "서울 잠실구장",
                message = null,
                skillLevel = SkillLevel.INTERMEDIATE,
            )
        // createdAt을 과거로 설정 (리플렉션)
        val field = request.javaClass.superclass.getDeclaredField("createdAt")
        field.isAccessible = true
        field.set(request, Instant.now().minus(daysOld, ChronoUnit.DAYS))
        return request
    }
}
