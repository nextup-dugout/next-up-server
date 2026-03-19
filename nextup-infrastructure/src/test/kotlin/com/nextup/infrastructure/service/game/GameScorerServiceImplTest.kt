package com.nextup.infrastructure.service.game

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.GamePlayerNotFoundException
import com.nextup.common.exception.InvalidGameStateException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.event.GameCancelledEvent
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.domain.game.Base
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameResult
import com.nextup.core.domain.game.GameState
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.domain.game.PitchCountStatus
import com.nextup.core.domain.game.PitchingDecision
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
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

@DisplayName("GameLifecycleServiceImpl / PlateAppearanceRecordServiceImpl")
class GameScorerServiceImplTest {
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var gamePlayerRepository: GamePlayerRepositoryPort
    private lateinit var gameTeamRepository: GameTeamRepositoryPort
    private lateinit var boxScoreService: BoxScoreService
    private lateinit var gameEventRepository: GameEventRepositoryPort
    private lateinit var battingRecordRepository: BattingRecordRepositoryPort
    private lateinit var pitchingRecordRepository: PitchingRecordRepositoryPort
    private lateinit var eventPublisher: ApplicationEventPublisher
    private lateinit var gameLifecycleService: GameLifecycleServiceImpl
    private lateinit var plateAppearanceRecordService: PlateAppearanceRecordServiceImpl

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
        every { pitchingRecordRepository.findAllByGameId(any()) } returns emptyList()
        every { pitchingRecordRepository.findByGamePlayer(any()) } returns null
        every { gameEventRepository.save(any()) } answers { firstArg() }
        gameLifecycleService =
            GameLifecycleServiceImpl(
                gameRepository,
                gameTeamRepository,
                pitchingRecordRepository,
                com.nextup.core.service.game.PitchingDecisionService(),
                eventPublisher,
            )
        plateAppearanceRecordService =
            PlateAppearanceRecordServiceImpl(
                gameRepository,
                gamePlayerRepository,
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
            val result = gameLifecycleService.startGame(1L, 999L)

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
            assertThatThrownBy { gameLifecycleService.startGame(999L, 999L) }
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
            val result = gameLifecycleService.advanceHalfInning(1L, 999L)

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
            assertThatThrownBy { gameLifecycleService.advanceHalfInning(1L, 999L) }
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
            every { gameTeamRepository.findAllByGameId(1L) } returns game.gameTeams
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            val result = gameLifecycleService.endGame(1L, GameEndReason.REGULATION, 999L)

            // then
            assertThat(result.status).isEqualTo(GameStatus.FINISHED)
            verify { gameRepository.save(any()) }
        }

        @Test
        fun `should end game with MERCY_RULE reason`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameTeamRepository.findAllByGameId(1L) } returns game.gameTeams
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            val result = gameLifecycleService.endGame(1L, GameEndReason.MERCY_RULE, 999L)

            // then
            assertThat(result.status).isEqualTo(GameStatus.CALLED)
            verify { gameRepository.save(any()) }
        }

        @Test
        fun `should end game with WEATHER reason`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameTeamRepository.findAllByGameId(1L) } returns game.gameTeams
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            val result = gameLifecycleService.endGame(1L, GameEndReason.WEATHER, 999L)

            // then
            assertThat(result.status).isEqualTo(GameStatus.CALLED)
        }

        @Test
        fun `should throw exception for FORFEIT reason requiring dedicated API`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            every { gameRepository.findByIdOrNull(1L) } returns game

            // when & then
            assertThatThrownBy { gameLifecycleService.endGame(1L, GameEndReason.FORFEIT, 999L) }
                .isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("몰수 처리는 전용 API를 사용해주세요")
        }

        @Test
        fun `should end game with OTHER reason`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameTeamRepository.findAllByGameId(1L) } returns game.gameTeams
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            val result = gameLifecycleService.endGame(1L, GameEndReason.OTHER, 999L)

            // then
            assertThat(result.status).isEqualTo(GameStatus.CALLED)
        }

        @Test
        fun `should throw exception when game is not in progress`() {
            // given
            val game = createGame(1L, GameStatus.SCHEDULED)
            every { gameRepository.findByIdOrNull(1L) } returns game

            // when & then
            assertThatThrownBy { gameLifecycleService.endGame(1L, GameEndReason.REGULATION, 999L) }
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
            every { pitchingRecordRepository.findAllByTeamIdAndGameId(any(), any()) } returns emptyList()

            val eventSlot = slot<GameResultConfirmedEvent>()
            every { eventPublisher.publishEvent(capture(eventSlot)) } returns Unit

            // when
            gameLifecycleService.endGame(1L, GameEndReason.REGULATION, 999L)

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
            assertThatThrownBy { plateAppearanceRecordService.recordPlateAppearance(1L, request, 999L) }
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
            assertThatThrownBy { plateAppearanceRecordService.recordPlateAppearance(1L, request, 999L) }
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
            assertThatThrownBy { plateAppearanceRecordService.recordPlateAppearance(1L, request, 999L) }
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
            val result = plateAppearanceRecordService.recordPlateAppearance(1L, request, 999L)

            // then
            assertThat(result.game.gameState.runnerOnFirstId).isEqualTo(10L)
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
            val result = plateAppearanceRecordService.recordPlateAppearance(1L, request, 999L)

            // then
            assertThat(result.game.gameState.runnerOnSecondId).isEqualTo(10L)
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
            val result = plateAppearanceRecordService.recordPlateAppearance(1L, request, 999L)

            // then
            assertThat(result.game.gameState.runnerOnThirdId).isEqualTo(10L)
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
            plateAppearanceRecordService.recordPlateAppearance(1L, request, 999L)

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
            val result = plateAppearanceRecordService.recordPlateAppearance(1L, request, 999L)

            // then
            assertThat(result.game.gameState.runnerOnFirstId).isEqualTo(10L)
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
            val result = plateAppearanceRecordService.recordPlateAppearance(1L, request, 999L)

            // then
            assertThat(result.game.gameState.outs).isEqualTo(1)
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
            plateAppearanceRecordService.recordPlateAppearance(1L, request, 999L)

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
            val result = plateAppearanceRecordService.recordPlateAppearance(1L, request, 999L)

            // then
            assertThat(result.game.gameState.outs).isEqualTo(1)
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
            val result = plateAppearanceRecordService.recordPlateAppearance(1L, request, 999L)

            // then
            assertThat(result.game.gameState.runnerOnThirdId).isEqualTo(5L)
            assertThat(result.game.gameState.runnerOnFirstId).isEqualTo(10L)
        }

        @Test
        fun `should return no warnings when batting order is correct`() {
            // given
            val game =
                createGame(1L, GameStatus.IN_PROGRESS).apply {
                    isTopInning = true
                    gameState.awayBattingOrder = 3
                }
            val batter = createGamePlayer(10L)
            every { batter.battingOrder } returns 3
            val pitcher = createGamePlayer(20L)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameRepository.save(any()) } answers { firstArg() }
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns null

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                    runnerMovements = emptyList(),
                    rbis = 0,
                )

            // when
            val result = plateAppearanceRecordService.recordPlateAppearance(1L, request, 999L)

            // then
            assertThat(result.warnings).isEmpty()
        }

        @Test
        fun `should return batting order warning when batter order does not match expected`() {
            // given
            val game =
                createGame(1L, GameStatus.IN_PROGRESS).apply {
                    isTopInning = true
                    gameState.awayBattingOrder = 3
                }
            val batter = createGamePlayer(10L)
            every { batter.battingOrder } returns 5 // expected 3, actual 5
            val pitcher = createGamePlayer(20L)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameRepository.save(any()) } answers { firstArg() }
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns null

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                    runnerMovements = emptyList(),
                    rbis = 0,
                )

            // when
            val result = plateAppearanceRecordService.recordPlateAppearance(1L, request, 999L)

            // then
            assertThat(result.warnings).isNotEmpty()
            assertThat(result.warnings[0]).contains("타순 위반")
            assertThat(result.warnings[0]).contains("3")
            assertThat(result.warnings[0]).contains("5")
        }

        @Test
        fun `should return pitch count warning when pitcher exceeds threshold`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            val pitchingRecord = mockk<PitchingRecord>(relaxed = true)
            every { pitchingRecord.pitchesThrown } returns 105
            every { pitchingRecord.checkPitchCountStatus(any(), any()) } returns PitchCountStatus.LIMIT_REACHED

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameRepository.save(any()) } answers { firstArg() }
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.STRIKEOUT,
                    runnerMovements = emptyList(),
                    rbis = 0,
                )

            // when
            val result = plateAppearanceRecordService.recordPlateAppearance(1L, request, 999L)

            // then
            assertThat(result.warnings).isNotEmpty()
            assertThat(result.warnings[0]).contains("투구 수 경고")
            assertThat(result.warnings[0]).contains("105")
        }

        @Test
        fun `should return pitch count approaching warning when pitcher is near threshold`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            val pitchingRecord = mockk<PitchingRecord>(relaxed = true)
            every { pitchingRecord.pitchesThrown } returns 95 // 100 - 95 = 5 remaining (< threshold 10)
            every { pitchingRecord.checkPitchCountStatus(any(), any()) } returns PitchCountStatus.APPROACHING_LIMIT

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameRepository.save(any()) } answers { firstArg() }
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.STRIKEOUT,
                    runnerMovements = emptyList(),
                    rbis = 0,
                )

            // when
            val result = plateAppearanceRecordService.recordPlateAppearance(1L, request, 999L)

            // then
            assertThat(result.warnings).isNotEmpty()
            assertThat(result.warnings[0]).contains("투구 수 주의")
            assertThat(result.warnings[0]).contains("5")
        }

        @Test
        fun `should return no pitch count warning when pitcher has no pitch count recorded`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            val pitchingRecord = mockk<PitchingRecord>(relaxed = true)
            every { pitchingRecord.pitchesThrown } returns null
            every { pitchingRecord.checkPitchCountStatus(any(), any()) } returns null

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameRepository.save(any()) } answers { firstArg() }
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                    runnerMovements = emptyList(),
                    rbis = 0,
                )

            // when
            val result = plateAppearanceRecordService.recordPlateAppearance(1L, request, 999L)

            // then
            assertThat(result.warnings).isEmpty()
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
                gameLifecycleService.forfeitGame(
                    gameId = 1L,
                    winnerTeamId = 10L,
                    reason = "상대팀 불참",
                    scorerId = 999L,
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
                gameLifecycleService.forfeitGame(
                    gameId = 1L,
                    winnerTeamId = 20L,
                    reason = "홈팀 규정 위반",
                    scorerId = 999L,
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
                gameLifecycleService.forfeitGame(
                    gameId = 1L,
                    winnerTeamId = 10L,
                    reason = "사유",
                    scorerId = 999L,
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
                gameLifecycleService.forfeitGame(
                    gameId = 1L,
                    winnerTeamId = 10L,
                    reason = "사유",
                    scorerId = 999L,
                )
            }.isInstanceOf(InvalidGameStateException::class.java)
        }

        @Test
        fun `should throw exception when game not found`() {
            // given
            every { gameRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                gameLifecycleService.forfeitGame(
                    gameId = 999L,
                    winnerTeamId = 10L,
                    reason = "사유",
                    scorerId = 999L,
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
                gameLifecycleService.forfeitGame(
                    gameId = 1L,
                    winnerTeamId = 10L,
                    reason = "사유",
                    scorerId = 999L,
                )
            }.isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("정확히 2개의 팀이 필요합니다")
        }
    }

    @Nested
    @DisplayName("assignPitchingDecisions - endGame 시 투수 결정 할당")
    inner class AssignPitchingDecisions {

        @Test
        fun `경기 종료 시 승리팀 투수에게 WIN, 패전팀 투수에게 LOSS를 할당한다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val homeTeam = createMockTeam(10L, "홈팀")
            val awayTeam = createMockTeam(20L, "원정팀")
            val homeGameTeam = createMockGameTeam(1L, game, homeTeam)
            val awayGameTeam = createMockGameTeam(2L, game, awayTeam)

            every { homeGameTeam.homeAway } returns HomeAway.HOME
            every { awayGameTeam.homeAway } returns HomeAway.AWAY
            every { homeGameTeam.totalScore } returns 5
            every { awayGameTeam.totalScore } returns 2
            every { homeGameTeam.result } returns GameResult.WIN
            every { awayGameTeam.result } returns GameResult.LOSS
            every { homeGameTeam.inningScores } returns "5,0,0,0,0,0,0,0,0"
            every { awayGameTeam.inningScores } returns "0,0,0,0,0,0,2,0,0"

            val winnerPitcher = mockk<PitchingRecord>(relaxed = true)
            val winnerGamePlayer = mockk<GamePlayer>(relaxed = true)
            every { winnerGamePlayer.gameTeam } returns homeGameTeam
            every { winnerGamePlayer.entryInning } returns 1
            every { winnerGamePlayer.exitInning } returns null
            every { winnerPitcher.gamePlayer } returns winnerGamePlayer
            every { winnerPitcher.isStartingPitcher } returns true
            every { winnerPitcher.inningsPitchedOuts } returns 27
            every { winnerPitcher.completeInnings } returns 9
            every { winnerPitcher.decision } returns PitchingDecision.NONE

            val loserPitcher = mockk<PitchingRecord>(relaxed = true)
            val loserGamePlayer = mockk<GamePlayer>(relaxed = true)
            every { loserGamePlayer.gameTeam } returns awayGameTeam
            every { loserGamePlayer.entryInning } returns 1
            every { loserGamePlayer.exitInning } returns null
            every { loserPitcher.gamePlayer } returns loserGamePlayer
            every { loserPitcher.isStartingPitcher } returns true
            every { loserPitcher.inningsPitchedOuts } returns 27
            every { loserPitcher.completeInnings } returns 9
            every { loserPitcher.decision } returns PitchingDecision.NONE

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameTeamRepository.findAllByGameId(1L) } returns listOf(homeGameTeam, awayGameTeam)
            every { pitchingRecordRepository.findAllByGameId(1L) } returns listOf(winnerPitcher, loserPitcher)
            every { pitchingRecordRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameLifecycleService.endGame(1L, GameEndReason.REGULATION, 999L)

            // then
            verify { winnerPitcher.assignWin() }
            verify { loserPitcher.assignLoss() }
            verify(exactly = 2) { pitchingRecordRepository.save(any()) }
        }

        @Test
        fun `gameTeams가 2개가 아니면 투수 결정을 할당하지 않는다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val homeTeam = createMockTeam(10L, "홈팀")
            val homeGameTeam = createMockGameTeam(1L, game, homeTeam)

            every { gameRepository.findByIdOrNull(1L) } returns game
            // 첫 호출(endGame에서 finish용): 정상 2개, 이후 호출(assignPitchingDecisions/publishGameResultEvent): 1개
            every { gameTeamRepository.findAllByGameId(1L) } returnsMany
                listOf(game.gameTeams, listOf(homeGameTeam), listOf(homeGameTeam))
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameLifecycleService.endGame(1L, GameEndReason.REGULATION, 999L)

            // then
            verify(exactly = 0) { pitchingRecordRepository.save(any()) }
        }

        @Test
        fun `투수 기록이 없으면 결정을 할당하지 않는다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val homeTeam = createMockTeam(10L, "홈팀")
            val awayTeam = createMockTeam(20L, "원정팀")
            val homeGameTeam = createMockGameTeam(1L, game, homeTeam)
            val awayGameTeam = createMockGameTeam(2L, game, awayTeam)

            every { homeGameTeam.homeAway } returns HomeAway.HOME
            every { awayGameTeam.homeAway } returns HomeAway.AWAY
            every { homeGameTeam.totalScore } returns 3
            every { awayGameTeam.totalScore } returns 1

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameTeamRepository.findAllByGameId(1L) } returns listOf(homeGameTeam, awayGameTeam)
            every { pitchingRecordRepository.findAllByGameId(1L) } returns emptyList()
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameLifecycleService.endGame(1L, GameEndReason.REGULATION, 999L)

            // then
            verify(exactly = 0) { pitchingRecordRepository.save(any()) }
        }

        @Test
        fun `무승부(동점)이면 모든 투수에게 결정을 부여하지 않는다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val homeTeam = createMockTeam(10L, "홈팀")
            val awayTeam = createMockTeam(20L, "원정팀")
            val homeGameTeam = createMockGameTeam(1L, game, homeTeam)
            val awayGameTeam = createMockGameTeam(2L, game, awayTeam)

            every { homeGameTeam.homeAway } returns HomeAway.HOME
            every { awayGameTeam.homeAway } returns HomeAway.AWAY
            every { homeGameTeam.totalScore } returns 3
            every { awayGameTeam.totalScore } returns 3
            every { homeGameTeam.result } returns GameResult.DRAW
            every { awayGameTeam.result } returns GameResult.DRAW

            val pitcher = mockk<PitchingRecord>(relaxed = true)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameTeamRepository.findAllByGameId(1L) } returns listOf(homeGameTeam, awayGameTeam)
            every { pitchingRecordRepository.findAllByGameId(1L) } returns listOf(pitcher)
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameLifecycleService.endGame(1L, GameEndReason.REGULATION, 999L)

            // then - 무승부이므로 PitchingDecisionService가 투수 결정을 부여하지 않음
            // → save 호출 없음
            verify(exactly = 0) { pitchingRecordRepository.save(any()) }
        }

        @Test
        fun `homeTeam이 없으면 투수 결정을 할당하지 않는다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val awayTeam = createMockTeam(20L, "원정팀")
            val awayGameTeam = createMockGameTeam(2L, game, awayTeam)

            // 홈팀이 없는 비정상 케이스 (두 팀 모두 AWAY)
            every { awayGameTeam.homeAway } returns HomeAway.AWAY

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameTeamRepository.findAllByGameId(1L) } returns listOf(awayGameTeam, awayGameTeam)
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameLifecycleService.endGame(1L, GameEndReason.REGULATION, 999L)

            // then
            verify(exactly = 0) { pitchingRecordRepository.save(any()) }
        }

        @Test
        fun `awayTeam이 없으면 투수 결정을 할당하지 않는다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val homeTeam = createMockTeam(10L, "홈팀")
            val homeGameTeam = createMockGameTeam(1L, game, homeTeam)

            // 원정팀이 없는 비정상 케이스 (두 팀 모두 HOME)
            every { homeGameTeam.homeAway } returns HomeAway.HOME

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameTeamRepository.findAllByGameId(1L) } returns listOf(homeGameTeam, homeGameTeam)
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameLifecycleService.endGame(1L, GameEndReason.REGULATION, 999L)

            // then
            verify(exactly = 0) { pitchingRecordRepository.save(any()) }
        }

        @Test
        fun `세이브, 홀드, 블론세이브 결정도 올바르게 할당된다`() {
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
            every { homeGameTeam.result } returns GameResult.WIN
            every { awayGameTeam.result } returns GameResult.LOSS
            every { homeGameTeam.inningScores } returns "5,0,0,0,0,0,0,0,0"
            every { awayGameTeam.inningScores } returns "0,0,0,0,0,0,0,3,0"

            // 홈팀 선발: 7이닝 (승리 투수)
            val homeStarter = mockk<PitchingRecord>(relaxed = true)
            val homeStarterGp = mockk<GamePlayer>(relaxed = true)
            every { homeStarterGp.gameTeam } returns homeGameTeam
            every { homeStarterGp.entryInning } returns 1
            every { homeStarterGp.exitInning } returns 7
            every { homeStarter.gamePlayer } returns homeStarterGp
            every { homeStarter.isStartingPitcher } returns true
            every { homeStarter.inningsPitchedOuts } returns 21
            every { homeStarter.completeInnings } returns 7
            every { homeStarter.decision } returns PitchingDecision.NONE

            // 홈팀 마무리: 2이닝 (2점 리드 → 세이브)
            val homeCloser = mockk<PitchingRecord>(relaxed = true)
            val homeCloserGp = mockk<GamePlayer>(relaxed = true)
            every { homeCloserGp.gameTeam } returns homeGameTeam
            every { homeCloserGp.entryInning } returns 8
            every { homeCloserGp.exitInning } returns null
            every { homeCloser.gamePlayer } returns homeCloserGp
            every { homeCloser.isStartingPitcher } returns false
            every { homeCloser.inningsPitchedOuts } returns 6
            every { homeCloser.completeInnings } returns 2
            every { homeCloser.decision } returns PitchingDecision.NONE

            // 원정팀 선발 (패전 투수)
            val awayStarter = mockk<PitchingRecord>(relaxed = true)
            val awayStarterGp = mockk<GamePlayer>(relaxed = true)
            every { awayStarterGp.gameTeam } returns awayGameTeam
            every { awayStarterGp.entryInning } returns 1
            every { awayStarterGp.exitInning } returns null
            every { awayStarter.gamePlayer } returns awayStarterGp
            every { awayStarter.isStartingPitcher } returns true
            every { awayStarter.inningsPitchedOuts } returns 27
            every { awayStarter.completeInnings } returns 9
            every { awayStarter.decision } returns PitchingDecision.NONE

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameTeamRepository.findAllByGameId(1L) } returns listOf(homeGameTeam, awayGameTeam)
            every { pitchingRecordRepository.findAllByGameId(1L) } returns
                listOf(homeStarter, homeCloser, awayStarter)
            every { pitchingRecordRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameLifecycleService.endGame(1L, GameEndReason.REGULATION, 999L)

            // then
            verify { homeStarter.assignWin() }
            verify { homeCloser.assignSave() }
            verify { awayStarter.assignLoss() }
            verify(exactly = 3) { pitchingRecordRepository.save(any()) }
        }

        @Test
        fun `MERCY_RULE로 경기 종료 시에도 투수 결정이 할당된다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val homeTeam = createMockTeam(10L, "홈팀")
            val awayTeam = createMockTeam(20L, "원정팀")
            val homeGameTeam = createMockGameTeam(1L, game, homeTeam)
            val awayGameTeam = createMockGameTeam(2L, game, awayTeam)

            every { homeGameTeam.homeAway } returns HomeAway.HOME
            every { awayGameTeam.homeAway } returns HomeAway.AWAY
            every { homeGameTeam.totalScore } returns 15
            every { awayGameTeam.totalScore } returns 2
            every { homeGameTeam.result } returns GameResult.WIN
            every { awayGameTeam.result } returns GameResult.LOSS
            every { homeGameTeam.inningScores } returns "5,5,5,0,0"
            every { awayGameTeam.inningScores } returns "1,1,0,0,0"

            val winnerPitcher = mockk<PitchingRecord>(relaxed = true)
            val winnerGp = mockk<GamePlayer>(relaxed = true)
            every { winnerGp.gameTeam } returns homeGameTeam
            every { winnerGp.entryInning } returns 1
            every { winnerGp.exitInning } returns null
            every { winnerPitcher.gamePlayer } returns winnerGp
            every { winnerPitcher.isStartingPitcher } returns true
            every { winnerPitcher.inningsPitchedOuts } returns 15
            every { winnerPitcher.completeInnings } returns 5
            every { winnerPitcher.decision } returns PitchingDecision.NONE

            val loserPitcher = mockk<PitchingRecord>(relaxed = true)
            val loserGp = mockk<GamePlayer>(relaxed = true)
            every { loserGp.gameTeam } returns awayGameTeam
            every { loserGp.entryInning } returns 1
            every { loserGp.exitInning } returns null
            every { loserPitcher.gamePlayer } returns loserGp
            every { loserPitcher.isStartingPitcher } returns true
            every { loserPitcher.inningsPitchedOuts } returns 15
            every { loserPitcher.completeInnings } returns 5
            every { loserPitcher.decision } returns PitchingDecision.NONE

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameTeamRepository.findAllByGameId(1L) } returns listOf(homeGameTeam, awayGameTeam)
            every { pitchingRecordRepository.findAllByGameId(1L) } returns listOf(winnerPitcher, loserPitcher)
            every { pitchingRecordRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameLifecycleService.endGame(1L, GameEndReason.MERCY_RULE, 999L)

            // then
            verify { winnerPitcher.assignWin() }
            verify { loserPitcher.assignLoss() }
        }

        @Test
        fun `loserTeam이 result로 결정되지 않으면 winnerTeam 기반으로 유추한다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val homeTeam = createMockTeam(10L, "홈팀")
            val awayTeam = createMockTeam(20L, "원정팀")
            val homeGameTeam = createMockGameTeam(1L, game, homeTeam)
            val awayGameTeam = createMockGameTeam(2L, game, awayTeam)

            every { homeGameTeam.homeAway } returns HomeAway.HOME
            every { awayGameTeam.homeAway } returns HomeAway.AWAY
            every { homeGameTeam.totalScore } returns 5
            every { awayGameTeam.totalScore } returns 2
            // result가 UNDECIDED인 경우 → winnerTeam 기반으로 loserTeam 유추
            every { homeGameTeam.result } returns GameResult.UNDECIDED
            every { awayGameTeam.result } returns GameResult.UNDECIDED
            every { homeGameTeam.inningScores } returns "5,0,0,0,0,0,0,0,0"
            every { awayGameTeam.inningScores } returns "0,0,0,0,0,0,2,0,0"

            val winnerPitcher = mockk<PitchingRecord>(relaxed = true)
            val winnerGp = mockk<GamePlayer>(relaxed = true)
            every { winnerGp.gameTeam } returns homeGameTeam
            every { winnerGp.entryInning } returns 1
            every { winnerGp.exitInning } returns null
            every { winnerPitcher.gamePlayer } returns winnerGp
            every { winnerPitcher.isStartingPitcher } returns true
            every { winnerPitcher.inningsPitchedOuts } returns 27
            every { winnerPitcher.completeInnings } returns 9
            every { winnerPitcher.decision } returns PitchingDecision.NONE

            val loserPitcher = mockk<PitchingRecord>(relaxed = true)
            val loserGp = mockk<GamePlayer>(relaxed = true)
            every { loserGp.gameTeam } returns awayGameTeam
            every { loserGp.entryInning } returns 1
            every { loserGp.exitInning } returns null
            every { loserPitcher.gamePlayer } returns loserGp
            every { loserPitcher.isStartingPitcher } returns true
            every { loserPitcher.inningsPitchedOuts } returns 27
            every { loserPitcher.completeInnings } returns 9
            every { loserPitcher.decision } returns PitchingDecision.NONE

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameTeamRepository.findAllByGameId(1L) } returns listOf(homeGameTeam, awayGameTeam)
            every { pitchingRecordRepository.findAllByGameId(1L) } returns listOf(winnerPitcher, loserPitcher)
            every { pitchingRecordRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameLifecycleService.endGame(1L, GameEndReason.REGULATION, 999L)

            // then - winnerTeam==homeTeam → loserTeam==awayTeam
            verify { winnerPitcher.assignWin() }
            verify { loserPitcher.assignLoss() }
            verify(exactly = 2) { pitchingRecordRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("cancelGame")
    inner class CancelGame {
        @Test
        fun `should cancel game when status is SCHEDULED`() {
            // given
            val game = createGame(1L, GameStatus.SCHEDULED)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            val result = gameLifecycleService.cancelGame(1L, "우천 취소", 999L)

            // then
            assertThat(result.status).isEqualTo(GameStatus.CANCELLED)
            verify { gameRepository.save(any()) }
            verify { eventPublisher.publishEvent(any<GameCancelledEvent>()) }
        }

        @Test
        fun `should cancel game when status is POSTPONED`() {
            // given
            val game = createGame(1L, GameStatus.POSTPONED)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            val result = gameLifecycleService.cancelGame(1L, "대회 취소", 999L)

            // then
            assertThat(result.status).isEqualTo(GameStatus.CANCELLED)
            verify { gameRepository.save(any()) }
            verify { eventPublisher.publishEvent(any<GameCancelledEvent>()) }
        }

        @Test
        fun `should cancel game without reason`() {
            // given
            val game = createGame(1L, GameStatus.SCHEDULED)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            val result = gameLifecycleService.cancelGame(1L, null, 999L)

            // then
            assertThat(result.status).isEqualTo(GameStatus.CANCELLED)
            verify { gameRepository.save(any()) }
        }

        @Test
        fun `should throw exception when game is IN_PROGRESS`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            every { gameRepository.findByIdOrNull(1L) } returns game

            // when & then
            assertThatThrownBy { gameLifecycleService.cancelGame(1L, "사유", 999L) }
                .isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("예정, 연기, 또는 중단 상태의 경기만 취소할 수 있습니다")
        }

        @Test
        fun `should throw exception when game is FINISHED`() {
            // given
            val game = createGame(1L, GameStatus.FINISHED)
            every { gameRepository.findByIdOrNull(1L) } returns game

            // when & then
            assertThatThrownBy { gameLifecycleService.cancelGame(1L, "사유", 999L) }
                .isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("예정, 연기, 또는 중단 상태의 경기만 취소할 수 있습니다")
        }

        @Test
        fun `should throw exception when game not found`() {
            // given
            every { gameRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy { gameLifecycleService.cancelGame(999L, "사유", 999L) }
                .isInstanceOf(GameNotFoundException::class.java)
        }

        @Test
        fun `should publish GameCancelledEvent with correct gameId`() {
            // given
            val game = createGame(1L, GameStatus.SCHEDULED)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameRepository.save(any()) } answers { firstArg() }

            val eventSlot = slot<GameCancelledEvent>()
            every { eventPublisher.publishEvent(capture(eventSlot)) } returns Unit

            // when
            gameLifecycleService.cancelGame(1L, "우천 취소", 999L)

            // then
            verify(exactly = 1) { eventPublisher.publishEvent(any<GameCancelledEvent>()) }
            assertThat(eventSlot.captured.gameId).isEqualTo(1L)
        }
    }

    private fun createGamePlayer(id: Long): GamePlayer {
        val gamePlayer = mockk<GamePlayer>(relaxed = true)
        every { gamePlayer.id } returns id
        every { gamePlayer.battingOrder } returns 1
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
        val homeTeam =
            Team(
                league = league,
                name = "홈팀",
                city = "서울",
                foundedYear = 2020,
                id = 10L,
            )
        val awayTeam =
            Team(
                league = league,
                name = "원정팀",
                city = "부산",
                foundedYear = 2020,
                id = 11L,
            )

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
            gameState = GameState(),
            scorerId = 999L,
            id = id,
        )
    }
}
