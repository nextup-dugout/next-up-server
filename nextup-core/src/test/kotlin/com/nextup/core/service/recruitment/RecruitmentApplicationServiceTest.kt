package com.nextup.core.service.recruitment

import com.nextup.common.exception.AlreadyTeamMemberApplicationException
import com.nextup.common.exception.DuplicateApplicationException
import com.nextup.common.exception.InvalidStateException
import com.nextup.common.exception.RecruitmentApplicationNotFoundException
import com.nextup.common.exception.RecruitmentNotFoundException
import com.nextup.common.exception.RecruitmentNotOpenException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import com.nextup.core.domain.recruitment.ApplicationStatus
import com.nextup.core.domain.recruitment.RecruitmentApplication
import com.nextup.core.domain.recruitment.TeamRecruitment
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.RecruitmentApplicationRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import com.nextup.core.port.repository.TeamRecruitmentRepositoryPort
import com.nextup.core.service.recruitment.dto.ApplyRecruitmentRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDate

class RecruitmentApplicationServiceTest {
    private lateinit var applicationRepository: RecruitmentApplicationRepositoryPort
    private lateinit var recruitmentRepository: TeamRecruitmentRepositoryPort
    private lateinit var teamMemberRepository: TeamMemberRepositoryPort
    private lateinit var eventPublisher: ApplicationEventPublisher
    private lateinit var service: RecruitmentApplicationService

    private lateinit var association: Association
    private lateinit var league: League
    private lateinit var team: Team
    private lateinit var recruitment: TeamRecruitment

    @BeforeEach
    fun setUp() {
        applicationRepository = mockk()
        recruitmentRepository = mockk()
        teamMemberRepository = mockk()
        eventPublisher = mockk(relaxed = true)
        service =
            RecruitmentApplicationService(
                applicationRepository,
                recruitmentRepository,
                teamMemberRepository,
                eventPublisher,
            )

        association =
            Association(
                name = "서울시야구협회",
                id = 1L,
            )
        league =
            League(
                association = association,
                name = "1부 리그",
                foundedYear = 2020,
                id = 1L,
            )
        team =
            Team(
                league = league,
                name = "타이거즈",
                city = "서울",
                foundedYear = 2021,
                id = 1L,
            )
        recruitment =
            TeamRecruitment.create(
                team = team,
                title = "투수 모집",
                description = "주말 리그 투수를 모집합니다",
                positionsNeeded = "투수, 포수",
                ageRange = "20-30대",
                skillLevel = "중급 이상",
                location = "서울 강남구",
                deadline = LocalDate.now().plusDays(30),
            )
    }

    // ========== apply Tests ==========

    @Test
    fun `모집 공고에 지원할 수 있다`() {
        // given
        val request =
            ApplyRecruitmentRequest(
                recruitmentId = 1L,
                applicantId = 10L,
                message = "열심히 하겠습니다",
                preferredPositions = "투수",
            )

        every { recruitmentRepository.findByIdOrNull(1L) } returns recruitment
        every {
            applicationRepository.existsByRecruitmentIdAndApplicantIdAndStatusIn(
                1L,
                10L,
                setOf(ApplicationStatus.PENDING, ApplicationStatus.ACCEPTED),
            )
        } returns false
        every { teamMemberRepository.existsByTeamIdAndUserId(1L, 10L) } returns false
        every { applicationRepository.save(any()) } answers { firstArg() }

        // when
        val result = service.apply(request)

        // then
        assertThat(result.applicantId).isEqualTo(10L)
        assertThat(result.message).isEqualTo("열심히 하겠습니다")
        assertThat(result.preferredPositions).isEqualTo("투수")
        assertThat(result.status).isEqualTo(ApplicationStatus.PENDING)

        verify(exactly = 1) { recruitmentRepository.findByIdOrNull(1L) }
        verify(exactly = 1) { applicationRepository.save(any()) }
    }

    @Test
    fun `존재하지 않는 모집 공고에 지원하면 예외가 발생한다`() {
        // given
        val request =
            ApplyRecruitmentRequest(
                recruitmentId = 999L,
                applicantId = 10L,
                message = "열심히 하겠습니다",
                preferredPositions = "투수",
            )

        every { recruitmentRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThatThrownBy { service.apply(request) }
            .isInstanceOf(RecruitmentNotFoundException::class.java)

        verify(exactly = 1) { recruitmentRepository.findByIdOrNull(999L) }
        verify(exactly = 0) { applicationRepository.save(any()) }
    }

    @Test
    fun `마감된 모집 공고에 지원하면 예외가 발생한다`() {
        // given
        val closedRecruitment =
            TeamRecruitment.create(
                team = team,
                title = "투수 모집",
                description = "설명",
                positionsNeeded = "투수",
                ageRange = null,
                skillLevel = null,
                location = null,
                deadline = LocalDate.now().plusDays(30),
            )
        closedRecruitment.close()

        val request =
            ApplyRecruitmentRequest(
                recruitmentId = 2L,
                applicantId = 10L,
                message = "열심히 하겠습니다",
                preferredPositions = "투수",
            )

        every { recruitmentRepository.findByIdOrNull(2L) } returns closedRecruitment

        // when & then
        assertThatThrownBy { service.apply(request) }
            .isInstanceOf(RecruitmentNotOpenException::class.java)

        verify(exactly = 1) { recruitmentRepository.findByIdOrNull(2L) }
        verify(exactly = 0) { applicationRepository.save(any()) }
    }

    @Test
    fun `중복 지원하면 예외가 발생한다`() {
        // given
        val request =
            ApplyRecruitmentRequest(
                recruitmentId = 1L,
                applicantId = 10L,
                message = "열심히 하겠습니다",
                preferredPositions = "투수",
            )

        every { recruitmentRepository.findByIdOrNull(1L) } returns recruitment
        every {
            applicationRepository.existsByRecruitmentIdAndApplicantIdAndStatusIn(
                1L,
                10L,
                setOf(ApplicationStatus.PENDING, ApplicationStatus.ACCEPTED),
            )
        } returns true

        // when & then
        assertThatThrownBy { service.apply(request) }
            .isInstanceOf(DuplicateApplicationException::class.java)

        verify(exactly = 0) { applicationRepository.save(any()) }
    }

    @Test
    fun `이미 팀에 소속된 사용자가 지원하면 예외가 발생한다`() {
        // given
        val request =
            ApplyRecruitmentRequest(
                recruitmentId = 1L,
                applicantId = 10L,
                message = "열심히 하겠습니다",
                preferredPositions = "투수",
            )

        every { recruitmentRepository.findByIdOrNull(1L) } returns recruitment
        every {
            applicationRepository.existsByRecruitmentIdAndApplicantIdAndStatusIn(
                1L,
                10L,
                setOf(ApplicationStatus.PENDING, ApplicationStatus.ACCEPTED),
            )
        } returns false
        every { teamMemberRepository.existsByTeamIdAndUserId(1L, 10L) } returns true

        // when & then
        assertThatThrownBy { service.apply(request) }
            .isInstanceOf(AlreadyTeamMemberApplicationException::class.java)

        verify(exactly = 0) { applicationRepository.save(any()) }
    }

    // ========== acceptApplication Tests ==========

    @Test
    fun `지원을 수락할 수 있다`() {
        // given
        val application =
            RecruitmentApplication.create(
                recruitment = recruitment,
                applicantId = 10L,
                message = "열심히 하겠습니다",
                preferredPositions = "투수",
            )

        every { applicationRepository.findByIdOrNull(1L) } returns application

        // when
        val result = service.acceptApplication(1L, 100L)

        // then
        assertThat(result.status).isEqualTo(ApplicationStatus.ACCEPTED)
        assertThat(result.processedBy).isEqualTo(100L)
        assertThat(result.processedAt).isNotNull()

        verify(exactly = 1) {
            eventPublisher.publishEvent(any<com.nextup.core.domain.event.RecruitmentApplicationAcceptedEvent>())
        }
    }

    @Test
    fun `존재하지 않는 지원을 수락하면 예외가 발생한다`() {
        // given
        every { applicationRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThatThrownBy { service.acceptApplication(999L, 100L) }
            .isInstanceOf(RecruitmentApplicationNotFoundException::class.java)
    }

    @Test
    fun `이미 처리된 지원을 수락하면 예외가 발생한다`() {
        // given
        val application =
            RecruitmentApplication.create(
                recruitment = recruitment,
                applicantId = 10L,
                message = "열심히 하겠습니다",
                preferredPositions = "투수",
            )
        application.reject(100L)

        every { applicationRepository.findByIdOrNull(1L) } returns application

        // when & then
        assertThatThrownBy { service.acceptApplication(1L, 100L) }
            .isInstanceOf(InvalidStateException::class.java)
    }

    // ========== rejectApplication Tests ==========

    @Test
    fun `지원을 거절할 수 있다`() {
        // given
        val application =
            RecruitmentApplication.create(
                recruitment = recruitment,
                applicantId = 10L,
                message = "열심히 하겠습니다",
                preferredPositions = "투수",
            )

        every { applicationRepository.findByIdOrNull(1L) } returns application

        // when
        val result = service.rejectApplication(1L, 100L)

        // then
        assertThat(result.status).isEqualTo(ApplicationStatus.REJECTED)
        assertThat(result.processedBy).isEqualTo(100L)
        assertThat(result.processedAt).isNotNull()
    }

    @Test
    fun `존재하지 않는 지원을 거절하면 예외가 발생한다`() {
        // given
        every { applicationRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThatThrownBy { service.rejectApplication(999L, 100L) }
            .isInstanceOf(RecruitmentApplicationNotFoundException::class.java)
    }

    @Test
    fun `이미 처리된 지원을 거절하면 예외가 발생한다`() {
        // given
        val application =
            RecruitmentApplication.create(
                recruitment = recruitment,
                applicantId = 10L,
                message = "열심히 하겠습니다",
                preferredPositions = "투수",
            )
        application.accept(100L)

        every { applicationRepository.findByIdOrNull(1L) } returns application

        // when & then
        assertThatThrownBy { service.rejectApplication(1L, 100L) }
            .isInstanceOf(InvalidStateException::class.java)
    }

    // ========== withdrawApplication Tests ==========

    @Test
    fun `지원을 취소할 수 있다`() {
        // given
        val application =
            RecruitmentApplication.create(
                recruitment = recruitment,
                applicantId = 10L,
                message = "열심히 하겠습니다",
                preferredPositions = "투수",
            )

        every { applicationRepository.findByIdOrNull(1L) } returns application

        // when
        service.withdrawApplication(1L, 10L)

        // then
        assertThat(application.status).isEqualTo(ApplicationStatus.WITHDRAWN)
    }

    @Test
    fun `다른 사용자의 지원을 취소하면 예외가 발생한다`() {
        // given
        val application =
            RecruitmentApplication.create(
                recruitment = recruitment,
                applicantId = 10L,
                message = "열심히 하겠습니다",
                preferredPositions = "투수",
            )

        every { applicationRepository.findByIdOrNull(1L) } returns application

        // when & then
        assertThatThrownBy { service.withdrawApplication(1L, 999L) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `이미 처리된 지원을 취소하면 예외가 발생한다`() {
        // given
        val application =
            RecruitmentApplication.create(
                recruitment = recruitment,
                applicantId = 10L,
                message = "열심히 하겠습니다",
                preferredPositions = "투수",
            )
        application.accept(100L)

        every { applicationRepository.findByIdOrNull(1L) } returns application

        // when & then
        assertThatThrownBy { service.withdrawApplication(1L, 10L) }
            .isInstanceOf(InvalidStateException::class.java)
    }

    // ========== getApplicationsByRecruitment Tests ==========

    @Test
    fun `모집 공고별 지원자 목록을 조회할 수 있다`() {
        // given
        val app1 =
            RecruitmentApplication.create(
                recruitment = recruitment,
                applicantId = 10L,
                message = "메시지1",
                preferredPositions = "투수",
            )
        val app2 =
            RecruitmentApplication.create(
                recruitment = recruitment,
                applicantId = 20L,
                message = "메시지2",
                preferredPositions = "포수",
            )

        every { applicationRepository.findByRecruitmentId(1L) } returns listOf(app1, app2)

        // when
        val result = service.getApplicationsByRecruitment(1L)

        // then
        assertThat(result).hasSize(2)
        verify(exactly = 1) { applicationRepository.findByRecruitmentId(1L) }
    }

    // ========== getApplicationsByApplicant Tests ==========

    @Test
    fun `사용자별 지원 현황을 조회할 수 있다`() {
        // given
        val app1 =
            RecruitmentApplication.create(
                recruitment = recruitment,
                applicantId = 10L,
                message = "메시지1",
                preferredPositions = "투수",
            )

        every { applicationRepository.findByApplicantId(10L) } returns listOf(app1)

        // when
        val result = service.getApplicationsByApplicant(10L)

        // then
        assertThat(result).hasSize(1)
        verify(exactly = 1) { applicationRepository.findByApplicantId(10L) }
    }

    @Test
    fun `지원이 없으면 빈 리스트를 반환한다`() {
        // given
        every { applicationRepository.findByApplicantId(10L) } returns emptyList()

        // when
        val result = service.getApplicationsByApplicant(10L)

        // then
        assertThat(result).isEmpty()
    }
}
