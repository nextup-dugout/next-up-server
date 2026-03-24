package com.nextup.api.controller.discipline

import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.core.domain.discipline.PlayerBan
import com.nextup.core.service.discipline.PlayerBanService
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

@DisplayName("PlayerBanController 테스트")
class PlayerBanControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var playerBanService: PlayerBanService

    @BeforeEach
    fun setUp() {
        playerBanService = mockk()
        val controller = PlayerBanController(playerBanService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    private fun createBan(
        id: Long = 1L,
        playerId: Long = 10L,
        competitionId: Long = 100L,
    ): PlayerBan {
        val ban =
            PlayerBan.create(
                playerId = playerId,
                competitionId = competitionId,
                reason = "폭력 행위",
                issuedBy = "관리자",
            )
        val idField = PlayerBan::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(ban, id)
        return ban
    }

    @Nested
    @DisplayName("GET /api/v1/player-bans/players/{playerId}")
    inner class GetPlayerBans {
        @Test
        fun `선수의 제재 이력을 조회할 수 있다`() {
            // given
            val bans = listOf(createBan(id = 1L, playerId = 10L))
            every { playerBanService.getBansByPlayer(10L) } returns bans

            // when & then
            mockMvc
                .perform(get("/api/v1/player-bans/players/10"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.playerId").value(10))
                .andExpect(jsonPath("$.data.totalBans").value(1))
                .andExpect(jsonPath("$.data.bans[0].playerId").value(10))
        }

        @Test
        fun `대회별로 선수의 제재 이력을 조회할 수 있다`() {
            // given
            val bans = listOf(createBan(id = 1L, playerId = 10L, competitionId = 100L))
            every {
                playerBanService.getBansByPlayerAndCompetition(10L, 100L)
            } returns bans

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/player-bans/players/10")
                        .param("competitionId", "100"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalBans").value(1))
        }

        @Test
        fun `제재 이력이 없으면 빈 목록을 반환한다`() {
            // given
            every { playerBanService.getBansByPlayer(10L) } returns emptyList()

            // when & then
            mockMvc
                .perform(get("/api/v1/player-bans/players/10"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalBans").value(0))
                .andExpect(jsonPath("$.data.bans").isEmpty)
        }
    }

    @Nested
    @DisplayName("GET /api/v1/player-bans/players/{playerId}/eligibility")
    inner class CheckPlayerEligibility {
        @Test
        fun `제재가 없는 선수는 출장 가능으로 반환한다`() {
            // given
            every { playerBanService.canPlayerPlay(10L, 100L) } returns true
            every {
                playerBanService.getBansByPlayerAndCompetition(10L, 100L)
            } returns emptyList()

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/player-bans/players/10/eligibility")
                        .param("competitionId", "100"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.canPlay").value(true))
                .andExpect(jsonPath("$.data.bansCount").value(0))
        }

        @Test
        fun `제재가 있는 선수는 출장 불가능으로 반환한다`() {
            // given
            val bans = listOf(createBan(playerId = 10L, competitionId = 100L))
            every { playerBanService.canPlayerPlay(10L, 100L) } returns false
            every {
                playerBanService.getBansByPlayerAndCompetition(10L, 100L)
            } returns bans

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/player-bans/players/10/eligibility")
                        .param("competitionId", "100"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.canPlay").value(false))
                .andExpect(jsonPath("$.data.bansCount").value(1))
        }
    }
}
