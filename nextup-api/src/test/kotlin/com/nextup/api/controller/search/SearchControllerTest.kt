package com.nextup.api.controller.search

import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.InvalidInputException
import com.nextup.core.domain.player.Position
import com.nextup.core.service.search.SearchService
import com.nextup.core.service.search.dto.CompetitionSearchDto
import com.nextup.core.service.search.dto.PlayerSearchDto
import com.nextup.core.service.search.dto.SearchResultDto
import com.nextup.core.service.search.dto.TeamSearchDto
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@DisplayName("SearchController 테스트")
class SearchControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var searchService: SearchService

    @BeforeEach
    fun setUp() {
        searchService = mockk()

        val controller = SearchController(searchService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Nested
    @DisplayName("GET /api/v1/search")
    inner class Search {
        @Test
        fun `키워드로 선수, 팀, 대회를 검색한다`() {
            // given
            val result =
                SearchResultDto(
                    players =
                        listOf(
                            PlayerSearchDto(
                                playerId = 1L,
                                playerName = "홍길동",
                                primaryPosition = Position.STARTING_PITCHER,
                                profileImageUrl = null,
                                teamName = "서울팀",
                            ),
                        ),
                    teams =
                        listOf(
                            TeamSearchDto(
                                teamId = 10L,
                                teamName = "서울야구단",
                                city = "서울",
                                logoUrl = "https://example.com/logo.png",
                                isActive = true,
                            ),
                        ),
                    competitions =
                        listOf(
                            CompetitionSearchDto(
                                competitionId = 100L,
                                competitionName = "서울 춘계대회",
                                leagueName = "서울리그",
                                year = 2025,
                            ),
                        ),
                )
            every { searchService.search(keyword = "서울", limit = 5) } returns result

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/search")
                        .param("q", "서울"),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.players").isArray)
                .andExpect(jsonPath("$.data.players.length()").value(1))
                .andExpect(jsonPath("$.data.players[0].playerId").value(1))
                .andExpect(jsonPath("$.data.players[0].playerName").value("홍길동"))
                .andExpect(jsonPath("$.data.teams").isArray)
                .andExpect(jsonPath("$.data.teams.length()").value(1))
                .andExpect(jsonPath("$.data.teams[0].teamId").value(10))
                .andExpect(jsonPath("$.data.teams[0].teamName").value("서울야구단"))
                .andExpect(jsonPath("$.data.competitions").isArray)
                .andExpect(jsonPath("$.data.competitions.length()").value(1))
                .andExpect(jsonPath("$.data.competitions[0].competitionId").value(100))

            verify(exactly = 1) { searchService.search(keyword = "서울", limit = 5) }
        }

        @Test
        fun `limit 파라미터를 지정해 검색한다`() {
            // given
            val result =
                SearchResultDto(
                    players = emptyList(),
                    teams = emptyList(),
                    competitions = emptyList(),
                )
            every { searchService.search(keyword = "홍", limit = 3) } returns result

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/search")
                        .param("q", "홍")
                        .param("limit", "3"),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.players").isArray)
                .andExpect(jsonPath("$.data.teams").isArray)
                .andExpect(jsonPath("$.data.competitions").isArray)

            verify(exactly = 1) { searchService.search(keyword = "홍", limit = 3) }
        }

        @Test
        fun `검색 결과가 없으면 빈 목록을 반환한다`() {
            // given
            val result =
                SearchResultDto(
                    players = emptyList(),
                    teams = emptyList(),
                    competitions = emptyList(),
                )
            every { searchService.search(keyword = "존재하지않는키워드", limit = 5) } returns result

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/search")
                        .param("q", "존재하지않는키워드"),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.players.length()").value(0))
                .andExpect(jsonPath("$.data.teams.length()").value(0))
                .andExpect(jsonPath("$.data.competitions.length()").value(0))
        }

        @Test
        fun `유효하지 않은 키워드로 검색 시 400을 반환한다`() {
            // given
            every {
                searchService.search(keyword = " ", limit = 5)
            } throws InvalidInputException("SEARCH_KEYWORD_TOO_SHORT", "검색 키워드는 최소 1자 이상이어야 합니다")

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/search")
                        .param("q", " "),
                )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }
    }
}
