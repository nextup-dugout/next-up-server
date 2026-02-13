package com.nextup.core.domain.schedule

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

class ScheduleConflictDetectorTest {
    private lateinit var detector: ScheduleConflictDetector
    private lateinit var association: Association
    private lateinit var league: League
    private lateinit var competition: Competition
    private lateinit var teamA: Team
    private lateinit var teamB: Team
    private lateinit var teamC: Team
    private lateinit var teamD: Team

    @BeforeEach
    fun setUp() {
        detector = ScheduleConflictDetector()

        association =
            Association(
                name = "Test Association",
                abbreviation = "TA",
                region = "서울",
            )

        league =
            League(
                association = association,
                name = "Test League",
                abbreviation = "TL",
                foundedYear = 2024,
                divisionLevel = 1,
            )

        competition =
            Competition(
                league = league,
                name = "2024 시즌",
                year = 2024,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2024, 3, 1),
            )

        teamA =
            Team(
                league = league,
                name = "팀A",
                city = "서울",
                foundedYear = 2020,
                id = 1L,
            )

        teamB =
            Team(
                league = league,
                name = "팀B",
                city = "서울",
                foundedYear = 2020,
                id = 2L,
            )

        teamC =
            Team(
                league = league,
                name = "팀C",
                city = "서울",
                foundedYear = 2020,
                id = 3L,
            )

        teamD =
            Team(
                league = league,
                name = "팀D",
                city = "서울",
                foundedYear = 2020,
                id = 4L,
            )
    }

    private fun createScheduleWithId(
        id: Long,
        round: Int,
        matchNumber: Int,
        homeTeam: Team,
        awayTeam: Team,
        scheduledDate: LocalDate,
        scheduledTime: LocalTime?,
        venue: String?,
    ): LeagueSchedule {
        val schedule =
            LeagueSchedule.create(
                competition = competition,
                round = round,
                matchNumber = matchNumber,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime,
                venue = venue,
            )
        // Use reflection to set the ID
        val idField = LeagueSchedule::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(schedule, id)
        return schedule
    }

    @Test
    fun `should detect no conflicts when no existing schedules`() {
        // given
        val schedule =
            createScheduleWithId(
                id = 1L,
                round = 1,
                matchNumber = 1,
                homeTeam = teamA,
                awayTeam = teamB,
                scheduledDate = LocalDate.of(2024, 5, 1),
                scheduledTime = LocalTime.of(14, 0),
                venue = "구장A",
            )

        // when
        val conflicts = detector.detectAllConflicts(schedule, emptyList())

        // then
        assertThat(conflicts).isEmpty()
    }

    @Test
    fun `should detect no conflicts when time is not specified`() {
        // given
        val schedule1 =
            createScheduleWithId(
                id = 1L,
                round = 1,
                matchNumber = 1,
                homeTeam = teamA,
                awayTeam = teamB,
                scheduledDate = LocalDate.of(2024, 5, 1),
                scheduledTime = null,
                venue = "구장A",
            )

        val schedule2 =
            createScheduleWithId(
                id = 2L,
                round = 1,
                matchNumber = 2,
                homeTeam = teamA,
                awayTeam = teamC,
                scheduledDate = LocalDate.of(2024, 5, 1),
                scheduledTime = null,
                venue = "구장A",
            )

        // when
        val conflicts = detector.detectAllConflicts(schedule1, listOf(schedule2))

        // then
        assertThat(conflicts).isEmpty()
    }

    @Test
    fun `should detect team conflict when same team plays at same time`() {
        // given
        val existingSchedule =
            createScheduleWithId(
                id = 100L,
                round = 1,
                matchNumber = 1,
                homeTeam = teamA,
                awayTeam = teamB,
                scheduledDate = LocalDate.of(2024, 5, 1),
                scheduledTime = LocalTime.of(14, 0),
                venue = "구장A",
            )

        val newSchedule =
            createScheduleWithId(
                id = 200L,
                round = 1,
                matchNumber = 2,
                homeTeam = teamA,
                awayTeam = teamC,
                scheduledDate = LocalDate.of(2024, 5, 1),
                scheduledTime = LocalTime.of(14, 0),
                venue = "구장B",
            )

        // when
        val conflicts = detector.detectTeamConflicts(newSchedule, listOf(existingSchedule))

        // then
        assertThat(conflicts).hasSize(1)
        assertThat(conflicts[0].type).isEqualTo(ConflictType.TEAM_TIME_CONFLICT)
        assertThat(conflicts[0].description).contains("팀A")
    }

    @Test
    fun `should detect team conflict when away team plays at same time`() {
        // given
        val existingSchedule =
            createScheduleWithId(
                id = 100L,
                round = 1,
                matchNumber = 1,
                homeTeam = teamA,
                awayTeam = teamB,
                scheduledDate = LocalDate.of(2024, 5, 1),
                scheduledTime = LocalTime.of(14, 0),
                venue = "구장A",
            )

        val newSchedule =
            createScheduleWithId(
                id = 200L,
                round = 1,
                matchNumber = 2,
                homeTeam = teamC,
                awayTeam = teamB,
                scheduledDate = LocalDate.of(2024, 5, 1),
                scheduledTime = LocalTime.of(15, 0),
                venue = "구장B",
            )

        // when
        val conflicts = detector.detectTeamConflicts(newSchedule, listOf(existingSchedule))

        // then
        assertThat(conflicts).hasSize(1)
        assertThat(conflicts[0].type).isEqualTo(ConflictType.TEAM_TIME_CONFLICT)
        assertThat(conflicts[0].description).contains("팀B")
    }

    @Test
    fun `should not detect team conflict when games are 3 hours apart`() {
        // given
        val existingSchedule =
            createScheduleWithId(
                id = 100L,
                round = 1,
                matchNumber = 1,
                homeTeam = teamA,
                awayTeam = teamB,
                scheduledDate = LocalDate.of(2024, 5, 1),
                scheduledTime = LocalTime.of(10, 0),
                venue = "구장A",
            )

        val newSchedule =
            createScheduleWithId(
                id = 200L,
                round = 1,
                matchNumber = 2,
                homeTeam = teamA,
                awayTeam = teamC,
                scheduledDate = LocalDate.of(2024, 5, 1),
                scheduledTime = LocalTime.of(14, 0),
                venue = "구장B",
            )

        // when
        val conflicts = detector.detectTeamConflicts(newSchedule, listOf(existingSchedule))

        // then
        assertThat(conflicts).isEmpty()
    }

    @Test
    fun `should detect venue conflict when same venue is used at same time`() {
        // given
        val existingSchedule =
            createScheduleWithId(
                id = 100L,
                round = 1,
                matchNumber = 1,
                homeTeam = teamA,
                awayTeam = teamB,
                scheduledDate = LocalDate.of(2024, 5, 1),
                scheduledTime = LocalTime.of(14, 0),
                venue = "구장A",
            )

        val newSchedule =
            createScheduleWithId(
                id = 200L,
                round = 1,
                matchNumber = 2,
                homeTeam = teamC,
                awayTeam = teamD,
                scheduledDate = LocalDate.of(2024, 5, 1),
                scheduledTime = LocalTime.of(14, 0),
                venue = "구장A",
            )

        // when
        val conflicts = detector.detectVenueConflicts(newSchedule, listOf(existingSchedule))

        // then
        assertThat(conflicts).hasSize(1)
        assertThat(conflicts[0].type).isEqualTo(ConflictType.VENUE_TIME_CONFLICT)
        assertThat(conflicts[0].description).contains("구장A")
    }

    @Test
    fun `should detect venue conflict case insensitively`() {
        // given
        val existingSchedule =
            createScheduleWithId(
                id = 100L,
                round = 1,
                matchNumber = 1,
                homeTeam = teamA,
                awayTeam = teamB,
                scheduledDate = LocalDate.of(2024, 5, 1),
                scheduledTime = LocalTime.of(14, 0),
                venue = "구장A",
            )

        val newSchedule =
            createScheduleWithId(
                id = 200L,
                round = 1,
                matchNumber = 2,
                homeTeam = teamC,
                awayTeam = teamD,
                scheduledDate = LocalDate.of(2024, 5, 1),
                scheduledTime = LocalTime.of(15, 0),
                venue = "구장a",
            )

        // when
        val conflicts = detector.detectVenueConflicts(newSchedule, listOf(existingSchedule))

        // then
        assertThat(conflicts).hasSize(1)
    }

    @Test
    fun `should not detect venue conflict when venue is not specified`() {
        // given
        val existingSchedule =
            createScheduleWithId(
                id = 100L,
                round = 1,
                matchNumber = 1,
                homeTeam = teamA,
                awayTeam = teamB,
                scheduledDate = LocalDate.of(2024, 5, 1),
                scheduledTime = LocalTime.of(14, 0),
                venue = null,
            )

        val newSchedule =
            createScheduleWithId(
                id = 200L,
                round = 1,
                matchNumber = 2,
                homeTeam = teamC,
                awayTeam = teamD,
                scheduledDate = LocalDate.of(2024, 5, 1),
                scheduledTime = LocalTime.of(14, 0),
                venue = null,
            )

        // when
        val conflicts = detector.detectVenueConflicts(newSchedule, listOf(existingSchedule))

        // then
        assertThat(conflicts).isEmpty()
    }

    @Test
    fun `should not detect venue conflict when venues are 3 hours apart`() {
        // given
        val existingSchedule =
            createScheduleWithId(
                id = 100L,
                round = 1,
                matchNumber = 1,
                homeTeam = teamA,
                awayTeam = teamB,
                scheduledDate = LocalDate.of(2024, 5, 1),
                scheduledTime = LocalTime.of(10, 0),
                venue = "구장A",
            )

        val newSchedule =
            createScheduleWithId(
                id = 200L,
                round = 1,
                matchNumber = 2,
                homeTeam = teamC,
                awayTeam = teamD,
                scheduledDate = LocalDate.of(2024, 5, 1),
                scheduledTime = LocalTime.of(14, 0),
                venue = "구장A",
            )

        // when
        val conflicts = detector.detectVenueConflicts(newSchedule, listOf(existingSchedule))

        // then
        assertThat(conflicts).isEmpty()
    }

    @Test
    fun `should detect both team and venue conflicts`() {
        // given
        val existingSchedule =
            createScheduleWithId(
                id = 100L,
                round = 1,
                matchNumber = 1,
                homeTeam = teamA,
                awayTeam = teamB,
                scheduledDate = LocalDate.of(2024, 5, 1),
                scheduledTime = LocalTime.of(14, 0),
                venue = "구장A",
            )

        val newSchedule =
            createScheduleWithId(
                id = 200L,
                round = 1,
                matchNumber = 2,
                homeTeam = teamA,
                awayTeam = teamC,
                scheduledDate = LocalDate.of(2024, 5, 1),
                scheduledTime = LocalTime.of(14, 0),
                venue = "구장A",
            )

        // when
        val conflicts = detector.detectAllConflicts(newSchedule, listOf(existingSchedule))

        // then
        assertThat(conflicts).hasSize(2)
        assertThat(conflicts.map { it.type })
            .containsExactlyInAnyOrder(
                ConflictType.TEAM_TIME_CONFLICT,
                ConflictType.VENUE_TIME_CONFLICT,
            )
    }

    @Test
    fun `should detect multiple team conflicts`() {
        // given
        val existingSchedule1 =
            createScheduleWithId(
                id = 100L,
                round = 1,
                matchNumber = 1,
                homeTeam = teamA,
                awayTeam = teamB,
                scheduledDate = LocalDate.of(2024, 5, 1),
                scheduledTime = LocalTime.of(14, 0),
                venue = "구장A",
            )

        val existingSchedule2 =
            createScheduleWithId(
                id = 101L,
                round = 1,
                matchNumber = 2,
                homeTeam = teamC,
                awayTeam = teamB,
                scheduledDate = LocalDate.of(2024, 5, 1),
                scheduledTime = LocalTime.of(15, 0),
                venue = "구장B",
            )

        val newSchedule =
            createScheduleWithId(
                id = 200L,
                round = 1,
                matchNumber = 3,
                homeTeam = teamB,
                awayTeam = teamD,
                scheduledDate = LocalDate.of(2024, 5, 1),
                scheduledTime = LocalTime.of(14, 30),
                venue = "구장C",
            )

        // when
        val conflicts =
            detector.detectTeamConflicts(
                newSchedule,
                listOf(existingSchedule1, existingSchedule2),
            )

        // then
        assertThat(conflicts).hasSize(2)
        assertThat(conflicts.all { it.type == ConflictType.TEAM_TIME_CONFLICT }).isTrue()
    }
}
