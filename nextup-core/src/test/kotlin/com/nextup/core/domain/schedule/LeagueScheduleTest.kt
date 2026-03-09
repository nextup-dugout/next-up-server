package com.nextup.core.domain.schedule

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class LeagueScheduleTest {
    @Test
    fun `should create schedule with SCHEDULED status`() {
        // given & when
        val schedule = createSchedule()

        // then
        assertThat(schedule.status).isEqualTo(ScheduleStatus.SCHEDULED)
        assertThat(schedule.round).isEqualTo(1)
        assertThat(schedule.matchNumber).isEqualTo(1)
        assertThat(schedule.game).isNull()
    }

    @Test
    fun `should throw when round is less than 1`() {
        // when & then
        assertThrows<IllegalArgumentException> {
            LeagueSchedule.create(
                competition = createCompetition(),
                round = 0,
                matchNumber = 1,
                homeTeam = createTeam("홈팀", 1L),
                awayTeam = createTeam("원정팀", 2L),
                scheduledDate = LocalDate.now().plusDays(7),
            )
        }
    }

    @Test
    fun `should throw when matchNumber is less than 1`() {
        // when & then
        assertThrows<IllegalArgumentException> {
            LeagueSchedule.create(
                competition = createCompetition(),
                round = 1,
                matchNumber = 0,
                homeTeam = createTeam("홈팀", 1L),
                awayTeam = createTeam("원정팀", 2L),
                scheduledDate = LocalDate.now().plusDays(7),
            )
        }
    }

    @Test
    fun `should throw when homeTeam and awayTeam are the same`() {
        // given
        val team = createTeam("같은팀", 1L)

        // when & then
        assertThrows<IllegalArgumentException> {
            LeagueSchedule.create(
                competition = createCompetition(),
                round = 1,
                matchNumber = 1,
                homeTeam = team,
                awayTeam = team,
                scheduledDate = LocalDate.now().plusDays(7),
            )
        }
    }

    @Test
    fun `should link game and change status to GAME_CREATED`() {
        // given
        val schedule = createSchedule()
        val game = createGame()

        // when
        schedule.linkGame(game)

        // then
        assertThat(schedule.status).isEqualTo(ScheduleStatus.GAME_CREATED)
        assertThat(schedule.game).isEqualTo(game)
    }

    @Test
    fun `should throw when linking game to cancelled schedule`() {
        // given
        val schedule = createSchedule()
        schedule.cancel()

        // when & then
        assertThrows<IllegalArgumentException> {
            schedule.linkGame(createGame())
        }
    }

    @Test
    fun `should postpone schedule`() {
        // given
        val schedule = createSchedule()

        // when
        schedule.postpone("우천")

        // then
        assertThat(schedule.status).isEqualTo(ScheduleStatus.POSTPONED)
    }

    @Test
    fun `should throw when postponing non-scheduled schedule`() {
        // given
        val schedule = createSchedule()
        schedule.postpone("우천")

        // when & then
        assertThrows<IllegalArgumentException> {
            schedule.postpone("우천")
        }
    }

    @Test
    fun `should cancel schedule`() {
        // given
        val schedule = createSchedule()

        // when
        schedule.cancel()

        // then
        assertThat(schedule.status).isEqualTo(ScheduleStatus.CANCELLED)
    }

    @Test
    fun `should throw when cancelling completed schedule`() {
        // given
        val schedule = createSchedule()
        schedule.linkGame(createGame())
        schedule.complete()

        // when & then
        assertThrows<IllegalArgumentException> {
            schedule.cancel()
        }
    }

    @Test
    fun `should complete schedule with GAME_CREATED status`() {
        // given
        val schedule = createSchedule()
        schedule.linkGame(createGame())

        // when
        schedule.complete()

        // then
        assertThat(schedule.status).isEqualTo(ScheduleStatus.COMPLETED)
    }

    @Test
    fun `should throw when completing scheduled schedule without game`() {
        // given
        val schedule = createSchedule()

        // when & then
        assertThrows<IllegalArgumentException> {
            schedule.complete()
        }
    }

    @Test
    fun `should reschedule with new date`() {
        // given
        val schedule = createSchedule()
        val newDate = LocalDate.now().plusDays(14)
        val newTime = LocalTime.of(15, 0)

        // when
        schedule.reschedule(newDate, newTime, "새 구장")

        // then
        assertThat(schedule.scheduledDate).isEqualTo(newDate)
        assertThat(schedule.scheduledTime).isEqualTo(newTime)
        assertThat(schedule.venue).isEqualTo("새 구장")
        assertThat(schedule.status).isEqualTo(ScheduleStatus.SCHEDULED)
    }

    @Test
    fun `should reschedule postponed schedule and change status to SCHEDULED`() {
        // given
        val schedule = createSchedule()
        schedule.postpone("우천")
        val newDate = LocalDate.now().plusDays(14)

        // when
        schedule.reschedule(newDate)

        // then
        assertThat(schedule.scheduledDate).isEqualTo(newDate)
        assertThat(schedule.status).isEqualTo(ScheduleStatus.SCHEDULED)
    }

    @Test
    fun `should throw when rescheduling cancelled schedule`() {
        // given
        val schedule = createSchedule()
        schedule.cancel()

        // when & then
        assertThrows<IllegalArgumentException> {
            schedule.reschedule(LocalDate.now().plusDays(14))
        }
    }

    @Test
    fun `should link game to postponed schedule`() {
        // given
        val schedule = createSchedule()
        schedule.postpone("우천")
        val game = createGame()

        // when
        schedule.linkGame(game)

        // then
        assertThat(schedule.status).isEqualTo(ScheduleStatus.GAME_CREATED)
        assertThat(schedule.game).isEqualTo(game)
    }

    // ========== Helper Methods ==========

    private fun createSchedule(): LeagueSchedule =
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

    private fun createCompetition(): Competition {
        val association = Association(name = "서울시야구협회", abbreviation = "서야협", region = "서울")
        val league = League(association = association, name = "서울시 리그", foundedYear = 2020)
        return Competition(
            league = league,
            name = "2024 시즌",
            year = 2024,
            startDate = LocalDate.now().minusDays(30),
        )
    }

    private fun createTeam(
        name: String,
        id: Long
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

    private fun createGame(): Game =
        Game.createForTest(
            competition = createCompetition(),
            homeTeam = createTeam("홈팀", 10L),
            awayTeam = createTeam("원정팀", 20L),
            scheduledAt = LocalDateTime.now().plusDays(7),
            location = "서울야구장",
        )
}
