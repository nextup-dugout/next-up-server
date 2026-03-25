package com.nextup.backoffice.controller.discipline

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nextup.backoffice.dto.discipline.IssuePlayerBanRequest
import com.nextup.backoffice.exception.GlobalExceptionHandler
import com.nextup.common.exception.PlayerBanNotFoundException
import com.nextup.core.domain.discipline.PlayerBan
import com.nextup.core.service.discipline.PlayerBanService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@DisplayName("PlayerBanAdminController 테스트")
class PlayerBanAdminControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var playerBanService: PlayerBanService
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        playerBanService = mockk()
        val controller = PlayerBanAdminController(playerBanService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
        objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    }

    private fun createBan(
        id: Long = 1L,
        playerId: Long = 10L,
        competitionId: Long = 100L,
        reason: String = "폭력 행위",
        issuedBy: String = "관리자",
    ): PlayerBan {
        val ban =
            PlayerBan.create(
                playerId = playerId,
                competitionId = competitionId,
                reason = reason,
                issuedBy = issuedBy,
            )
        val idField = PlayerBan::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(ban, id)
        return ban
    }

    @Nested
    @DisplayName("GET /api/backoffice/player-bans")
    inner class GetAllBans {
        @Test
        fun `모든 제재 목록을 조회할 수 있다`() {
            // given
            val bans = listOf(createBan(id = 1L), createBan(id = 2L, playerId = 20L))
            every { playerBanService.getAll() } returns bans

            // when & then
            mockMvc
                .perform(get("/api/backoffice/player-bans"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
        }

        @Test
        fun `선수 ID로 필터링하여 조회할 수 있다`() {
            // given
            val bans = listOf(createBan(playerId = 10L))
            every { playerBanService.getBansByPlayer(10L) } returns bans

            // when & then
            mockMvc
                .perform(
                    get("/api/backoffice/player-bans")
                        .param("playerId", "10"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
        }

        @Test
        fun `대회 ID로 필터링하여 조회할 수 있다`() {
            // given
            val bans = listOf(createBan(competitionId = 100L))
            every { playerBanService.getBansByCompetition(100L) } returns bans

            // when & then
            mockMvc
                .perform(
                    get("/api/backoffice/player-bans")
                        .param("competitionId", "100"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
        }

        @Test
        fun `선수 ID와 대회 ID로 동시에 필터링할 수 있다`() {
            // given
            val bans = listOf(createBan(playerId = 10L, competitionId = 100L))
            every {
                playerBanService.getBansByPlayerAndCompetition(10L, 100L)
            } returns bans

            // when & then
            mockMvc
                .perform(
                    get("/api/backoffice/player-bans")
                        .param("playerId", "10")
                        .param("competitionId", "100"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
        }
    }

    @Nested
    @DisplayName("GET /api/backoffice/player-bans/{id}")
    inner class GetBan {
        @Test
        fun `제재 상세 정보를 조회할 수 있다`() {
            // given
            val ban = createBan(id = 1L)
            every { playerBanService.getById(1L) } returns ban

            // when & then
            mockMvc
                .perform(get("/api/backoffice/player-bans/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.playerId").value(10))
                .andExpect(jsonPath("$.data.reason").value("폭력 행위"))
        }

        @Test
        fun `존재하지 않는 제재 조회 시 404를 반환한다`() {
            // given
            every { playerBanService.getById(999L) } throws PlayerBanNotFoundException(999L)

            // when & then
            mockMvc
                .perform(get("/api/backoffice/player-bans/999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
        }
    }

    @Nested
    @DisplayName("GET /api/backoffice/player-bans/eligibility")
    inner class CheckEligibility {
        @Test
        fun `선수 출장 가능 여부를 확인할 수 있다`() {
            // given
            every { playerBanService.canPlayerPlay(10L, 100L) } returns true

            // when & then
            mockMvc
                .perform(
                    get("/api/backoffice/player-bans/eligibility")
                        .param("playerId", "10")
                        .param("competitionId", "100"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.canPlay").value(true))
        }

        @Test
        fun `제재가 있는 선수는 출장 불가능으로 반환한다`() {
            // given
            every { playerBanService.canPlayerPlay(10L, 100L) } returns false

            // when & then
            mockMvc
                .perform(
                    get("/api/backoffice/player-bans/eligibility")
                        .param("playerId", "10")
                        .param("competitionId", "100"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.canPlay").value(false))
        }
    }

    @Nested
    @DisplayName("POST /api/backoffice/player-bans")
    inner class IssueBan {
        @Test
        fun `제재를 발급할 수 있다`() {
            // given
            val request =
                IssuePlayerBanRequest(
                    playerId = 10L,
                    competitionId = 100L,
                    reason = "폭력 행위",
                    issuedBy = "관리자",
                )
            val ban = createBan(id = 1L, playerId = 10L, competitionId = 100L)
            every {
                playerBanService.issueBan(
                    playerId = 10L,
                    competitionId = 100L,
                    reason = "폭력 행위",
                    issuedBy = "관리자",
                )
            } returns ban

            // when & then
            mockMvc
                .perform(
                    post("/api/backoffice/player-bans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.playerId").value(10))

            verify(exactly = 1) {
                playerBanService.issueBan(
                    playerId = 10L,
                    competitionId = 100L,
                    reason = "폭력 행위",
                    issuedBy = "관리자",
                )
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/backoffice/player-bans/{id}")
    inner class DeleteBan {
        @Test
        fun `제재를 삭제할 수 있다`() {
            // given
            every { playerBanService.deleteBan(1L) } returns Unit

            // when & then
            mockMvc
                .perform(delete("/api/backoffice/player-bans/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))

            verify(exactly = 1) { playerBanService.deleteBan(1L) }
        }

        @Test
        fun `존재하지 않는 제재 삭제 시 404를 반환한다`() {
            // given
            every { playerBanService.deleteBan(999L) } throws PlayerBanNotFoundException(999L)

            // when & then
            mockMvc
                .perform(delete("/api/backoffice/player-bans/999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
        }
    }
}
