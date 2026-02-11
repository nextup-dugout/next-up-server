package com.nextup.api.controller.title

import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.core.service.title.TitleCategory
import com.nextup.core.service.title.TitleService
import com.nextup.core.service.title.dto.TitleCandidateDto
import com.nextup.core.service.title.dto.TitleDto
import com.nextup.core.service.title.dto.TitleWinnerDto
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

@DisplayName("TitleController")
class TitleControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var titleService: TitleService

    @BeforeEach
    fun setUp() {
        titleService = mockk()
        val controller = TitleController(titleService)
        mockMvc =
            MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Nested
    @DisplayName("GET /api/v1/competitions/{competitionId}/titles")
    inner class GetTitles {
        @Test
        fun `대회의 모든 타이틀을 조회한다`() {
            // given
            val competitionId = 1L
            val battingAvgTitle =
                TitleDto(
                    category = TitleCategory.BATTING_AVG,
                    displayName = "타격왕",
                    winner =
                        TitleWinnerDto(
                            playerId = 1L,
                            playerName = "김타격",
                            teamName = "Tigers",
                            statValue = 0.350,
                        ),
                    topCandidates =
                        listOf(
                            TitleCandidateDto(
                                rank = 1,
                                playerId = 1L,
                                playerName = "김타격",
                                teamName = "Tigers",
                                statValue = 0.350,
                                isQualified = true,
                            ),
                        ),
                )

            val homeRunsTitle =
                TitleDto(
                    category = TitleCategory.HOME_RUNS,
                    displayName = "홈런왕",
                    winner =
                        TitleWinnerDto(
                            playerId = 2L,
                            playerName = "박홈런",
                            teamName = "Lions",
                            statValue = 15.0,
                        ),
                    topCandidates =
                        listOf(
                            TitleCandidateDto(
                                rank = 1,
                                playerId = 2L,
                                playerName = "박홈런",
                                teamName = "Lions",
                                statValue = 15.0,
                                isQualified = true,
                            ),
                        ),
                )

            every { titleService.getTitles(competitionId) } returns listOf(battingAvgTitle, homeRunsTitle)

            // when & then
            mockMvc
                .perform(get("/api/v1/competitions/{competitionId}/titles", competitionId))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.competitionId").value(competitionId))
                .andExpect(jsonPath("$.data.titles").isArray)
                .andExpect(jsonPath("$.data.titles.length()").value(2))
                .andExpect(jsonPath("$.data.titles[0].category").value("BATTING_AVG"))
                .andExpect(jsonPath("$.data.titles[0].displayName").value("타격왕"))
                .andExpect(jsonPath("$.data.titles[0].winner.playerId").value(1))
                .andExpect(jsonPath("$.data.titles[0].winner.playerName").value("김타격"))
                .andExpect(jsonPath("$.data.titles[0].winner.statValue").value(0.350))
                .andExpect(jsonPath("$.data.titles[1].category").value("HOME_RUNS"))
                .andExpect(jsonPath("$.data.titles[1].displayName").value("홈런왕"))
        }

        @Test
        fun `대회가 존재하지 않으면 404 응답`() {
            // given
            val competitionId = 999L
            every { titleService.getTitles(competitionId) } throws CompetitionNotFoundException(competitionId)

            // when & then
            mockMvc
                .perform(get("/api/v1/competitions/{competitionId}/titles", competitionId))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMPETITION_NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/competitions/{competitionId}/titles/{category}")
    inner class GetTitleByCategory {
        @Test
        fun `특정 카테고리의 타이틀을 조회한다`() {
            // given
            val competitionId = 1L
            val category = "BATTING_AVG"
            val title =
                TitleDto(
                    category = TitleCategory.BATTING_AVG,
                    displayName = "타격왕",
                    winner =
                        TitleWinnerDto(
                            playerId = 1L,
                            playerName = "김타격",
                            teamName = "Tigers",
                            statValue = 0.350,
                        ),
                    topCandidates =
                        listOf(
                            TitleCandidateDto(
                                rank = 1,
                                playerId = 1L,
                                playerName = "김타격",
                                teamName = "Tigers",
                                statValue = 0.350,
                                isQualified = true,
                            ),
                            TitleCandidateDto(
                                rank = 2,
                                playerId = 2L,
                                playerName = "박안타",
                                teamName = "Lions",
                                statValue = 0.320,
                                isQualified = true,
                            ),
                        ),
                )

            every { titleService.getTitleByCategory(competitionId, TitleCategory.BATTING_AVG) } returns title

            // when & then
            mockMvc
                .perform(get("/api/v1/competitions/{competitionId}/titles/{category}", competitionId, category))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.category").value("BATTING_AVG"))
                .andExpect(jsonPath("$.data.displayName").value("타격왕"))
                .andExpect(jsonPath("$.data.winner.playerId").value(1))
                .andExpect(jsonPath("$.data.winner.playerName").value("김타격"))
                .andExpect(jsonPath("$.data.winner.teamName").value("Tigers"))
                .andExpect(jsonPath("$.data.winner.statValue").value(0.350))
                .andExpect(jsonPath("$.data.topCandidates").isArray)
                .andExpect(jsonPath("$.data.topCandidates.length()").value(2))
                .andExpect(jsonPath("$.data.topCandidates[0].rank").value(1))
                .andExpect(jsonPath("$.data.topCandidates[0].isQualified").value(true))
                .andExpect(jsonPath("$.data.topCandidates[1].rank").value(2))
        }

        @Test
        fun `우승자가 없는 경우 winner는 null`() {
            // given
            val competitionId = 1L
            val category = "BATTING_AVG"
            val title =
                TitleDto(
                    category = TitleCategory.BATTING_AVG,
                    displayName = "타격왕",
                    winner = null, // 규정 타석 충족자 없음
                    topCandidates =
                        listOf(
                            TitleCandidateDto(
                                rank = 1,
                                playerId = 1L,
                                playerName = "김타격",
                                teamName = "Tigers",
                                statValue = 0.350,
                                isQualified = false, // 규정 미충족
                            ),
                        ),
                )

            every { titleService.getTitleByCategory(competitionId, TitleCategory.BATTING_AVG) } returns title

            // when & then
            mockMvc
                .perform(get("/api/v1/competitions/{competitionId}/titles/{category}", competitionId, category))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.category").value("BATTING_AVG"))
                .andExpect(jsonPath("$.data.winner").doesNotExist())
                .andExpect(jsonPath("$.data.topCandidates.length()").value(1))
                .andExpect(jsonPath("$.data.topCandidates[0].isQualified").value(false))
        }

        @Test
        fun `잘못된 카테고리 입력 시 400 응답`() {
            // given
            val competitionId = 1L
            val invalidCategory = "INVALID_CATEGORY"

            // when & then
            mockMvc
                .perform(get("/api/v1/competitions/{competitionId}/titles/{category}", competitionId, invalidCategory))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        fun `소문자 카테고리도 정상 처리`() {
            // given
            val competitionId = 1L
            val category = "batting_avg"
            val title =
                TitleDto(
                    category = TitleCategory.BATTING_AVG,
                    displayName = "타격왕",
                    winner = null,
                    topCandidates = emptyList(),
                )

            every { titleService.getTitleByCategory(competitionId, TitleCategory.BATTING_AVG) } returns title

            // when & then
            mockMvc
                .perform(get("/api/v1/competitions/{competitionId}/titles/{category}", competitionId, category))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.category").value("BATTING_AVG"))
        }
    }
}
