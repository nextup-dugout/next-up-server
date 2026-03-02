package com.nextup.api.controller.player

import com.nextup.core.common.PageResult
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.service.player.PlayerService
import com.nextup.core.service.player.PlayerTeamService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@DisplayName("PlayerController")
class PlayerControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var playerService: PlayerService
    private lateinit var playerTeamService: PlayerTeamService
    private lateinit var controller: PlayerController

    @BeforeEach
    fun setUp() {
        playerService = mockk()
        playerTeamService = mockk()
        controller = PlayerController(playerService, playerTeamService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(PageableHandlerMethodArgumentResolver())
                .build()
    }

    @Nested
    @DisplayName("GET /api/v1/players")
    inner class SearchPlayers {
        @Test
        fun `should return paginated player search results`() {
            // given
            val players =
                listOf(
                    createPlayer(1L, "김철수", Position.STARTING_PITCHER),
                    createPlayer(2L, "이영희", Position.CATCHER),
                )
            val pageResult =
                PageResult(
                    content = players,
                    page = 0,
                    size = 20,
                    totalElements = 2L,
                    totalPages = 1,
                )

            every {
                playerService.search(
                    name = null,
                    teamId = null,
                    position = null,
                    pageCommand = any(),
                )
            } returns pageResult
            every { playerTeamService.getActiveAffiliationsByPlayer(any()) } returns emptyList()

            // when & then
            mockMvc
                .perform(get("/api/v1/players"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray)
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].name").value("김철수"))
        }

        @Test
        fun `should filter by name parameter`() {
            // given
            val players =
                listOf(
                    createPlayer(1L, "김철수", Position.STARTING_PITCHER),
                )
            val pageResult =
                PageResult(
                    content = players,
                    page = 0,
                    size = 20,
                    totalElements = 1L,
                    totalPages = 1,
                )

            every {
                playerService.search(
                    name = "김철수",
                    teamId = null,
                    position = null,
                    pageCommand = any(),
                )
            } returns pageResult
            every { playerTeamService.getActiveAffiliationsByPlayer(any()) } returns emptyList()

            // when & then
            mockMvc
                .perform(get("/api/v1/players").param("name", "김철수"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("김철수"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/players/{playerId}")
    inner class GetPlayer {
        @Test
        fun `should return player detail when found`() {
            // given
            val player = createPlayer(1L, "김철수", Position.STARTING_PITCHER)
            every { playerService.getById(1L) } returns player
            every { playerTeamService.getActiveAffiliationsByPlayer(1L) } returns emptyList()

            // when & then
            mockMvc
                .perform(get("/api/v1/players/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("김철수"))
        }
    }

    private fun createPlayer(
        id: Long,
        name: String,
        position: Position,
    ): Player =
        Player(
            name = name,
            primaryPosition = position,
        ).apply {
            val idField = Player::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
}
