package com.nextup.core.domain.recruitment

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class TeamRecruitmentTest {
    private lateinit var team: Team
    private lateinit var league: League
    private lateinit var association: Association

    @BeforeEach
    fun setUp() {
        association = Association(name = "테스트 협회")
        league =
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
    }

    @Test
    fun `should create recruitment with valid data`() {
        // given
        val title = "투수 모집"
        val description = "경험 많은 투수를 모집합니다"
        val positionsNeeded = "투수,포수"
        val deadline = LocalDate.now().plusDays(30)

        // when
        val recruitment =
            TeamRecruitment.create(
                team = team,
                title = title,
                description = description,
                positionsNeeded = positionsNeeded,
                ageRange = "20-35",
                skillLevel = "중급",
                location = "서울",
                deadline = deadline,
            )

        // then
        assertThat(recruitment.team).isEqualTo(team)
        assertThat(recruitment.title).isEqualTo(title)
        assertThat(recruitment.description).isEqualTo(description)
        assertThat(recruitment.positionsNeeded).isEqualTo(positionsNeeded)
        assertThat(recruitment.ageRange).isEqualTo("20-35")
        assertThat(recruitment.skillLevel).isEqualTo("중급")
        assertThat(recruitment.location).isEqualTo("서울")
        assertThat(recruitment.deadline).isEqualTo(deadline)
        assertThat(recruitment.status).isEqualTo(RecruitmentStatus.OPEN)
    }

    @Test
    fun `should throw exception when creating recruitment with blank title`() {
        // given
        val deadline = LocalDate.now().plusDays(30)

        // when & then
        assertThrows<IllegalArgumentException> {
            TeamRecruitment.create(
                team = team,
                title = "",
                description = "설명",
                positionsNeeded = "투수",
                ageRange = null,
                skillLevel = null,
                location = null,
                deadline = deadline,
            )
        }
    }

    @Test
    fun `should throw exception when creating recruitment with blank description`() {
        // given
        val deadline = LocalDate.now().plusDays(30)

        // when & then
        assertThrows<IllegalArgumentException> {
            TeamRecruitment.create(
                team = team,
                title = "제목",
                description = "",
                positionsNeeded = "투수",
                ageRange = null,
                skillLevel = null,
                location = null,
                deadline = deadline,
            )
        }
    }

    @Test
    fun `should throw exception when creating recruitment with blank positions`() {
        // given
        val deadline = LocalDate.now().plusDays(30)

        // when & then
        assertThrows<IllegalArgumentException> {
            TeamRecruitment.create(
                team = team,
                title = "제목",
                description = "설명",
                positionsNeeded = "",
                ageRange = null,
                skillLevel = null,
                location = null,
                deadline = deadline,
            )
        }
    }

    @Test
    fun `should throw exception when creating recruitment with past deadline`() {
        // given
        val pastDeadline = LocalDate.now().minusDays(1)

        // when & then
        assertThrows<IllegalArgumentException> {
            TeamRecruitment.create(
                team = team,
                title = "제목",
                description = "설명",
                positionsNeeded = "투수",
                ageRange = null,
                skillLevel = null,
                location = null,
                deadline = pastDeadline,
            )
        }
    }

    @Test
    fun `should close open recruitment`() {
        // given
        val recruitment =
            TeamRecruitment.create(
                team = team,
                title = "제목",
                description = "설명",
                positionsNeeded = "투수",
                ageRange = null,
                skillLevel = null,
                location = null,
                deadline = LocalDate.now().plusDays(30),
            )

        // when
        recruitment.close()

        // then
        assertThat(recruitment.status).isEqualTo(RecruitmentStatus.CLOSED)
    }

    @Test
    fun `should throw exception when closing already closed recruitment`() {
        // given
        val recruitment =
            TeamRecruitment.create(
                team = team,
                title = "제목",
                description = "설명",
                positionsNeeded = "투수",
                ageRange = null,
                skillLevel = null,
                location = null,
                deadline = LocalDate.now().plusDays(30),
            )
        recruitment.close()

        // when & then
        assertThrows<IllegalArgumentException> {
            recruitment.close()
        }
    }

    @Test
    fun `should return false when deadline has not passed`() {
        // given
        val recruitment =
            TeamRecruitment.create(
                team = team,
                title = "제목",
                description = "설명",
                positionsNeeded = "투수",
                ageRange = null,
                skillLevel = null,
                location = null,
                deadline = LocalDate.now().plusDays(30),
            )

        // when
        val isExpired = recruitment.isExpired()

        // then
        assertThat(isExpired).isFalse()
    }

    @Test
    fun `should update recruitment with valid data`() {
        // given
        val recruitment =
            TeamRecruitment.create(
                team = team,
                title = "제목",
                description = "설명",
                positionsNeeded = "투수",
                ageRange = null,
                skillLevel = null,
                location = null,
                deadline = LocalDate.now().plusDays(30),
            )

        val newTitle = "새 제목"
        val newDescription = "새 설명"
        val newPositions = "투수,포수"
        val newDeadline = LocalDate.now().plusDays(60)

        // when
        recruitment.update(
            title = newTitle,
            description = newDescription,
            positionsNeeded = newPositions,
            deadline = newDeadline,
        )

        // then
        assertThat(recruitment.title).isEqualTo(newTitle)
        assertThat(recruitment.description).isEqualTo(newDescription)
        assertThat(recruitment.positionsNeeded).isEqualTo(newPositions)
        assertThat(recruitment.deadline).isEqualTo(newDeadline)
    }

    @Test
    fun `should throw exception when updating closed recruitment`() {
        // given
        val recruitment =
            TeamRecruitment.create(
                team = team,
                title = "제목",
                description = "설명",
                positionsNeeded = "투수",
                ageRange = null,
                skillLevel = null,
                location = null,
                deadline = LocalDate.now().plusDays(30),
            )
        recruitment.close()

        // when & then
        assertThrows<IllegalArgumentException> {
            recruitment.update(title = "새 제목")
        }
    }

    @Test
    fun `should throw exception when updating with past deadline`() {
        // given
        val recruitment =
            TeamRecruitment.create(
                team = team,
                title = "제목",
                description = "설명",
                positionsNeeded = "투수",
                ageRange = null,
                skillLevel = null,
                location = null,
                deadline = LocalDate.now().plusDays(30),
            )

        // when & then
        assertThrows<IllegalArgumentException> {
            recruitment.update(deadline = LocalDate.now().minusDays(1))
        }
    }
}
