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
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
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

@DisplayName("GameUndoServiceImpl - Undo")
class GameScorerServiceUndoTest {
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var gameEventRepository: GameEventRepositoryPort
    private lateinit var battingRecordRepository: BattingRecordRepositoryPort
    private lateinit var pitchingRecordRepository: PitchingRecordRepositoryPort
    private lateinit var eventPublisher: ApplicationEventPublisher
    private lateinit var gameUndoService: GameUndoServiceImpl

    @BeforeEach
    fun setUp() {
        gameRepository = mockk()
        gameEventRepository = mockk()
        battingRecordRepository = mockk()
        pitchingRecordRepository = mockk()
        eventPublisher = mockk(relaxed = true)
        gameUndoService =
            GameUndoServiceImpl(
                gameRepository,
                gameEventRepository,
                battingRecordRepository,
                pitchingRecordRepository,
                eventPublisher,
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
            val result = gameUndoService.undoLastEvent(1L, 999L)

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
            val result = gameUndoService.undoLastEvent(1L, 999L)

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
            gameUndoService.undoLastEvent(1L, 999L)

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
            assertThatThrownBy { gameUndoService.undoLastEvent(1L, 999L) }
                .isInstanceOf(UndoNotAvailableException::class.java)
        }

        @Test
        fun `should throw exception when no event to undo`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns null

            // when & then
            assertThatThrownBy { gameUndoService.undoLastEvent(1L, 999L) }
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
            gameUndoService.undoLastEvent(1L, 999L)

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
            gameUndoService.undoLastEvent(1L, 999L)

            // then
            assertThat(event.undone).isTrue()
            assertThat(game.currentInning).isEqualTo(1)
            assertThat(game.isTopInning).isTrue() // 1회초로 복원
            assertThat(game.gameState.outs).isEqualTo(3) // 3아웃 상태로 복원
        }

        @Test
        fun `should undo base running event with simple marking`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)

            val event =
                GameEvent(
                    game = game,
                    inning = 1,
                    isTopInning = true,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    eventType = GameEventType.BASE_RUNNING,
                    description = "도루 성공: 1루 → 2루",
                )
            setEntityId(event, 200L)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            val result = gameUndoService.undoLastEvent(1L, 999L)

            // then
            assertThat(result.undone).isTrue()
            assertThat(result.eventType).isEqualTo(GameEventType.BASE_RUNNING)
            verify { gameEventRepository.save(any()) }
            verify { gameRepository.save(any()) }
        }

        @Test
        fun `should undo game status event with simple marking`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)

            val event =
                GameEvent(
                    game = game,
                    inning = 1,
                    isTopInning = true,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    eventType = GameEventType.GAME_STATUS,
                    description = "경기 상태 변경",
                )
            setEntityId(event, 300L)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            val result = gameUndoService.undoLastEvent(1L, 999L)

            // then
            assertThat(result.undone).isTrue()
            assertThat(result.eventType).isEqualTo(GameEventType.GAME_STATUS)
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
            currentInning = 1,
            isTopInning = true,
            totalInnings = 9,
            gameState = GameState(),
            scorerId = 999L,
            id = id,
        )
    }
}
