package com.nextup.infrastructure.service

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.GamePlayerNotFoundException
import com.nextup.common.exception.InvalidGameStateException
import com.nextup.common.exception.PitchEventNotFoundException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.*
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.PitchEventRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

class PitchEventServiceImplTest {
    private lateinit var pitchEventRepository: PitchEventRepositoryPort
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var gamePlayerRepository: GamePlayerRepositoryPort
    private lateinit var pitchEventService: PitchEventServiceImpl

    private lateinit var game: Game
    private lateinit var pitcher: GamePlayer
    private lateinit var batter: GamePlayer

    @BeforeEach
    fun setUp() {
        pitchEventRepository = mockk()
        gameRepository = mockk()
        gamePlayerRepository = mockk()
        pitchEventService =
            PitchEventServiceImpl(
                pitchEventRepository,
                gameRepository,
                gamePlayerRepository,
            )

        // Test fixtures
        game = createGame()
        pitcher = createPitcher(game)
        batter = createBatter(game)
    }

    @Test
    fun `투구를 기록할 수 있다`() {
        // given
        val gameId = 1L
        val pitcherId = 2L
        val batterId = 3L

        every { gameRepository.findByIdOrNull(gameId) } returns game
        every { gamePlayerRepository.findByIdOrNull(pitcherId) } returns pitcher
        every { gamePlayerRepository.findByIdOrNull(batterId) } returns batter
        every { pitchEventRepository.getNextPitchNumber(gameId) } returns 1
        every { pitchEventRepository.save(any()) } answers { firstArg() }

        // when
        val result =
            pitchEventService.recordPitch(
                gameId = gameId,
                pitcherId = pitcherId,
                batterId = batterId,
                result = PitchResult.STRIKE,
                description = "스트라이크",
            )

        // then
        assertThat(result.result).isEqualTo(PitchResult.STRIKE)
        assertThat(result.ballCount).isEqualTo(0)
        assertThat(result.strikeCount).isEqualTo(1)
        verify { pitchEventRepository.save(any()) }
    }

    @Test
    fun `경기가 진행 중이 아니면 투구를 기록할 수 없다`() {
        // given
        val gameId = 1L
        val finishedGame = createGame().apply { status = GameStatus.FINISHED }

        every { gameRepository.findByIdOrNull(gameId) } returns finishedGame

        // when & then
        assertThrows<InvalidGameStateException> {
            pitchEventService.recordPitch(
                gameId = gameId,
                pitcherId = 2L,
                batterId = 3L,
                result = PitchResult.STRIKE,
            )
        }
    }

    @Test
    fun `존재하지 않는 경기에 투구를 기록할 수 없다`() {
        // given
        val gameId = 999L
        every { gameRepository.findByIdOrNull(gameId) } returns null

        // when & then
        assertThrows<GameNotFoundException> {
            pitchEventService.recordPitch(
                gameId = gameId,
                pitcherId = 2L,
                batterId = 3L,
                result = PitchResult.STRIKE,
            )
        }
    }

    @Test
    fun `존재하지 않는 투수로 투구를 기록할 수 없다`() {
        // given
        val gameId = 1L
        val pitcherId = 999L

        every { gameRepository.findByIdOrNull(gameId) } returns game
        every { gamePlayerRepository.findByIdOrNull(pitcherId) } returns null

        // when & then
        assertThrows<GamePlayerNotFoundException> {
            pitchEventService.recordPitch(
                gameId = gameId,
                pitcherId = pitcherId,
                batterId = 3L,
                result = PitchResult.STRIKE,
            )
        }
    }

    @Test
    fun `볼 투구 시 볼카운트가 증가한다`() {
        // given
        val gameId = 1L
        val pitcherId = 2L
        val batterId = 3L

        game.gameState.balls = 2
        game.gameState.strikes = 1

        every { gameRepository.findByIdOrNull(gameId) } returns game
        every { gamePlayerRepository.findByIdOrNull(pitcherId) } returns pitcher
        every { gamePlayerRepository.findByIdOrNull(batterId) } returns batter
        every { pitchEventRepository.getNextPitchNumber(gameId) } returns 1
        every { pitchEventRepository.save(any()) } answers { firstArg() }

        // when
        val result =
            pitchEventService.recordPitch(
                gameId = gameId,
                pitcherId = pitcherId,
                batterId = batterId,
                result = PitchResult.BALL,
            )

        // then
        assertThat(result.ballCount).isEqualTo(3)
        assertThat(result.strikeCount).isEqualTo(1)
    }

    @Test
    fun `스트라이크 투구 시 스트라이크 카운트가 증가한다`() {
        // given
        val gameId = 1L
        val pitcherId = 2L
        val batterId = 3L

        game.gameState.balls = 1
        game.gameState.strikes = 1

        every { gameRepository.findByIdOrNull(gameId) } returns game
        every { gamePlayerRepository.findByIdOrNull(pitcherId) } returns pitcher
        every { gamePlayerRepository.findByIdOrNull(batterId) } returns batter
        every { pitchEventRepository.getNextPitchNumber(gameId) } returns 1
        every { pitchEventRepository.save(any()) } answers { firstArg() }

        // when
        val result =
            pitchEventService.recordPitch(
                gameId = gameId,
                pitcherId = pitcherId,
                batterId = batterId,
                result = PitchResult.STRIKE,
            )

        // then
        assertThat(result.ballCount).isEqualTo(1)
        assertThat(result.strikeCount).isEqualTo(2)
    }

    @Test
    fun `파울은 2스트라이크 미만에서만 카운트가 증가한다`() {
        // given
        val gameId = 1L
        val pitcherId = 2L
        val batterId = 3L

        // Case 1: 1스트라이크 상태에서 파울
        game.gameState.balls = 1
        game.gameState.strikes = 1

        every { gameRepository.findByIdOrNull(gameId) } returns game
        every { gamePlayerRepository.findByIdOrNull(pitcherId) } returns pitcher
        every { gamePlayerRepository.findByIdOrNull(batterId) } returns batter
        every { pitchEventRepository.getNextPitchNumber(gameId) } returns 1
        every { pitchEventRepository.save(any()) } answers { firstArg() }

        // when
        val result1 =
            pitchEventService.recordPitch(
                gameId = gameId,
                pitcherId = pitcherId,
                batterId = batterId,
                result = PitchResult.FOUL,
            )

        // then
        assertThat(result1.strikeCount).isEqualTo(2)

        // Case 2: 2스트라이크 상태에서 파울
        game.gameState.balls = 1
        game.gameState.strikes = 2

        every { pitchEventRepository.getNextPitchNumber(gameId) } returns 2
        every { pitchEventRepository.save(any()) } answers { firstArg() }

        // when
        val result2 =
            pitchEventService.recordPitch(
                gameId = gameId,
                pitcherId = pitcherId,
                batterId = batterId,
                result = PitchResult.FOUL,
            )

        // then
        assertThat(result2.strikeCount).isEqualTo(2) // 증가하지 않음
    }

    @Test
    fun `투구 이벤트를 조회할 수 있다`() {
        // given
        val pitchEventId = 1L
        val pitchEvent = createPitchEvent()

        every { pitchEventRepository.findByIdOrNull(pitchEventId) } returns pitchEvent

        // when
        val result = pitchEventService.getPitchEvent(pitchEventId)

        // then
        assertThat(result).isEqualTo(pitchEvent)
    }

    @Test
    fun `존재하지 않는 투구 이벤트를 조회하면 예외가 발생한다`() {
        // given
        val pitchEventId = 999L
        every { pitchEventRepository.findByIdOrNull(pitchEventId) } returns null

        // when & then
        assertThrows<PitchEventNotFoundException> {
            pitchEventService.getPitchEvent(pitchEventId)
        }
    }

    @Test
    fun `경기의 투구 이벤트 목록을 조회할 수 있다`() {
        // given
        val gameId = 1L
        val pitchEvents = listOf(createPitchEvent(), createPitchEvent())

        every { gameRepository.findByIdOrNull(gameId) } returns game
        every { pitchEventRepository.findByGameId(gameId) } returns pitchEvents

        // when
        val result = pitchEventService.getGamePitchEvents(gameId)

        // then
        assertThat(result).hasSize(2)
    }

    @Test
    fun `현재 볼카운트를 조회할 수 있다`() {
        // given
        val gameId = 1L
        game.gameState.balls = 2
        game.gameState.strikes = 1

        every { gameRepository.findByIdOrNull(gameId) } returns game

        // when
        val (balls, strikes) = pitchEventService.getCurrentBallCount(gameId)

        // then
        assertThat(balls).isEqualTo(2)
        assertThat(strikes).isEqualTo(1)
    }

    @Test
    fun `투수의 투구 통계를 계산할 수 있다`() {
        // given
        val pitcherId = 2L
        val pitchEvents =
            listOf(
                createPitchEvent(result = PitchResult.STRIKE),
                createPitchEvent(result = PitchResult.BALL),
                createPitchEvent(result = PitchResult.FOUL),
                createPitchEvent(result = PitchResult.SWING_MISS),
                createPitchEvent(result = PitchResult.IN_PLAY),
            )

        every { gamePlayerRepository.findByIdOrNull(pitcherId) } returns pitcher
        every { pitchEventRepository.findByPitcherId(pitcherId) } returns pitchEvents

        // when
        val stats = pitchEventService.calculatePitcherStats(pitcherId)

        // then
        assertThat(stats.totalPitches).isEqualTo(5)
        assertThat(stats.strikes).isEqualTo(2) // STRIKE + SWING_MISS
        assertThat(stats.balls).isEqualTo(1)
        assertThat(stats.fouls).isEqualTo(1)
        assertThat(stats.inPlayPitches).isEqualTo(1)
        assertThat(stats.strikePercentage).isEqualTo(60.0) // (2 + 1) / 5 * 100
    }

    // Helper methods
    private fun createGame(): Game {
        val association = Association(name = "테스트 협회", id = 1L)
        val league = League(association = association, name = "테스트 리그", foundedYear = 2020, id = 1L)
        val competition =
            Competition(
                league = league,
                name = "테스트 대회",
                year = 2024,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2024, 3, 1),
                id = 1L,
            )
        val homeTeam = Team(league = league, name = "홈팀", city = "서울", foundedYear = 2020, id = 1L)
        val awayTeam = Team(league = league, name = "원정팀", city = "부산", foundedYear = 2020, id = 2L)

        return Game.createForTest(
            competition = competition,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            scheduledAt = LocalDateTime.of(2024, 6, 1, 14, 0),
            status = GameStatus.IN_PROGRESS,
            currentInning = 1,
            id = 1L,
        )
    }

    private fun createPitcher(game: Game): GamePlayer {
        val awayTeam = game.gameTeams.first { it.homeAway == HomeAway.AWAY }
        val player =
            Player(
                name = "투수",
                birthDate = LocalDate.of(1995, 1, 1),
                primaryPosition = Position.STARTING_PITCHER,
            )
        return GamePlayer(
            gameTeam = awayTeam,
            player = player,
            position = Position.STARTING_PITCHER,
            battingOrder = null,
        )
    }

    private fun createBatter(game: Game): GamePlayer {
        val homeTeam = game.gameTeams.first { it.homeAway == HomeAway.HOME }
        val player =
            Player(
                name = "타자",
                birthDate = LocalDate.of(1995, 1, 1),
                primaryPosition = Position.FIRST_BASE,
            )
        return GamePlayer(
            gameTeam = homeTeam,
            player = player,
            position = Position.FIRST_BASE,
            battingOrder = 1,
        )
    }

    private fun createPitchEvent(result: PitchResult = PitchResult.STRIKE): PitchEvent =
        PitchEvent.create(
            game = game,
            pitcher = pitcher,
            batter = batter,
            pitchNumber = 1,
            result = result,
            ballCount = 0,
            strikeCount = 1,
        )
}
