package com.nextup.infrastructure.service.game

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.GamePlayerNotFoundException
import com.nextup.common.exception.InvalidGameStateException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameEvent
import com.nextup.core.domain.game.GameEventType
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameState
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.ScorerLock
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.service.game.dto.PositionChangeRequest
import com.nextup.core.service.game.dto.PositionSwapRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("GamePositionChangeServiceImpl")
class GamePositionChangeServiceImplTest {
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var gamePlayerRepository: GamePlayerRepositoryPort
    private lateinit var gameEventRepository: GameEventRepositoryPort
    private val eventPublisher: ApplicationEventPublisher = mockk(relaxed = true)
    private lateinit var service: GamePositionChangeServiceImpl

    @BeforeEach
    fun setUp() {
        gameRepository = mockk()
        gamePlayerRepository = mockk()
        gameEventRepository = mockk()

        service =
            GamePositionChangeServiceImpl(
                gameRepository,
                gamePlayerRepository,
                gameEventRepository,
                eventPublisher,
            )
    }

    @Nested
    @DisplayName("changePosition - 정상 시나리오")
    inner class ChangePositionSuccess {
        @Test
        fun `출전 중인 선수의 포지션을 변경할 수 있다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val player = createStartingPlayer(10L, Position.LEFT_FIELD, 5)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns player
            every { gamePlayerRepository.save(any()) } answers { firstArg() }

            val savedEvent = mockk<GameEvent>(relaxed = true)
            every { savedEvent.id } returns 100L
            every { savedEvent.eventType } returns GameEventType.POSITION_CHANGE
            every { gameEventRepository.save(any()) } returns savedEvent

            val request = PositionChangeRequest(playerId = 10L, newPosition = Position.CENTER_FIELD)

            // when
            val result = service.changePosition(1L, request, 999L)

            // then
            assertThat(result).isNotNull()
            assertThat(player.position).isEqualTo(Position.CENTER_FIELD)
            verify { gamePlayerRepository.save(player) }
            verify { gameEventRepository.save(any()) }
        }

        @Test
        fun `포지션 변경 이벤트가 POSITION_CHANGE 타입으로 저장된다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val player = createStartingPlayer(10L, Position.LEFT_FIELD, 3)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns player
            every { gamePlayerRepository.save(any()) } answers { firstArg() }

            var capturedEvent: GameEvent? = null
            every { gameEventRepository.save(any()) } answers {
                capturedEvent = firstArg()
                firstArg<GameEvent>().also {
                    val f = GameEvent::class.java.getDeclaredField("id")
                    f.isAccessible = true
                    f.set(it, 1L)
                }
            }

            val request = PositionChangeRequest(playerId = 10L, newPosition = Position.RIGHT_FIELD)

            // when
            service.changePosition(1L, request, 999L)

            // then
            assertThat(capturedEvent).isNotNull()
            assertThat(capturedEvent!!.eventType).isEqualTo(GameEventType.POSITION_CHANGE)
        }

        @Test
        fun `투수 포지션으로 변경 시 currentPitcherId가 갱신된다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val player = createStartingPlayer(10L, Position.LEFT_FIELD, 5)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns player
            every { gamePlayerRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            val savedEvent = mockk<GameEvent>(relaxed = true)
            every { savedEvent.id } returns 100L
            every { gameEventRepository.save(any()) } returns savedEvent

            val request = PositionChangeRequest(playerId = 10L, newPosition = Position.RELIEF_PITCHER)

            // when
            service.changePosition(1L, request, 999L)

            // then
            assertThat(game.gameState.currentPitcherId).isEqualTo(10L)
            verify { gameRepository.save(game) }
        }

        @Test
        fun `하위 이닝에서 포지션 변경 시 설명에 말이 포함된다`() {
            // given
            val game =
                createGame(1L, GameStatus.IN_PROGRESS).apply {
                    isTopInning = false
                }
            val player = createStartingPlayer(10L, Position.LEFT_FIELD, 5)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns player
            every { gamePlayerRepository.save(any()) } answers { firstArg() }

            var capturedEvent: GameEvent? = null
            every { gameEventRepository.save(any()) } answers {
                capturedEvent = firstArg()
                firstArg<GameEvent>().also {
                    val f = GameEvent::class.java.getDeclaredField("id")
                    f.isAccessible = true
                    f.set(it, 1L)
                }
            }

            val request = PositionChangeRequest(playerId = 10L, newPosition = Position.CENTER_FIELD)

            // when
            service.changePosition(1L, request, 999L)

            // then
            assertThat(capturedEvent).isNotNull()
            assertThat(capturedEvent!!.description).contains("말")
        }
    }

    @Nested
    @DisplayName("changePosition - 예외 시나리오")
    inner class ChangePositionFailure {
        @Test
        fun `경기를 찾을 수 없으면 GameNotFoundException이 발생한다`() {
            // given
            every { gameRepository.findByIdOrNull(999L) } returns null
            val request = PositionChangeRequest(playerId = 10L, newPosition = Position.CENTER_FIELD)

            // when & then
            assertThatThrownBy {
                service.changePosition(999L, request, 1L)
            }.isInstanceOf(GameNotFoundException::class.java)
        }

        @Test
        fun `진행 중이 아닌 경기에서 포지션 변경 시 InvalidGameStateException이 발생한다`() {
            // given
            val game = createGame(1L, GameStatus.SCHEDULED)
            every { gameRepository.findByIdOrNull(1L) } returns game
            val request = PositionChangeRequest(playerId = 10L, newPosition = Position.CENTER_FIELD)

            // when & then
            assertThatThrownBy {
                service.changePosition(1L, request, 999L)
            }.isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("진행 중인 경기")
        }

        @Test
        fun `선수를 찾을 수 없으면 GamePlayerNotFoundException이 발생한다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns null
            val request = PositionChangeRequest(playerId = 10L, newPosition = Position.CENTER_FIELD)

            // when & then
            assertThatThrownBy {
                service.changePosition(1L, request, 999L)
            }.isInstanceOf(GamePlayerNotFoundException::class.java)
        }

        @Test
        fun `현재 출전 중이 아닌 선수의 포지션 변경 시 InvalidGameStateException이 발생한다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val benchPlayer = createBenchPlayer(10L, Position.LEFT_FIELD)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns benchPlayer
            val request = PositionChangeRequest(playerId = 10L, newPosition = Position.CENTER_FIELD)

            // when & then
            assertThatThrownBy {
                service.changePosition(1L, request, 999L)
            }.isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("현재 출전 중인 선수만")
        }

        @Test
        fun `동일한 포지션으로 변경 시 InvalidGameStateException이 발생한다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val player = createStartingPlayer(10L, Position.LEFT_FIELD, 5)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns player
            val request = PositionChangeRequest(playerId = 10L, newPosition = Position.LEFT_FIELD)

            // when & then
            assertThatThrownBy {
                service.changePosition(1L, request, 999L)
            }.isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("동일")
        }

        @Test
        fun `DH 해제 후 DH 포지션으로 변경 시 InvalidGameStateException이 발생한다`() {
            // given
            val gameState = GameState().apply { wasDhReleased = true }
            val game = createGameWithState(1L, GameStatus.IN_PROGRESS, gameState)
            val player = createStartingPlayer(10L, Position.LEFT_FIELD, 5)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns player
            val request = PositionChangeRequest(playerId = 10L, newPosition = Position.DESIGNATED_HITTER)

            // when & then
            assertThatThrownBy {
                service.changePosition(1L, request, 999L)
            }.isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("DH")
        }
    }

    @Nested
    @DisplayName("swapPositions - 정상 시나리오")
    inner class SwapPositionsSuccess {
        @Test
        fun `두 선수의 포지션을 교환할 수 있다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val player1 = createStartingPlayer(10L, Position.LEFT_FIELD, 5)
            val player2 = createStartingPlayer(20L, Position.CENTER_FIELD, 7)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns player1
            every { gamePlayerRepository.findByIdOrNull(20L) } returns player2
            every { gamePlayerRepository.save(any()) } answers { firstArg() }

            val savedEvent1 = mockk<GameEvent>(relaxed = true)
            val savedEvent2 = mockk<GameEvent>(relaxed = true)
            every { savedEvent1.id } returns 100L
            every { savedEvent2.id } returns 101L
            every { gameEventRepository.save(any()) } returnsMany listOf(savedEvent1, savedEvent2)

            val request = PositionSwapRequest(player1Id = 10L, player2Id = 20L)

            // when
            val results = service.swapPositions(1L, request, 999L)

            // then
            assertThat(results).hasSize(2)
            assertThat(player1.position).isEqualTo(Position.CENTER_FIELD)
            assertThat(player2.position).isEqualTo(Position.LEFT_FIELD)
            verify(exactly = 2) { gamePlayerRepository.save(any()) }
            verify(exactly = 2) { gameEventRepository.save(any()) }
        }

        @Test
        fun `투수 포지션으로 교환 시 currentPitcherId가 갱신된다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val player1 = createStartingPlayer(10L, Position.LEFT_FIELD, 5)
            val pitcher = createStartingPlayer(20L, Position.STARTING_PITCHER, null)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns player1
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gamePlayerRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            val savedEvent1 = mockk<GameEvent>(relaxed = true)
            val savedEvent2 = mockk<GameEvent>(relaxed = true)
            every { savedEvent1.id } returns 100L
            every { savedEvent2.id } returns 101L
            every { gameEventRepository.save(any()) } returnsMany listOf(savedEvent1, savedEvent2)

            val request = PositionSwapRequest(player1Id = 10L, player2Id = 20L)

            // when
            service.swapPositions(1L, request, 999L)

            // then
            // player1이 투수 포지션(STARTING_PITCHER)으로 이동
            assertThat(game.gameState.currentPitcherId).isEqualTo(10L)
        }
    }

    @Nested
    @DisplayName("swapPositions - 예외 시나리오")
    inner class SwapPositionsFailure {
        @Test
        fun `같은 선수 간 교환 시 InvalidGameStateException이 발생한다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            every { gameRepository.findByIdOrNull(1L) } returns game
            val request = PositionSwapRequest(player1Id = 10L, player2Id = 10L)

            // when & then
            assertThatThrownBy {
                service.swapPositions(1L, request, 999L)
            }.isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("같은 선수")
        }

        @Test
        fun `동일 포지션 교환 시 InvalidGameStateException이 발생한다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val player1 = createStartingPlayer(10L, Position.LEFT_FIELD, 5)
            val player2 = createStartingPlayer(20L, Position.LEFT_FIELD, 7)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns player1
            every { gamePlayerRepository.findByIdOrNull(20L) } returns player2
            val request = PositionSwapRequest(player1Id = 10L, player2Id = 20L)

            // when & then
            assertThatThrownBy {
                service.swapPositions(1L, request, 999L)
            }.isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("동일")
        }

        @Test
        fun `DH 해제 후 DH 포지션 관련 교환 시 InvalidGameStateException이 발생한다`() {
            // given
            val gameState = GameState().apply { wasDhReleased = true }
            val game = createGameWithState(1L, GameStatus.IN_PROGRESS, gameState)
            val dhPlayer = createStartingPlayer(10L, Position.DESIGNATED_HITTER, 4)
            val player2 = createStartingPlayer(20L, Position.LEFT_FIELD, 5)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns dhPlayer
            every { gamePlayerRepository.findByIdOrNull(20L) } returns player2
            val request = PositionSwapRequest(player1Id = 10L, player2Id = 20L)

            // when & then
            assertThatThrownBy {
                service.swapPositions(1L, request, 999L)
            }.isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("DH")
        }

        @Test
        fun `미출전 선수 포함 교환 시 InvalidGameStateException이 발생한다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val player1 = createStartingPlayer(10L, Position.LEFT_FIELD, 5)
            val benchPlayer = createBenchPlayer(20L, Position.CENTER_FIELD)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns player1
            every { gamePlayerRepository.findByIdOrNull(20L) } returns benchPlayer
            val request = PositionSwapRequest(player1Id = 10L, player2Id = 20L)

            // when & then
            assertThatThrownBy {
                service.swapPositions(1L, request, 999L)
            }.isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("현재 출전 중인 선수만")
        }
    }

    // ──────────────────────────── helpers ────────────────────────────

    private fun createGame(
        id: Long,
        status: GameStatus,
    ): Game = createGameWithState(id, status, GameState())

    private fun createGameWithState(
        id: Long,
        status: GameStatus,
        gameState: GameState,
    ): Game {
        val association =
            Association(
                name = "서울시야구협회",
                abbreviation = null,
                region = "서울",
                description = null,
                logoUrl = null,
                websiteUrl = null,
            )
        val league =
            League(
                association = association,
                name = "1부 리그",
                abbreviation = null,
                foundedYear = 2020,
                divisionLevel = 1,
                description = null,
                logoUrl = null,
            )
        val competition =
            Competition(
                league = league,
                name = "2025 춘계대회",
                year = 2025,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2025, 3, 1),
                endDate = LocalDate.of(2025, 6, 30),
                status = CompetitionStatus.IN_PROGRESS,
                description = null,
                maxTeams = null,
            )
        val homeTeam = Team(league = league, name = "홈팀", city = "서울", foundedYear = 2020, id = 1L)
        val awayTeam = Team(league = league, name = "원정팀", city = "부산", foundedYear = 2020, id = 2L)
        return Game.createForTest(
            competition = competition,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
            location = "잠실구장",
            fieldName = "1구장",
            gameNumber = 1,
            status = status,
            currentInning = 5,
            isTopInning = true,
            totalInnings = 9,
            gameState = gameState,
            scorerLock = ScorerLock(scorerId = 999L),
            id = id,
        )
    }

    private fun createStartingPlayer(
        id: Long,
        position: Position,
        battingOrder: Int?,
    ): GamePlayer {
        val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>(relaxed = true)
        val player = mockk<com.nextup.core.domain.player.Player>(relaxed = true)
        every { player.name } returns "선수$id"
        return GamePlayer.createStarter(
            gameTeam = gameTeam,
            player = player,
            position = position,
            battingOrder = battingOrder,
        ).apply {
            val f = GamePlayer::class.java.getDeclaredField("id")
            f.isAccessible = true
            f.set(this, id)
        }
    }

    private fun createBenchPlayer(
        id: Long,
        position: Position,
    ): GamePlayer {
        val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>(relaxed = true)
        val player = mockk<com.nextup.core.domain.player.Player>(relaxed = true)
        every { player.name } returns "선수$id"
        return GamePlayer.createBench(
            gameTeam = gameTeam,
            player = player,
            position = position,
        ).apply {
            val f = GamePlayer::class.java.getDeclaredField("id")
            f.isAccessible = true
            f.set(this, id)
        }
    }
}
