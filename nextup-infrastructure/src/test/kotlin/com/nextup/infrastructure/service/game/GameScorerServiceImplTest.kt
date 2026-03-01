package com.nextup.infrastructure.service.game

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.GamePlayerNotFoundException
import com.nextup.common.exception.InvalidGameStateException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.domain.game.Base
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameState
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.domain.league.League
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.service.game.BoxScoreService
import com.nextup.core.service.game.dto.GameEndReason
import com.nextup.core.service.game.dto.PlateAppearanceRequest
import com.nextup.core.service.game.dto.RunnerMovement
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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

@DisplayName("GameScorerServiceImpl")
class GameScorerServiceImplTest {
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var gamePlayerRepository: GamePlayerRepositoryPort
    private lateinit var gameTeamRepository: GameTeamRepositoryPort
    private lateinit var boxScoreService: BoxScoreService
    private lateinit var gameEventRepository: GameEventRepositoryPort
    private lateinit var battingRecordRepository: BattingRecordRepositoryPort
    private lateinit var pitchingRecordRepository: PitchingRecordRepositoryPort
    private lateinit var eventPublisher: ApplicationEventPublisher
    private lateinit var gameScorerService: GameScorerServiceImpl

    @BeforeEach
    fun setUp() {
        gameRepository = mockk()
        gamePlayerRepository = mockk()
        gameTeamRepository = mockk()
        boxScoreService = mockk(relaxed = true)
        gameEventRepository = mockk()
        battingRecordRepository = mockk()
        pitchingRecordRepository = mockk()
        eventPublisher = mockk(relaxed = true)
        every { gameTeamRepository.findAllByGameId(any()) } returns emptyList()
        gameScorerService =
            GameScorerServiceImpl(
                gameRepository,
                gamePlayerRepository,
                gameTeamRepository,
                boxScoreService,
                gameEventRepository,
                battingRecordRepository,
                pitchingRecordRepository,
                eventPublisher,
            )
    }

    @Nested
    @DisplayName("startGame")
    inner class StartGame {
        @Test
        fun `should start game when status is SCHEDULED`() {
            // given
            val game = createGame(1L, GameStatus.SCHEDULED)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            val result = gameScorerService.startGame(1L)

            // then
            assertThat(result.status).isEqualTo(GameStatus.IN_PROGRESS)
            assertThat(result.currentInning).isEqualTo(1)
            assertThat(result.isTopInning).isTrue()
            verify { gameRepository.save(any()) }
        }

        @Test
        fun `should throw exception when game not found`() {
            // given
            every { gameRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy { gameScorerService.startGame(999L) }
                .isInstanceOf(GameNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("advanceHalfInning")
    inner class AdvanceHalfInning {
        @Test
        fun `should advance to bottom of inning`() {
            // given
            val game =
                createGame(1L, GameStatus.IN_PROGRESS).apply {
                    currentInning = 1
                    isTopInning = true
                }
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            val result = gameScorerService.advanceHalfInning(1L)

            // then
            assertThat(result.isTopInning).isFalse()
            verify { gameRepository.save(any()) }
        }

        @Test
        fun `should throw exception when game is not in progress`() {
            // given
            val game = createGame(1L, GameStatus.SCHEDULED)
            every { gameRepository.findByIdOrNull(1L) } returns game

            // when & then
            assertThatThrownBy { gameScorerService.advanceHalfInning(1L) }
                .isInstanceOf(InvalidGameStateException::class.java)
        }
    }

    @Nested
    @DisplayName("endGame")
    inner class EndGame {
        @Test
        fun `should end game with REGULATION reason`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            val result = gameScorerService.endGame(1L, GameEndReason.REGULATION)

            // then
            assertThat(result.status).isEqualTo(GameStatus.FINISHED)
            verify { gameRepository.save(any()) }
        }

        @Test
        fun `should end game with MERCY_RULE reason`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            val result = gameScorerService.endGame(1L, GameEndReason.MERCY_RULE)

            // then
            assertThat(result.status).isEqualTo(GameStatus.CALLED)
            verify { gameRepository.save(any()) }
        }

        @Test
        fun `should end game with WEATHER reason`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            val result = gameScorerService.endGame(1L, GameEndReason.WEATHER)

            // then
            assertThat(result.status).isEqualTo(GameStatus.CALLED)
        }

        @Test
        fun `should throw exception for FORFEIT reason requiring dedicated API`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            every { gameRepository.findByIdOrNull(1L) } returns game

            // when & then
            assertThatThrownBy { gameScorerService.endGame(1L, GameEndReason.FORFEIT) }
                .isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("몰수 처리는 전용 API를 사용해주세요")
        }

        @Test
        fun `should end game with OTHER reason`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            val result = gameScorerService.endGame(1L, GameEndReason.OTHER)

            // then
            assertThat(result.status).isEqualTo(GameStatus.CALLED)
        }

        @Test
        fun `should throw exception when game is not in progress`() {
            // given
            val game = createGame(1L, GameStatus.SCHEDULED)
            every { gameRepository.findByIdOrNull(1L) } returns game

            // when & then
            assertThatThrownBy { gameScorerService.endGame(1L, GameEndReason.REGULATION) }
                .isInstanceOf(InvalidGameStateException::class.java)
        }

        @Test
        fun `should publish GameResultConfirmedEvent when game ends`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val homeTeam = createMockTeam(10L, "홈팀")
            val awayTeam = createMockTeam(20L, "원정팀")
            val homeGameTeam = createMockGameTeam(1L, game, homeTeam)
            val awayGameTeam = createMockGameTeam(2L, game, awayTeam)

            every { homeGameTeam.homeAway } returns HomeAway.HOME
            every { awayGameTeam.homeAway } returns HomeAway.AWAY
            every { homeGameTeam.totalScore } returns 5
            every { awayGameTeam.totalScore } returns 3

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameTeamRepository.findAllByGameId(1L) } returns listOf(homeGameTeam, awayGameTeam)
            every { gameRepository.save(any()) } answers { firstArg() }

            val eventSlot = slot<GameResultConfirmedEvent>()
            every { eventPublisher.publishEvent(capture(eventSlot)) } returns Unit

            // when
            gameScorerService.endGame(1L, GameEndReason.REGULATION)

            // then
            verify(exactly = 1) { eventPublisher.publishEvent(any<GameResultConfirmedEvent>()) }
            assertThat(eventSlot.captured.gameId).isEqualTo(1L)
            assertThat(eventSlot.captured.homeTeamId).isEqualTo(10L)
            assertThat(eventSlot.captured.awayTeamId).isEqualTo(20L)
            assertThat(eventSlot.captured.homeScore).isEqualTo(5)
            assertThat(eventSlot.captured.awayScore).isEqualTo(3)
        }
    }

    @Nested
    @DisplayName("recordPlateAppearance")
    inner class RecordPlateAppearance {
        @Test
        fun `should throw exception when game is not in progress`() {
            // given
            val game = createGame(1L, GameStatus.SCHEDULED)
            every { gameRepository.findByIdOrNull(1L) } returns game

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                    runnerMovements = emptyList(),
                    rbis = 0,
                    balls = 0,
                    strikes = 0,
                )

            // when & then
            assertThatThrownBy { gameScorerService.recordPlateAppearance(1L, request) }
                .isInstanceOf(InvalidGameStateException::class.java)
        }

        @Test
        fun `should throw exception when batter not found`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns null

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                    runnerMovements = emptyList(),
                    rbis = 0,
                    balls = 0,
                    strikes = 0,
                )

            // when & then
            assertThatThrownBy { gameScorerService.recordPlateAppearance(1L, request) }
                .isInstanceOf(GamePlayerNotFoundException::class.java)
        }

        @Test
        fun `should throw exception when pitcher not found`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns null

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                    runnerMovements = emptyList(),
                    rbis = 0,
                    balls = 0,
                    strikes = 0,
                )

            // when & then
            assertThatThrownBy { gameScorerService.recordPlateAppearance(1L, request) }
                .isInstanceOf(GamePlayerNotFoundException::class.java)
        }

        @Test
        fun `should record single hit`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameRepository.save(any()) } answers { firstArg() }

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                    runnerMovements = emptyList(),
                    rbis = 0,
                    balls = 2,
                    strikes = 1,
                )

            // when
            val result = gameScorerService.recordPlateAppearance(1L, request)

            // then
            assertThat(result.gameState.runnerOnFirstId).isEqualTo(10L)
            verify { boxScoreService.updateOnPlateAppearance(any(), any(), any(), any(), any(), any(), any()) }
        }

        @Test
        fun `should record double hit`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameRepository.save(any()) } answers { firstArg() }

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.DOUBLE,
                    runnerMovements = emptyList(),
                    rbis = 0,
                    balls = 0,
                    strikes = 0,
                )

            // when
            val result = gameScorerService.recordPlateAppearance(1L, request)

            // then
            assertThat(result.gameState.runnerOnSecondId).isEqualTo(10L)
        }

        @Test
        fun `should record triple hit`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameRepository.save(any()) } answers { firstArg() }

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.TRIPLE,
                    runnerMovements = emptyList(),
                    rbis = 0,
                    balls = 0,
                    strikes = 0,
                )

            // when
            val result = gameScorerService.recordPlateAppearance(1L, request)

            // then
            assertThat(result.gameState.runnerOnThirdId).isEqualTo(10L)
        }

        @Test
        fun `should record home run`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameRepository.save(any()) } answers { firstArg() }

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.HOME_RUN,
                    runnerMovements = emptyList(),
                    rbis = 1,
                    balls = 0,
                    strikes = 0,
                )

            // when
            gameScorerService.recordPlateAppearance(1L, request)

            // then
            verify {
                boxScoreService.updateOnPlateAppearance(
                    any(),
                    any(),
                    any(),
                    eq(PlateAppearanceResult.HOME_RUN),
                    any(),
                    any(),
                    any()
                )
            }
        }

        @Test
        fun `should record walk`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameRepository.save(any()) } answers { firstArg() }

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.WALK,
                    runnerMovements = emptyList(),
                    rbis = 0,
                    balls = 4,
                    strikes = 0,
                )

            // when
            val result = gameScorerService.recordPlateAppearance(1L, request)

            // then
            assertThat(result.gameState.runnerOnFirstId).isEqualTo(10L)
        }

        @Test
        fun `should record strikeout with out increase`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameRepository.save(any()) } answers { firstArg() }

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.STRIKEOUT,
                    runnerMovements = emptyList(),
                    rbis = 0,
                    balls = 0,
                    strikes = 3,
                )

            // when
            val result = gameScorerService.recordPlateAppearance(1L, request)

            // then
            assertThat(result.gameState.outs).isEqualTo(1)
        }

        @Test
        fun `should handle runner movement with score`() {
            // given
            val game =
                createGame(1L, GameStatus.IN_PROGRESS).apply {
                    gameState.runnerOnThirdId = 5L
                }
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameRepository.save(any()) } answers { firstArg() }

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                    runnerMovements =
                        listOf(
                            RunnerMovement(
                                runnerId = 5L,
                                fromBase = Base.THIRD,
                                toBase = Base.HOME,
                                isOut = false,
                            ),
                        ),
                    rbis = 1,
                    balls = 0,
                    strikes = 0,
                )

            // when
            gameScorerService.recordPlateAppearance(1L, request)

            // then
            verify {
                boxScoreService.updateOnPlateAppearance(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    match {
                        it.contains(5L)
                    },
                    any()
                )
            }
        }

        @Test
        fun `should handle runner movement with out`() {
            // given
            val game =
                createGame(1L, GameStatus.IN_PROGRESS).apply {
                    gameState.runnerOnFirstId = 5L
                }
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameRepository.save(any()) } answers { firstArg() }

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.FIELDERS_CHOICE,
                    runnerMovements =
                        listOf(
                            RunnerMovement(
                                runnerId = 5L,
                                fromBase = Base.FIRST,
                                toBase = Base.SECOND,
                                isOut = true,
                            ),
                        ),
                    rbis = 0,
                    balls = 0,
                    strikes = 0,
                )

            // when
            val result = gameScorerService.recordPlateAppearance(1L, request)

            // then
            assertThat(result.gameState.outs).isEqualTo(1)
        }

        @Test
        fun `should handle runner advancement without score or out`() {
            // given
            val game =
                createGame(1L, GameStatus.IN_PROGRESS).apply {
                    gameState.runnerOnFirstId = 5L
                }
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameRepository.save(any()) } answers { firstArg() }

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                    runnerMovements =
                        listOf(
                            RunnerMovement(
                                runnerId = 5L,
                                fromBase = Base.FIRST,
                                toBase = Base.THIRD,
                                isOut = false,
                            ),
                        ),
                    rbis = 0,
                    balls = 0,
                    strikes = 0,
                )

            // when
            val result = gameScorerService.recordPlateAppearance(1L, request)

            // then
            assertThat(result.gameState.runnerOnThirdId).isEqualTo(5L)
            assertThat(result.gameState.runnerOnFirstId).isEqualTo(10L)
        }
    }

    @Nested
    @DisplayName("forfeitGame")
    inner class ForfeitGame {
        @Test
        fun `should forfeit game when status is SCHEDULED`() {
            // given
            val game = createGame(1L, GameStatus.SCHEDULED)
            val homeTeam = createMockTeam(10L, "홈팀")
            val awayTeam = createMockTeam(20L, "원정팀")
            val homeGameTeam = createMockGameTeam(1L, game, homeTeam)
            val awayGameTeam = createMockGameTeam(2L, game, awayTeam)
            val gameTeams = listOf(homeGameTeam, awayGameTeam)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameTeamRepository.findAllByGameId(1L) } returns gameTeams
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            val result =
                gameScorerService.forfeitGame(
                    gameId = 1L,
                    winnerTeamId = 10L,
                    reason = "상대팀 불참",
                )

            // then
            assertThat(result.status).isEqualTo(GameStatus.FORFEITED)
            assertThat(result.forfeitReason).isEqualTo("상대팀 불참")
            assertThat(result.endedAt).isNotNull()
            verify { gameRepository.save(any()) }
            verify { homeGameTeam.updateScore(7, 0, 0) }
            verify { homeGameTeam.updateResult(any()) }
            verify { awayGameTeam.updateScore(0, 0, 0) }
            verify { awayGameTeam.updateResult(any()) }
        }

        @Test
        fun `should forfeit game when status is IN_PROGRESS`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val homeTeam = createMockTeam(10L, "홈팀")
            val awayTeam = createMockTeam(20L, "원정팀")
            val homeGameTeam = createMockGameTeam(1L, game, homeTeam)
            val awayGameTeam = createMockGameTeam(2L, game, awayTeam)
            val gameTeams = listOf(homeGameTeam, awayGameTeam)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameTeamRepository.findAllByGameId(1L) } returns gameTeams
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            val result =
                gameScorerService.forfeitGame(
                    gameId = 1L,
                    winnerTeamId = 20L,
                    reason = "홈팀 규정 위반",
                )

            // then
            assertThat(result.status).isEqualTo(GameStatus.FORFEITED)
            assertThat(result.forfeitReason).isEqualTo("홈팀 규정 위반")
            verify { gameRepository.save(any()) }
        }

        @Test
        fun `should throw exception when game is already finished`() {
            // given
            val game = createGame(1L, GameStatus.FINISHED)
            every { gameRepository.findByIdOrNull(1L) } returns game

            // when & then
            assertThatThrownBy {
                gameScorerService.forfeitGame(
                    gameId = 1L,
                    winnerTeamId = 10L,
                    reason = "사유",
                )
            }.isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("예정 또는 진행 중인 경기만 몰수 처리할 수 있습니다")
        }

        @Test
        fun `should throw exception when game is cancelled`() {
            // given
            val game = createGame(1L, GameStatus.CANCELLED)
            every { gameRepository.findByIdOrNull(1L) } returns game

            // when & then
            assertThatThrownBy {
                gameScorerService.forfeitGame(
                    gameId = 1L,
                    winnerTeamId = 10L,
                    reason = "사유",
                )
            }.isInstanceOf(InvalidGameStateException::class.java)
        }

        @Test
        fun `should throw exception when game not found`() {
            // given
            every { gameRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                gameScorerService.forfeitGame(
                    gameId = 999L,
                    winnerTeamId = 10L,
                    reason = "사유",
                )
            }.isInstanceOf(GameNotFoundException::class.java)
        }

        @Test
        fun `should throw exception when GameTeams count is not 2`() {
            // given
            val game = createGame(1L, GameStatus.SCHEDULED)
            val homeTeam = createMockTeam(10L, "홈팀")
            val homeGameTeam = createMockGameTeam(1L, game, homeTeam)
            val gameTeams = listOf(homeGameTeam) // Only 1 team

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameTeamRepository.findAllByGameId(1L) } returns gameTeams

            // when & then
            assertThatThrownBy {
                gameScorerService.forfeitGame(
                    gameId = 1L,
                    winnerTeamId = 10L,
                    reason = "사유",
                )
            }.isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("정확히 2개의 팀이 필요합니다")
        }
    }

    private fun createGamePlayer(id: Long): GamePlayer {
        val gamePlayer = mockk<GamePlayer>(relaxed = true)
        every { gamePlayer.id } returns id
        return gamePlayer
    }

    private fun createMockTeam(
        id: Long,
        name: String,
    ): com.nextup.core.domain.team.Team {
        val team = mockk<com.nextup.core.domain.team.Team>(relaxed = true)
        every { team.id } returns id
        every { team.name } returns name
        return team
    }

    private fun createMockGameTeam(
        id: Long,
        game: Game,
        team: com.nextup.core.domain.team.Team,
    ): com.nextup.core.domain.game.GameTeam {
        val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>(relaxed = true)
        every { gameTeam.id } returns id
        every { gameTeam.game } returns game
        every { gameTeam.team } returns team
        every { gameTeam.updateScore(any(), any(), any()) } returns Unit
        every { gameTeam.updateResult(any()) } returns Unit
        return gameTeam
    }

    private fun createAssociation(id: Long): Association =
        Association(
            name = "서울시야구협회",
            abbreviation = null,
            region = "서울",
            description = null,
            logoUrl = null,
            websiteUrl = null,
        ).apply {
            val idField = Association::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    private fun createLeague(
        id: Long,
        association: Association,
    ): League =
        League(
            association = association,
            name = "1부 리그",
            abbreviation = null,
            foundedYear = 2020,
            divisionLevel = 1,
            description = null,
            logoUrl = null,
        ).apply {
            val idField = League::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    private fun createCompetition(
        id: Long,
        league: League,
    ): Competition =
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
        ).apply {
            val idField = Competition::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    private fun createGame(
        id: Long,
        status: GameStatus,
    ): Game {
        val association = createAssociation(1L)
        val league = createLeague(1L, association)
        val competition = createCompetition(1L, league)

        return Game(
            competition = competition,
            scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
            location = "잠실구장",
            fieldName = "1구장",
            gameNumber = 1,
            status = status,
            currentInning = 5,
            isTopInning = true,
            totalInnings = 9,
            gameState = GameState(),
        ).apply {
            val idField = Game::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
    }
}
