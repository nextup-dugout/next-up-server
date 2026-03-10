package com.nextup.api.integration

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.TeamRepositoryPort
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

@Transactional
class TeamRepositoryIntegrationTest : IntegrationTestBase() {
    @Autowired
    private lateinit var teamRepository: TeamRepositoryPort

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var league: League

    @BeforeEach
    fun setUp() {
        val association =
            Association(
                name = "서울시야구협회",
                abbreviation = "SBA",
                region = "서울",
            )
        entityManager.persist(association)

        league =
            League(
                association = association,
                name = "1부 리그",
                abbreviation = "L1",
                foundedYear = 2020,
            )
        entityManager.persist(league)
        entityManager.flush()
    }

    @Test
    fun `팀을 저장하고 ID로 조회할 수 있다`() {
        // given
        val team =
            Team(
                league = league,
                name = "테스트팀",
                city = "서울",
                foundedYear = 2024,
                abbreviation = "TST",
            )
        val saved = teamRepository.save(team)

        entityManager.flush()
        entityManager.clear()

        // when
        val found = teamRepository.findByIdOrNull(saved.id)

        // then
        assertThat(found).isNotNull
        assertThat(found!!.name).isEqualTo("테스트팀")
        assertThat(found.city).isEqualTo("서울")
        assertThat(found.foundedYear).isEqualTo(2024)
    }

    @Test
    fun `이름으로 팀을 조회할 수 있다`() {
        // given
        val team =
            Team(
                league = league,
                name = "유니크팀",
                city = "부산",
                foundedYear = 2023,
            )
        teamRepository.save(team)
        entityManager.flush()
        entityManager.clear()

        // when
        val found = teamRepository.findByName("유니크팀")

        // then
        assertThat(found).isNotNull
        assertThat(found!!.city).isEqualTo("부산")
    }

    @Test
    fun `활성 팀만 필터 조회할 수 있다`() {
        // given
        val activeTeam =
            Team(
                league = league,
                name = "활성팀",
                city = "서울",
                foundedYear = 2024,
                isActive = true,
            )
        val inactiveTeam =
            Team(
                league = league,
                name = "비활성팀",
                city = "인천",
                foundedYear = 2024,
                isActive = false,
            )
        teamRepository.save(activeTeam)
        teamRepository.save(inactiveTeam)
        entityManager.flush()
        entityManager.clear()

        // when
        val activeTeams = teamRepository.findActiveTeams()

        // then
        assertThat(activeTeams.map { it.name }).contains("활성팀")
        assertThat(activeTeams.map { it.name }).doesNotContain("비활성팀")
    }

    @Test
    fun `리그 ID로 팀 목록을 조회할 수 있다`() {
        // given
        val team1 =
            Team(
                league = league,
                name = "팀A",
                city = "서울",
                foundedYear = 2024,
            )
        val team2 =
            Team(
                league = league,
                name = "팀B",
                city = "부산",
                foundedYear = 2024,
            )
        teamRepository.save(team1)
        teamRepository.save(team2)
        entityManager.flush()
        entityManager.clear()

        // when
        val teams = teamRepository.findByLeagueId(league.id)

        // then
        assertThat(teams).hasSizeGreaterThanOrEqualTo(2)
        assertThat(teams.map { it.name }).containsAll(listOf("팀A", "팀B"))
    }
}
