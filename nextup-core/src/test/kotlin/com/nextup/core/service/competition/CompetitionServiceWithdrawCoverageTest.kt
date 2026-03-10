package com.nextup.core.service.competition

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.BracketEntry
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionPlayer
import com.nextup.core.domain.competition.CompetitionPlayerStatus
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.BracketEntryRepositoryPort
import com.nextup.core.port.repository.CompetitionPlayerRepositoryPort
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.LeagueRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("CompetitionService.withdrawTeam 커버리지 보완")
class CompetitionServiceWithdrawCoverageTest {
    private lateinit var competitionRepository: CompetitionRepositoryPort
    private lateinit var leagueRepository: LeagueRepositoryPort
    private lateinit var competitionPlayerRepository: CompetitionPlayerRepositoryPort
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var teamRepository: TeamRepositoryPort
    private lateinit var bracketEntryRepository: BracketEntryRepositoryPort
    private lateinit var competitionService: CompetitionService

    private lateinit var association: Association
    private lateinit var league: League
    private lateinit var competition: Competition
    private lateinit var team: Team
    private lateinit var opponentTeam: Team

    @BeforeEach
    fun setUp() {
        competitionRepository = mockk()
        leagueRepository = mockk()
        competitionPlayerRepository = mockk()
        gameRepository = mockk()
        teamRepository = mockk()
        bracketEntryRepository = mockk()
        competitionService =
            CompetitionService(
                competitionRepository,
                leagueRepository,
                competitionPlayerRepository,
                gameRepository,
                teamRepository,
                bracketEntryRepository,
            )

        association = Association(name = "서울시야구협회", region = "서울")
        league = League(association = association, name = "1부 리그", foundedYear = 2020)
        competition =
            Competition(
                league = league,
                name = "2025 춨계대회",
                year = 2025,
                season = 1,
                type = CompetitionType.TOURNAMENT,
                startDate = LocalDate.of(2025, 3, 1),
                status = CompetitionStatus.IN_PROGRESS,
            ).apply {
                val idField = Competition::class.java.getDeclaredField("id")
                idField.isAccessible = true
                idField.set(this, 1L)
            }
        team = createTeam(10L, "한강 타이거즈")
        opponentTeam = createTeam(20L, "잠실 이글스")
    }

    @Test
    fun `이미 WITHDRAWN 상태인 선수는 카운트에서 제외된다`() {
        // given
        val player1 = createPlayer(100L, "홍길동")
        val player2 = createPlayer(101L, "김철수")
        val cp1 = CompetitionPlayer.register(competition, team, player1)
        val cp2 = CompetitionPlayer.register(competition, team, player2)
        cp2.withdraw() // 이미 WITHDRAWN

        every { competitionRepository.findByIdOrNull(1L) } returns competition
        every { teamRepository.findByIdOrNull(10L) } returns team
        every { competitionPlayerRepository.findByCompetitionIdAndTeamId(1L, 10L) } returns listOf(cp1, cp2)
        every { competitionPlayerRepository.saveAll(any()) } answers { firstArg() }
        every { gameRepository.findByCompetitionId(1L) } returns emptyList()
        every { bracketEntryRepository.findByCompetitionId(1L) } returns emptyList()

        // when
        val result = competitionService.withdrawTeam(1L, 10L, "탈퇴 사유")

        // then
        assertThat(result.withdrawnPlayerCount).isEqualTo(1) // cp2는 이미 WITHDRAWN이므로 제외
    }

    @Test
    fun `탈퇴 팀이 참여하지 않는 경기는 건너뛴다`() {
        // given
        val player = createPlayer(100L, "홍길동")
        val cp = CompetitionPlayer.register(competition, team, player)
        val otherTeam = createTeam(30L, "다른팀")

        // 탈퇴 팀(10L)이 참여하지 않는 경기
        val game =
            Game.createForTest(
                competition = competition,
                homeTeam = opponentTeam,
                awayTeam = otherTeam,
                id = 1L,
            )

        every { competitionRepository.findByIdOrNull(1L) } returns competition
        every { teamRepository.findByIdOrNull(10L) } returns team
        every { competitionPlayerRepository.findByCompetitionIdAndTeamId(1L, 10L) } returns listOf(cp)
        every { competitionPlayerRepository.saveAll(any()) } answers { firstArg() }
        every { gameRepository.findByCompetitionId(1L) } returns listOf(game)
        every { bracketEntryRepository.findByCompetitionId(1L) } returns emptyList()

        // when
        val result = competitionService.withdrawTeam(1L, 10L, "탈퇴")

        // then
        assertThat(result.forfeitedGameCount).isEqualTo(0)
        verify(exactly = 0) { gameRepository.save(any()) }
    }

    @Test
    fun `POSTPONED 상태 경기도 몰수 처리된다`() {
        // given
        val player = createPlayer(100L, "홍길동")
        val cp = CompetitionPlayer.register(competition, team, player)

        val game =
            Game.createForTest(
                competition = competition,
                homeTeam = team,
                awayTeam = opponentTeam,
                status = GameStatus.POSTPONED,
                id = 1L,
            )

        every { competitionRepository.findByIdOrNull(1L) } returns competition
        every { teamRepository.findByIdOrNull(10L) } returns team
        every { competitionPlayerRepository.findByCompetitionIdAndTeamId(1L, 10L) } returns listOf(cp)
        every { competitionPlayerRepository.saveAll(any()) } answers { firstArg() }
        every { gameRepository.findByCompetitionId(1L) } returns listOf(game)
        every { gameRepository.save(any()) } answers { firstArg() }
        every { bracketEntryRepository.findByCompetitionId(1L) } returns emptyList()

        // when
        val result = competitionService.withdrawTeam(1L, 10L, "탈퇴")

        // then
        assertThat(result.forfeitedGameCount).isEqualTo(1)
        assertThat(game.status).isEqualTo(GameStatus.FORFEITED)
    }

    @Test
    fun `대진표에서 team1이 탈퇴하면 team2가 승자가 된다`() {
        // given
        val player = createPlayer(100L, "홍길동")
        val cp = CompetitionPlayer.register(competition, team, player)

        val entry =
            BracketEntry(
                competition = competition,
                roundNumber = 1,
                matchNumber = 1,
                team1 = team,
                team2 = opponentTeam,
            )

        every { competitionRepository.findByIdOrNull(1L) } returns competition
        every { teamRepository.findByIdOrNull(10L) } returns team
        every { competitionPlayerRepository.findByCompetitionIdAndTeamId(1L, 10L) } returns listOf(cp)
        every { competitionPlayerRepository.saveAll(any()) } answers { firstArg() }
        every { gameRepository.findByCompetitionId(1L) } returns emptyList()
        every { bracketEntryRepository.findByCompetitionId(1L) } returns listOf(entry)
        every { bracketEntryRepository.save(any()) } answers { firstArg() }

        // when
        val result = competitionService.withdrawTeam(1L, 10L, "탈퇴")

        // then
        assertThat(result.updatedBracketEntryCount).isEqualTo(1)
        assertThat(entry.winner).isEqualTo(opponentTeam)
    }

    @Test
    fun `대진표에서 team2가 탈퇴하면 team1이 승자가 된다`() {
        // given
        val player = createPlayer(100L, "홍길동")
        val cp = CompetitionPlayer.register(competition, team, player)

        val entry =
            BracketEntry(
                competition = competition,
                roundNumber = 1,
                matchNumber = 1,
                team1 = opponentTeam,
                team2 = team,
            )

        every { competitionRepository.findByIdOrNull(1L) } returns competition
        every { teamRepository.findByIdOrNull(10L) } returns team
        every { competitionPlayerRepository.findByCompetitionIdAndTeamId(1L, 10L) } returns listOf(cp)
        every { competitionPlayerRepository.saveAll(any()) } answers { firstArg() }
        every { gameRepository.findByCompetitionId(1L) } returns emptyList()
        every { bracketEntryRepository.findByCompetitionId(1L) } returns listOf(entry)
        every { bracketEntryRepository.save(any()) } answers { firstArg() }

        // when
        val result = competitionService.withdrawTeam(1L, 10L, "탈퇴")

        // then
        assertThat(result.updatedBracketEntryCount).isEqualTo(1)
        assertThat(entry.winner).isEqualTo(opponentTeam)
    }

    @Test
    fun `이미 완료된 대진표 엔트리는 건너뛴다`() {
        // given
        val player = createPlayer(100L, "홍길동")
        val cp = CompetitionPlayer.register(competition, team, player)

        val entry =
            BracketEntry(
                competition = competition,
                roundNumber = 1,
                matchNumber = 1,
                team1 = team,
                team2 = opponentTeam,
                winner = opponentTeam, // 이미 승자 결정
            )

        every { competitionRepository.findByIdOrNull(1L) } returns competition
        every { teamRepository.findByIdOrNull(10L) } returns team
        every { competitionPlayerRepository.findByCompetitionIdAndTeamId(1L, 10L) } returns listOf(cp)
        every { competitionPlayerRepository.saveAll(any()) } answers { firstArg() }
        every { gameRepository.findByCompetitionId(1L) } returns emptyList()
        every { bracketEntryRepository.findByCompetitionId(1L) } returns listOf(entry)

        // when
        val result = competitionService.withdrawTeam(1L, 10L, "탈퇴")

        // then
        assertThat(result.updatedBracketEntryCount).isEqualTo(0)
        verify(exactly = 0) { bracketEntryRepository.save(any()) }
    }

    private fun createTeam(
        id: Long,
        name: String,
    ): Team =
        Team(
            league = league,
            name = name,
            city = "서울",
            foundedYear = 2020,
        ).apply {
            val idField = Team::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    private fun createPlayer(
        id: Long,
        name: String,
    ): Player =
        Player(
            name = name,
            primaryPosition = Position.STARTING_PITCHER,
        ).apply {
            val idField = Player::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
}
