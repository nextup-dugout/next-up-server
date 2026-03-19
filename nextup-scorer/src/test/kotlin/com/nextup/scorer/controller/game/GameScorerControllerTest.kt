package com.nextup.scorer.controller.game

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.Base
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameEvent
import com.nextup.core.domain.game.GameEventType
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameState
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.service.game.BaseRunningRecordService
import com.nextup.core.service.game.GameLifecycleService
import com.nextup.core.service.game.GameStateQueryService
import com.nextup.core.service.game.GameSubstitutionService
import com.nextup.core.service.game.GameTimelineService
import com.nextup.core.service.game.GameUndoService
import com.nextup.core.service.game.PlateAppearanceRecordService
import com.nextup.core.service.game.dto.GameEndReason
import com.nextup.core.service.game.dto.GameTimelineDto
import com.nextup.core.service.game.dto.PlateAppearanceRecordResult
import com.nextup.core.service.game.dto.TimelineEventDto
import com.nextup.scorer.dto.game.GameEndRequestDto
import com.nextup.scorer.dto.game.PlateAppearanceRequestDto
import com.nextup.scorer.dto.game.RunnerMovementDto
import com.nextup.scorer.dto.game.SubstitutionRequestDto
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("GameScorerController")
class GameScorerControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var gameLifecycleService: GameLifecycleService
    private lateinit var plateAppearanceRecordService: PlateAppearanceRecordService
    private lateinit var gameUndoService: GameUndoService
    private lateinit var baseRunningRecordService: BaseRunningRecordService
    private lateinit var gameSubstitutionService: GameSubstitutionService
    private lateinit var gameStateQueryService: GameStateQueryService
    private lateinit var gameTimelineService: GameTimelineService
    private lateinit var controller: GameScorerController
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        gameLifecycleService = mockk()
        plateAppearanceRecordService = mockk()
        gameUndoService = mockk()
        baseRunningRecordService = mockk()
        gameSubstitutionService = mockk()
        gameStateQueryService = mockk()
        gameTimelineService = mockk()
        controller =
            GameScorerController(
                gameLifecycleService,
                plateAppearanceRecordService,
                gameUndoService,
                baseRunningRecordService,
                gameSubstitutionService,
                gameStateQueryService,
                gameTimelineService,
            )
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
        objectMapper = ObjectMapper().registerModule(JavaTimeModule())
    }

    @Nested
    @DisplayName("POST /api/scorer/games/{gameId}/start")
    inner class StartGame {

        @Test
        fun `should start game successfully`() {
            // given
            val gameId = 1L
            val scorerId = 100L
            val game =
                createGame(gameId, GameStatus.IN_PROGRESS).apply {
                    currentInning = 1
                    isTopInning = true
                }
            every { gameLifecycleService.startGame(gameId, scorerId) } returns game

            // when & then
            mockMvc.perform(
                post("/api/v1/scorer/games/$gameId/start")
                    .param("scorerId", scorerId.toString()),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(gameId))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.currentInning").value(1))
                .andExpect(jsonPath("$.data.isTopInning").value(true))

            verify(exactly = 1) { gameLifecycleService.startGame(gameId, scorerId) }
        }
    }

    @Nested
    @DisplayName("POST /api/scorer/games/{gameId}/plate-appearances")
    inner class RecordPlateAppearance {

        @Test
        fun `should record plate appearance with single hit`() {
            // given
            val gameId = 1L
            val scorerId = 100L
            val request =
                PlateAppearanceRequestDto(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                    runnerMovements = emptyList(),
                    rbis = 0,
                    balls = 2,
                    strikes = 1
                )
            val game =
                createGame(gameId, GameStatus.IN_PROGRESS).apply {
                    currentInning = 3
                    isTopInning = false
                    gameState.runnerOnFirstId = 10L
                }
            every { plateAppearanceRecordService.recordPlateAppearance(gameId, any(), scorerId) } returns
                PlateAppearanceRecordResult(game)

            // when & then
            mockMvc.perform(
                post("/api/v1/scorer/games/$gameId/plate-appearances")
                    .param("scorerId", scorerId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(gameId))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.currentInning").value(3))

            verify(exactly = 1) { plateAppearanceRecordService.recordPlateAppearance(gameId, any(), scorerId) }
        }

        @Test
        fun `should record plate appearance with runner movements`() {
            // given
            val gameId = 1L
            val scorerId = 100L
            val request =
                PlateAppearanceRequestDto(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.DOUBLE,
                    runnerMovements =
                        listOf(
                            RunnerMovementDto(
                                runnerId = 5L,
                                fromBase = Base.FIRST,
                                toBase = Base.THIRD,
                                isOut = false
                            )
                        ),
                    rbis = 1,
                    balls = 0,
                    strikes = 2
                )
            val game =
                createGame(gameId, GameStatus.IN_PROGRESS).apply {
                    currentInning = 5
                    isTopInning = true
                    gameState.runnerOnSecondId = 10L
                    gameState.runnerOnThirdId = 5L
                }
            every { plateAppearanceRecordService.recordPlateAppearance(gameId, any(), scorerId) } returns
                PlateAppearanceRecordResult(game)

            // when & then
            mockMvc.perform(
                post("/api/v1/scorer/games/$gameId/plate-appearances")
                    .param("scorerId", scorerId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(gameId))
                .andExpect(jsonPath("$.data.gameState.runnerOnSecondId").value(10))
                .andExpect(jsonPath("$.data.gameState.runnerOnThirdId").value(5))

            verify(exactly = 1) { plateAppearanceRecordService.recordPlateAppearance(gameId, any(), scorerId) }
        }

        @Test
        fun `should record strikeout`() {
            // given
            val gameId = 1L
            val scorerId = 100L
            val request =
                PlateAppearanceRequestDto(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.STRIKEOUT,
                    runnerMovements = emptyList(),
                    rbis = 0,
                    balls = 1,
                    strikes = 3
                )
            val game =
                createGame(gameId, GameStatus.IN_PROGRESS).apply {
                    currentInning = 2
                    isTopInning = true
                    gameState.outs = 1
                }
            every { plateAppearanceRecordService.recordPlateAppearance(gameId, any(), scorerId) } returns
                PlateAppearanceRecordResult(game)

            // when & then
            mockMvc.perform(
                post("/api/v1/scorer/games/$gameId/plate-appearances")
                    .param("scorerId", scorerId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.gameState.outs").value(1))

            verify(exactly = 1) { plateAppearanceRecordService.recordPlateAppearance(gameId, any(), scorerId) }
        }
    }

    @Nested
    @DisplayName("POST /api/scorer/games/{gameId}/half-inning")
    inner class AdvanceHalfInning {

        @Test
        fun `should advance to next half inning`() {
            // given
            val gameId = 1L
            val scorerId = 100L
            val game =
                createGame(gameId, GameStatus.IN_PROGRESS).apply {
                    currentInning = 1
                    isTopInning = false // 1회말로 진행
                    gameState.resetForNewInning()
                }
            every { gameLifecycleService.advanceHalfInning(gameId, scorerId) } returns game

            // when & then
            mockMvc.perform(
                post("/api/v1/scorer/games/$gameId/half-inning")
                    .param("scorerId", scorerId.toString()),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(gameId))
                .andExpect(jsonPath("$.data.currentInning").value(1))
                .andExpect(jsonPath("$.data.isTopInning").value(false))
                .andExpect(jsonPath("$.data.gameState.outs").value(0))

            verify(exactly = 1) { gameLifecycleService.advanceHalfInning(gameId, scorerId) }
        }

        @Test
        fun `should advance from bottom to next top inning`() {
            // given
            val gameId = 1L
            val scorerId = 100L
            val game =
                createGame(gameId, GameStatus.IN_PROGRESS).apply {
                    currentInning = 2
                    isTopInning = true // 2회초로 진행
                    gameState.resetForNewInning()
                }
            every { gameLifecycleService.advanceHalfInning(gameId, scorerId) } returns game

            // when & then
            mockMvc.perform(
                post("/api/v1/scorer/games/$gameId/half-inning")
                    .param("scorerId", scorerId.toString()),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentInning").value(2))
                .andExpect(jsonPath("$.data.isTopInning").value(true))

            verify(exactly = 1) { gameLifecycleService.advanceHalfInning(gameId, scorerId) }
        }
    }

    @Nested
    @DisplayName("POST /api/scorer/games/{gameId}/end")
    inner class EndGame {

        @Test
        fun `should end game with regulation finish`() {
            // given
            val gameId = 1L
            val scorerId = 100L
            val request = GameEndRequestDto(reason = GameEndReason.REGULATION)
            val game =
                createGame(gameId, GameStatus.FINISHED).apply {
                    currentInning = 9
                    isTopInning = false
                }
            every { gameLifecycleService.endGame(gameId, GameEndReason.REGULATION, scorerId) } returns game

            // when & then
            mockMvc.perform(
                post("/api/v1/scorer/games/$gameId/end")
                    .param("scorerId", scorerId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(gameId))
                .andExpect(jsonPath("$.data.status").value("FINISHED"))

            verify(exactly = 1) { gameLifecycleService.endGame(gameId, GameEndReason.REGULATION, scorerId) }
        }

        @Test
        fun `should end game with mercy rule`() {
            // given
            val gameId = 1L
            val scorerId = 100L
            val request = GameEndRequestDto(reason = GameEndReason.MERCY_RULE)
            val game =
                createGame(gameId, GameStatus.CALLED).apply {
                    currentInning = 7
                    isTopInning = true
                }
            every { gameLifecycleService.endGame(gameId, GameEndReason.MERCY_RULE, scorerId) } returns game

            // when & then
            mockMvc.perform(
                post("/api/v1/scorer/games/$gameId/end")
                    .param("scorerId", scorerId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CALLED"))

            verify(exactly = 1) { gameLifecycleService.endGame(gameId, GameEndReason.MERCY_RULE, scorerId) }
        }

        @Test
        fun `should end game due to weather`() {
            // given
            val gameId = 1L
            val scorerId = 100L
            val request = GameEndRequestDto(reason = GameEndReason.WEATHER)
            val game =
                createGame(gameId, GameStatus.CALLED).apply {
                    currentInning = 5
                    isTopInning = false
                }
            every { gameLifecycleService.endGame(gameId, GameEndReason.WEATHER, scorerId) } returns game

            // when & then
            mockMvc.perform(
                post("/api/v1/scorer/games/$gameId/end")
                    .param("scorerId", scorerId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CALLED"))

            verify(exactly = 1) { gameLifecycleService.endGame(gameId, GameEndReason.WEATHER, scorerId) }
        }

        @Test
        fun `should end game with forfeit`() {
            // given
            val gameId = 1L
            val scorerId = 100L
            val request = GameEndRequestDto(reason = GameEndReason.FORFEIT)
            val game =
                createGame(gameId, GameStatus.FORFEITED).apply {
                    currentInning = 3
                    isTopInning = true
                }
            every { gameLifecycleService.endGame(gameId, GameEndReason.FORFEIT, scorerId) } returns game

            // when & then
            mockMvc.perform(
                post("/api/v1/scorer/games/$gameId/end")
                    .param("scorerId", scorerId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("FORFEITED"))

            verify(exactly = 1) { gameLifecycleService.endGame(gameId, GameEndReason.FORFEIT, scorerId) }
        }
    }

    @Nested
    @DisplayName("POST /api/scorer/games/{gameId}/cancel")
    inner class CancelGame {

        @Test
        fun `should cancel game successfully`() {
            // given
            val gameId = 1L
            val scorerId = 100L
            val game = createGame(gameId, GameStatus.CANCELLED)
            every { gameLifecycleService.cancelGame(gameId, "우천 취소", scorerId) } returns game

            // when & then
            mockMvc.perform(
                post("/api/v1/scorer/games/$gameId/cancel")
                    .param("scorerId", scorerId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf("reason" to "우천 취소")))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(gameId))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))

            verify(exactly = 1) { gameLifecycleService.cancelGame(gameId, "우천 취소", scorerId) }
        }

        @Test
        fun `should cancel game without reason`() {
            // given
            val gameId = 1L
            val scorerId = 100L
            val game = createGame(gameId, GameStatus.CANCELLED)
            every { gameLifecycleService.cancelGame(gameId, null, scorerId) } returns game

            // when & then
            mockMvc.perform(
                post("/api/v1/scorer/games/$gameId/cancel")
                    .param("scorerId", scorerId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf<String, String?>()))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))

            verify(exactly = 1) { gameLifecycleService.cancelGame(gameId, null, scorerId) }
        }
    }

    @Nested
    @DisplayName("POST /api/scorer/games/{gameId}/substitutions")
    inner class SubstitutePlayer {

        @Test
        fun `선수 교체 요청이 성공적으로 처리된다`() {
            // given
            val gameId = 1L
            val scorerId = 100L
            val game =
                createGame(gameId, GameStatus.IN_PROGRESS).apply {
                    currentInning = 5
                    isTopInning = true
                }
            val incomingPlayer = mockk<GamePlayer>(relaxed = true)
            val outgoingPlayer = mockk<GamePlayer>(relaxed = true)
            every { incomingPlayer.id } returns 20L
            every { outgoingPlayer.id } returns 10L

            val substitutionEvent =
                GameEvent(
                    game = game,
                    inning = 5,
                    isTopInning = true,
                    outCountBefore = 1,
                    outCountAfter = 1,
                    eventType = GameEventType.SUBSTITUTION,
                    description = "5회초: 홍길동 → 김철수 (좌익수)",
                    batter = incomingPlayer,
                    pitcher = outgoingPlayer,
                ).apply {
                    val idField = GameEvent::class.java.getDeclaredField("id")
                    idField.isAccessible = true
                    idField.set(this, 100L)
                }

            every { gameSubstitutionService.substitutePlayer(gameId, any(), scorerId) } returns substitutionEvent

            val request =
                SubstitutionRequestDto(
                    gameTeamId = 1L,
                    outgoingPlayerId = 10L,
                    incomingPlayerId = 20L,
                    newPosition = Position.LEFT_FIELD,
                    newBattingOrder = 5,
                )

            // when & then
            mockMvc.perform(
                post("/api/v1/scorer/games/$gameId/substitutions")
                    .param("scorerId", scorerId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventId").value(100))
                .andExpect(jsonPath("$.data.inning").value(5))
                .andExpect(jsonPath("$.data.isTopInning").value(true))
                .andExpect(jsonPath("$.data.description").value("5회초: 홍길동 → 김철수 (좌익수)"))

            verify(exactly = 1) { gameSubstitutionService.substitutePlayer(gameId, any(), scorerId) }
        }
    }

    // ===== GET 조회 엔드포인트 테스트 (H-16, M-25) =====

    @Nested
    @DisplayName("GET /api/scorer/games/{gameId}/state")
    inner class GetGameState {

        @Test
        fun `현재 경기 상태를 조회한다`() {
            // given
            val gameId = 1L
            val game =
                createGame(gameId, GameStatus.IN_PROGRESS).apply {
                    currentInning = 5
                    isTopInning = false
                    gameState.outs = 2
                    gameState.balls = 3
                    gameState.strikes = 1
                    gameState.runnerOnFirstId = 10L
                }

            every { gameStateQueryService.getGame(gameId) } returns game
            every { gameStateQueryService.getGameTeams(gameId) } returns game.gameTeams

            // when & then
            mockMvc.perform(get("/api/v1/scorer/games/$gameId/state"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.gameId").value(gameId))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.currentInning").value(5))
                .andExpect(jsonPath("$.data.isTopInning").value(false))
                .andExpect(jsonPath("$.data.currentInningDisplay").value("5회말"))
                .andExpect(jsonPath("$.data.totalInnings").value(9))
                .andExpect(jsonPath("$.data.gameState.outs").value(2))
                .andExpect(jsonPath("$.data.gameState.balls").value(3))
                .andExpect(jsonPath("$.data.gameState.strikes").value(1))
                .andExpect(jsonPath("$.data.gameState.runnerOnFirstId").value(10))
                .andExpect(jsonPath("$.data.homeTeam.teamName").value("홈팀"))
                .andExpect(jsonPath("$.data.awayTeam.teamName").value("원정팀"))

            verify(exactly = 1) { gameStateQueryService.getGame(gameId) }
            verify(exactly = 1) { gameStateQueryService.getGameTeams(gameId) }
        }

        @Test
        fun `경기 시작 전 상태를 조회한다`() {
            // given
            val gameId = 1L
            val game = createGame(gameId, GameStatus.SCHEDULED)

            every { gameStateQueryService.getGame(gameId) } returns game
            every { gameStateQueryService.getGameTeams(gameId) } returns game.gameTeams

            // when & then
            mockMvc.perform(get("/api/v1/scorer/games/$gameId/state"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.currentInning").value(1))

            verify(exactly = 1) { gameStateQueryService.getGame(gameId) }
        }
    }

    @Nested
    @DisplayName("GET /api/scorer/games/{gameId}/lineup")
    inner class GetCurrentLineup {

        @Test
        fun `현재 라인업을 조회한다`() {
            // given
            val gameId = 1L
            val game = createGame(gameId, GameStatus.IN_PROGRESS)
            val homeTeam = game.gameTeams.first { it.homeAway == HomeAway.HOME }
            val awayTeam = game.gameTeams.first { it.homeAway == HomeAway.AWAY }

            val homePlayer = createGamePlayer(1L, homeTeam, "홍길동", Position.STARTING_PITCHER, 1, 18)
            val awayPlayer = createGamePlayer(2L, awayTeam, "김철수", Position.CATCHER, 4, 22)

            every { gameStateQueryService.getCurrentLineup(gameId) } returns listOf(homePlayer, awayPlayer)

            // when & then
            mockMvc.perform(get("/api/v1/scorer/games/$gameId/lineup"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.gameId").value(gameId))
                .andExpect(jsonPath("$.data.homeLineup").isArray)
                .andExpect(jsonPath("$.data.homeLineup[0].playerName").value("홍길동"))
                .andExpect(jsonPath("$.data.homeLineup[0].position").value("STARTING_PITCHER"))
                .andExpect(jsonPath("$.data.homeLineup[0].battingOrder").value(1))
                .andExpect(jsonPath("$.data.homeLineup[0].backNumber").value(18))
                .andExpect(jsonPath("$.data.awayLineup").isArray)
                .andExpect(jsonPath("$.data.awayLineup[0].playerName").value("김철수"))

            verify(exactly = 1) { gameStateQueryService.getCurrentLineup(gameId) }
        }

        @Test
        fun `라인업이 비어있을 때 빈 배열을 반환한다`() {
            // given
            val gameId = 1L
            every { gameStateQueryService.getCurrentLineup(gameId) } returns emptyList()

            // when & then
            mockMvc.perform(get("/api/v1/scorer/games/$gameId/lineup"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.homeLineup").isEmpty)
                .andExpect(jsonPath("$.data.awayLineup").isEmpty)

            verify(exactly = 1) { gameStateQueryService.getCurrentLineup(gameId) }
        }
    }

    @Nested
    @DisplayName("GET /api/scorer/games/{gameId}/events")
    inner class GetEventTimeline {

        @Test
        fun `이벤트 타임라인을 조회한다`() {
            // given
            val gameId = 1L
            val timeline =
                GameTimelineDto(
                    gameId = gameId,
                    events =
                        listOf(
                            TimelineEventDto(
                                eventId = 1L,
                                inning = 1,
                                isTopInning = true,
                                inningDisplay = "1회초",
                                eventType = "타석 결과",
                                description = "홍길동 안타",
                                batterId = 10L,
                                batterName = "홍길동",
                                pitcherId = 20L,
                                pitcherName = "김투수",
                                plateAppearanceResult = "안타",
                                runsScored = 0,
                                outCountBefore = 0,
                                outCountAfter = 0,
                                eventTimestamp = Instant.parse("2025-04-15T05:00:00Z"),
                            ),
                        ),
                    totalEvents = 1,
                )

            every { gameTimelineService.getTimeline(gameId, null, null) } returns timeline

            // when & then
            mockMvc.perform(get("/api/v1/scorer/games/$gameId/events"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.gameId").value(gameId))
                .andExpect(jsonPath("$.data.totalEvents").value(1))
                .andExpect(jsonPath("$.data.events[0].eventId").value(1))
                .andExpect(jsonPath("$.data.events[0].inning").value(1))
                .andExpect(jsonPath("$.data.events[0].isTopInning").value(true))
                .andExpect(jsonPath("$.data.events[0].inningDisplay").value("1회초"))
                .andExpect(jsonPath("$.data.events[0].description").value("홍길동 안타"))

            verify(exactly = 1) { gameTimelineService.getTimeline(gameId, null, null) }
        }

        @Test
        fun `이닝 범위를 지정하여 타임라인을 조회한다`() {
            // given
            val gameId = 1L
            val timeline =
                GameTimelineDto(
                    gameId = gameId,
                    events = emptyList(),
                    totalEvents = 0,
                )

            every { gameTimelineService.getTimeline(gameId, 3, 5) } returns timeline

            // when & then
            mockMvc.perform(
                get("/api/v1/scorer/games/$gameId/events")
                    .param("fromInning", "3")
                    .param("toInning", "5"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalEvents").value(0))
                .andExpect(jsonPath("$.data.events").isEmpty)

            verify(exactly = 1) { gameTimelineService.getTimeline(gameId, 3, 5) }
        }
    }

    @Nested
    @DisplayName("GET /api/scorer/games/{gameId}/scoreboard")
    inner class GetScoreboard {

        @Test
        fun `스코어보드를 조회한다`() {
            // given
            val gameId = 1L
            val game =
                createGame(gameId, GameStatus.IN_PROGRESS).apply {
                    currentInning = 3
                    isTopInning = true
                }

            every { gameStateQueryService.getGame(gameId) } returns game
            every { gameStateQueryService.getGameTeams(gameId) } returns game.gameTeams

            // when & then
            mockMvc.perform(get("/api/v1/scorer/games/$gameId/scoreboard"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.gameId").value(gameId))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.currentInning").value(3))
                .andExpect(jsonPath("$.data.isTopInning").value(true))
                .andExpect(jsonPath("$.data.currentInningDisplay").value("3회초"))
                .andExpect(jsonPath("$.data.homeTeam.teamName").value("홈팀"))
                .andExpect(jsonPath("$.data.homeTeam.runs").value(0))
                .andExpect(jsonPath("$.data.homeTeam.hits").value(0))
                .andExpect(jsonPath("$.data.homeTeam.errors").value(0))
                .andExpect(jsonPath("$.data.awayTeam.teamName").value("원정팀"))
                .andExpect(jsonPath("$.data.awayTeam.runs").value(0))

            verify(exactly = 1) { gameStateQueryService.getGame(gameId) }
            verify(exactly = 1) { gameStateQueryService.getGameTeams(gameId) }
        }
    }

    @Nested
    @DisplayName("POST /api/scorer/games/{gameId}/lock")
    inner class LockGame {

        @Test
        fun `경기를 잠금한다`() {
            // given
            val gameId = 1L
            val scorerId = 100L
            val game = createGame(gameId, GameStatus.SCHEDULED)
            game.lockForScorer(scorerId)
            every { gameLifecycleService.lockGame(gameId, scorerId) } returns game

            // when & then
            mockMvc.perform(
                post("/api/v1/scorer/games/$gameId/lock")
                    .param("scorerId", scorerId.toString()),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(gameId))

            verify(exactly = 1) { gameLifecycleService.lockGame(gameId, scorerId) }
        }
    }

    @Nested
    @DisplayName("POST /api/scorer/games/{gameId}/unlock")
    inner class UnlockGame {

        @Test
        fun `경기 잠금을 해제한다`() {
            // given
            val gameId = 1L
            val scorerId = 100L
            val game = createGame(gameId, GameStatus.SCHEDULED)
            every { gameLifecycleService.unlockGame(gameId, scorerId) } returns game

            // when & then
            mockMvc.perform(
                post("/api/v1/scorer/games/$gameId/unlock")
                    .param("scorerId", scorerId.toString()),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(gameId))

            verify(exactly = 1) { gameLifecycleService.unlockGame(gameId, scorerId) }
        }
    }

    @Nested
    @DisplayName("POST /api/scorer/games/{gameId}/forfeit")
    inner class ForfeitGame {

        @Test
        fun `경기를 몰수 처리한다`() {
            // given
            val gameId = 1L
            val scorerId = 100L
            val game = createGame(gameId, GameStatus.FORFEITED)
            every {
                gameLifecycleService.forfeitGame(
                    gameId = gameId,
                    winnerTeamId = 1L,
                    reason = "선수 부족",
                    scorerId = scorerId,
                )
            } returns game

            val request =
                mapOf(
                    "winnerTeamId" to 1L,
                    "reason" to "선수 부족",
                )

            // when & then
            mockMvc.perform(
                post("/api/v1/scorer/games/$gameId/forfeit")
                    .param("scorerId", scorerId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(gameId))
                .andExpect(jsonPath("$.data.status").value("FORFEITED"))

            verify(exactly = 1) {
                gameLifecycleService.forfeitGame(
                    gameId = gameId,
                    winnerTeamId = 1L,
                    reason = "선수 부족",
                    scorerId = scorerId,
                )
            }
        }
    }

    // ===== 테스트 헬퍼 메서드 =====

    @Nested
    @DisplayName("POST /api/scorer/games/{gameId}/suspend")
    inner class SuspendGame {
        @Test
        fun `should suspend game successfully`() {
            // given
            val scorerId = 100L
            val game = createGame(1L, GameStatus.SUSPENDED)
            every { gameLifecycleService.suspendGame(1L, "우천", scorerId) } returns game

            // when & then
            mockMvc.perform(
                post("/api/v1/scorer/games/1/suspend")
                    .param("scorerId", scorerId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf("reason" to "우천"))),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"))

            verify(exactly = 1) { gameLifecycleService.suspendGame(1L, "우천", scorerId) }
        }

        @Test
        fun `should suspend game without reason`() {
            // given
            val scorerId = 100L
            val game = createGame(1L, GameStatus.SUSPENDED)
            every { gameLifecycleService.suspendGame(1L, null, scorerId) } returns game

            // when & then
            mockMvc.perform(
                post("/api/v1/scorer/games/1/suspend")
                    .param("scorerId", scorerId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"))

            verify(exactly = 1) { gameLifecycleService.suspendGame(1L, null, scorerId) }
        }

        @Test
        fun `should suspend game without request body`() {
            // given
            val scorerId = 100L
            val game = createGame(1L, GameStatus.SUSPENDED)
            every { gameLifecycleService.suspendGame(1L, null, scorerId) } returns game

            // when & then
            mockMvc.perform(
                post("/api/v1/scorer/games/1/suspend")
                    .param("scorerId", scorerId.toString()),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"))

            verify(exactly = 1) { gameLifecycleService.suspendGame(1L, null, scorerId) }
        }
    }

    @Nested
    @DisplayName("POST /api/scorer/games/{gameId}/resume")
    inner class ResumeGame {
        @Test
        fun `should resume game successfully`() {
            // given
            val scorerId = 100L
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            every { gameLifecycleService.resumeGame(1L, scorerId) } returns game

            // when & then
            mockMvc.perform(
                post("/api/v1/scorer/games/1/resume")
                    .param("scorerId", scorerId.toString()),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))

            verify(exactly = 1) { gameLifecycleService.resumeGame(1L, scorerId) }
        }
    }

    private fun createGamePlayer(
        id: Long,
        gameTeam: GameTeam,
        name: String,
        position: Position,
        battingOrder: Int,
        backNumber: Int,
    ): GamePlayer {
        val player =
            Player(
                name = name,
                primaryPosition = position,
            ).apply {
                val idField = Player::class.java.getDeclaredField("id")
                idField.isAccessible = true
                idField.set(this, id * 100)
            }
        return GamePlayer(
            gameTeam = gameTeam,
            player = player,
            position = position,
            battingOrder = battingOrder,
            backNumber = backNumber,
            id = id,
        )
    }

    private fun createAssociation(
        id: Long,
        name: String
    ): Association {
        return Association(
            name = name,
            abbreviation = null,
            region = "서울",
            description = null,
            logoUrl = null,
            websiteUrl = null
        ).apply {
            val idField = Association::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
    }

    private fun createLeague(
        id: Long,
        name: String,
        association: Association
    ): League {
        return League(
            association = association,
            name = name,
            abbreviation = null,
            foundedYear = 2020,
            divisionLevel = 1,
            description = null,
            logoUrl = null
        ).apply {
            val idField = League::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
    }

    private fun createCompetition(
        id: Long,
        name: String,
        league: League
    ): Competition {
        return Competition(
            league = league,
            name = name,
            year = 2025,
            season = 1,
            type = CompetitionType.LEAGUE,
            startDate = LocalDate.of(2025, 3, 1),
            endDate = LocalDate.of(2025, 6, 30),
            status = CompetitionStatus.IN_PROGRESS,
            description = null,
            maxTeams = null
        ).apply {
            val idField = Competition::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
    }

    private fun createGame(
        id: Long,
        status: GameStatus
    ): Game {
        val association = createAssociation(1L, "서울시야구협회")
        val league = createLeague(1L, "1부 리그", association)
        val competition = createCompetition(1L, "2025 춘계대회", league)

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
            id = id,
        )
    }
}
