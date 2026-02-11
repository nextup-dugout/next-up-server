package com.nextup.api.controller.discipline

import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.discipline.Discipline
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import com.nextup.core.service.discipline.DisciplineService
import com.nextup.core.service.player.PlayerService
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
import java.time.LocalDate

@DisplayName("DisciplineController 테스트")
class DisciplineControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var disciplineService: DisciplineService
    private lateinit var playerService: PlayerService
    private lateinit var controller: DisciplineController

    private lateinit var player: Player
    private lateinit var competition: Competition

    @BeforeEach
    fun setUp() {
        disciplineService = mockk()
        playerService = mockk()
        controller = DisciplineController(disciplineService, playerService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()

        // 테스트용 Player 및 Competition 설정
        val association =
            Association(
                name = "서울시야구협회",
                abbreviation = "SBA",
                region = "서울",
            )

        val league =
            League(
                association = association,
                name = "1부 리그",
                abbreviation = "1st",
                foundedYear = 2020,
                divisionLevel = 1,
            )

        competition =
            Competition(
                league = league,
                name = "2025 춘계대회",
                year = 2025,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2025, 3, 1),
            )

        player =
            Player(
                name = "홍길동",
                birthDate = LocalDate.of(1995, 5, 15),
                primaryPosition = Position.CATCHER,
                throwingHand = ThrowingHand.RIGHT,
                battingHand = BattingHand.RIGHT,
            )

        setId(player, 1L)
        setId(competition, 1L)
    }

    @Nested
    @DisplayName("GET /api/v1/disciplines/players/{playerId} - 선수 징계 이력 조회")
    inner class GetPlayerDisciplines {
        @Test
        fun `선수의 모든 징계 이력을 조회할 수 있다`() {
            // given
            val disciplines =
                listOf(
                    createWarning(1L),
                    createSuspension(2L, 3),
                    createWarning(3L),
                )
            every { playerService.getById(1L) } returns player
            every { disciplineService.getDisciplinesByPlayer(1L) } returns disciplines

            // when & then
            mockMvc
                .perform(get("/api/v1/disciplines/players/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.playerId").value(1))
                .andExpect(jsonPath("$.data.playerName").value("홍길동"))
                .andExpect(jsonPath("$.data.totalDisciplines").value(3))
                .andExpect(jsonPath("$.data.activeDisciplines").value(3))
                .andExpect(jsonPath("$.data.disciplines").isArray)
                .andExpect(jsonPath("$.data.disciplines.length()").value(3))

            verify(exactly = 1) { playerService.getById(1L) }
            verify(exactly = 1) { disciplineService.getDisciplinesByPlayer(1L) }
        }

        @Test
        fun `대회 ID로 필터링하여 선수의 징계 이력을 조회할 수 있다`() {
            // given
            val disciplines = listOf(createWarning(1L), createSuspension(2L, 2))
            every { playerService.getById(1L) } returns player
            every {
                disciplineService.getDisciplinesByPlayerAndCompetition(1L, 1L)
            } returns disciplines

            // when & then
            mockMvc
                .perform(get("/api/v1/disciplines/players/1").param("competitionId", "1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.playerId").value(1))
                .andExpect(jsonPath("$.data.totalDisciplines").value(2))
                .andExpect(jsonPath("$.data.activeDisciplines").value(2))
                .andExpect(jsonPath("$.data.disciplines.length()").value(2))

            verify(exactly = 1) { playerService.getById(1L) }
            verify(exactly = 1) {
                disciplineService.getDisciplinesByPlayerAndCompetition(1L, 1L)
            }
        }

        @Test
        fun `취소된 징계는 활성 징계 수에서 제외된다`() {
            // given
            val warning = createWarning(1L)
            warning.cancel()
            val suspension = createSuspension(2L, 3)
            val disciplines = listOf(warning, suspension)

            every { playerService.getById(1L) } returns player
            every { disciplineService.getDisciplinesByPlayer(1L) } returns disciplines

            // when & then
            mockMvc
                .perform(get("/api/v1/disciplines/players/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalDisciplines").value(2))
                .andExpect(jsonPath("$.data.activeDisciplines").value(1))

            verify(exactly = 1) { playerService.getById(1L) }
            verify(exactly = 1) { disciplineService.getDisciplinesByPlayer(1L) }
        }

        @Test
        fun `이행 완료된 출장 정지는 활성 징계 수에서 제외된다`() {
            // given
            val suspension = createSuspension(1L, 2)
            suspension.incrementServedGames()
            suspension.incrementServedGames() // 이행 완료
            val disciplines = listOf(suspension)

            every { playerService.getById(1L) } returns player
            every { disciplineService.getDisciplinesByPlayer(1L) } returns disciplines

            // when & then
            mockMvc
                .perform(get("/api/v1/disciplines/players/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalDisciplines").value(1))
                .andExpect(jsonPath("$.data.activeDisciplines").value(0))

            verify(exactly = 1) { playerService.getById(1L) }
            verify(exactly = 1) { disciplineService.getDisciplinesByPlayer(1L) }
        }

        @Test
        fun `존재하지 않는 선수 조회 시 404 오류를 반환한다`() {
            // given
            every { playerService.getById(999L) } throws PlayerNotFoundException(999L)

            // when & then
            mockMvc
                .perform(get("/api/v1/disciplines/players/999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("PLAYER_NOT_FOUND"))

            verify(exactly = 1) { playerService.getById(999L) }
        }

        @Test
        fun `징계가 없는 선수는 빈 목록을 반환한다`() {
            // given
            every { playerService.getById(1L) } returns player
            every { disciplineService.getDisciplinesByPlayer(1L) } returns emptyList()

            // when & then
            mockMvc
                .perform(get("/api/v1/disciplines/players/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.playerId").value(1))
                .andExpect(jsonPath("$.data.totalDisciplines").value(0))
                .andExpect(jsonPath("$.data.activeDisciplines").value(0))
                .andExpect(jsonPath("$.data.disciplines").isEmpty)

            verify(exactly = 1) { playerService.getById(1L) }
            verify(exactly = 1) { disciplineService.getDisciplinesByPlayer(1L) }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/disciplines/players/{playerId}/eligibility - 출장 가능 여부 확인")
    inner class CheckPlayerEligibility {
        @Test
        fun `선수가 출장 가능한 경우 true와 빈 징계 목록을 반환한다`() {
            // given
            every { disciplineService.canPlayerPlay(1L, 1L) } returns true
            every { disciplineService.getActiveDisciplines(1L, 1L) } returns emptyList()

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/disciplines/players/1/eligibility")
                        .param("competitionId", "1"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.canPlay").value(true))
                .andExpect(jsonPath("$.data.activeDisciplinesCount").value(0))
                .andExpect(jsonPath("$.data.disciplines").isEmpty)

            verify(exactly = 1) { disciplineService.canPlayerPlay(1L, 1L) }
            verify(exactly = 1) { disciplineService.getActiveDisciplines(1L, 1L) }
        }

        @Test
        fun `경고만 있는 선수는 출장 가능하다`() {
            // given
            val disciplines = listOf(createWarning(1L))
            every { disciplineService.canPlayerPlay(1L, 1L) } returns true
            every { disciplineService.getActiveDisciplines(1L, 1L) } returns disciplines

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/disciplines/players/1/eligibility")
                        .param("competitionId", "1"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.canPlay").value(true))
                .andExpect(jsonPath("$.data.activeDisciplinesCount").value(1))
                .andExpect(jsonPath("$.data.disciplines.length()").value(1))
                .andExpect(jsonPath("$.data.disciplines[0].type").value("WARNING"))

            verify(exactly = 1) { disciplineService.canPlayerPlay(1L, 1L) }
            verify(exactly = 1) { disciplineService.getActiveDisciplines(1L, 1L) }
        }

        @Test
        fun `출장 정지 징계가 있는 선수는 출장 불가능하다`() {
            // given
            val disciplines = listOf(createSuspension(1L, 3))
            every { disciplineService.canPlayerPlay(1L, 1L) } returns false
            every { disciplineService.getActiveDisciplines(1L, 1L) } returns disciplines

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/disciplines/players/1/eligibility")
                        .param("competitionId", "1"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.canPlay").value(false))
                .andExpect(jsonPath("$.data.activeDisciplinesCount").value(1))
                .andExpect(jsonPath("$.data.disciplines[0].type").value("SUSPENSION"))
                .andExpect(jsonPath("$.data.disciplines[0].suspensionGames").value(3))
                .andExpect(jsonPath("$.data.disciplines[0].servedGames").value(0))

            verify(exactly = 1) { disciplineService.canPlayerPlay(1L, 1L) }
            verify(exactly = 1) { disciplineService.getActiveDisciplines(1L, 1L) }
        }

        @Test
        fun `영구 제재 징계가 있는 선수는 출장 불가능하다`() {
            // given
            val disciplines = listOf(createBan(1L))
            every { disciplineService.canPlayerPlay(1L, 1L) } returns false
            every { disciplineService.getActiveDisciplines(1L, 1L) } returns disciplines

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/disciplines/players/1/eligibility")
                        .param("competitionId", "1"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.canPlay").value(false))
                .andExpect(jsonPath("$.data.activeDisciplinesCount").value(1))
                .andExpect(jsonPath("$.data.disciplines[0].type").value("BAN"))

            verify(exactly = 1) { disciplineService.canPlayerPlay(1L, 1L) }
            verify(exactly = 1) { disciplineService.getActiveDisciplines(1L, 1L) }
        }

        @Test
        fun `복수의 활성 징계가 있는 경우 모두 반환한다`() {
            // given
            val disciplines =
                listOf(
                    createWarning(1L),
                    createSuspension(2L, 2),
                )
            every { disciplineService.canPlayerPlay(1L, 1L) } returns false
            every { disciplineService.getActiveDisciplines(1L, 1L) } returns disciplines

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/disciplines/players/1/eligibility")
                        .param("competitionId", "1"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.canPlay").value(false))
                .andExpect(jsonPath("$.data.activeDisciplinesCount").value(2))
                .andExpect(jsonPath("$.data.disciplines.length()").value(2))

            verify(exactly = 1) { disciplineService.canPlayerPlay(1L, 1L) }
            verify(exactly = 1) { disciplineService.getActiveDisciplines(1L, 1L) }
        }

        @Test
        fun `일부 경기를 소화한 출장 정지 징계 정보를 확인할 수 있다`() {
            // given
            val suspension = createSuspension(1L, 3)
            suspension.incrementServedGames()
            val disciplines = listOf(suspension)
            every { disciplineService.canPlayerPlay(1L, 1L) } returns false
            every { disciplineService.getActiveDisciplines(1L, 1L) } returns disciplines

            // when & then
            mockMvc
                .perform(
                    get("/api/v1/disciplines/players/1/eligibility")
                        .param("competitionId", "1"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.canPlay").value(false))
                .andExpect(jsonPath("$.data.disciplines[0].suspensionGames").value(3))
                .andExpect(jsonPath("$.data.disciplines[0].servedGames").value(1))
                .andExpect(jsonPath("$.data.disciplines[0].isEffective").value(true))

            verify(exactly = 1) { disciplineService.canPlayerPlay(1L, 1L) }
            verify(exactly = 1) { disciplineService.getActiveDisciplines(1L, 1L) }
        }
    }

    // Helper methods
    private fun createWarning(id: Long): Discipline {
        val discipline =
            Discipline.createWarning(
                player = player,
                competition = competition,
                reason = "과도한 항의",
                issuedBy = "심판장",
            )
        setId(discipline, id)
        return discipline
    }

    private fun createSuspension(
        id: Long,
        suspensionGames: Int,
    ): Discipline {
        val discipline =
            Discipline.createSuspension(
                player = player,
                competition = competition,
                reason = "폭력 행위",
                suspensionGames = suspensionGames,
                issuedBy = "기술위원장",
            )
        setId(discipline, id)
        return discipline
    }

    private fun createBan(id: Long): Discipline {
        val discipline =
            Discipline.createBan(
                player = player,
                competition = competition,
                reason = "승부 조작",
                issuedBy = "협회장",
            )
        setId(discipline, id)
        return discipline
    }

    private fun setId(
        entity: Any,
        id: Long,
    ) {
        val idField = entity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }
}
