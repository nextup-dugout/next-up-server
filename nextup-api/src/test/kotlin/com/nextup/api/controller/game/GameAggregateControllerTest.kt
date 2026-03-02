package com.nextup.api.controller.game

import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.service.game.GameAggregateService
import com.nextup.core.service.game.dto.GameAggregateDto
import com.nextup.core.service.game.dto.GameDetailDto
import com.nextup.core.service.game.dto.GameTimelineDto
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime

@DisplayName("GameAggregateController")
class GameAggregateControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var gameAggregateService: GameAggregateService

    @BeforeEach
    fun setUp() {
        gameAggregateService = mockk()
        val controller = GameAggregateController(gameAggregateService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Nested
    @DisplayName("GET /api/v1/games/{gameId}/aggregate")
    inner class GetGameAggregate {
        @Test
        fun `경기 상세 통합 데이터를 조회한다`() {
            // given
            val gameId = 1L
            val gameDetail =
                GameDetailDto(
                    gameId = gameId,
                    competitionId = 100L,
                    competitionName = "2026 사회인야구 리그",
                    homeTeamId = 10L,
                    homeTeamName = "Tigers",
                    awayTeamId = 20L,
                    awayTeamName = "Lions",
                    scheduledAt = LocalDateTime.of(2026, 5, 1, 14, 0),
                    status = GameStatus.FINISHED,
                    homeScore = 5,
                    awayScore = 3,
                    location = "서울",
                    fieldName = "잠실야구장",
                    gameNumber = 1,
                    currentInning = "경기 종료",
                    totalInnings = 9,
                    startedAt = LocalDateTime.of(2026, 5, 1, 14, 5),
                    endedAt = LocalDateTime.of(2026, 5, 1, 17, 0),
                    note = null,
                    forfeitReason = null,
                )
            val timeline = GameTimelineDto(gameId = gameId, events = emptyList(), totalEvents = 0)
            val aggregate =
                GameAggregateDto(
                    gameDetail = gameDetail,
                    boxScore = null,
                    timeline = timeline,
                    scoresheet = null,
                )

            every { gameAggregateService.getGameAggregate(gameId) } returns aggregate

            // when & then
            mockMvc
                .perform(get("/api/v1/games/{gameId}/aggregate", gameId))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.gameInfo.gameId").value(gameId))
                .andExpect(jsonPath("$.data.gameInfo.competitionName").value("2026 사회인야구 리그"))
                .andExpect(jsonPath("$.data.gameInfo.homeTeam.teamId").value(10))
                .andExpect(jsonPath("$.data.gameInfo.homeTeam.teamName").value("Tigers"))
                .andExpect(jsonPath("$.data.gameInfo.homeTeam.score").value(5))
                .andExpect(jsonPath("$.data.gameInfo.awayTeam.teamId").value(20))
                .andExpect(jsonPath("$.data.gameInfo.awayTeam.score").value(3))
                .andExpect(jsonPath("$.data.gameInfo.status").value("FINISHED"))
                .andExpect(jsonPath("$.data.timeline.totalEvents").value(0))
        }

        @Test
        fun `경기가 존재하지 않으면 404 응답`() {
            // given
            val gameId = 999L
            every { gameAggregateService.getGameAggregate(gameId) } throws GameNotFoundException(gameId)

            // when & then
            mockMvc
                .perform(get("/api/v1/games/{gameId}/aggregate", gameId))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("GAME_NOT_FOUND"))
        }

        @Test
        fun `박스스코어가 없으면 null로 반환된다`() {
            // given
            val gameId = 2L
            val gameDetail =
                GameDetailDto(
                    gameId = gameId,
                    competitionId = 100L,
                    competitionName = "2026 사회인야구 리그",
                    homeTeamId = 10L,
                    homeTeamName = "Tigers",
                    awayTeamId = 20L,
                    awayTeamName = "Lions",
                    scheduledAt = LocalDateTime.of(2026, 5, 10, 14, 0),
                    status = GameStatus.SCHEDULED,
                    homeScore = 0,
                    awayScore = 0,
                    location = null,
                    fieldName = null,
                    gameNumber = null,
                    currentInning = "경기 전",
                    totalInnings = 9,
                    startedAt = null,
                    endedAt = null,
                    note = null,
                    forfeitReason = null,
                )
            val timeline = GameTimelineDto(gameId = gameId, events = emptyList(), totalEvents = 0)
            val aggregate =
                GameAggregateDto(
                    gameDetail = gameDetail,
                    boxScore = null,
                    timeline = timeline,
                    scoresheet = null,
                )

            every { gameAggregateService.getGameAggregate(gameId) } returns aggregate

            // when & then
            mockMvc
                .perform(get("/api/v1/games/{gameId}/aggregate", gameId))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.gameInfo.status").value("SCHEDULED"))
        }
    }
}
