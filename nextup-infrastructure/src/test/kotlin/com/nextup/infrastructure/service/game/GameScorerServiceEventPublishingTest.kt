package com.nextup.infrastructure.service.game

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.event.PlateAppearanceRecordedEvent
import com.nextup.core.domain.event.PlateAppearanceUndoneEvent
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
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.service.game.BoxScoreService
import com.nextup.core.service.game.dto.PlateAppearanceRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("PlateAppearanceRecordServiceImpl / GameUndoServiceImpl - 이벤트 발행 테스트")
class GameScorerServiceEventPublishingTest {
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var gamePlayerRepository: GamePlayerRepositoryPort
    private lateinit var boxScoreService: BoxScoreService
    private lateinit var gameEventRepository: GameEventRepositoryPort
    private lateinit var battingRecordRepository: BattingRecordRepositoryPort
    private lateinit var pitchingRecordRepository: PitchingRecordRepositoryPort
    private lateinit var gameTeamRepository: GameTeamRepositoryPort
    private lateinit var eventPublisher: ApplicationEventPublisher
    private lateinit var plateAppearanceRecordService: PlateAppearanceRecordServiceImpl
    private lateinit var gameUndoService: GameUndoServiceImpl

    @BeforeEach
    fun setUp() {
        gameRepository = mockk()
        gamePlayerRepository = mockk()
        boxScoreService = mockk(relaxed = true)
        gameEventRepository = mockk()
        battingRecordRepository = mockk()
        pitchingRecordRepository = mockk()
        gameTeamRepository = mockk()
        eventPublisher = mockk(relaxed = true)
        every { pitchingRecordRepository.findByGamePlayer(any()) } returns null
        every { gameTeamRepository.findAllByGameId(any()) } returns emptyList()
        every { gameEventRepository.save(any()) } answers { firstArg() }
        plateAppearanceRecordService =
            PlateAppearanceRecordServiceImpl(
                gameRepository,
                gamePlayerRepository,
                boxScoreService,
                gameEventRepository,
                battingRecordRepository,
                pitchingRecordRepository,
                gameTeamRepository,
                eventPublisher,
            )
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
    @DisplayName("recordPlateAppearance - PlateAppearanceRecordedEvent 발행")
    inner class RecordPlateAppearanceEventPublishing {
        @Test
        fun `단타 기록 시 PlateAppearanceRecordedEvent 발행됨`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L, playerId = 100L)
            val pitcher = createGamePlayer(20L, playerId = 200L)
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
                    balls = 0,
                    strikes = 0,
                )

            // when
            plateAppearanceRecordService.recordPlateAppearance(1L, request, 999L)

            // then
            val eventSlot = slot<PlateAppearanceRecordedEvent>()
            verify { eventPublisher.publishEvent(capture(eventSlot)) }
            assertThat(eventSlot.captured.gameId).isEqualTo(1L)
            assertThat(eventSlot.captured.playerId).isEqualTo(100L)
            assertThat(eventSlot.captured.result).isEqualTo(PlateAppearanceResult.SINGLE)
        }

        @Test
        fun `홈런 기록 시 PlateAppearanceRecordedEvent 발행됨`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L, playerId = 100L)
            val pitcher = createGamePlayer(20L, playerId = 200L)
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
            val eventSlot = slot<PlateAppearanceRecordedEvent>()
            verify { eventPublisher.publishEvent(capture(eventSlot)) }
            assertThat(eventSlot.captured.result).isEqualTo(PlateAppearanceResult.HOME_RUN)
        }

        @Test
        fun `볼넷 기록 시 PlateAppearanceRecordedEvent 발행됨`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L, playerId = 100L)
            val pitcher = createGamePlayer(20L, playerId = 200L)
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
            plateAppearanceRecordService.recordPlateAppearance(1L, request, 999L)

            // then
            verify { eventPublisher.publishEvent(any<PlateAppearanceRecordedEvent>()) }
        }

        @Test
        fun `삼진 기록 시 PlateAppearanceRecordedEvent 발행됨`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L, playerId = 100L)
            val pitcher = createGamePlayer(20L, playerId = 200L)
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
            plateAppearanceRecordService.recordPlateAppearance(1L, request, 999L)

            // then
            val eventSlot = slot<PlateAppearanceRecordedEvent>()
            verify { eventPublisher.publishEvent(capture(eventSlot)) }
            assertThat(eventSlot.captured.result).isEqualTo(PlateAppearanceResult.STRIKEOUT)
        }
    }

    @Nested
    @DisplayName("undoLastEvent - PlateAppearanceUndoneEvent 발행")
    inner class UndoLastEventPublishing {
        @Test
        fun `타석 결과 Undo 시 PlateAppearanceUndoneEvent 발행됨`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L, playerId = 100L)
            val pitcher = createGamePlayer(20L, playerId = 200L)

            val battingRecord =
                BattingRecord.create(batter).apply {
                    applyPlateAppearanceResult(PlateAppearanceResult.SINGLE, 0)
                }
            val pitchingRecord =
                PitchingRecord.create(pitcher).apply {
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
            gameUndoService.undoLastEvent(1L, 999L)

            // then
            val eventSlot = slot<PlateAppearanceUndoneEvent>()
            verify { eventPublisher.publishEvent(capture(eventSlot)) }
            assertThat(eventSlot.captured.gameId).isEqualTo(1L)
            assertThat(eventSlot.captured.playerId).isEqualTo(100L)
            assertThat(eventSlot.captured.result).isEqualTo(PlateAppearanceResult.SINGLE)
        }

        @Test
        fun `홈런 Undo 시 PlateAppearanceUndoneEvent 발행됨`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L, playerId = 100L)
            val pitcher = createGamePlayer(20L, playerId = 200L)
            val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>(relaxed = true)
            every { batter.gameTeam } returns gameTeam

            val battingRecord =
                BattingRecord.create(batter).apply {
                    applyPlateAppearanceResult(PlateAppearanceResult.HOME_RUN, 1)
                }
            val pitchingRecord =
                PitchingRecord.create(pitcher).apply {
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
            gameUndoService.undoLastEvent(1L, 999L)

            // then
            val eventSlot = slot<PlateAppearanceUndoneEvent>()
            verify { eventPublisher.publishEvent(capture(eventSlot)) }
            assertThat(eventSlot.captured.result).isEqualTo(PlateAppearanceResult.HOME_RUN)
        }

        @Test
        fun `이닝 전환 Undo 시 PlateAppearanceUndoneEvent 발행 안 됨`() {
            // given
            val game =
                createGame(1L, GameStatus.IN_PROGRESS).apply {
                    currentInning = 1
                    isTopInning = false
                    gameState.outs = 0
                }

            val event =
                GameEvent(
                    game = game,
                    inning = 1,
                    isTopInning = true,
                    outCountBefore = 3,
                    outCountAfter = 0,
                    eventType = GameEventType.INNING_CHANGE,
                    description = "1회초 종료",
                )
            setEntityId(event, 100L)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameUndoService.undoLastEvent(1L, 999L)

            // then - PlateAppearanceUndoneEvent 는 발행되지 않아야 함
            verify(exactly = 0) { eventPublisher.publishEvent(any<PlateAppearanceUndoneEvent>()) }
        }

        @Test
        fun `GAME_STATUS 타입 Undo 시 PlateAppearanceUndoneEvent 발행 안 됨`() {
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
                    description = "경기 시작",
                )
            setEntityId(event, 200L)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameUndoService.undoLastEvent(1L, 999L)

            // then
            verify(exactly = 0) { eventPublisher.publishEvent(any<PlateAppearanceUndoneEvent>()) }
        }

        @Test
        fun `타석 이벤트이지만 batter가 null일 때 PlateAppearanceUndoneEvent 발행 안 됨`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)

            val event =
                GameEvent(
                    game = game,
                    inning = 1,
                    isTopInning = true,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    eventType = GameEventType.PLATE_APPEARANCE,
                    description = "타석 결과",
                    batter = null,
                    pitcher = null,
                    plateAppearanceResult = null,
                )
            setEntityId(event, 300L)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            gameUndoService.undoLastEvent(1L, 999L)

            // then - plateAppearanceResult가 null이므로 이벤트 발행 안 됨
            verify(exactly = 0) { eventPublisher.publishEvent(any<PlateAppearanceUndoneEvent>()) }
        }
    }

    // Helper methods

    private fun createGamePlayer(
        id: Long,
        playerId: Long = id * 10,
    ): GamePlayer {
        val player = mockk<com.nextup.core.domain.player.Player>(relaxed = true)
        every { player.id } returns playerId

        val gamePlayer = mockk<GamePlayer>(relaxed = true)
        every { gamePlayer.id } returns id
        every { gamePlayer.player } returns player
        return gamePlayer
    }

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
                description = result.displayName,
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
        ).apply { setEntityId(this, id) }

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
        ).apply { setEntityId(this, id) }

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
        ).apply { setEntityId(this, id) }

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
