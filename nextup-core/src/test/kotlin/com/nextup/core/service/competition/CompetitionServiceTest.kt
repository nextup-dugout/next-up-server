package com.nextup.core.service.competition

import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.common.exception.InvalidCompetitionStateException
import com.nextup.common.exception.LeagueNotFoundException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.league.League
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.LeagueRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Optional

@DisplayName("CompetitionService")
class CompetitionServiceTest {

    private lateinit var competitionRepository: CompetitionRepositoryPort
    private lateinit var leagueRepository: LeagueRepositoryPort
    private lateinit var competitionService: CompetitionService

    @BeforeEach
    fun setUp() {
        competitionRepository = mockk()
        leagueRepository = mockk()
        competitionService = CompetitionService(competitionRepository, leagueRepository)
    }

    @Nested
    @DisplayName("create")
    inner class Create {

        @Test
        fun `should create competition successfully`() {
            // given
            val leagueId = 1L
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(leagueId, "1부 리그", association)
            val name = "2025 춘계대회"
            val year = 2025
            val season = 1
            val startDate = LocalDate.of(2025, 3, 1)

            every { leagueRepository.findByIdOrNull(leagueId) } returns league
            every { competitionRepository.findByLeagueIdAndYearAndSeason(leagueId, year, season) } returns null
            every { competitionRepository.save(any()) } answers { firstArg() }

            // when
            val result = competitionService.create(
                leagueId = leagueId,
                name = name,
                year = year,
                season = season,
                type = CompetitionType.LEAGUE,
                startDate = startDate,
                description = "2025년 춘계 정규 리그"
            )

            // then
            assertThat(result.name).isEqualTo(name)
            assertThat(result.year).isEqualTo(year)
            assertThat(result.season).isEqualTo(season)
            assertThat(result.league).isEqualTo(league)
            assertThat(result.status).isEqualTo(CompetitionStatus.SCHEDULED)
            verify { competitionRepository.save(any()) }
        }

        @Test
        fun `should throw exception when league not found`() {
            // given
            val leagueId = 999L
            every { leagueRepository.findByIdOrNull(leagueId) } returns null

            // when & then
            assertThatThrownBy {
                competitionService.create(
                    leagueId = leagueId,
                    name = "2025 춘계대회",
                    year = 2025,
                    startDate = LocalDate.of(2025, 3, 1)
                )
            }.isInstanceOf(LeagueNotFoundException::class.java)
        }

        @Test
        fun `should throw exception when competition already exists for same league year season`() {
            // given
            val leagueId = 1L
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(leagueId, "1부 리그", association)
            val year = 2025
            val season = 1
            val existingCompetition = createCompetition(1L, league, "2025 춘계대회", year, season)

            every { leagueRepository.findByIdOrNull(leagueId) } returns league
            every { competitionRepository.findByLeagueIdAndYearAndSeason(leagueId, year, season) } returns existingCompetition

            // when & then
            assertThatThrownBy {
                competitionService.create(
                    leagueId = leagueId,
                    name = "2025 춘계대회",
                    year = year,
                    season = season,
                    startDate = LocalDate.of(2025, 3, 1)
                )
            }.isInstanceOf(InvalidCompetitionStateException::class.java)
        }
    }

    @Nested
    @DisplayName("getById")
    inner class GetById {

        @Test
        fun `should return competition when found`() {
            // given
            val id = 1L
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association)
            val competition = createCompetition(id, league, "2025 춘계대회", 2025, 1)
            every { competitionRepository.findByIdOrNull(id) } returns competition

            // when
            val result = competitionService.getById(id)

            // then
            assertThat(result.id).isEqualTo(id)
            assertThat(result.name).isEqualTo("2025 춘계대회")
        }

        @Test
        fun `should throw exception when not found`() {
            // given
            val id = 999L
            every { competitionRepository.findByIdOrNull(id) } returns null

            // when & then
            assertThatThrownBy {
                competitionService.getById(id)
            }.isInstanceOf(CompetitionNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("getInProgress")
    inner class GetInProgress {

        @Test
        fun `should return only in-progress competitions`() {
            // given
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association)
            val competitions = listOf(
                createCompetition(1L, league, "2025 춘계대회", 2025, 1).apply { start() },
                createCompetition(2L, league, "2025 하계대회", 2025, 2).apply { start() }
            )
            every { competitionRepository.findInProgressCompetitions() } returns competitions

            // when
            val result = competitionService.getInProgress()

            // then
            assertThat(result).hasSize(2)
            assertThat(result.all { it.status == CompetitionStatus.IN_PROGRESS }).isTrue()
        }
    }

    @Nested
    @DisplayName("getByLeagueId")
    inner class GetByLeagueId {

        @Test
        fun `should return competitions by league`() {
            // given
            val leagueId = 1L
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(leagueId, "1부 리그", association)
            val competitions = listOf(
                createCompetition(1L, league, "2025 춘계대회", 2025, 1),
                createCompetition(2L, league, "2025 하계대회", 2025, 2)
            )
            every { competitionRepository.findByLeagueId(leagueId) } returns competitions

            // when
            val result = competitionService.getByLeagueId(leagueId)

            // then
            assertThat(result).hasSize(2)
            assertThat(result.all { it.league.id == leagueId }).isTrue()
        }
    }

    @Nested
    @DisplayName("start")
    inner class Start {

        @Test
        fun `should start scheduled competition`() {
            // given
            val id = 1L
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association)
            val competition = createCompetition(id, league, "2025 춘계대회", 2025, 1)
            every { competitionRepository.findByIdOrNull(id) } returns competition

            // when
            val result = competitionService.start(id)

            // then
            assertThat(result.status).isEqualTo(CompetitionStatus.IN_PROGRESS)
        }

        @Test
        fun `should throw exception when competition is not scheduled`() {
            // given
            val id = 1L
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association)
            val competition = createCompetition(id, league, "2025 춘계대회", 2025, 1).apply { start() }
            every { competitionRepository.findByIdOrNull(id) } returns competition

            // when & then
            assertThatThrownBy {
                competitionService.start(id)
            }.isInstanceOf(InvalidCompetitionStateException::class.java)
        }
    }

    @Nested
    @DisplayName("complete")
    inner class Complete {

        @Test
        fun `should complete in-progress competition`() {
            // given
            val id = 1L
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association)
            val competition = createCompetition(id, league, "2025 춘계대회", 2025, 1).apply { start() }
            val endDate = LocalDate.of(2025, 5, 31)
            every { competitionRepository.findByIdOrNull(id) } returns competition

            // when
            val result = competitionService.complete(id, endDate)

            // then
            assertThat(result.status).isEqualTo(CompetitionStatus.COMPLETED)
            assertThat(result.endDate).isEqualTo(endDate)
        }

        @Test
        fun `should throw exception when competition is not in progress`() {
            // given
            val id = 1L
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association)
            val competition = createCompetition(id, league, "2025 춘계대회", 2025, 1)
            every { competitionRepository.findByIdOrNull(id) } returns competition

            // when & then
            assertThatThrownBy {
                competitionService.complete(id)
            }.isInstanceOf(InvalidCompetitionStateException::class.java)
        }
    }

    @Nested
    @DisplayName("cancel")
    inner class Cancel {

        @Test
        fun `should cancel scheduled competition`() {
            // given
            val id = 1L
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association)
            val competition = createCompetition(id, league, "2025 춘계대회", 2025, 1)
            every { competitionRepository.findByIdOrNull(id) } returns competition

            // when
            val result = competitionService.cancel(id)

            // then
            assertThat(result.status).isEqualTo(CompetitionStatus.CANCELLED)
        }

        @Test
        fun `should cancel in-progress competition`() {
            // given
            val id = 1L
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association)
            val competition = createCompetition(id, league, "2025 춘계대회", 2025, 1).apply { start() }
            every { competitionRepository.findByIdOrNull(id) } returns competition

            // when
            val result = competitionService.cancel(id)

            // then
            assertThat(result.status).isEqualTo(CompetitionStatus.CANCELLED)
        }

        @Test
        fun `should throw exception when competition is already completed`() {
            // given
            val id = 1L
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(1L, "1부 리그", association)
            val competition = createCompetition(id, league, "2025 춘계대회", 2025, 1).apply {
                start()
                complete()
            }
            every { competitionRepository.findByIdOrNull(id) } returns competition

            // when & then
            assertThatThrownBy {
                competitionService.cancel(id)
            }.isInstanceOf(InvalidCompetitionStateException::class.java)
        }
    }

    private fun createAssociation(id: Long, name: String): Association {
        return Association(
            name = name,
            abbreviation = null,
            region = "서울",
            description = null,
            logoUrl = null,
            websiteUrl = null
        ).apply {
            val idField = Association::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
    }

    private fun createLeague(id: Long, name: String, association: Association): League {
        return League(
            association = association,
            name = name,
            abbreviation = null,
            foundedYear = 2020,
            divisionLevel = 1,
            description = null,
            logoUrl = null
        ).apply {
            val idField = League::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
    }

    private fun createCompetition(
        id: Long,
        league: League,
        name: String,
        year: Int,
        season: Int
    ): Competition {
        return Competition(
            league = league,
            name = name,
            year = year,
            season = season,
            type = CompetitionType.LEAGUE,
            startDate = LocalDate.of(year, 3, 1),
            endDate = null,
            description = null,
            maxTeams = null
        ).apply {
            val idField = Competition::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
    }
}
