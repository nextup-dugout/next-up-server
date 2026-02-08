package com.nextup.core.service.schedule

import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.common.exception.InvalidScheduleStateException
import com.nextup.common.exception.ScheduleNotFoundException
import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.league.League
import com.nextup.core.domain.schedule.LeagueSchedule
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.LeagueScheduleRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalTime

class LeagueScheduleServiceTest {
    private lateinit var scheduleRepository: LeagueScheduleRepositoryPort
    private lateinit var competitionRepository: CompetitionRepositoryPort
    private lateinit var teamRepository: TeamRepositoryPort
    private lateinit var service: LeagueScheduleService

    @BeforeEach
    fun setUp() {
        scheduleRepository = mockk()
        competitionRepository = mockk()
        teamRepository = mockk()
        service = LeagueScheduleService(scheduleRepository, competitionRepository, teamRepository)
    }

    @Test
    fun `should create schedule successfully`() {
        // given
        val competition = createCompetition()
        val homeTeam = createTeam("홈팀", 1L)
        val awayTeam = createTeam("원정팀", 2L)
        val scheduledDate = LocalDate.now().plusDays(7)

        every { competitionRepository.findByIdOrNull(1L) } returns competition
        every { teamRepository.findByIdOrNull(1L) } returns homeTeam
        every { teamRepository.findByIdOrNull(2L) } returns awayTeam
        every {
            scheduleRepository.existsByCompetitionIdAndRoundAndMatchNumber(any(), any(), any())
        } returns false
        every { scheduleRepository.save(any<LeagueSchedule>()) } answers { firstArg() }

        // when
        val result =
            service.createSchedule(
                competitionId = 1L,
                round = 1,
                matchNumber = 1,
                homeTeamId = 1L,
                awayTeamId = 2L,
                scheduledDate = scheduledDate,
                scheduledTime = LocalTime.of(14, 0),
                venue = "서울야구장",
            )

        // then
        assertThat(result.round).isEqualTo(1)
        assertThat(result.matchNumber).isEqualTo(1)
        assertThat(result.scheduledDate).isEqualTo(scheduledDate)
    }

    @Test
    fun `should throw CompetitionNotFoundException when competition not found`() {
        // given
        every { competitionRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThrows<CompetitionNotFoundException> {
            service.createSchedule(
                competitionId = 999L,
                round = 1,
                matchNumber = 1,
                homeTeamId = 1L,
                awayTeamId = 2L,
                scheduledDate = LocalDate.now().plusDays(7),
            )
        }
    }

    @Test
    fun `should throw when home team not found`() {
        // given
        every { competitionRepository.findByIdOrNull(1L) } returns createCompetition()
        every { teamRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThrows<TeamNotFoundException> {
            service.createSchedule(
                competitionId = 1L,
                round = 1,
                matchNumber = 1,
                homeTeamId = 999L,
                awayTeamId = 2L,
                scheduledDate = LocalDate.now().plusDays(7),
            )
        }
    }

    @Test
    fun `should throw InvalidScheduleStateException when duplicate schedule exists`() {
        // given
        val competition = createCompetition()
        val homeTeam = createTeam("홈팀", 1L)
        val awayTeam = createTeam("원정팀", 2L)

        every { competitionRepository.findByIdOrNull(1L) } returns competition
        every { teamRepository.findByIdOrNull(1L) } returns homeTeam
        every { teamRepository.findByIdOrNull(2L) } returns awayTeam
        every {
            scheduleRepository.existsByCompetitionIdAndRoundAndMatchNumber(1L, 1, 1)
        } returns true

        // when & then
        assertThrows<InvalidScheduleStateException> {
            service.createSchedule(
                competitionId = 1L,
                round = 1,
                matchNumber = 1,
                homeTeamId = 1L,
                awayTeamId = 2L,
                scheduledDate = LocalDate.now().plusDays(7),
            )
        }
    }

    @Test
    fun `should throw ScheduleNotFoundException when schedule not found`() {
        // given
        every { scheduleRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThrows<ScheduleNotFoundException> {
            service.getById(999L)
        }
    }

    @Test
    fun `should delete schedule successfully`() {
        // given
        val schedule = createLeagueSchedule()
        every { scheduleRepository.findByIdOrNull(1L) } returns schedule
        every { scheduleRepository.delete(schedule) } returns Unit

        // when
        service.deleteSchedule(1L)

        // then
        verify { scheduleRepository.delete(schedule) }
    }

    @Test
    fun `should get schedules by competition`() {
        // given
        val schedules = listOf(createLeagueSchedule())
        every { scheduleRepository.findByCompetitionId(1L) } returns schedules

        // when
        val result = service.getSchedulesByCompetition(1L)

        // then
        assertThat(result).hasSize(1)
    }

    @Test
    fun `should get schedules by round`() {
        // given
        val schedules = listOf(createLeagueSchedule())
        every { scheduleRepository.findByCompetitionIdAndRound(1L, 1) } returns schedules

        // when
        val result = service.getSchedulesByRound(1L, 1)

        // then
        assertThat(result).hasSize(1)
    }

    // ========== Helper Methods ==========

    private fun createCompetition(): Competition {
        val association = Association(name = "서울시야구협회", abbreviation = "서야협", region = "서울")
        val league = League(association = association, name = "서울시 리그", foundedYear = 2020)
        return Competition(
            league = league,
            name = "2024 시즌",
            year = 2024,
            startDate = LocalDate.now().minusDays(30),
            id = 1L,
        )
    }

    private fun createTeam(
        name: String,
        id: Long,
    ): Team {
        val association = Association(name = "서울시야구협회", abbreviation = "서야협", region = "서울")
        val league = League(association = association, name = "서울시 리그", foundedYear = 2020)
        return Team(
            league = league,
            name = name,
            city = "서울",
            foundedYear = 2020,
            id = id,
        )
    }

    private fun createLeagueSchedule(): LeagueSchedule =
        LeagueSchedule.create(
            competition = createCompetition(),
            round = 1,
            matchNumber = 1,
            homeTeam = createTeam("홈팀", 1L),
            awayTeam = createTeam("원정팀", 2L),
            scheduledDate = LocalDate.now().plusDays(7),
            scheduledTime = LocalTime.of(14, 0),
            venue = "서울야구장",
        )
}
