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
        every {
            scheduleRepository.findByCompetitionIdAndScheduledDate(any(), any())
        } returns emptyList()
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

    @Test
    fun `should throw InvalidScheduleStateException when team time conflict detected`() {
        // given
        val competition = createCompetition()
        val homeTeam = createTeam("팀A", 1L)
        val awayTeam = createTeam("팀B", 2L)
        val teamC = createTeam("팀C", 3L)
        val scheduledDate = LocalDate.now().plusDays(7)
        val scheduledTime = LocalTime.of(14, 0)

        val existingSchedule =
            LeagueSchedule.create(
                competition = competition,
                round = 1,
                matchNumber = 1,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime,
                venue = "구장A",
            ).apply {
                val idField = LeagueSchedule::class.java.getDeclaredField("id")
                idField.isAccessible = true
                idField.set(this, 100L)
            }

        every { competitionRepository.findByIdOrNull(1L) } returns competition
        every { teamRepository.findByIdOrNull(1L) } returns homeTeam
        every { teamRepository.findByIdOrNull(3L) } returns teamC
        every {
            scheduleRepository.existsByCompetitionIdAndRoundAndMatchNumber(any(), any(), any())
        } returns false
        every {
            scheduleRepository.findByCompetitionIdAndScheduledDate(1L, scheduledDate)
        } returns listOf(existingSchedule)

        // when & then
        val exception =
            assertThrows<InvalidScheduleStateException> {
                service.createSchedule(
                    competitionId = 1L,
                    round = 1,
                    matchNumber = 2,
                    homeTeamId = 1L,
                    awayTeamId = 3L,
                    scheduledDate = scheduledDate,
                    scheduledTime = scheduledTime,
                    venue = "구장B",
                )
            }

        assertThat(exception.message).contains("충돌이 감지되었습니다")
        assertThat(exception.message).contains("팀A")
    }

    @Test
    fun `should throw InvalidScheduleStateException when venue time conflict detected`() {
        // given
        val competition = createCompetition()
        val homeTeam = createTeam("팀A", 1L)
        val awayTeam = createTeam("팀B", 2L)
        val teamC = createTeam("팀C", 3L)
        val teamD = createTeam("팀D", 4L)
        val scheduledDate = LocalDate.now().plusDays(7)
        val scheduledTime = LocalTime.of(14, 0)

        val existingSchedule =
            LeagueSchedule.create(
                competition = competition,
                round = 1,
                matchNumber = 1,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime,
                venue = "구장A",
            ).apply {
                val idField = LeagueSchedule::class.java.getDeclaredField("id")
                idField.isAccessible = true
                idField.set(this, 100L)
            }

        every { competitionRepository.findByIdOrNull(1L) } returns competition
        every { teamRepository.findByIdOrNull(3L) } returns teamC
        every { teamRepository.findByIdOrNull(4L) } returns teamD
        every {
            scheduleRepository.existsByCompetitionIdAndRoundAndMatchNumber(any(), any(), any())
        } returns false
        every {
            scheduleRepository.findByCompetitionIdAndScheduledDate(1L, scheduledDate)
        } returns listOf(existingSchedule)

        // when & then
        val exception =
            assertThrows<InvalidScheduleStateException> {
                service.createSchedule(
                    competitionId = 1L,
                    round = 1,
                    matchNumber = 2,
                    homeTeamId = 3L,
                    awayTeamId = 4L,
                    scheduledDate = scheduledDate,
                    scheduledTime = scheduledTime,
                    venue = "구장A",
                )
            }

        assertThat(exception.message).contains("충돌이 감지되었습니다")
        assertThat(exception.message).contains("구장A")
    }

    @Test
    fun `should validate schedule and return conflicts without saving`() {
        // given
        val competition = createCompetition()
        val homeTeam = createTeam("팀A", 1L)
        val awayTeam = createTeam("팀B", 2L)
        val teamC = createTeam("팀C", 3L)
        val scheduledDate = LocalDate.now().plusDays(7)
        val scheduledTime = LocalTime.of(14, 0)

        val existingSchedule =
            LeagueSchedule.create(
                competition = competition,
                round = 1,
                matchNumber = 1,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime,
                venue = "구장A",
            ).apply {
                val idField = LeagueSchedule::class.java.getDeclaredField("id")
                idField.isAccessible = true
                idField.set(this, 100L)
            }

        every { competitionRepository.findByIdOrNull(1L) } returns competition
        every { teamRepository.findByIdOrNull(1L) } returns homeTeam
        every { teamRepository.findByIdOrNull(3L) } returns teamC
        every {
            scheduleRepository.findByCompetitionIdAndScheduledDate(1L, scheduledDate)
        } returns listOf(existingSchedule)

        // when
        val conflicts =
            service.validateSchedule(
                competitionId = 1L,
                round = 1,
                matchNumber = 2,
                homeTeamId = 1L,
                awayTeamId = 3L,
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime,
                venue = "구장B",
            )

        // then
        assertThat(conflicts).hasSize(1)
        assertThat(conflicts[0].description).contains("팀A")
        verify(exactly = 0) { scheduleRepository.save(any()) }
    }

    @Test
    fun `should validate schedule and return no conflicts when valid`() {
        // given
        val competition = createCompetition()
        val homeTeam = createTeam("팀A", 1L)
        val awayTeam = createTeam("팀B", 2L)
        val scheduledDate = LocalDate.now().plusDays(7)
        val scheduledTime = LocalTime.of(14, 0)

        every { competitionRepository.findByIdOrNull(1L) } returns competition
        every { teamRepository.findByIdOrNull(1L) } returns homeTeam
        every { teamRepository.findByIdOrNull(2L) } returns awayTeam
        every {
            scheduleRepository.findByCompetitionIdAndScheduledDate(1L, scheduledDate)
        } returns emptyList()

        // when
        val conflicts =
            service.validateSchedule(
                competitionId = 1L,
                round = 1,
                matchNumber = 1,
                homeTeamId = 1L,
                awayTeamId = 2L,
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime,
                venue = "구장A",
            )

        // then
        assertThat(conflicts).isEmpty()
    }

    @Test
    fun `should generate round robin schedule for 4 teams`() {
        // given
        val competition = createCompetition()
        val teams =
            listOf(
                createTeam("팀A", 1L),
                createTeam("팀B", 2L),
                createTeam("팀C", 3L),
                createTeam("팀D", 4L),
            )

        every { competitionRepository.findByIdOrNull(1L) } returns competition
        teams.forEach { team ->
            every { teamRepository.findByIdOrNull(team.id) } returns team
        }
        every { scheduleRepository.save(any<LeagueSchedule>()) } answers { firstArg() }

        // when
        val result =
            service.generateRoundRobinSchedule(
                competitionId = 1L,
                teamIds = listOf(1L, 2L, 3L, 4L),
                doubleRoundRobin = false,
            )

        // then
        assertThat(result).hasSize(6) // 4C2 = 6 matches
        assertThat(result.map { it.round }.distinct()).containsExactlyInAnyOrder(1, 2, 3)

        // 각 팀이 정확히 3번씩 경기
        val teamMatchCounts = mutableMapOf<Long, Int>()
        result.forEach { schedule ->
            teamMatchCounts[schedule.homeTeam.id] =
                teamMatchCounts.getOrDefault(schedule.homeTeam.id, 0) + 1
            teamMatchCounts[schedule.awayTeam.id] =
                teamMatchCounts.getOrDefault(schedule.awayTeam.id, 0) + 1
        }
        assertThat(teamMatchCounts.values).allMatch { it == 3 }

        verify(exactly = 6) { scheduleRepository.save(any<LeagueSchedule>()) }
    }

    @Test
    fun `should generate double round robin schedule`() {
        // given
        val competition = createCompetition()
        val teams =
            listOf(
                createTeam("팀A", 1L),
                createTeam("팀B", 2L),
                createTeam("팀C", 3L),
            )

        every { competitionRepository.findByIdOrNull(1L) } returns competition
        teams.forEach { team ->
            every { teamRepository.findByIdOrNull(team.id) } returns team
        }
        every { scheduleRepository.save(any<LeagueSchedule>()) } answers { firstArg() }

        // when
        val result =
            service.generateRoundRobinSchedule(
                competitionId = 1L,
                teamIds = listOf(1L, 2L, 3L),
                doubleRoundRobin = true,
            )

        // then
        assertThat(result).hasSize(6) // 3C2 * 2 = 6 matches

        // 각 팀이 정확히 4번씩 경기 (모든 상대와 2번씩)
        val teamMatchCounts = mutableMapOf<Long, Int>()
        result.forEach { schedule ->
            teamMatchCounts[schedule.homeTeam.id] =
                teamMatchCounts.getOrDefault(schedule.homeTeam.id, 0) + 1
            teamMatchCounts[schedule.awayTeam.id] =
                teamMatchCounts.getOrDefault(schedule.awayTeam.id, 0) + 1
        }
        assertThat(teamMatchCounts.values).allMatch { it == 4 }

        verify(exactly = 6) { scheduleRepository.save(any<LeagueSchedule>()) }
    }

    @Test
    fun `should throw CompetitionNotFoundException when generating schedule for non-existent competition`() {
        // given
        every { competitionRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThrows<CompetitionNotFoundException> {
            service.generateRoundRobinSchedule(
                competitionId = 999L,
                teamIds = listOf(1L, 2L, 3L),
            )
        }
    }

    @Test
    fun `should throw TeamNotFoundException when generating schedule with non-existent team`() {
        // given
        val competition = createCompetition()
        every { competitionRepository.findByIdOrNull(1L) } returns competition
        every { teamRepository.findByIdOrNull(1L) } returns createTeam("팀A", 1L)
        every { teamRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThrows<TeamNotFoundException> {
            service.generateRoundRobinSchedule(
                competitionId = 1L,
                teamIds = listOf(1L, 999L),
            )
        }
    }

    @Test
    fun `should assign temporary dates with weekly intervals`() {
        // given
        val competition = createCompetition()
        val teams =
            listOf(
                createTeam("팀A", 1L),
                createTeam("팀B", 2L),
                createTeam("팀C", 3L),
            )

        every { competitionRepository.findByIdOrNull(1L) } returns competition
        teams.forEach { team ->
            every { teamRepository.findByIdOrNull(team.id) } returns team
        }
        every { scheduleRepository.save(any<LeagueSchedule>()) } answers { firstArg() }

        // when
        val result =
            service.generateRoundRobinSchedule(
                competitionId = 1L,
                teamIds = listOf(1L, 2L, 3L),
            )

        // then
        val round1Date = result.first { it.round == 1 }.scheduledDate
        val round2Date = result.first { it.round == 2 }.scheduledDate
        val round3Date = result.first { it.round == 3 }.scheduledDate

        assertThat(round2Date).isEqualTo(round1Date.plusWeeks(1))
        assertThat(round3Date).isEqualTo(round1Date.plusWeeks(2))
    }

    @Test
    fun `should update schedule with new date`() {
        // given
        val schedule = createLeagueSchedule()
        val newDate = LocalDate.now().plusDays(14)
        every { scheduleRepository.findByIdOrNull(1L) } returns schedule

        // when
        val result = service.updateSchedule(
            scheduleId = 1L,
            scheduledDate = newDate,
            scheduledTime = LocalTime.of(18, 0),
            venue = "부산야구장",
        )

        // then
        assertThat(result.scheduledDate).isEqualTo(newDate)
        assertThat(result.scheduledTime).isEqualTo(LocalTime.of(18, 0))
        assertThat(result.venue).isEqualTo("부산야구장")
    }

    @Test
    fun `should update schedule with only venue change`() {
        // given
        val schedule = createLeagueSchedule()
        every { scheduleRepository.findByIdOrNull(1L) } returns schedule

        // when
        val result = service.updateSchedule(
            scheduleId = 1L,
            venue = "대전야구장",
        )

        // then
        assertThat(result.venue).isEqualTo("대전야구장")
    }

    @Test
    fun `should update schedule with only time change`() {
        // given
        val schedule = createLeagueSchedule()
        every { scheduleRepository.findByIdOrNull(1L) } returns schedule

        // when
        val result = service.updateSchedule(
            scheduleId = 1L,
            scheduledTime = LocalTime.of(19, 0),
        )

        // then
        assertThat(result.scheduledTime).isEqualTo(LocalTime.of(19, 0))
    }

    @Test
    fun `should throw InvalidScheduleStateException when updating completed schedule date`() {
        // given
        val schedule = createLeagueSchedule()
        // SCHEDULED -> GAME_CREATED -> COMPLETED
        val game = mockk<com.nextup.core.domain.game.Game>(relaxed = true)
        schedule.linkGame(game)
        schedule.complete()

        every { scheduleRepository.findByIdOrNull(1L) } returns schedule

        // when & then
        assertThrows<InvalidScheduleStateException> {
            service.updateSchedule(
                scheduleId = 1L,
                scheduledDate = LocalDate.now().plusDays(30),
            )
        }
    }

    @Test
    fun `should postpone games in bulk successfully`() {
        // given
        val date = LocalDate.now().plusDays(7)
        val schedules = listOf(createLeagueSchedule(), createLeagueSchedule())
        every { scheduleRepository.findByCompetitionIdAndScheduledDate(1L, date) } returns schedules

        // when
        val result = service.postponeGamesBulk(1L, date, "우천")

        // then
        assertThat(result).hasSize(2)
    }

    @Test
    fun `should throw InvalidScheduleStateException when no games to postpone`() {
        // given
        val date = LocalDate.now().plusDays(7)
        every { scheduleRepository.findByCompetitionIdAndScheduledDate(1L, date) } returns emptyList()

        // when & then
        val exception = assertThrows<InvalidScheduleStateException> {
            service.postponeGamesBulk(1L, date, "우천")
        }
        assertThat(exception.message).contains("연기할 경기가 없습니다")
    }

    @Test
    fun `should reschedule game successfully`() {
        // given
        val schedule = createLeagueSchedule()
        // postpone first
        schedule.postpone("우천")
        val newDate = LocalDate.now().plusDays(14)
        every { scheduleRepository.findByIdOrNull(1L) } returns schedule
        every {
            scheduleRepository.findByCompetitionIdAndScheduledDate(any(), eq(newDate))
        } returns emptyList()

        // when
        val result = service.rescheduleGame(1L, newDate, "인천야구장")

        // then
        assertThat(result.scheduledDate).isEqualTo(newDate)
        assertThat(result.venue).isEqualTo("인천야구장")
    }

    @Test
    fun `should throw InvalidScheduleStateException when reschedule has conflicts`() {
        // given
        val competition = createCompetition()
        val homeTeam = createTeam("팀A", 1L)
        val awayTeam = createTeam("팀B", 2L)
        val teamC = createTeam("팀C", 3L)
        val newDate = LocalDate.now().plusDays(14)
        val scheduledTime = LocalTime.of(14, 0)

        val schedule = LeagueSchedule.create(
            competition = competition,
            round = 1,
            matchNumber = 1,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            scheduledDate = LocalDate.now().plusDays(7),
            scheduledTime = scheduledTime,
            venue = "구장A",
        ).apply {
            val idField = LeagueSchedule::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, 1L)
        }
        schedule.postpone("우천")

        val conflictingSchedule = LeagueSchedule.create(
            competition = competition,
            round = 2,
            matchNumber = 1,
            homeTeam = homeTeam,
            awayTeam = teamC,
            scheduledDate = newDate,
            scheduledTime = scheduledTime,
            venue = "구장B",
        ).apply {
            val idField = LeagueSchedule::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, 2L)
        }

        every { scheduleRepository.findByIdOrNull(1L) } returns schedule
        every {
            scheduleRepository.findByCompetitionIdAndScheduledDate(any(), eq(newDate))
        } returns listOf(conflictingSchedule)

        // when & then
        val exception = assertThrows<InvalidScheduleStateException> {
            service.rescheduleGame(1L, newDate)
        }
        assertThat(exception.message).contains("충돌이 감지되었습니다")
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
