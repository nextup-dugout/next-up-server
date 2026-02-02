package com.nextup.backoffice.controller.player

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.nextup.backoffice.dto.player.*
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.*
import com.nextup.core.domain.team.Team
import com.nextup.core.service.player.PlayerTeamService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate

@DisplayName("PlayerTeamAdminController")
class PlayerTeamAdminControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var playerTeamService: PlayerTeamService
    private lateinit var controller: PlayerTeamAdminController
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        playerTeamService = mockk()
        controller = PlayerTeamAdminController(playerTeamService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
        objectMapper = ObjectMapper().registerModule(JavaTimeModule())
    }

    @Nested
    @DisplayName("POST /api/backoffice/player-teams")
    inner class RegisterAffiliation {

        @Test
        fun `선수 소속을 등록할 수 있다`() {
            // given
            val playerId = 1L
            val teamId = 1L
            val startDate = LocalDate.of(2024, 1, 1)
            val position = Position.STARTING_PITCHER
            val uniformNumber = 10

            val player = createPlayer(playerId, "홍길동")
            val team = createTeam(teamId, "서울 타이거즈", 1L)
            val affiliation = createPlayerTeamHistory(1L, player, team)

            val request = RegisterAffiliationRequest(
                playerId = playerId,
                teamId = teamId,
                startDate = startDate,
                position = position,
                uniformNumber = uniformNumber,
                contractType = ContractType.REGULAR
            )

            every {
                playerTeamService.registerAffiliation(
                    playerId = playerId,
                    teamId = teamId,
                    startDate = startDate,
                    position = position,
                    uniformNumber = uniformNumber,
                    contractType = ContractType.REGULAR
                )
            } returns affiliation

            // when & then
            mockMvc.perform(
                post("/api/backoffice/player-teams")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.playerId").value(playerId))
                .andExpect(jsonPath("$.data.teamId").value(teamId))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.position").value("STARTING_PITCHER"))
                .andExpect(jsonPath("$.data.uniformNumber").value(uniformNumber))

            verify(exactly = 1) {
                playerTeamService.registerAffiliation(
                    playerId = playerId,
                    teamId = teamId,
                    startDate = startDate,
                    position = position,
                    uniformNumber = uniformNumber,
                    contractType = ContractType.REGULAR
                )
            }
        }
    }

    @Nested
    @DisplayName("PUT /api/backoffice/player-teams/{id}/end")
    inner class EndAffiliation {

        @Test
        fun `선수 소속을 종료할 수 있다`() {
            // given
            val affiliationId = 1L
            val endDate = LocalDate.of(2024, 12, 31)
            val player = createPlayer(1L, "홍길동")
            val team = createTeam(1L, "서울 타이거즈", 1L)
            val affiliation = createPlayerTeamHistory(affiliationId, player, team).apply {
                deactivate(endDate)
            }

            val request = EndAffiliationRequest(endDate = endDate)

            every {
                playerTeamService.endAffiliation(affiliationId, endDate)
            } returns affiliation

            // when & then
            mockMvc.perform(
                put("/api/backoffice/player-teams/$affiliationId/end")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(affiliationId))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"))

            verify(exactly = 1) {
                playerTeamService.endAffiliation(affiliationId, endDate)
            }
        }
    }

    @Nested
    @DisplayName("POST /api/backoffice/player-teams/transfer")
    inner class TransferPlayer {

        @Test
        fun `선수를 이적시킬 수 있다`() {
            // given
            val playerId = 1L
            val fromTeamId = 1L
            val toTeamId = 2L
            val transferDate = LocalDate.of(2024, 6, 1)
            val newPosition = Position.LEFT_FIELD
            val newUniformNumber = 20

            val player = createPlayer(playerId, "홍길동")
            val toTeam = createTeam(toTeamId, "서울 라이온즈", 1L)
            val newAffiliation = createPlayerTeamHistory(2L, player, toTeam).apply {
                changePosition(newPosition)
                changeUniformNumber(newUniformNumber)
            }

            val request = TransferPlayerRequest(
                playerId = playerId,
                fromTeamId = fromTeamId,
                toTeamId = toTeamId,
                transferDate = transferDate,
                newPosition = newPosition,
                newUniformNumber = newUniformNumber,
                newContractType = ContractType.REGULAR
            )

            every {
                playerTeamService.transferPlayer(
                    playerId = playerId,
                    fromTeamId = fromTeamId,
                    toTeamId = toTeamId,
                    transferDate = transferDate,
                    newPosition = newPosition,
                    newUniformNumber = newUniformNumber,
                    newContractType = ContractType.REGULAR
                )
            } returns newAffiliation

            // when & then
            mockMvc.perform(
                post("/api/backoffice/player-teams/transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.playerId").value(playerId))
                .andExpect(jsonPath("$.data.teamId").value(toTeamId))
                .andExpect(jsonPath("$.data.position").value("LEFT_FIELD"))
                .andExpect(jsonPath("$.data.uniformNumber").value(newUniformNumber))

            verify(exactly = 1) {
                playerTeamService.transferPlayer(
                    playerId = playerId,
                    fromTeamId = fromTeamId,
                    toTeamId = toTeamId,
                    transferDate = transferDate,
                    newPosition = newPosition,
                    newUniformNumber = newUniformNumber,
                    newContractType = ContractType.REGULAR
                )
            }
        }
    }

    @Nested
    @DisplayName("PUT /api/backoffice/player-teams/{id}/uniform-number")
    inner class ChangeUniformNumber {

        @Test
        fun `등번호를 변경할 수 있다`() {
            // given
            val affiliationId = 1L
            val newUniformNumber = 99
            val player = createPlayer(1L, "홍길동")
            val team = createTeam(1L, "서울 타이거즈", 1L)
            val affiliation = createPlayerTeamHistory(affiliationId, player, team).apply {
                changeUniformNumber(newUniformNumber)
            }

            val request = ChangeUniformNumberRequest(uniformNumber = newUniformNumber)

            every {
                playerTeamService.changeUniformNumber(affiliationId, newUniformNumber)
            } returns affiliation

            // when & then
            mockMvc.perform(
                put("/api/backoffice/player-teams/$affiliationId/uniform-number")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.uniformNumber").value(newUniformNumber))

            verify(exactly = 1) {
                playerTeamService.changeUniformNumber(affiliationId, newUniformNumber)
            }
        }
    }

    @Nested
    @DisplayName("PUT /api/backoffice/player-teams/{id}/position")
    inner class ChangePosition {

        @Test
        fun `포지션을 변경할 수 있다`() {
            // given
            val affiliationId = 1L
            val newPosition = Position.FIRST_BASE
            val player = createPlayer(1L, "홍길동")
            val team = createTeam(1L, "서울 타이거즈", 1L)
            val affiliation = createPlayerTeamHistory(affiliationId, player, team).apply {
                changePosition(newPosition)
            }

            val request = ChangePositionRequest(position = newPosition)

            every {
                playerTeamService.changePosition(affiliationId, newPosition)
            } returns affiliation

            // when & then
            mockMvc.perform(
                put("/api/backoffice/player-teams/$affiliationId/position")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.position").value("FIRST_BASE"))

            verify(exactly = 1) {
                playerTeamService.changePosition(affiliationId, newPosition)
            }
        }
    }

    @Nested
    @DisplayName("GET /api/backoffice/player-teams/player/{playerId}")
    inner class GetPlayerAffiliations {

        @Test
        fun `선수의 활성 소속 목록을 조회할 수 있다`() {
            // given
            val playerId = 1L
            val player = createPlayer(playerId, "홍길동")
            val team1 = createTeam(1L, "서울 타이거즈", 1L)
            val team2 = createTeam(2L, "경기 라이온즈", 2L)
            val affiliations = listOf(
                createPlayerTeamHistory(1L, player, team1),
                createPlayerTeamHistory(2L, player, team2)
            )

            every { playerTeamService.getActiveAffiliationsByPlayer(playerId) } returns affiliations

            // when & then
            mockMvc.perform(
                get("/api/backoffice/player-teams/player/$playerId")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))

            verify(exactly = 1) {
                playerTeamService.getActiveAffiliationsByPlayer(playerId)
            }
        }
    }

    @Nested
    @DisplayName("GET /api/backoffice/player-teams/team/{teamId}/roster")
    inner class GetTeamRoster {

        @Test
        fun `팀의 현재 로스터를 조회할 수 있다`() {
            // given
            val teamId = 1L
            val team = createTeam(teamId, "서울 타이거즈", 1L)
            val player1 = createPlayer(1L, "홍길동")
            val player2 = createPlayer(2L, "김철수")
            val roster = listOf(
                createPlayerTeamHistory(1L, player1, team),
                createPlayerTeamHistory(2L, player2, team)
            )

            every { playerTeamService.getTeamRoster(teamId) } returns roster

            // when & then
            mockMvc.perform(
                get("/api/backoffice/player-teams/team/$teamId/roster")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))

            verify(exactly = 1) {
                playerTeamService.getTeamRoster(teamId)
            }
        }
    }

    @Nested
    @DisplayName("GET /api/backoffice/player-teams/player/{playerId}/history")
    inner class GetPlayerHistory {

        @Test
        fun `선수의 전체 소속 이력을 조회할 수 있다`() {
            // given
            val playerId = 1L
            val player = createPlayer(playerId, "홍길동")
            val team = createTeam(1L, "서울 타이거즈", 1L)
            val history = listOf(
                createPlayerTeamHistory(1L, player, team),
                createPlayerTeamHistory(2L, player, team).apply {
                    deactivate(LocalDate.of(2024, 12, 31))
                }
            )

            every { playerTeamService.getPlayerHistory(playerId) } returns history

            // when & then
            mockMvc.perform(
                get("/api/backoffice/player-teams/player/$playerId/history")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))

            verify(exactly = 1) {
                playerTeamService.getPlayerHistory(playerId)
            }
        }
    }

    // Helper methods

    private fun createPlayer(id: Long, name: String): Player {
        val player = Player(
            name = name,
            primaryPosition = Position.STARTING_PITCHER
        )
        setEntityId(player, id)
        return player
    }

    private fun createTeam(id: Long, name: String, leagueId: Long): Team {
        val league = createLeague(leagueId)
        val team = Team(
            league = league,
            name = name,
            city = "서울",
            foundedYear = 2020
        )
        setEntityId(team, id)
        return team
    }

    private fun createLeague(id: Long): League {
        val association = createAssociation(1L)
        val league = League(
            association = association,
            name = "서울리그",
            foundedYear = 2020
        )
        setEntityId(league, id)
        return league
    }

    private fun createAssociation(id: Long): Association {
        val association = Association(
            name = "서울협회",
            region = "서울"
        )
        setEntityId(association, id)
        return association
    }

    private fun createPlayerTeamHistory(
        id: Long,
        player: Player,
        team: Team
    ): PlayerTeamHistory {
        val history = PlayerTeamHistory(
            player = player,
            team = team,
            startDate = LocalDate.of(2024, 1, 1),
            position = Position.STARTING_PITCHER,
            uniformNumber = 10,
            contractType = ContractType.REGULAR,
            status = PlayerTeamStatus.ACTIVE
        )
        setEntityId(history, id)
        return history
    }

    private fun setEntityId(entity: Any, id: Long) {
        val idField = entity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }
}
