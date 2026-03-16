package com.nextup.core.domain.recruitment

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class RecruitmentApplicationTest {
    private lateinit var team: Team
    private lateinit var recruitment: TeamRecruitment

    @BeforeEach
    fun setUp() {
        val association = Association(name = "테스트 협회")
        val league =
            League(
                association = association,
                name = "테스트 리그",
                foundedYear = 2020,
            )
        team =
            Team(
                league = league,
                name = "테스트 팀",
                city = "서울",
                foundedYear = 2020,
            )
        recruitment =
            TeamRecruitment.create(
                team = team,
                title = "투수 모집",
                description = "경험 많은 투수를 모집합니다",
                positionsNeeded = "투수,포수",
                ageRange = "20-35",
                skillLevel = "중급",
                location = "서울",
                deadline = LocalDate.now().plusDays(30),
            )
    }

    @Test
    fun `should create application with valid data`() {
        // given
        val applicantId = 1L
        val message = "열심히 하겠습니다"
        val preferredPositions = "투수,포수"

        // when
        val application =
            RecruitmentApplication.create(
                recruitment = recruitment,
                applicantId = applicantId,
                message = message,
                preferredPositions = preferredPositions,
            )

        // then
        assertThat(application.recruitment).isEqualTo(recruitment)
        assertThat(application.applicantId).isEqualTo(applicantId)
        assertThat(application.message).isEqualTo(message)
        assertThat(application.preferredPositions).isEqualTo(preferredPositions)
        assertThat(application.status).isEqualTo(ApplicationStatus.PENDING)
        assertThat(application.processedAt).isNull()
        assertThat(application.processedBy).isNull()
    }

    @Test
    fun `should throw exception when creating application with blank message`() {
        // when & then
        assertThrows<IllegalArgumentException> {
            RecruitmentApplication.create(
                recruitment = recruitment,
                applicantId = 1L,
                message = "",
                preferredPositions = "투수",
            )
        }
    }

    @Test
    fun `should throw exception when creating application with blank positions`() {
        // when & then
        assertThrows<IllegalArgumentException> {
            RecruitmentApplication.create(
                recruitment = recruitment,
                applicantId = 1L,
                message = "열심히 하겠습니다",
                preferredPositions = "",
            )
        }
    }

    @Test
    fun `should throw exception when applying to closed recruitment`() {
        // given
        recruitment.close()

        // when & then
        assertThrows<IllegalArgumentException> {
            RecruitmentApplication.create(
                recruitment = recruitment,
                applicantId = 1L,
                message = "열심히 하겠습니다",
                preferredPositions = "투수",
            )
        }
    }

    @Test
    fun `should accept pending application`() {
        // given
        val application =
            RecruitmentApplication.create(
                recruitment = recruitment,
                applicantId = 1L,
                message = "열심히 하겠습니다",
                preferredPositions = "투수",
            )
        val processorId = 100L

        // when
        application.accept(processorId)

        // then
        assertThat(application.status).isEqualTo(ApplicationStatus.ACCEPTED)
        assertThat(application.processedAt).isNotNull()
        assertThat(application.processedBy).isEqualTo(processorId)
    }

    @Test
    fun `should throw exception when accepting non-pending application`() {
        // given
        val application =
            RecruitmentApplication.create(
                recruitment = recruitment,
                applicantId = 1L,
                message = "열심히 하겠습니다",
                preferredPositions = "투수",
            )
        application.accept(100L) // ACCEPTED 상태로 변경

        // when & then
        assertThrows<IllegalStateException> {
            application.accept(100L)
        }
    }

    @Test
    fun `should reject pending application`() {
        // given
        val application =
            RecruitmentApplication.create(
                recruitment = recruitment,
                applicantId = 1L,
                message = "열심히 하겠습니다",
                preferredPositions = "투수",
            )
        val processorId = 100L

        // when
        application.reject(processorId)

        // then
        assertThat(application.status).isEqualTo(ApplicationStatus.REJECTED)
        assertThat(application.processedAt).isNotNull()
        assertThat(application.processedBy).isEqualTo(processorId)
    }

    @Test
    fun `should throw exception when rejecting non-pending application`() {
        // given
        val application =
            RecruitmentApplication.create(
                recruitment = recruitment,
                applicantId = 1L,
                message = "열심히 하겠습니다",
                preferredPositions = "투수",
            )
        application.reject(100L) // REJECTED 상태로 변경

        // when & then
        assertThrows<IllegalStateException> {
            application.reject(100L)
        }
    }

    @Test
    fun `should withdraw pending application`() {
        // given
        val application =
            RecruitmentApplication.create(
                recruitment = recruitment,
                applicantId = 1L,
                message = "열심히 하겠습니다",
                preferredPositions = "투수",
            )

        // when
        application.withdraw()

        // then
        assertThat(application.status).isEqualTo(ApplicationStatus.WITHDRAWN)
        assertThat(application.processedAt).isNotNull()
        assertThat(application.processedBy).isNull()
    }

    @Test
    fun `should throw exception when withdrawing non-pending application`() {
        // given
        val application =
            RecruitmentApplication.create(
                recruitment = recruitment,
                applicantId = 1L,
                message = "열심히 하겠습니다",
                preferredPositions = "투수",
            )
        application.withdraw() // WITHDRAWN 상태로 변경

        // when & then
        assertThrows<IllegalStateException> {
            application.withdraw()
        }
    }

    @Test
    fun `should not accept already rejected application`() {
        // given
        val application =
            RecruitmentApplication.create(
                recruitment = recruitment,
                applicantId = 1L,
                message = "열심히 하겠습니다",
                preferredPositions = "투수",
            )
        application.reject(100L)

        // when & then
        assertThrows<IllegalStateException> {
            application.accept(100L)
        }
    }

    @Test
    fun `should not withdraw already accepted application`() {
        // given
        val application =
            RecruitmentApplication.create(
                recruitment = recruitment,
                applicantId = 1L,
                message = "열심히 하겠습니다",
                preferredPositions = "투수",
            )
        application.accept(100L)

        // when & then
        assertThrows<IllegalStateException> {
            application.withdraw()
        }
    }
}
