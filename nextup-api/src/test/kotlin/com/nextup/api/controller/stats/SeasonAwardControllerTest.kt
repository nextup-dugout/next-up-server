package com.nextup.api.controller.stats

import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.stats.SeasonAward
import com.nextup.core.domain.stats.SeasonAwardTitle
import com.nextup.core.service.stats.SeasonAwardService
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
import java.math.BigDecimal

@DisplayName("SeasonAwardController 테스트")
class SeasonAwardControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var seasonAwardService: SeasonAwardService

    private lateinit var player1: Player
    private lateinit var player2: Player

    @BeforeEach
    fun setUp() {
        seasonAwardService = mockk()
        val controller = SeasonAwardController(seasonAwardService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()

        player1 = Player(name = "홍길동", primaryPosition = Position.SHORTSTOP)
        setId(player1, 10L)

        player2 = Player(name = "김철수", primaryPosition = Position.STARTING_PITCHER)
        setId(player2, 20L)
    }

    @Nested
    @DisplayName("GET /api/v1/competitions/{competitionId}/awards")
    inner class GetAwardsByCompetition {
        @Test
        fun `대회 시상 목록을 조회한다`() {
            // given
            val awards =
                listOf(
                    SeasonAward.create(
                        player = player1,
                        year = 2026,
                        title = SeasonAwardTitle.BATTING_CHAMPION,
                        statValue = BigDecimal("0.350"),
                        competitionId = 1L,
                    ),
                    SeasonAward.create(
                        player = player2,
                        year = 2026,
                        title = SeasonAwardTitle.ERA_TITLE,
                        statValue = BigDecimal("2.50"),
                        competitionId = 1L,
                    ),
                )
            every { seasonAwardService.getAwardsByCompetitionId(1L) } returns awards

            // when & then
            mockMvc
                .perform(get("/api/v1/competitions/1/awards"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].playerName").value("홍길동"))
                .andExpect(jsonPath("$.data[0].title").value("BATTING_CHAMPION"))
                .andExpect(jsonPath("$.data[0].titleDisplayName").value("타격왕"))
                .andExpect(jsonPath("$.data[0].statValue").value("0.350"))
                .andExpect(jsonPath("$.data[1].playerName").value("김철수"))
                .andExpect(jsonPath("$.data[1].title").value("ERA_TITLE"))
        }

        @Test
        fun `해당 대회에 시상이 없으면 빈 목록을 반환한다`() {
            // given
            every { seasonAwardService.getAwardsByCompetitionId(999L) } returns emptyList()

            // when & then
            mockMvc
                .perform(get("/api/v1/competitions/999/awards"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data").isEmpty)
        }
    }

    @Nested
    @DisplayName("GET /api/v1/awards")
    inner class GetAwardsByYear {
        @Test
        fun `연도별 시상 목록을 조회한다`() {
            // given
            val awards =
                listOf(
                    SeasonAward.create(
                        player = player1,
                        year = 2026,
                        title = SeasonAwardTitle.HOME_RUN_KING,
                        statValue = BigDecimal("15"),
                    ),
                )
            every { seasonAwardService.getAwardsByYear(2026) } returns awards

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/awards")
                        .param("year", "2026"),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].playerName").value("홍길동"))
                .andExpect(jsonPath("$.data[0].title").value("HOME_RUN_KING"))
                .andExpect(jsonPath("$.data[0].titleDisplayName").value("홈런왕"))
                .andExpect(jsonPath("$.data[0].statValue").value("15"))
        }

        @Test
        fun `해당 연도에 시상이 없으면 빈 목록을 반환한다`() {
            // given
            every { seasonAwardService.getAwardsByYear(2025) } returns emptyList()

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/awards")
                        .param("year", "2025"),
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty)
        }
    }

    @Nested
    @DisplayName("GET /api/v1/players/{playerId}/awards")
    inner class GetAwardsByPlayer {
        @Test
        fun `선수별 시상 목록을 조회한다`() {
            // given
            val awards =
                listOf(
                    SeasonAward.create(
                        player = player1,
                        year = 2026,
                        title = SeasonAwardTitle.BATTING_CHAMPION,
                        statValue = BigDecimal("0.350"),
                    ),
                    SeasonAward.create(
                        player = player1,
                        year = 2025,
                        title = SeasonAwardTitle.STOLEN_BASE_KING,
                        statValue = BigDecimal("20"),
                    ),
                )
            every { seasonAwardService.getAwardsByPlayerId(10L) } returns awards

            // when & then
            mockMvc
                .perform(get("/api/v1/players/10/awards"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].year").value(2026))
                .andExpect(jsonPath("$.data[0].title").value("BATTING_CHAMPION"))
                .andExpect(jsonPath("$.data[1].year").value(2025))
                .andExpect(jsonPath("$.data[1].title").value("STOLEN_BASE_KING"))
        }

        @Test
        fun `해당 선수에 시상이 없으면 빈 목록을 반환한다`() {
            // given
            every { seasonAwardService.getAwardsByPlayerId(999L) } returns emptyList()

            // when & then
            mockMvc
                .perform(get("/api/v1/players/999/awards"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty)
        }
    }

    private fun setId(
        player: Player,
        id: Long,
    ) {
        val idField = Player::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(player, id)
    }
}
