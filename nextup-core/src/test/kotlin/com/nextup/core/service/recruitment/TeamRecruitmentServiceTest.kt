package com.nextup.core.service.recruitment

import com.nextup.common.exception.InvalidStateException
import com.nextup.common.exception.RecruitmentNotFoundException
import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import com.nextup.core.domain.recruitment.RecruitmentStatus
import com.nextup.core.domain.recruitment.TeamRecruitment
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.TeamRecruitmentRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.service.recruitment.dto.CreateRecruitmentRequest
import com.nextup.core.service.recruitment.dto.UpdateRecruitmentRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TeamRecruitmentServiceTest {
    private lateinit var recruitmentRepository: TeamRecruitmentRepositoryPort
    private lateinit var teamRepository: TeamRepositoryPort
    private lateinit var service: TeamRecruitmentService

    private lateinit var association: Association
    private lateinit var league: League
    private lateinit var team: Team

    @BeforeEach
    fun setUp() {
        recruitmentRepository = mockk()
        teamRepository = mockk()
        service = TeamRecruitmentService(recruitmentRepository, teamRepository)

        // Test fixtures
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
    }

    // ========== createRecruitment Tests ==========

    @Test
    fun `모집 공고를 생성할 수 있다`() {
        // given
        val request =
            CreateRecruitmentRequest(
                teamId = 1L,
                title = "투수 모집",
                description = "주말 리그 투수를 모집합니다",
                positionsNeeded = "투수, 포수",
                ageRange = "20-30대",
                skillLevel = "중급 이상",
                location = "서울 강남구",
                deadline = LocalDate.now().plusDays(30),
            )

        val recruitment =
            TeamRecruitment.create(
                team = team,
                title = request.title,
                description = request.description,
                positionsNeeded = request.positionsNeeded,
                ageRange = request.ageRange,
                skillLevel = request.skillLevel,
                location = request.location,
                deadline = request.deadline,
            )

        every { teamRepository.findByIdOrNull(1L) } returns team
        every { recruitmentRepository.save(any()) } returns recruitment

        // when
        val result = service.createRecruitment(request)

        // then
        assertThat(result.title).isEqualTo("투수 모집")
        assertThat(result.description).isEqualTo("주말 리그 투수를 모집합니다")
        assertThat(result.positionsNeeded).isEqualTo("투수, 포수")
        assertThat(result.status).isEqualTo(RecruitmentStatus.OPEN)
        assertThat(result.team).isEqualTo(team)

        verify(exactly = 1) { teamRepository.findByIdOrNull(1L) }
        verify(exactly = 1) { recruitmentRepository.save(any()) }
    }

    @Test
    fun `존재하지 않는 팀으로 모집 공고를 생성하면 예외가 발생한다`() {
        // given
        val request =
            CreateRecruitmentRequest(
                teamId = 999L,
                title = "투수 모집",
                description = "주말 리그 투수를 모집합니다",
                positionsNeeded = "투수",
                ageRange = null,
                skillLevel = null,
                location = null,
                deadline = LocalDate.now().plusDays(30),
            )

        every { teamRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThatThrownBy { service.createRecruitment(request) }
            .isInstanceOf(TeamNotFoundException::class.java)
            .hasMessageContaining("999")

        verify(exactly = 1) { teamRepository.findByIdOrNull(999L) }
        verify(exactly = 0) { recruitmentRepository.save(any()) }
    }

    // ========== updateRecruitment Tests ==========

    @Test
    fun `모집 공고를 수정할 수 있다`() {
        // given
        val recruitment =
            TeamRecruitment.create(
                team = team,
                title = "투수 모집",
                description = "주말 리그 투수를 모집합니다",
                positionsNeeded = "투수",
                ageRange = null,
                skillLevel = null,
                location = null,
                deadline = LocalDate.now().plusDays(30),
            )

        val updateRequest =
            UpdateRecruitmentRequest(
                title = "투수 및 포수 모집",
                description = "주말 리그 선수를 모집합니다",
                positionsNeeded = "투수, 포수",
                deadline = LocalDate.now().plusDays(60),
            )

        every { recruitmentRepository.findByIdOrNull(1L) } returns recruitment

        // when
        val result = service.updateRecruitment(1L, updateRequest)

        // then
        assertThat(result.title).isEqualTo("투수 및 포수 모집")
        assertThat(result.description).isEqualTo("주말 리그 선수를 모집합니다")
        assertThat(result.positionsNeeded).isEqualTo("투수, 포수")

        verify(exactly = 1) { recruitmentRepository.findByIdOrNull(1L) }
    }

    @Test
    fun `모집 공고 제목만 수정할 수 있다`() {
        // given
        val recruitment =
            TeamRecruitment.create(
                team = team,
                title = "투수 모집",
                description = "주말 리그 투수를 모집합니다",
                positionsNeeded = "투수",
                ageRange = null,
                skillLevel = null,
                location = null,
                deadline = LocalDate.now().plusDays(30),
            )

        val updateRequest =
            UpdateRecruitmentRequest(
                title = "투수 긴급 모집",
                description = null,
                positionsNeeded = null,
                deadline = null,
            )

        every { recruitmentRepository.findByIdOrNull(1L) } returns recruitment

        // when
        val result = service.updateRecruitment(1L, updateRequest)

        // then
        assertThat(result.title).isEqualTo("투수 긴급 모집")
        assertThat(result.description).isEqualTo("주말 리그 투수를 모집합니다") // 변경되지 않음

        verify(exactly = 1) { recruitmentRepository.findByIdOrNull(1L) }
    }

    @Test
    fun `마감된 모집 공고는 수정할 수 없다`() {
        // given
        val recruitment =
            TeamRecruitment.create(
                team = team,
                title = "투수 모집",
                description = "주말 리그 투수를 모집합니다",
                positionsNeeded = "투수",
                ageRange = null,
                skillLevel = null,
                location = null,
                deadline = LocalDate.now().plusDays(30),
            )
        recruitment.close() // 모집 마감

        val updateRequest =
            UpdateRecruitmentRequest(
                title = "투수 긴급 모집",
                description = null,
                positionsNeeded = null,
                deadline = null,
            )

        every { recruitmentRepository.findByIdOrNull(1L) } returns recruitment

        // when & then
        assertThatThrownBy { service.updateRecruitment(1L, updateRequest) }
            .isInstanceOf(InvalidStateException::class.java)

        verify(exactly = 1) { recruitmentRepository.findByIdOrNull(1L) }
    }

    @Test
    fun `존재하지 않는 모집 공고를 수정하면 예외가 발생한다`() {
        // given
        val updateRequest =
            UpdateRecruitmentRequest(
                title = "투수 모집",
                description = null,
                positionsNeeded = null,
                deadline = null,
            )

        every { recruitmentRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThatThrownBy { service.updateRecruitment(999L, updateRequest) }
            .isInstanceOf(RecruitmentNotFoundException::class.java)

        verify(exactly = 1) { recruitmentRepository.findByIdOrNull(999L) }
    }

    // ========== closeRecruitment Tests ==========

    @Test
    fun `모집 공고를 마감할 수 있다`() {
        // given
        val recruitment =
            TeamRecruitment.create(
                team = team,
                title = "투수 모집",
                description = "주말 리그 투수를 모집합니다",
                positionsNeeded = "투수",
                ageRange = null,
                skillLevel = null,
                location = null,
                deadline = LocalDate.now().plusDays(30),
            )

        every { recruitmentRepository.findByIdOrNull(1L) } returns recruitment

        // when
        val result = service.closeRecruitment(1L)

        // then
        assertThat(result.status).isEqualTo(RecruitmentStatus.CLOSED)

        verify(exactly = 1) { recruitmentRepository.findByIdOrNull(1L) }
    }

    @Test
    fun `이미 마감된 모집 공고는 다시 마감할 수 없다`() {
        // given
        val recruitment =
            TeamRecruitment.create(
                team = team,
                title = "투수 모집",
                description = "주말 리그 투수를 모집합니다",
                positionsNeeded = "투수",
                ageRange = null,
                skillLevel = null,
                location = null,
                deadline = LocalDate.now().plusDays(30),
            )
        recruitment.close() // 이미 마감됨

        every { recruitmentRepository.findByIdOrNull(1L) } returns recruitment

        // when & then
        assertThatThrownBy { service.closeRecruitment(1L) }
            .isInstanceOf(InvalidStateException::class.java)

        verify(exactly = 1) { recruitmentRepository.findByIdOrNull(1L) }
    }

    @Test
    fun `존재하지 않는 모집 공고를 마감하면 예외가 발생한다`() {
        // given
        every { recruitmentRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThatThrownBy { service.closeRecruitment(999L) }
            .isInstanceOf(RecruitmentNotFoundException::class.java)

        verify(exactly = 1) { recruitmentRepository.findByIdOrNull(999L) }
    }

    // ========== getById Tests ==========

    @Test
    fun `ID로 모집 공고를 조회할 수 있다`() {
        // given
        val recruitment =
            TeamRecruitment.create(
                team = team,
                title = "투수 모집",
                description = "주말 리그 투수를 모집합니다",
                positionsNeeded = "투수",
                ageRange = null,
                skillLevel = null,
                location = null,
                deadline = LocalDate.now().plusDays(30),
            )

        every { recruitmentRepository.findByIdOrNull(1L) } returns recruitment

        // when
        val result = service.getById(1L)

        // then
        assertThat(result).isEqualTo(recruitment)
        assertThat(result.title).isEqualTo("투수 모집")

        verify(exactly = 1) { recruitmentRepository.findByIdOrNull(1L) }
    }

    @Test
    fun `존재하지 않는 ID로 조회하면 예외가 발생한다`() {
        // given
        every { recruitmentRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThatThrownBy { service.getById(999L) }
            .isInstanceOf(RecruitmentNotFoundException::class.java)

        verify(exactly = 1) { recruitmentRepository.findByIdOrNull(999L) }
    }

    // ========== getByTeam Tests ==========

    @Test
    fun `팀별 모집 공고 목록을 조회할 수 있다`() {
        // given
        val recruitment1 =
            TeamRecruitment.create(
                team = team,
                title = "투수 모집",
                description = "투수를 모집합니다",
                positionsNeeded = "투수",
                ageRange = null,
                skillLevel = null,
                location = null,
                deadline = LocalDate.now().plusDays(30),
            )

        val recruitment2 =
            TeamRecruitment.create(
                team = team,
                title = "포수 모집",
                description = "포수를 모집합니다",
                positionsNeeded = "포수",
                ageRange = null,
                skillLevel = null,
                location = null,
                deadline = LocalDate.now().plusDays(60),
            )

        every { recruitmentRepository.findByTeamId(1L) } returns listOf(recruitment1, recruitment2)

        // when
        val result = service.getByTeam(1L)

        // then
        assertThat(result).hasSize(2)
        assertThat(result).containsExactly(recruitment1, recruitment2)

        verify(exactly = 1) { recruitmentRepository.findByTeamId(1L) }
    }

    @Test
    fun `모집 공고가 없는 팀은 빈 리스트를 반환한다`() {
        // given
        every { recruitmentRepository.findByTeamId(1L) } returns emptyList()

        // when
        val result = service.getByTeam(1L)

        // then
        assertThat(result).isEmpty()

        verify(exactly = 1) { recruitmentRepository.findByTeamId(1L) }
    }

    // ========== getAllOpen Tests ==========

    @Test
    fun `진행 중인 모든 모집 공고를 조회할 수 있다`() {
        // given
        val recruitment1 =
            TeamRecruitment.create(
                team = team,
                title = "투수 모집",
                description = "투수를 모집합니다",
                positionsNeeded = "투수",
                ageRange = null,
                skillLevel = null,
                location = null,
                deadline = LocalDate.now().plusDays(30),
            )

        val recruitment2 =
            TeamRecruitment.create(
                team = team,
                title = "포수 모집",
                description = "포수를 모집합니다",
                positionsNeeded = "포수",
                ageRange = null,
                skillLevel = null,
                location = null,
                deadline = LocalDate.now().plusDays(60),
            )

        every { recruitmentRepository.findAllOpen() } returns listOf(recruitment1, recruitment2)

        // when
        val result = service.getAllOpen()

        // then
        assertThat(result).hasSize(2)
        assertThat(result).allMatch { it.status == RecruitmentStatus.OPEN }

        verify(exactly = 1) { recruitmentRepository.findAllOpen() }
    }

    @Test
    fun `진행 중인 모집 공고가 없으면 빈 리스트를 반환한다`() {
        // given
        every { recruitmentRepository.findAllOpen() } returns emptyList()

        // when
        val result = service.getAllOpen()

        // then
        assertThat(result).isEmpty()

        verify(exactly = 1) { recruitmentRepository.findAllOpen() }
    }

    // ========== getByStatus Tests ==========

    @Test
    fun `특정 상태의 모집 공고를 조회할 수 있다`() {
        // given
        val recruitment1 =
            TeamRecruitment.create(
                team = team,
                title = "투수 모집",
                description = "투수를 모집합니다",
                positionsNeeded = "투수",
                ageRange = null,
                skillLevel = null,
                location = null,
                deadline = LocalDate.now().plusDays(30),
            )
        recruitment1.close()

        val recruitment2 =
            TeamRecruitment.create(
                team = team,
                title = "포수 모집",
                description = "포수를 모집합니다",
                positionsNeeded = "포수",
                ageRange = null,
                skillLevel = null,
                location = null,
                deadline = LocalDate.now().plusDays(60),
            )
        recruitment2.close()

        every { recruitmentRepository.findByStatus(RecruitmentStatus.CLOSED) } returns
            listOf(
                recruitment1,
                recruitment2,
            )

        // when
        val result = service.getByStatus(RecruitmentStatus.CLOSED)

        // then
        assertThat(result).hasSize(2)
        assertThat(result).allMatch { it.status == RecruitmentStatus.CLOSED }

        verify(exactly = 1) { recruitmentRepository.findByStatus(RecruitmentStatus.CLOSED) }
    }

    @Test
    fun `해당 상태의 모집 공고가 없으면 빈 리스트를 반환한다`() {
        // given
        every { recruitmentRepository.findByStatus(RecruitmentStatus.EXPIRED) } returns emptyList()

        // when
        val result = service.getByStatus(RecruitmentStatus.EXPIRED)

        // then
        assertThat(result).isEmpty()

        verify(exactly = 1) { recruitmentRepository.findByStatus(RecruitmentStatus.EXPIRED) }
    }

    // ========== deleteRecruitment Tests ==========

    @Test
    fun `모집 공고를 삭제할 수 있다`() {
        // given
        val recruitment =
            TeamRecruitment.create(
                team = team,
                title = "투수 모집",
                description = "주말 리그 투수를 모집합니다",
                positionsNeeded = "투수",
                ageRange = null,
                skillLevel = null,
                location = null,
                deadline = LocalDate.now().plusDays(30),
            )

        every { recruitmentRepository.findByIdOrNull(1L) } returns recruitment
        every { recruitmentRepository.delete(recruitment) } returns Unit

        // when
        service.deleteRecruitment(1L)

        // then
        verify(exactly = 1) { recruitmentRepository.findByIdOrNull(1L) }
        verify(exactly = 1) { recruitmentRepository.delete(recruitment) }
    }

    @Test
    fun `존재하지 않는 모집 공고를 삭제하면 예외가 발생한다`() {
        // given
        every { recruitmentRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThatThrownBy { service.deleteRecruitment(999L) }
            .isInstanceOf(RecruitmentNotFoundException::class.java)

        verify(exactly = 1) { recruitmentRepository.findByIdOrNull(999L) }
        verify(exactly = 0) { recruitmentRepository.delete(any()) }
    }
}
