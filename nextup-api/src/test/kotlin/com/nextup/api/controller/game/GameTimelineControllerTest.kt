package com.nextup.api.controller.game

import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.core.service.game.GameTimelineService
import com.nextup.core.service.game.dto.GameTimelineDto
import com.nextup.core.service.game.dto.TimelineEventDto
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
import java.time.Instant

@DisplayName("GameTimelineController 테스트")
class GameTimelineControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var gameTimelineService: GameTimelineService

    @BeforeEach
    fun setUp() {
        gameTimelineService = mockk()

        val controller = GameTimelineController(gameTimelineService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Nested
    @DisplayName("GET /api/v1/games/{gameId}/timeline")
    inner class GetTimeline {
        @Test
        fun `경기 타임라인을 정상적으로 조회한다`() {
            // given
            val gameId = 1L
            val timelineDto =
                GameTimelineDto(
                    gameId = gameId,
                    events =
                        listOf(
                            TimelineEventDto(
                                eventId = 1L,
                                inning = 1,
                                isTopInning = true,
                                inningDisplay = "1회초",
                                eventType = "PLATE_APPEARANCE",
                                description = "홍길동 안타",
                                batterId = 100L,
                                batterName = "홍길동",
                                pitcherId = 200L,
                                pitcherName = "김철수",
                                plateAppearanceResult = "SINGLE",
                                runsScored = 0,
                                outCountBefore = 0,
                                outCountAfter = 0,
                                eventTimestamp = Instant.parse("2026-01-01T10:00:00Z"),
                            ),
                        ),
                    totalEvents = 1,
                )

            every { gameTimelineService.getTimeline(gameId, null, null) } returns timelineDto

            // when & then
            mockMvc
                .perform(get("/api/v1/games/$gameId/timeline"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.gameId").value(gameId))
                .andExpect(jsonPath("$.data.totalEvents").value(1))
                .andExpect(jsonPath("$.data.events[0].eventId").value(1))
                .andExpect(jsonPath("$.data.events[0].inningDisplay").value("1회초"))
                .andExpect(jsonPath("$.data.events[0].description").value("홍길동 안타"))
        }

        @Test
        fun `이닝 필터로 타임라인을 조회한다`() {
            // given
            val gameId = 1L
            val fromInning = 3
            val toInning = 5
            val timelineDto =
                GameTimelineDto(
                    gameId = gameId,
                    events =
                        listOf(
                            TimelineEventDto(
                                eventId = 10L,
                                inning = 3,
                                isTopInning = true,
                                inningDisplay = "3회초",
                                eventType = "PLATE_APPEARANCE",
                                description = "3회초 첫 타석",
                                batterId = 100L,
                                batterName = "타자A",
                                pitcherId = 200L,
                                pitcherName = "투수B",
                                plateAppearanceResult = "OUT",
                                runsScored = 0,
                                outCountBefore = 0,
                                outCountAfter = 1,
                                eventTimestamp = Instant.parse("2026-01-01T10:30:00Z"),
                            ),
                        ),
                    totalEvents = 1,
                )

            every { gameTimelineService.getTimeline(gameId, fromInning, toInning) } returns timelineDto

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/games/$gameId/timeline")
                        .param("fromInning", fromInning.toString())
                        .param("toInning", toInning.toString()),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.events[0].inning").value(3))
        }

        @Test
        fun `타자 및 투수 정보를 정확하게 반환한다`() {
            // given
            val gameId = 1L
            val timelineDto =
                GameTimelineDto(
                    gameId = gameId,
                    events =
                        listOf(
                            TimelineEventDto(
                                eventId = 5L,
                                inning = 2,
                                isTopInning = false,
                                inningDisplay = "2회말",
                                eventType = "PLATE_APPEARANCE",
                                description = "홈런",
                                batterId = 300L,
                                batterName = "박지성",
                                pitcherId = 400L,
                                pitcherName = "이영표",
                                plateAppearanceResult = "HOME_RUN",
                                runsScored = 2,
                                outCountBefore = 1,
                                outCountAfter = 1,
                                eventTimestamp = Instant.parse("2026-01-01T11:00:00Z"),
                            ),
                        ),
                    totalEvents = 1,
                )

            every { gameTimelineService.getTimeline(gameId, null, null) } returns timelineDto

            // when & then
            mockMvc
                .perform(get("/api/v1/games/$gameId/timeline"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.events[0].batterId").value(300))
                .andExpect(jsonPath("$.data.events[0].batterName").value("박지성"))
                .andExpect(jsonPath("$.data.events[0].pitcherId").value(400))
                .andExpect(jsonPath("$.data.events[0].pitcherName").value("이영표"))
                .andExpect(jsonPath("$.data.events[0].runsScored").value(2))
        }

        @Test
        fun `예외 발생 시 에러 응답을 반환한다`() {
            // given
            val gameId = 999L
            every { gameTimelineService.getTimeline(gameId, null, null) } throws
                IllegalArgumentException("경기를 찾을 수 없습니다")

            // when & then
            mockMvc
                .perform(get("/api/v1/games/$gameId/timeline"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }
    }
}
