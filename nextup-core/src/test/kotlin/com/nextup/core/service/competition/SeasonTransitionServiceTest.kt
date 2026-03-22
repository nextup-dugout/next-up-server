package com.nextup.core.service.competition

import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.common.exception.InvalidCompetitionStateException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionPlayer
import com.nextup.core.domain.competition.CompetitionPlayerStatus
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import com.nextup.core.domain.stats.SeasonBattingStats
import com.nextup.core.domain.stats.SeasonFieldingStats
import com.nextup.core.domain.stats.SeasonPitchingStats
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.CompetitionPlayerRepositoryPort
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.SeasonBattingStatsRepositoryPort
import com.nextup.core.port.repository.SeasonFieldingStatsRepositoryPort
import com.nextup.core.port.repository.SeasonPitchingStatsRepositoryPort
import com.nextup.core.service.standings.StandingsService
import com.nextup.core.service.standings.dto.StandingsDto
import com.nextup.core.service.standings.dto.TeamStandingDto
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("SeasonTransitionService")
class SeasonTransitionServiceTest {
    private lateinit var competitionRepository: CompetitionRepositoryPort
    private lateinit var competitionPlayerRepository: CompetitionPlayerRepositoryPort
    private lateinit var standingsService: StandingsService
    private lateinit var competitionService: CompetitionService
    private lateinit var seasonBattingStatsRepository: SeasonBattingStatsRepositoryPort
    private lateinit var seasonPitchingStatsRepository: SeasonPitchingStatsRepositoryPort
    private lateinit var seasonFieldingStatsRepository: SeasonFieldingStatsRepositoryPort
    private lateinit var seasonTransitionService: SeasonTransitionService

    @BeforeEach
    fun setUp() {
        competitionRepository = mockk()
        competitionPlayerRepository = mockk()
        standingsService = mockk()
        competitionService = mockk()
        seasonBattingStatsRepository = mockk()
        seasonPitchingStatsRepository = mockk()
        seasonFieldingStatsRepository = mockk()
        seasonTransitionService =
            SeasonTransitionService(
                competitionRepository,
                competitionPlayerRepository,
                standingsService,
                competitionService,
                seasonBattingStatsRepository,
                seasonPitchingStatsRepository,
                seasonFieldingStatsRepository,
            )
    }

    @Nested
    @DisplayName("getSeasonSummary")
    inner class GetSeasonSummary {
        @Test
        fun `should return season summary for completed competition`() {
            // given
            val competition = createCompletedCompetition(1L)
            val standings = createStandings(1L)
            val players = createCompetitionPlayers(competition, 3)

            every { competitionRepository.findByIdWithLeague(1L) } returns competition
            every { standingsService.getStandings(1L) } returns standings
            every { competitionPlayerRepository.findByCompetitionId(1L) } returns players

            // when
            val result = seasonTransitionService.getSeasonSummary(1L)

            // then
            assertThat(result.competitionId).isEqualTo(1L)
            assertThat(result.competitionName).isEqualTo("2025 춘계대회")
            assertThat(result.year).isEqualTo(2025)
            assertThat(result.season).isEqualTo(1)
            assertThat(result.totalPlayers).isEqualTo(3)
            assertThat(result.finalStandings).hasSize(2)
        }

        @Test
        fun `should throw exception when competition not found`() {
            // given
            every { competitionRepository.findByIdWithLeague(999L) } returns null

            // when & then
            assertThatThrownBy { seasonTransitionService.getSeasonSummary(999L) }
                .isInstanceOf(CompetitionNotFoundException::class.java)
        }

        @Test
        fun `should throw exception when competition is not completed`() {
            // given
            val competition = createInProgressCompetition(1L)
            every { competitionRepository.findByIdWithLeague(1L) } returns competition

            // when & then
            assertThatThrownBy { seasonTransitionService.getSeasonSummary(1L) }
                .isInstanceOf(InvalidCompetitionStateException::class.java)
        }
    }

    @Nested
    @DisplayName("prepareNextSeason")
    inner class PrepareNextSeason {
        @Test
        fun `should prepare next season with active players from previous season`() {
            // given
            val previousCompetition = createCompletedCompetition(1L)
            val activePlayers = createCompetitionPlayers(previousCompetition, 5)
            val withdrawnPlayer =
                createCompetitionPlayer(previousCompetition, 6L, "탈퇴선수").apply {
                    withdraw()
                }
            val allPlayers = activePlayers + withdrawnPlayer

            val newCompetition = createScheduledCompetition(2L, "2025 추계대회", 2025, 2)

            every { competitionRepository.findByIdWithLeague(1L) } returns previousCompetition
            every {
                competitionService.create(
                    leagueId = any(),
                    name = "2025 추계대회",
                    year = 2025,
                    season = 2,
                    type = CompetitionType.LEAGUE,
                    startDate = LocalDate.of(2025, 9, 1),
                    endDate = null,
                    description = null,
                    maxTeams = any(),
                )
            } returns newCompetition
            every {
                competitionPlayerRepository.findByCompetitionIdAndStatus(
                    1L,
                    CompetitionPlayerStatus.ACTIVE,
                )
            } returns activePlayers
            every { competitionPlayerRepository.findByCompetitionId(1L) } returns allPlayers
            every { competitionPlayerRepository.saveAll(any()) } answers { firstArg() }

            // when
            val result =
                seasonTransitionService.prepareNextSeason(
                    previousCompetitionId = 1L,
                    name = "2025 추계대회",
                    startDate = LocalDate.of(2025, 9, 1),
                )

            // then
            assertThat(result.newCompetitionId).isEqualTo(2L)
            assertThat(result.newCompetitionName).isEqualTo("2025 추계대회")
            assertThat(result.year).isEqualTo(2025)
            assertThat(result.season).isEqualTo(2)
            assertThat(result.registeredPlayerCount).isEqualTo(5)
            assertThat(result.skippedPlayerCount).isEqualTo(1)

            verify(exactly = 1) { competitionPlayerRepository.saveAll(any()) }
        }

        @Test
        fun `should prepare next year season when start date is next year`() {
            // given
            val previousCompetition = createCompletedCompetition(1L)
            val newCompetition = createScheduledCompetition(2L, "2026 춘계대회", 2026, 1)

            every { competitionRepository.findByIdWithLeague(1L) } returns previousCompetition
            every {
                competitionService.create(
                    leagueId = any(),
                    name = "2026 춘계대회",
                    year = 2026,
                    season = 1,
                    type = CompetitionType.LEAGUE,
                    startDate = LocalDate.of(2026, 3, 1),
                    endDate = null,
                    description = null,
                    maxTeams = any(),
                )
            } returns newCompetition
            every {
                competitionPlayerRepository.findByCompetitionIdAndStatus(1L, CompetitionPlayerStatus.ACTIVE)
            } returns emptyList()
            every { competitionPlayerRepository.findByCompetitionId(1L) } returns emptyList()
            every { competitionPlayerRepository.saveAll(any()) } answers { firstArg() }

            // when
            val result =
                seasonTransitionService.prepareNextSeason(
                    previousCompetitionId = 1L,
                    name = "2026 춘계대회",
                    startDate = LocalDate.of(2026, 3, 1),
                )

            // then
            assertThat(result.year).isEqualTo(2026)
            assertThat(result.season).isEqualTo(1)
            assertThat(result.registeredPlayerCount).isEqualTo(0)
            assertThat(result.skippedPlayerCount).isEqualTo(0)
        }

        @Test
        fun `should throw exception when previous competition is not completed`() {
            // given
            val competition = createInProgressCompetition(1L)
            every { competitionRepository.findByIdWithLeague(1L) } returns competition

            // when & then
            assertThatThrownBy {
                seasonTransitionService.prepareNextSeason(
                    previousCompetitionId = 1L,
                    name = "다음 시즌",
                    startDate = LocalDate.of(2025, 9, 1),
                )
            }.isInstanceOf(InvalidCompetitionStateException::class.java)
        }

        @Test
        fun `should throw exception when previous competition not found`() {
            // given
            every { competitionRepository.findByIdWithLeague(999L) } returns null

            // when & then
            assertThatThrownBy {
                seasonTransitionService.prepareNextSeason(
                    previousCompetitionId = 999L,
                    name = "다음 시즌",
                    startDate = LocalDate.of(2025, 9, 1),
                )
            }.isInstanceOf(CompetitionNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("archiveSeason")
    inner class ArchiveSeason {
        @Test
        fun `should finalize all season stats for the competition year`() {
            // given
            val competition = createCompletedCompetition(1L)
            val player1 = createPlayerWithHands(1L, "선수1")
            val player2 = createPlayerWithHands(2L, "선수2")

            val battingStats =
                listOf(
                    SeasonBattingStats.create(player1, 2025),
                    SeasonBattingStats.create(player2, 2025),
                )
            val pitchingStats = listOf(SeasonPitchingStats.create(player1, 2025))
            val fieldingStats = listOf(SeasonFieldingStats.create(player1, 2025))

            every { competitionRepository.findByIdWithLeague(1L) } returns competition
            every { seasonBattingStatsRepository.findAllByYear(2025) } returns battingStats
            every { seasonPitchingStatsRepository.findAllByYear(2025) } returns pitchingStats
            every { seasonFieldingStatsRepository.findAllByYear(2025) } returns fieldingStats

            // when
            val result = seasonTransitionService.archiveSeason(1L)

            // then
            assertThat(result.competitionId).isEqualTo(1L)
            assertThat(result.year).isEqualTo(2025)
            assertThat(result.battingStatsFinalized).isEqualTo(2)
            assertThat(result.pitchingStatsFinalized).isEqualTo(1)
            assertThat(result.fieldingStatsFinalized).isEqualTo(1)
            assertThat(result.totalStatsFinalized).isEqualTo(4)

            battingStats.forEach { assertThat(it.isFinalized).isTrue() }
            pitchingStats.forEach { assertThat(it.isFinalized).isTrue() }
            fieldingStats.forEach { assertThat(it.isFinalized).isTrue() }
        }

        @Test
        fun `should skip already finalized stats`() {
            // given
            val competition = createCompletedCompetition(1L)
            val player = createPlayerWithHands(1L, "선수1")

            val alreadyFinalized = SeasonBattingStats.create(player, 2025).apply { finalize() }
            val notFinalized = SeasonBattingStats.create(player, 2025, teamId = 2L)

            every { competitionRepository.findByIdWithLeague(1L) } returns competition
            every { seasonBattingStatsRepository.findAllByYear(2025) } returns listOf(alreadyFinalized, notFinalized)
            every { seasonPitchingStatsRepository.findAllByYear(2025) } returns emptyList()
            every { seasonFieldingStatsRepository.findAllByYear(2025) } returns emptyList()

            // when
            val result = seasonTransitionService.archiveSeason(1L)

            // then
            assertThat(result.battingStatsFinalized).isEqualTo(1) // 1개만 새로 확정
        }

        @Test
        fun `should throw exception when competition is not completed`() {
            // given
            val competition = createInProgressCompetition(1L)
            every { competitionRepository.findByIdWithLeague(1L) } returns competition

            // when & then
            assertThatThrownBy { seasonTransitionService.archiveSeason(1L) }
                .isInstanceOf(InvalidCompetitionStateException::class.java)
        }

        @Test
        fun `should throw exception when competition not found`() {
            // given
            every { competitionRepository.findByIdWithLeague(999L) } returns null

            // when & then
            assertThatThrownBy { seasonTransitionService.archiveSeason(999L) }
                .isInstanceOf(CompetitionNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("unarchiveSeason")
    inner class UnarchiveSeason {
        @Test
        fun `should unfinalize all finalized season stats for the competition year`() {
            // given
            val competition = createCompletedCompetition(1L)
            val player = createPlayerWithHands(1L, "선수1")

            val battingStats =
                listOf(SeasonBattingStats.create(player, 2025).apply { finalize() })
            val pitchingStats =
                listOf(SeasonPitchingStats.create(player, 2025).apply { finalize() })
            val fieldingStats =
                listOf(SeasonFieldingStats.create(player, 2025).apply { finalize() })

            every { competitionRepository.findByIdWithLeague(1L) } returns competition
            every { seasonBattingStatsRepository.findAllByYear(2025) } returns battingStats
            every { seasonPitchingStatsRepository.findAllByYear(2025) } returns pitchingStats
            every { seasonFieldingStatsRepository.findAllByYear(2025) } returns fieldingStats

            // when
            val result = seasonTransitionService.unarchiveSeason(1L)

            // then
            assertThat(result.competitionId).isEqualTo(1L)
            assertThat(result.battingStatsFinalized).isEqualTo(1)
            assertThat(result.pitchingStatsFinalized).isEqualTo(1)
            assertThat(result.fieldingStatsFinalized).isEqualTo(1)

            battingStats.forEach { assertThat(it.isFinalized).isFalse() }
            pitchingStats.forEach { assertThat(it.isFinalized).isFalse() }
            fieldingStats.forEach { assertThat(it.isFinalized).isFalse() }
        }

        @Test
        fun `should skip stats that are not finalized`() {
            // given
            val competition = createCompletedCompetition(1L)
            val player = createPlayerWithHands(1L, "선수1")

            val notFinalized = SeasonBattingStats.create(player, 2025)

            every { competitionRepository.findByIdWithLeague(1L) } returns competition
            every { seasonBattingStatsRepository.findAllByYear(2025) } returns listOf(notFinalized)
            every { seasonPitchingStatsRepository.findAllByYear(2025) } returns emptyList()
            every { seasonFieldingStatsRepository.findAllByYear(2025) } returns emptyList()

            // when
            val result = seasonTransitionService.unarchiveSeason(1L)

            // then
            assertThat(result.battingStatsFinalized).isEqualTo(0)
        }

        @Test
        fun `should throw exception when competition not found`() {
            // given
            every { competitionRepository.findByIdWithLeague(999L) } returns null

            // when & then
            assertThatThrownBy { seasonTransitionService.unarchiveSeason(999L) }
                .isInstanceOf(CompetitionNotFoundException::class.java)
        }
    }

    // --- Helper methods ---

    private fun createPlayerWithHands(
        id: Long,
        name: String,
    ): Player =
        Player(
            name = name,
            primaryPosition = Position.STARTING_PITCHER,
            throwingHand = ThrowingHand.RIGHT,
            battingHand = BattingHand.RIGHT,
        ).apply { setId(this, id) }

    private fun createCompletedCompetition(id: Long): Competition {
        val league = createLeague()
        return Competition(
            league = league,
            name = "2025 춘계대회",
            year = 2025,
            season = 1,
            type = CompetitionType.LEAGUE,
            startDate = LocalDate.of(2025, 3, 1),
            endDate = LocalDate.of(2025, 6, 30),
            status = CompetitionStatus.COMPLETED,
        ).apply {
            setId(this, id)
        }
    }

    private fun createInProgressCompetition(id: Long): Competition =
        Competition(
            league = createLeague(),
            name = "2025 춘계대회",
            year = 2025,
            season = 1,
            type = CompetitionType.LEAGUE,
            startDate = LocalDate.of(2025, 3, 1),
            status = CompetitionStatus.IN_PROGRESS,
        ).apply {
            setId(this, id)
        }

    private fun createScheduledCompetition(
        id: Long,
        name: String,
        year: Int,
        season: Int,
    ): Competition =
        Competition(
            league = createLeague(),
            name = name,
            year = year,
            season = season,
            type = CompetitionType.LEAGUE,
            startDate = LocalDate.of(year, 3, 1),
        ).apply {
            setId(this, id)
        }

    private fun createLeague(): League {
        val association =
            Association(
                name = "서울시야구협회",
                abbreviation = null,
                region = "서울",
                description = null,
                logoUrl = null,
                websiteUrl = null,
            ).apply { setId(this, 1L) }
        return League(
            association = association,
            name = "1부 리그",
            abbreviation = null,
            foundedYear = 2020,
            divisionLevel = 1,
            description = null,
            logoUrl = null,
        ).apply { setId(this, 1L) }
    }

    private fun createCompetitionPlayers(
        competition: Competition,
        count: Int,
    ): List<CompetitionPlayer> {
        val team = createTeam(1L, "한강 타이거즈")
        return (1..count).map { i ->
            createCompetitionPlayer(competition, i.toLong(), "선수$i", team)
        }
    }

    private fun createCompetitionPlayer(
        competition: Competition,
        playerId: Long,
        playerName: String,
        team: Team = createTeam(1L, "한강 타이거즈"),
    ): CompetitionPlayer {
        val player = createPlayer(playerId, playerName)
        return CompetitionPlayer.register(competition, team, player).apply {
            setId(this, playerId)
        }
    }

    private fun createTeam(
        id: Long,
        name: String,
    ): Team =
        Team(
            league = createLeague(),
            name = name,
            city = "서울",
            foundedYear = 2020,
        ).apply { setId(this, id) }

    private fun createPlayer(
        id: Long,
        name: String,
    ): Player =
        Player(
            name = name,
            primaryPosition = Position.STARTING_PITCHER,
        ).apply { setId(this, id) }

    private fun createStandings(competitionId: Long): StandingsDto =
        StandingsDto(
            competitionId = competitionId,
            competitionName = "2025 춘계대회",
            totalGamesPerTeam = 10,
            standings =
                listOf(
                    TeamStandingDto(
                        rank = 1,
                        teamId = 1L,
                        teamName = "한강 타이거즈",
                        gamesPlayed = 10,
                        remainingGames = 0,
                        wins = 8,
                        losses = 2,
                        draws = 0,
                        winningPercentage = BigDecimal("0.800"),
                        gamesBehind = BigDecimal.ZERO,
                        runsScored = 50,
                        runsAllowed = 30,
                        runDifferential = 20,
                    ),
                    TeamStandingDto(
                        rank = 2,
                        teamId = 2L,
                        teamName = "잠실 베어스",
                        gamesPlayed = 10,
                        remainingGames = 0,
                        wins = 5,
                        losses = 5,
                        draws = 0,
                        winningPercentage = BigDecimal("0.500"),
                        gamesBehind = BigDecimal("3.0"),
                        runsScored = 40,
                        runsAllowed = 40,
                        runDifferential = 0,
                    ),
                ),
            lastUpdated = LocalDateTime.now(),
        )

    private fun setId(
        entity: Any,
        id: Long,
    ) {
        val idField = entity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }
}
