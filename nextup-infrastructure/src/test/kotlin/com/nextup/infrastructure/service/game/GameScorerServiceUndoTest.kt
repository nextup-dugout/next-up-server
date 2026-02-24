package com.nextup.infrastructure.service.game

import com.nextup.common.exception.NoEventToUndoException
import com.nextup.common.exception.UndoNotAvailableException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameEvent
import com.nextup.core.domain.game.GameEventType
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameState
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.domain.league.League
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.service.game.BoxScoreService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("GameScorerServiceImpl - Undo")
class GameScorerServiceUndoTest {
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var gamePlayerRepository: GamePlayerRepositoryPort
    private lateinit var gameTeamRepository: GameTeamRepositoryPort
    private lateinit var boxScoreService: BoxScoreService
    private lateinit var gameEventRepository: GameEventRepositoryPort
    private lateinit var battingRecordRepository: BattingRecordRepositoryPort
    private lateinit var pitchingRecordRepository: PitchingRecordRepositoryPort
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
        gameScorerService =
            GameScorerServiceImpl(
                gameRepository,
                gamePlayerRepository,
                gameTeamRepository,
                boxScoreService,
                gameEventRepository,
                battingRecordRepository,
                pitchingRecordRepository,
            )
    }

    @Nested
    @DisplayName("undoLastEvent")
    inner class UndoLastEvent {
        @Test
        fun `should undo single hit event and revert batting record`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            game.gameState.runnerOnFirstId = 10L // 타자가 1루에 있음

            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            val battingRecord =
                createBattingRecord(batter).apply {
                    applyPlateAppearanceResult(PlateAppearanceResult.SINGLE, 0)
                }
            val pitchingRecord =
                createPitchingRecord(pitcher).apply {
                    applyBatterFaced(PlateAppearanceResult.SINGLE)
                }

            val event =
                createPlateAppearanceEvent(
                    game = game,
                    batter = batter,
                    pitcher = pitcher,
                    result = PlateAppearanceResult.SINGLE,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    runnersBeforeJson = null,
                    runsScored = 0,
                )

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            val result = gameScorerService.undoLastEvent(1L)

            // then
            assertThat(result.undone).isTrue()
            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.hits).isEqualTo(0)
            verify { gameEventRepository.save(any()) }
            verify { gameRepository.save(any()) }
        }

        @Test
        fun `should undo home run and revert score and batting record`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>(relaxed = true)
            every { batter.gameTeam } returns gameTeam

            val battingRecord =
                createBattingRecord(batter).apply {
                    applyPlateAppearanceResult(PlateAppearanceResult.HOME_RUN, 1)
                }
            val pitchingRecord =
                createPitchingRecord(pitcher).apply {
                    applyBatterFaced(PlateAppearanceResult.HOME_RUN)
                    recordEarnedRun(1)
                }

            val event =
                createPlateAppearanceEvent(
                    game = game,
                    batter = batter,
                    pitcher = pitcher,
                    result = PlateAppearanceResult.HOME_RUN,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    runnersBeforeJson = null,
                    runsScored = 1,
                    rbis = 1,
                )

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            val result = gameScorerService.undoLastEvent(1L)

            // then
            assertThat(result.undone).isTrue()
            assertThat(battingRecord.homeRuns).isEqualTo(0)
            assertThat(battingRecord.hits).isEqualTo(0)
            assertThat(battingRecord.runs).isEqualTo(0)
            assertThat(battingRecord.runsBattedIn).isEqualTo(0)
            verify { gameTeam.subtractRunInInning(any(), eq(1)) }
        }

        @Test
        fun `should undo strikeout and restore out count and pitching record`() {
            // given
            val game =
                createGame(1L, GameStatus.IN_PROGRESS).apply {
                    gameState.outs = 1 // 이미 1아웃
                }
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)

            val battingRecord =
                createBattingRecord(batter).apply {
                    applyPlateAppearanceResult(PlateAppearanceResult.STRIKEOUT, 0)
                }
            val pitchingRecord =
                createPitchingRecord(pitcher).apply {
                    applyBatterFaced(PlateAppearanceResult.STRIKEOUT)
                    recordOut()
                }

            val event =
                createPlateAppearanceEvent(
                    game = game,
                    batter = batter,
                    pitcher = pitcher,
                    result = PlateAppearanceResult.STRIKEOUT,
                    outCountBefore = 0,
                    outCountAfter = 1,
                    runnersBeforeJson = null,
                    runsScored = 0,
                )

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameScorerService.undoLastEvent(1L)

            // then
            assertThat(game.gameState.outs).isEqualTo(0) // 아웃 카운트 복원
            assertThat(battingRecord.strikeouts).isEqualTo(0)
            assertThat(pitchingRecord.strikeouts).isEqualTo(0)
            assertThat(pitchingRecord.inningsPitchedOuts).isEqualTo(0)
        }

        @Test
        fun `should throw exception when game is not in progress`() {
            // given
            val game = createGame(1L, GameStatus.SCHEDULED)
            every { gameRepository.findByIdOrNull(1L) } returns game

            // when & then
            assertThatThrownBy { gameScorerService.undoLastEvent(1L) }
                .isInstanceOf(UndoNotAvailableException::class.java)
        }

        @Test
        fun `should throw exception when no event to undo`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns null

            // when & then
            assertThatThrownBy { gameScorerService.undoLastEvent(1L) }
                .isInstanceOf(NoEventToUndoException::class.java)
        }

        @Test
        fun `should undo consecutive events correctly`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter1 = createGamePlayer(10L)
            val batter2 = createGamePlayer(11L)
            val pitcher = createGamePlayer(20L)

            val battingRecord2 =
                createBattingRecord(batter2).apply {
                    applyPlateAppearanceResult(PlateAppearanceResult.DOUBLE, 0)
                }
            val pitchingRecord =
                createPitchingRecord(pitcher).apply {
                    applyBatterFaced(PlateAppearanceResult.SINGLE)
                    applyBatterFaced(PlateAppearanceResult.DOUBLE)
                }

            // 두 번째 이벤트 (나중에 기록된 것)
            val event2 =
                createPlateAppearanceEvent(
                    game = game,
                    batter = batter2,
                    pitcher = pitcher,
                    result = PlateAppearanceResult.DOUBLE,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    runnersBeforeJson = "1루:10",
                    runsScored = 0,
                )

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event2
            every { battingRecordRepository.findByGamePlayer(batter2) } returns battingRecord2
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when - 첫 번째 Undo
            gameScorerService.undoLastEvent(1L)

            // then
            assertThat(event2.undone).isTrue()
            assertThat(battingRecord2.doubles).isEqualTo(0)
            assertThat(battingRecord2.hits).isEqualTo(0)
            // 주자가 이전 상태로 복원되었는지 확인
            assertThat(game.gameState.runnerOnFirstId).isEqualTo(10L)
        }

        @Test
        fun `should undo inning change event`() {
            // given - 이닝 전환 후 상태
            val game =
                createGame(1L, GameStatus.IN_PROGRESS).apply {
                    currentInning = 1
                    isTopInning = false // 1회말로 전환된 상태
                    gameState.outs = 0
                }

            val event =
                GameEvent(
                    game = game,
                    inning = 1,
                    isTopInning = true, // 이벤트는 1회초에서 발생
                    outCountBefore = 3,
                    outCountAfter = 0,
                    eventType = GameEventType.INNING_CHANGE,
                    description = "1회초 종료, 1회말 시작",
                )
            setEntityId(event, 100L)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameScorerService.undoLastEvent(1L)

            // then
            assertThat(event.undone).isTrue()
            assertThat(game.currentInning).isEqualTo(1)
            assertThat(game.isTopInning).isTrue() // 1회초로 복원
            assertThat(game.gameState.outs).isEqualTo(3) // 3아웃 상태로 복원
        }

        // ============================================================
        // 추가 Undo 테스트 (15건+)
        // ============================================================

        @Test
        fun `should undo double play and revert two out counts and batting record`() {
            // given - 병살타: 2아웃 발생, 주자 1명 있었음
            val game =
                createGame(1L, GameStatus.IN_PROGRESS).apply {
                    gameState.outs = 2
                }
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>(relaxed = true)
            every { batter.gameTeam } returns gameTeam

            val battingRecord =
                createBattingRecord(batter).apply {
                    applyPlateAppearanceResult(PlateAppearanceResult.DOUBLE_PLAY, 0)
                }
            val pitchingRecord =
                createPitchingRecord(pitcher).apply {
                    // 병살타: 투수에게 2아웃 기록
                    recordOut()
                    recordOut()
                }

            val event =
                createPlateAppearanceEvent(
                    game = game,
                    batter = batter,
                    pitcher = pitcher,
                    result = PlateAppearanceResult.DOUBLE_PLAY,
                    outCountBefore = 0,
                    outCountAfter = 2,
                    runnersBeforeJson = "1루:30",
                    runsScored = 0,
                )

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameScorerService.undoLastEvent(1L)

            // then
            assertThat(event.undone).isTrue()
            assertThat(game.gameState.outs).isEqualTo(0) // 아웃 카운트 복원
            assertThat(battingRecord.groundedIntoDoublePlays).isEqualTo(0)
            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(game.gameState.runnerOnFirstId).isEqualTo(30L) // 1루 주자 복원
        }

        @Test
        fun `should undo sacrifice bunt and revert sacrifice bunt count and runner`() {
            // given - 희생번트: 타수 제외, 주자 1루→2루 진루, 타자 아웃
            val game =
                createGame(1L, GameStatus.IN_PROGRESS).apply {
                    gameState.outs = 1
                }
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>(relaxed = true)
            every { batter.gameTeam } returns gameTeam

            val battingRecord =
                createBattingRecord(batter).apply {
                    applyPlateAppearanceResult(PlateAppearanceResult.SACRIFICE_BUNT, 0)
                }
            val pitchingRecord =
                createPitchingRecord(pitcher).apply {
                    recordOut()
                }

            val event =
                createPlateAppearanceEvent(
                    game = game,
                    batter = batter,
                    pitcher = pitcher,
                    result = PlateAppearanceResult.SACRIFICE_BUNT,
                    outCountBefore = 0,
                    outCountAfter = 1,
                    runnersBeforeJson = "1루:30",
                    runsScored = 0,
                )

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameScorerService.undoLastEvent(1L)

            // then
            assertThat(event.undone).isTrue()
            assertThat(game.gameState.outs).isEqualTo(0)
            assertThat(battingRecord.sacrificeBunts).isEqualTo(0)
            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(battingRecord.atBats).isEqualTo(0) // 희생번트는 타수 제외
            assertThat(game.gameState.runnerOnFirstId).isEqualTo(30L) // 주자 복원
        }

        @Test
        fun `should undo walk and revert walk count`() {
            // given - 볼넷: 타수 제외, 타자 1루 진루
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>(relaxed = true)
            every { batter.gameTeam } returns gameTeam

            val battingRecord =
                createBattingRecord(batter).apply {
                    applyPlateAppearanceResult(PlateAppearanceResult.WALK, 0)
                }
            val pitchingRecord =
                createPitchingRecord(pitcher).apply {
                    applyBatterFaced(PlateAppearanceResult.WALK)
                }

            val event =
                createPlateAppearanceEvent(
                    game = game,
                    batter = batter,
                    pitcher = pitcher,
                    result = PlateAppearanceResult.WALK,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    runnersBeforeJson = null,
                    runsScored = 0,
                )

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameScorerService.undoLastEvent(1L)

            // then
            assertThat(event.undone).isTrue()
            assertThat(battingRecord.walks).isEqualTo(0)
            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(battingRecord.atBats).isEqualTo(0) // 볼넷은 타수 제외
            assertThat(pitchingRecord.walksAllowed).isEqualTo(0)
        }

        @Test
        fun `should undo walk with rbi and revert score`() {
            // given - 볼넷 + 타점 (만루 볼넷): 1점 득점
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>(relaxed = true)
            every { batter.gameTeam } returns gameTeam

            val battingRecord =
                createBattingRecord(batter).apply {
                    applyPlateAppearanceResult(PlateAppearanceResult.WALK, 1)
                }
            val pitchingRecord =
                createPitchingRecord(pitcher).apply {
                    applyBatterFaced(PlateAppearanceResult.WALK)
                    recordEarnedRun(1)
                }

            val event =
                createPlateAppearanceEvent(
                    game = game,
                    batter = batter,
                    pitcher = pitcher,
                    result = PlateAppearanceResult.WALK,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    runnersBeforeJson = "1루:30,2루:31,3루:32",
                    runsScored = 1,
                    rbis = 1,
                )

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameScorerService.undoLastEvent(1L)

            // then
            assertThat(event.undone).isTrue()
            assertThat(battingRecord.runsBattedIn).isEqualTo(0)
            assertThat(pitchingRecord.earnedRuns).isEqualTo(0)
            assertThat(pitchingRecord.runsAllowed).isEqualTo(0)
            verify { gameTeam.subtractRunInInning(any(), eq(1)) }
        }

        @Test
        fun `should undo inning change from top to bottom and restore previous inning state`() {
            // given - 2회초 종료 후 2회말로 전환 Undo
            val game =
                createGame(1L, GameStatus.IN_PROGRESS).apply {
                    currentInning = 2
                    isTopInning = false
                    gameState.outs = 0
                }

            val event =
                GameEvent(
                    game = game,
                    inning = 2,
                    isTopInning = true,
                    outCountBefore = 3,
                    outCountAfter = 0,
                    eventType = GameEventType.INNING_CHANGE,
                    description = "2회초 종료, 2회말 시작",
                )
            setEntityId(event, 200L)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameScorerService.undoLastEvent(1L)

            // then
            assertThat(event.undone).isTrue()
            assertThat(game.currentInning).isEqualTo(2)
            assertThat(game.isTopInning).isTrue() // 2회초로 복원
            assertThat(game.gameState.outs).isEqualTo(3)
        }

        @Test
        fun `should undo inning change from bottom to next inning top and restore inning`() {
            // given - 1회말 종료 후 2회초로 전환 Undo
            val game =
                createGame(1L, GameStatus.IN_PROGRESS).apply {
                    currentInning = 2
                    isTopInning = true
                    gameState.outs = 0
                }

            val event =
                GameEvent(
                    game = game,
                    inning = 1,
                    isTopInning = false,
                    outCountBefore = 3,
                    outCountAfter = 0,
                    eventType = GameEventType.INNING_CHANGE,
                    description = "1회말 종료, 2회초 시작",
                )
            setEntityId(event, 300L)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameScorerService.undoLastEvent(1L)

            // then
            assertThat(event.undone).isTrue()
            assertThat(game.currentInning).isEqualTo(1) // 1회로 복원
            assertThat(game.isTopInning).isFalse() // 1회말로 복원
            assertThat(game.gameState.outs).isEqualTo(3)
        }

        @Test
        fun `should undo triple and revert triple count and runner positions`() {
            // given - 3루타: 타자가 3루에 있었음
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            game.gameState.runnerOnThirdId = 10L

            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>(relaxed = true)
            every { batter.gameTeam } returns gameTeam

            val battingRecord =
                createBattingRecord(batter).apply {
                    applyPlateAppearanceResult(PlateAppearanceResult.TRIPLE, 0)
                }
            val pitchingRecord =
                createPitchingRecord(pitcher).apply {
                    applyBatterFaced(PlateAppearanceResult.TRIPLE)
                }

            val event =
                createPlateAppearanceEvent(
                    game = game,
                    batter = batter,
                    pitcher = pitcher,
                    result = PlateAppearanceResult.TRIPLE,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    runnersBeforeJson = null,
                    runsScored = 0,
                )

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameScorerService.undoLastEvent(1L)

            // then
            assertThat(event.undone).isTrue()
            assertThat(battingRecord.triples).isEqualTo(0)
            assertThat(battingRecord.hits).isEqualTo(0)
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(pitchingRecord.hitsAllowed).isEqualTo(0)
        }

        @Test
        fun `should undo home run with multiple runners and revert all runs scored`() {
            // given - 만루 홈런: 4점 득점
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>(relaxed = true)
            every { batter.gameTeam } returns gameTeam

            val battingRecord =
                createBattingRecord(batter).apply {
                    applyPlateAppearanceResult(PlateAppearanceResult.HOME_RUN, 4)
                }
            val pitchingRecord =
                createPitchingRecord(pitcher).apply {
                    applyBatterFaced(PlateAppearanceResult.HOME_RUN)
                    recordEarnedRun(4)
                }

            val event =
                createPlateAppearanceEvent(
                    game = game,
                    batter = batter,
                    pitcher = pitcher,
                    result = PlateAppearanceResult.HOME_RUN,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    runnersBeforeJson = "1루:30,2루:31,3루:32",
                    runsScored = 4,
                    rbis = 4,
                )

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameScorerService.undoLastEvent(1L)

            // then
            assertThat(event.undone).isTrue()
            assertThat(battingRecord.homeRuns).isEqualTo(0)
            assertThat(battingRecord.runsBattedIn).isEqualTo(0)
            assertThat(pitchingRecord.earnedRuns).isEqualTo(0)
            assertThat(pitchingRecord.runsAllowed).isEqualTo(0)
            assertThat(pitchingRecord.homeRunsAllowed).isEqualTo(0)
            // 만루 주자 복원 확인
            assertThat(game.gameState.runnerOnFirstId).isEqualTo(30L)
            assertThat(game.gameState.runnerOnSecondId).isEqualTo(31L)
            assertThat(game.gameState.runnerOnThirdId).isEqualTo(32L)
            verify { gameTeam.subtractRunInInning(any(), eq(4)) }
        }

        @Test
        fun `should undo error and revert at bat count`() {
            // given - 실책: 타자 출루, 타수 기록
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>(relaxed = true)
            every { batter.gameTeam } returns gameTeam

            val battingRecord =
                createBattingRecord(batter).apply {
                    applyPlateAppearanceResult(PlateAppearanceResult.ERROR, 0)
                }
            val pitchingRecord =
                createPitchingRecord(pitcher)

            val event =
                createPlateAppearanceEvent(
                    game = game,
                    batter = batter,
                    pitcher = pitcher,
                    result = PlateAppearanceResult.ERROR,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    runnersBeforeJson = null,
                    runsScored = 0,
                )

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameScorerService.undoLastEvent(1L)

            // then
            assertThat(event.undone).isTrue()
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(battingRecord.hits).isEqualTo(0) // 실책은 안타가 아님
        }

        @Test
        fun `should undo fly out and revert out count and at bat`() {
            // given - 플라이 아웃
            val game =
                createGame(1L, GameStatus.IN_PROGRESS).apply {
                    gameState.outs = 1
                }
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>(relaxed = true)
            every { batter.gameTeam } returns gameTeam

            val battingRecord =
                createBattingRecord(batter).apply {
                    applyPlateAppearanceResult(PlateAppearanceResult.FLY_OUT, 0)
                }
            val pitchingRecord =
                createPitchingRecord(pitcher).apply {
                    recordOut()
                }

            val event =
                createPlateAppearanceEvent(
                    game = game,
                    batter = batter,
                    pitcher = pitcher,
                    result = PlateAppearanceResult.FLY_OUT,
                    outCountBefore = 0,
                    outCountAfter = 1,
                    runnersBeforeJson = null,
                    runsScored = 0,
                )

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameScorerService.undoLastEvent(1L)

            // then
            assertThat(event.undone).isTrue()
            assertThat(game.gameState.outs).isEqualTo(0)
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(pitchingRecord.inningsPitchedOuts).isEqualTo(0)
        }

        @Test
        fun `should undo ground out and revert out count`() {
            // given - 땅볼 아웃
            val game =
                createGame(1L, GameStatus.IN_PROGRESS).apply {
                    gameState.outs = 2
                }
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>(relaxed = true)
            every { batter.gameTeam } returns gameTeam

            val battingRecord =
                createBattingRecord(batter).apply {
                    applyPlateAppearanceResult(PlateAppearanceResult.GROUND_OUT, 0)
                }
            val pitchingRecord =
                createPitchingRecord(pitcher).apply {
                    recordOut()
                }

            val event =
                createPlateAppearanceEvent(
                    game = game,
                    batter = batter,
                    pitcher = pitcher,
                    result = PlateAppearanceResult.GROUND_OUT,
                    outCountBefore = 1,
                    outCountAfter = 2,
                    runnersBeforeJson = null,
                    runsScored = 0,
                )

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameScorerService.undoLastEvent(1L)

            // then
            assertThat(event.undone).isTrue()
            assertThat(game.gameState.outs).isEqualTo(1) // outCountBefore로 복원
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(pitchingRecord.inningsPitchedOuts).isEqualTo(0)
        }

        @Test
        fun `should undo hit by pitch and revert hit by pitch count`() {
            // given - 사구
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>(relaxed = true)
            every { batter.gameTeam } returns gameTeam

            val battingRecord =
                createBattingRecord(batter).apply {
                    applyPlateAppearanceResult(PlateAppearanceResult.HIT_BY_PITCH, 0)
                }
            val pitchingRecord =
                createPitchingRecord(pitcher).apply {
                    applyBatterFaced(PlateAppearanceResult.HIT_BY_PITCH)
                }

            val event =
                createPlateAppearanceEvent(
                    game = game,
                    batter = batter,
                    pitcher = pitcher,
                    result = PlateAppearanceResult.HIT_BY_PITCH,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    runnersBeforeJson = null,
                    runsScored = 0,
                )

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameScorerService.undoLastEvent(1L)

            // then
            assertThat(event.undone).isTrue()
            assertThat(battingRecord.hitByPitch).isEqualTo(0)
            assertThat(battingRecord.atBats).isEqualTo(0) // 사구는 타수 제외
            assertThat(pitchingRecord.hitBatsmen).isEqualTo(0)
        }

        @Test
        fun `should undo sacrifice fly and revert sacrifice fly count and rbi`() {
            // given - 희생플라이: 타자 아웃, 3루 주자 득점
            val game =
                createGame(1L, GameStatus.IN_PROGRESS).apply {
                    gameState.outs = 1
                }
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>(relaxed = true)
            every { batter.gameTeam } returns gameTeam

            val battingRecord =
                createBattingRecord(batter).apply {
                    applyPlateAppearanceResult(PlateAppearanceResult.SACRIFICE_FLY, 1)
                }
            val pitchingRecord =
                createPitchingRecord(pitcher).apply {
                    recordOut()
                    recordEarnedRun(1)
                }

            val event =
                createPlateAppearanceEvent(
                    game = game,
                    batter = batter,
                    pitcher = pitcher,
                    result = PlateAppearanceResult.SACRIFICE_FLY,
                    outCountBefore = 0,
                    outCountAfter = 1,
                    runnersBeforeJson = "3루:32",
                    runsScored = 1,
                    rbis = 1,
                )

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameScorerService.undoLastEvent(1L)

            // then
            assertThat(event.undone).isTrue()
            assertThat(battingRecord.sacrificeFlies).isEqualTo(0)
            assertThat(battingRecord.runsBattedIn).isEqualTo(0)
            assertThat(game.gameState.outs).isEqualTo(0)
            assertThat(game.gameState.runnerOnThirdId).isEqualTo(32L) // 3루 주자 복원
            verify { gameTeam.subtractRunInInning(any(), eq(1)) }
        }

        @Test
        fun `should undo double and revert double count and runners before state`() {
            // given - 2루타: 1루 주자가 있었고 2루타로 타자 2루, 1루 주자 득점
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>(relaxed = true)
            every { batter.gameTeam } returns gameTeam

            val battingRecord =
                createBattingRecord(batter).apply {
                    applyPlateAppearanceResult(PlateAppearanceResult.DOUBLE, 1)
                }
            val pitchingRecord =
                createPitchingRecord(pitcher).apply {
                    applyBatterFaced(PlateAppearanceResult.DOUBLE)
                    recordEarnedRun(1)
                }

            val event =
                createPlateAppearanceEvent(
                    game = game,
                    batter = batter,
                    pitcher = pitcher,
                    result = PlateAppearanceResult.DOUBLE,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    runnersBeforeJson = "1루:30",
                    runsScored = 1,
                    rbis = 1,
                )

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameScorerService.undoLastEvent(1L)

            // then
            assertThat(event.undone).isTrue()
            assertThat(battingRecord.doubles).isEqualTo(0)
            assertThat(battingRecord.hits).isEqualTo(0)
            assertThat(battingRecord.runsBattedIn).isEqualTo(0)
            assertThat(game.gameState.runnerOnFirstId).isEqualTo(30L) // 1루 주자 복원
            assertThat(pitchingRecord.earnedRuns).isEqualTo(0)
            verify { gameTeam.subtractRunInInning(any(), eq(1)) }
        }

        @Test
        fun `should undo intentional walk and revert intentional walk count`() {
            // given - 고의사구
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>(relaxed = true)
            every { batter.gameTeam } returns gameTeam

            val battingRecord =
                createBattingRecord(batter).apply {
                    applyPlateAppearanceResult(PlateAppearanceResult.INTENTIONAL_WALK, 0)
                }
            val pitchingRecord =
                createPitchingRecord(pitcher).apply {
                    applyBatterFaced(PlateAppearanceResult.INTENTIONAL_WALK)
                }

            val event =
                createPlateAppearanceEvent(
                    game = game,
                    batter = batter,
                    pitcher = pitcher,
                    result = PlateAppearanceResult.INTENTIONAL_WALK,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    runnersBeforeJson = null,
                    runsScored = 0,
                )

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameScorerService.undoLastEvent(1L)

            // then
            assertThat(event.undone).isTrue()
            assertThat(battingRecord.intentionalWalks).isEqualTo(0)
            assertThat(battingRecord.atBats).isEqualTo(0) // 고의사구는 타수 제외
            assertThat(pitchingRecord.walksAllowed).isEqualTo(0)
        }

        @Test
        fun `should undo line out and revert out count and at bat`() {
            // given - 라인 드라이브 아웃
            val game =
                createGame(1L, GameStatus.IN_PROGRESS).apply {
                    gameState.outs = 1
                }
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>(relaxed = true)
            every { batter.gameTeam } returns gameTeam

            val battingRecord =
                createBattingRecord(batter).apply {
                    applyPlateAppearanceResult(PlateAppearanceResult.LINE_OUT, 0)
                }
            val pitchingRecord =
                createPitchingRecord(pitcher).apply {
                    recordOut()
                }

            val event =
                createPlateAppearanceEvent(
                    game = game,
                    batter = batter,
                    pitcher = pitcher,
                    result = PlateAppearanceResult.LINE_OUT,
                    outCountBefore = 0,
                    outCountAfter = 1,
                    runnersBeforeJson = null,
                    runsScored = 0,
                )

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameScorerService.undoLastEvent(1L)

            // then
            assertThat(event.undone).isTrue()
            assertThat(game.gameState.outs).isEqualTo(0)
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(pitchingRecord.inningsPitchedOuts).isEqualTo(0)
        }

        @Test
        fun `should undo fielders choice and revert at bat and runner state`() {
            // given - 야수선택: 주자 있었고 타자가 1루에 진루
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>(relaxed = true)
            every { batter.gameTeam } returns gameTeam

            val battingRecord =
                createBattingRecord(batter).apply {
                    applyPlateAppearanceResult(PlateAppearanceResult.FIELDERS_CHOICE, 0)
                }
            val pitchingRecord =
                createPitchingRecord(pitcher).apply {
                    recordOut()
                }

            val event =
                createPlateAppearanceEvent(
                    game = game,
                    batter = batter,
                    pitcher = pitcher,
                    result = PlateAppearanceResult.FIELDERS_CHOICE,
                    outCountBefore = 0,
                    outCountAfter = 1,
                    runnersBeforeJson = "1루:30",
                    runsScored = 0,
                )

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameScorerService.undoLastEvent(1L)

            // then
            assertThat(event.undone).isTrue()
            assertThat(battingRecord.atBats).isEqualTo(0)
            assertThat(battingRecord.plateAppearances).isEqualTo(0)
            assertThat(game.gameState.runnerOnFirstId).isEqualTo(30L) // 주자 복원
        }

        @Test
        fun `should undo third strikeout and restore two outs state`() {
            // given - 2아웃 상태에서 삼진, Undo 후 2아웃으로 복원
            val game =
                createGame(1L, GameStatus.IN_PROGRESS).apply {
                    gameState.outs = 3
                }
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)

            val battingRecord =
                createBattingRecord(batter).apply {
                    applyPlateAppearanceResult(PlateAppearanceResult.STRIKEOUT, 0)
                }
            val pitchingRecord =
                createPitchingRecord(pitcher).apply {
                    applyBatterFaced(PlateAppearanceResult.STRIKEOUT)
                    recordOut()
                }

            val event =
                createPlateAppearanceEvent(
                    game = game,
                    batter = batter,
                    pitcher = pitcher,
                    result = PlateAppearanceResult.STRIKEOUT,
                    outCountBefore = 2,
                    outCountAfter = 3,
                    runnersBeforeJson = "1루:30,2루:31",
                    runsScored = 0,
                )

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameScorerService.undoLastEvent(1L)

            // then
            assertThat(event.undone).isTrue()
            assertThat(game.gameState.outs).isEqualTo(2) // 2아웃으로 복원
            assertThat(battingRecord.strikeouts).isEqualTo(0)
            assertThat(pitchingRecord.strikeouts).isEqualTo(0)
            // 주자 상태 복원
            assertThat(game.gameState.runnerOnFirstId).isEqualTo(30L)
            assertThat(game.gameState.runnerOnSecondId).isEqualTo(31L)
        }

        @Test
        fun `should undo when game status is finished and throw UndoNotAvailableException`() {
            // given - 종료된 경기는 Undo 불가
            val game = createGame(1L, GameStatus.FINISHED)
            every { gameRepository.findByIdOrNull(1L) } returns game

            // when & then
            assertThatThrownBy { gameScorerService.undoLastEvent(1L) }
                .isInstanceOf(UndoNotAvailableException::class.java)
        }

        @Test
        fun `should undo when game status is forfeited and throw UndoNotAvailableException`() {
            // given - 몰수 처리된 경기는 Undo 불가
            val game = createGame(1L, GameStatus.FORFEITED)
            every { gameRepository.findByIdOrNull(1L) } returns game

            // when & then
            assertThatThrownBy { gameScorerService.undoLastEvent(1L) }
                .isInstanceOf(UndoNotAvailableException::class.java)
        }

        @Test
        fun `should undo single with rbi and revert rbi and team hit`() {
            // given - 단타 + 타점: 3루 주자 득점
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>(relaxed = true)
            every { batter.gameTeam } returns gameTeam

            val battingRecord =
                createBattingRecord(batter).apply {
                    applyPlateAppearanceResult(PlateAppearanceResult.SINGLE, 1)
                }
            val pitchingRecord =
                createPitchingRecord(pitcher).apply {
                    applyBatterFaced(PlateAppearanceResult.SINGLE)
                    recordEarnedRun(1)
                }

            val event =
                createPlateAppearanceEvent(
                    game = game,
                    batter = batter,
                    pitcher = pitcher,
                    result = PlateAppearanceResult.SINGLE,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    runnersBeforeJson = "3루:32",
                    runsScored = 1,
                    rbis = 1,
                )

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameScorerService.undoLastEvent(1L)

            // then
            assertThat(event.undone).isTrue()
            assertThat(battingRecord.hits).isEqualTo(0)
            assertThat(battingRecord.runsBattedIn).isEqualTo(0)
            assertThat(pitchingRecord.hitsAllowed).isEqualTo(0)
            assertThat(pitchingRecord.earnedRuns).isEqualTo(0)
            assertThat(game.gameState.runnerOnThirdId).isEqualTo(32L) // 3루 주자 복원
            verify { gameTeam.subtractRunInInning(any(), eq(1)) }
            verify { gameTeam.subtractHit() }
        }

        @Test
        fun `should undo multiple sequential strikeouts correctly`() {
            // given - 연속 2번의 삼진 중 두 번째 삼진 Undo
            val game =
                createGame(1L, GameStatus.IN_PROGRESS).apply {
                    gameState.outs = 2
                }
            val batter2 = createGamePlayer(11L)
            val pitcher = createGamePlayer(20L)

            val battingRecord2 =
                createBattingRecord(batter2).apply {
                    applyPlateAppearanceResult(PlateAppearanceResult.STRIKEOUT, 0)
                }
            val pitchingRecord =
                createPitchingRecord(pitcher).apply {
                    // 2번의 삼진이 기록된 투수
                    applyBatterFaced(PlateAppearanceResult.STRIKEOUT)
                    recordOut()
                    applyBatterFaced(PlateAppearanceResult.STRIKEOUT)
                    recordOut()
                }

            val event2 =
                createPlateAppearanceEvent(
                    game = game,
                    batter = batter2,
                    pitcher = pitcher,
                    result = PlateAppearanceResult.STRIKEOUT,
                    outCountBefore = 1,
                    outCountAfter = 2,
                    runnersBeforeJson = null,
                    runsScored = 0,
                )

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event2
            every { battingRecordRepository.findByGamePlayer(batter2) } returns battingRecord2
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameScorerService.undoLastEvent(1L)

            // then
            assertThat(event2.undone).isTrue()
            assertThat(battingRecord2.strikeouts).isEqualTo(0) // 두 번째 타자 삼진 취소
            assertThat(game.gameState.outs).isEqualTo(1) // 1아웃으로 복원
            // 투수: 1번 삼진만 남음
            assertThat(pitchingRecord.strikeouts).isEqualTo(1)
            assertThat(pitchingRecord.inningsPitchedOuts).isEqualTo(1)
        }
    }

    // Helper methods

    private fun createGamePlayer(id: Long): GamePlayer {
        val gamePlayer = mockk<GamePlayer>(relaxed = true)
        every { gamePlayer.id } returns id
        return gamePlayer
    }

    private fun createBattingRecord(gamePlayer: GamePlayer): BattingRecord = BattingRecord.create(gamePlayer)

    private fun createPitchingRecord(gamePlayer: GamePlayer): PitchingRecord = PitchingRecord.create(gamePlayer)

    private fun createPlateAppearanceEvent(
        game: Game,
        batter: GamePlayer,
        pitcher: GamePlayer,
        result: PlateAppearanceResult,
        outCountBefore: Int,
        outCountAfter: Int,
        runnersBeforeJson: String?,
        runsScored: Int,
        rbis: Int = 0,
    ): GameEvent {
        val event =
            GameEvent(
                game = game,
                inning = game.currentInning,
                isTopInning = game.isTopInning,
                outCountBefore = outCountBefore,
                outCountAfter = outCountAfter,
                eventType = GameEventType.PLATE_APPEARANCE,
                description = "${result.displayName}",
                batter = batter,
                pitcher = pitcher,
                runnersBeforeJson = runnersBeforeJson,
                plateAppearanceResult = result,
                runsScored = runsScored,
                rbis = rbis,
            )
        setEntityId(event, 1L)
        return event
    }

    private fun setEntityId(
        entity: Any,
        id: Long,
    ) {
        val idField = entity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
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
            setEntityId(this, id)
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
            setEntityId(this, id)
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
            setEntityId(this, id)
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
            currentInning = 1,
            isTopInning = true,
            totalInnings = 9,
            gameState = GameState(),
        ).apply {
            setEntityId(this, id)
        }
    }
}
