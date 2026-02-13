package com.nextup.api.controller.game

import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.service.game.GameScheduleService
import com.nextup.core.service.game.dto.GameDetailDto
import com.nextup.core.service.game.dto.GameSummaryDto
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

@DisplayName("GameScheduleController 테스트")
class GameScheduleControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var gameScheduleService: GameScheduleService

    @BeforeEach
    fun setUp() {
        gameScheduleService = mockk()

        val controller = GameScheduleController(gameScheduleService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    private fun createGameSummaryDto(
        gameId: Long = 1L,
        status: GameStatus = GameStatus.SCHEDULED,
        homeScore: Int = 0,
        awayScore: Int = 0,
    ): GameSummaryDto =
        GameSummaryDto(
            gameId = gameId,
            competitionId = 10L,
            competitionName = "2025 춘계대회",
            homeTeamId = 100L,
            homeTeamName = "홈팀",
            awayTeamId = 200L,
            awayTeamName = "원정팀",
            scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
            status = status,
            homeScore = homeScore,
            awayScore = awayScore,
            location = "잠실야구장",
            fieldName = "1구장",
        )

    @Nested
    @DisplayName("GET /api/v1/games")
    inner class GetGames {
        @Test
        fun `경기 목록을 정상적으로 조회한다`() {
            // given
            val games =
                listOf(
                    createGameSummaryDto(gameId = 1L),
                    createGameSummaryDto(gameId = 2L, status = GameStatus.IN_PROGRESS, homeScore = 3, awayScore = 1),
                )
            every {
                gameScheduleService.getGames(
                    date = null,
                    teamId = null,
                    competitionId = null,
                    page = 0,
                    size = 20,
                )
            } returns games

            // when & then
            mockMvc
                .perform(get("/api/v1/games"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].gameId").value(1))
                .andExpect(jsonPath("$.data[0].homeTeam.teamName").value("홈팀"))
                .andExpect(jsonPath("$.data[0].awayTeam.teamName").value("원정팀"))
                .andExpect(jsonPath("$.data[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data[1].homeTeam.score").value(3))
                .andExpect(jsonPath("$.data[1].awayTeam.score").value(1))
        }

        @Test
        fun `빈 경기 목록을 반환한다`() {
            // given
            every {
                gameScheduleService.getGames(
                    date = null,
                    teamId = null,
                    competitionId = null,
                    page = 0,
                    size = 20,
                )
            } returns emptyList()

            // when & then
            mockMvc
                .perform(get("/api/v1/games"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(0))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/games/{gameId}")
    inner class GetGameDetail {
        @Test
        fun `경기 상세 정보를 정상적으로 조회한다`() {
            // given
            val detail =
                GameDetailDto(
                    gameId = 1L,
                    competitionId = 10L,
                    competitionName = "2025 춘계대회",
                    homeTeamId = 100L,
                    homeTeamName = "홈팀",
                    awayTeamId = 200L,
                    awayTeamName = "원정팀",
                    scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
                    status = GameStatus.FINISHED,
                    homeScore = 5,
                    awayScore = 3,
                    location = "잠실야구장",
                    fieldName = "1구장",
                    gameNumber = 1,
                    currentInning = "9회말",
                    totalInnings = 9,
                    startedAt = LocalDateTime.of(2025, 4, 15, 14, 0),
                    endedAt = LocalDateTime.of(2025, 4, 15, 16, 30),
                    note = null,
                    forfeitReason = null,
                )
            every { gameScheduleService.getGameDetail(1L) } returns detail

            // when & then
            mockMvc
                .perform(get("/api/v1/games/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.gameId").value(1))
                .andExpect(jsonPath("$.data.homeTeam.teamName").value("홈팀"))
                .andExpect(jsonPath("$.data.homeTeam.score").value(5))
                .andExpect(jsonPath("$.data.awayTeam.score").value(3))
                .andExpect(jsonPath("$.data.status").value("FINISHED"))
                .andExpect(jsonPath("$.data.statusDisplayName").value("종료"))
                .andExpect(jsonPath("$.data.currentInning").value("9회말"))
                .andExpect(jsonPath("$.data.totalInnings").value(9))
                .andExpect(jsonPath("$.data.gameNumber").value(1))
        }

        @Test
        fun `존재하지 않는 경기 조회 시 404를 반환한다`() {
            // given
            every { gameScheduleService.getGameDetail(999L) } throws GameNotFoundException(999L)

            // when & then
            mockMvc
                .perform(get("/api/v1/games/999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("GAME_NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/teams/{teamId}/games")
    inner class GetGamesByTeam {
        @Test
        fun `팀별 경기 일정을 정상적으로 조회한다`() {
            // given
            val games =
                listOf(
                    createGameSummaryDto(gameId = 1L),
                    createGameSummaryDto(gameId = 2L),
                )
            every { gameScheduleService.getGamesByTeam(100L) } returns games

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/100/games"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/teams/{teamId}/games/upcoming")
    inner class GetUpcomingGames {
        @Test
        fun `다가오는 경기를 정상적으로 조회한다`() {
            // given
            val games =
                listOf(
                    createGameSummaryDto(gameId = 3L),
                )
            every { gameScheduleService.getUpcomingGamesByTeam(100L, 5) } returns games

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/100/games/upcoming"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].gameId").value(3))
        }

        @Test
        fun `다가오는 경기가 없으면 빈 리스트를 반환한다`() {
            // given
            every { gameScheduleService.getUpcomingGamesByTeam(100L, 5) } returns emptyList()

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/100/games/upcoming"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(0))
        }
    }
}
