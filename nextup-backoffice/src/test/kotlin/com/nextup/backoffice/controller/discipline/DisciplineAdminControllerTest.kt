package com.nextup.backoffice.controller.discipline

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.nextup.backoffice.dto.discipline.IssueDisciplineRequest
import com.nextup.backoffice.exception.GlobalExceptionHandler
import com.nextup.common.exception.DisciplineNotFoundException
import com.nextup.common.exception.InvalidDisciplineStateException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.discipline.Discipline
import com.nextup.core.domain.discipline.DisciplineStatus
import com.nextup.core.domain.discipline.DisciplineType
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import com.nextup.core.service.discipline.DisciplineService
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("DisciplineAdminController 테스트")
class DisciplineAdminControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var disciplineService: DisciplineService
    private lateinit var controller: DisciplineAdminController
    private lateinit var objectMapper: ObjectMapper

    private lateinit var player: Player
    private lateinit var competition: Competition

    @BeforeEach
    fun setUp() {
        disciplineService = mockk()
        controller = DisciplineAdminController(disciplineService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
        objectMapper = ObjectMapper().registerModule(JavaTimeModule())

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
    @DisplayName("GET /api/backoffice/disciplines - 모든 징계 조회")
    inner class GetAllDisciplines {
        @Test
        fun `모든 징계 목록을 조회할 수 있다`() {
            // given
            val disciplines =
                listOf(
                    createWarning(1L),
                    createSuspension(2L, 3),
                )
            every { disciplineService.getAll() } returns disciplines

            // when & then
            mockMvc
                .perform(get("/api/backoffice/disciplines"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].type").value("WARNING"))
                .andExpect(jsonPath("$.data[1].type").value("SUSPENSION"))
                .andExpect(jsonPath("$.data[1].suspensionGames").value(3))

            verify(exactly = 1) { disciplineService.getAll() }
        }

        @Test
        fun `선수 ID로 필터링하여 조회할 수 있다`() {
            // given
            val disciplines = listOf(createWarning(1L))
            every { disciplineService.getDisciplinesByPlayer(1L) } returns disciplines

            // when & then
            mockMvc
                .perform(get("/api/backoffice/disciplines").param("playerId", "1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].playerId").value(1))

            verify(exactly = 1) { disciplineService.getDisciplinesByPlayer(1L) }
        }

        @Test
        fun `대회 ID로 필터링하여 조회할 수 있다`() {
            // given
            val disciplines = listOf(createWarning(1L))
            every { disciplineService.getDisciplinesByCompetition(1L) } returns disciplines

            // when & then
            mockMvc
                .perform(get("/api/backoffice/disciplines").param("competitionId", "1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))

            verify(exactly = 1) { disciplineService.getDisciplinesByCompetition(1L) }
        }

        @Test
        fun `선수와 대회 ID로 함께 필터링하여 조회할 수 있다`() {
            // given
            val disciplines = listOf(createWarning(1L))
            every {
                disciplineService.getDisciplinesByPlayerAndCompetition(1L, 1L)
            } returns disciplines

            // when & then
            mockMvc
                .perform(
                    get("/api/backoffice/disciplines")
                        .param("playerId", "1")
                        .param("competitionId", "1"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))

            verify(exactly = 1) {
                disciplineService.getDisciplinesByPlayerAndCompetition(1L, 1L)
            }
        }

        @Test
        fun `상태로 필터링하여 조회할 수 있다`() {
            // given
            val disciplines = listOf(createWarning(1L))
            every { disciplineService.getDisciplinesByStatus(DisciplineStatus.ACTIVE) } returns disciplines

            // when & then
            mockMvc
                .perform(get("/api/backoffice/disciplines").param("status", "ACTIVE"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))

            verify(exactly = 1) { disciplineService.getDisciplinesByStatus(DisciplineStatus.ACTIVE) }
        }
    }

    @Nested
    @DisplayName("GET /api/backoffice/disciplines/{id} - 징계 상세 조회")
    inner class GetDiscipline {
        @Test
        fun `징계 상세 정보를 조회할 수 있다`() {
            // given
            val discipline = createWarning(1L)
            every { disciplineService.getById(1L) } returns discipline

            // when & then
            mockMvc
                .perform(get("/api/backoffice/disciplines/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.type").value("WARNING"))
                .andExpect(jsonPath("$.data.reason").value("과도한 항의"))
                .andExpect(jsonPath("$.data.playerName").value("홍길동"))

            verify(exactly = 1) { disciplineService.getById(1L) }
        }

        @Test
        fun `존재하지 않는 징계 조회 시 404 오류를 반환한다`() {
            // given
            every { disciplineService.getById(999L) } throws DisciplineNotFoundException(999L)

            // when & then
            mockMvc
                .perform(get("/api/backoffice/disciplines/999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DISCIPLINE_NOT_FOUND"))

            verify(exactly = 1) { disciplineService.getById(999L) }
        }
    }

    @Nested
    @DisplayName("GET /api/backoffice/disciplines/active - 활성 징계 조회")
    inner class GetActiveDisciplines {
        @Test
        fun `선수의 활성 징계를 조회할 수 있다`() {
            // given
            val disciplines = listOf(createWarning(1L), createSuspension(2L, 2))
            every { disciplineService.getActiveDisciplines(1L, 1L) } returns disciplines

            // when & then
            mockMvc
                .perform(
                    get("/api/backoffice/disciplines/active")
                        .param("playerId", "1")
                        .param("competitionId", "1"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].isEffective").value(true))

            verify(exactly = 1) { disciplineService.getActiveDisciplines(1L, 1L) }
        }
    }

    @Nested
    @DisplayName("GET /api/backoffice/disciplines/eligibility - 출장 가능 여부 확인")
    inner class CheckPlayerEligibility {
        @Test
        fun `선수가 출장 가능한 경우 true를 반환한다`() {
            // given
            every { disciplineService.canPlayerPlay(1L, 1L) } returns true

            // when & then
            mockMvc
                .perform(
                    get("/api/backoffice/disciplines/eligibility")
                        .param("playerId", "1")
                        .param("competitionId", "1"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.canPlay").value(true))

            verify(exactly = 1) { disciplineService.canPlayerPlay(1L, 1L) }
        }

        @Test
        fun `선수가 출장 불가능한 경우 false를 반환한다`() {
            // given
            every { disciplineService.canPlayerPlay(1L, 1L) } returns false

            // when & then
            mockMvc
                .perform(
                    get("/api/backoffice/disciplines/eligibility")
                        .param("playerId", "1")
                        .param("competitionId", "1"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.canPlay").value(false))

            verify(exactly = 1) { disciplineService.canPlayerPlay(1L, 1L) }
        }
    }

    @Nested
    @DisplayName("POST /api/backoffice/disciplines - 징계 발급")
    inner class IssueDiscipline {
        @Test
        fun `경고 징계를 발급할 수 있다`() {
            // given
            val request =
                IssueDisciplineRequest(
                    playerId = 1L,
                    competitionId = 1L,
                    type = DisciplineType.WARNING,
                    reason = "과도한 항의",
                    issuedBy = "심판장",
                )
            val discipline = createWarning(1L)
            every {
                disciplineService.issueWarning(
                    playerId = 1L,
                    competitionId = 1L,
                    reason = "과도한 항의",
                    issuedBy = "심판장",
                    expiresAt = null,
                )
            } returns discipline

            // when & then
            mockMvc
                .perform(
                    post("/api/backoffice/disciplines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.type").value("WARNING"))

            verify(exactly = 1) {
                disciplineService.issueWarning(
                    playerId = 1L,
                    competitionId = 1L,
                    reason = "과도한 항의",
                    issuedBy = "심판장",
                    expiresAt = null,
                )
            }
        }

        @Test
        fun `만료일이 있는 경고 징계를 발급할 수 있다`() {
            // given
            val expiresAt = LocalDateTime.now().plusMonths(3)
            val request =
                IssueDisciplineRequest(
                    playerId = 1L,
                    competitionId = 1L,
                    type = DisciplineType.WARNING,
                    reason = "경고",
                    issuedBy = "심판장",
                    expiresAt = expiresAt,
                )
            val discipline = createWarning(1L, expiresAt)
            every {
                disciplineService.issueWarning(
                    playerId = 1L,
                    competitionId = 1L,
                    reason = "경고",
                    issuedBy = "심판장",
                    expiresAt = expiresAt,
                )
            } returns discipline

            // when & then
            mockMvc
                .perform(
                    post("/api/backoffice/disciplines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("WARNING"))

            verify(exactly = 1) {
                disciplineService.issueWarning(
                    playerId = 1L,
                    competitionId = 1L,
                    reason = "경고",
                    issuedBy = "심판장",
                    expiresAt = expiresAt,
                )
            }
        }

        @Test
        fun `출장 정지 징계를 발급할 수 있다`() {
            // given
            val request =
                IssueDisciplineRequest(
                    playerId = 1L,
                    competitionId = 1L,
                    type = DisciplineType.SUSPENSION,
                    reason = "폭력 행위",
                    suspensionGames = 3,
                    issuedBy = "기술위원장",
                )
            val discipline = createSuspension(1L, 3)
            every {
                disciplineService.issueSuspension(
                    playerId = 1L,
                    competitionId = 1L,
                    reason = "폭력 행위",
                    suspensionGames = 3,
                    issuedBy = "기술위원장",
                )
            } returns discipline

            // when & then
            mockMvc
                .perform(
                    post("/api/backoffice/disciplines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("SUSPENSION"))
                .andExpect(jsonPath("$.data.suspensionGames").value(3))

            verify(exactly = 1) {
                disciplineService.issueSuspension(
                    playerId = 1L,
                    competitionId = 1L,
                    reason = "폭력 행위",
                    suspensionGames = 3,
                    issuedBy = "기술위원장",
                )
            }
        }

        @Test
        fun `영구 제재 징계를 발급할 수 있다`() {
            // given
            val request =
                IssueDisciplineRequest(
                    playerId = 1L,
                    competitionId = 1L,
                    type = DisciplineType.BAN,
                    reason = "승부 조작",
                    issuedBy = "협회장",
                )
            val discipline = createBan(1L)
            every {
                disciplineService.issueBan(
                    playerId = 1L,
                    competitionId = 1L,
                    reason = "승부 조작",
                    issuedBy = "협회장",
                )
            } returns discipline

            // when & then
            mockMvc
                .perform(
                    post("/api/backoffice/disciplines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("BAN"))

            verify(exactly = 1) {
                disciplineService.issueBan(
                    playerId = 1L,
                    competitionId = 1L,
                    reason = "승부 조작",
                    issuedBy = "협회장",
                )
            }
        }

        @Test
        fun `출장 정지 징계 발급 시 경기 수가 없으면 400 오류를 반환한다`() {
            // given
            val request =
                IssueDisciplineRequest(
                    playerId = 1L,
                    competitionId = 1L,
                    type = DisciplineType.SUSPENSION,
                    reason = "폭력 행위",
                    suspensionGames = null,
                    issuedBy = "기술위원장",
                )

            // when & then
            mockMvc
                .perform(
                    post("/api/backoffice/disciplines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"))
        }

        @Test
        fun `출장 정지 징계 발급 시 경기 수가 0 이하이면 400 오류를 반환한다`() {
            // given
            val request =
                IssueDisciplineRequest(
                    playerId = 1L,
                    competitionId = 1L,
                    type = DisciplineType.SUSPENSION,
                    reason = "폭력 행위",
                    suspensionGames = 0,
                    issuedBy = "기술위원장",
                )

            // when & then
            mockMvc
                .perform(
                    post("/api/backoffice/disciplines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"))
        }
    }

    @Nested
    @DisplayName("PUT /api/backoffice/disciplines/{id}/cancel - 징계 취소")
    inner class CancelDiscipline {
        @Test
        fun `징계를 취소할 수 있다`() {
            // given
            val discipline = createWarning(1L)
            discipline.cancel()
            every { disciplineService.cancelDiscipline(1L) } returns discipline

            // when & then
            mockMvc
                .perform(put("/api/backoffice/disciplines/1/cancel"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))

            verify(exactly = 1) { disciplineService.cancelDiscipline(1L) }
        }

        @Test
        fun `이미 취소된 징계는 다시 취소할 수 없다`() {
            // given
            every { disciplineService.cancelDiscipline(1L) } throws
                InvalidDisciplineStateException("활성 상태의 징계만 취소할 수 있습니다.")

            // when & then
            mockMvc
                .perform(put("/api/backoffice/disciplines/1/cancel"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_DISCIPLINE_STATE"))

            verify(exactly = 1) { disciplineService.cancelDiscipline(1L) }
        }
    }

    @Nested
    @DisplayName("PUT /api/backoffice/disciplines/{id}/increment-served - 소화 경기 수 증가")
    inner class IncrementServedGames {
        @Test
        fun `출장 정지 징계의 소화 경기 수를 증가시킬 수 있다`() {
            // given
            val discipline = createSuspension(1L, 3)
            discipline.incrementServedGames()
            every { disciplineService.incrementServedGames(1L) } returns discipline

            // when & then
            mockMvc
                .perform(put("/api/backoffice/disciplines/1/increment-served"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.servedGames").value(1))

            verify(exactly = 1) { disciplineService.incrementServedGames(1L) }
        }

        @Test
        fun `모든 경기를 소화하면 이행 완료 상태가 된다`() {
            // given
            val discipline = createSuspension(1L, 2)
            discipline.incrementServedGames()
            discipline.incrementServedGames()
            every { disciplineService.incrementServedGames(1L) } returns discipline

            // when & then
            mockMvc
                .perform(put("/api/backoffice/disciplines/1/increment-served"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.servedGames").value(2))
                .andExpect(jsonPath("$.data.status").value("SERVED"))

            verify(exactly = 1) { disciplineService.incrementServedGames(1L) }
        }

        @Test
        fun `경고 징계는 소화 경기 수를 증가시킬 수 없다`() {
            // given
            every { disciplineService.incrementServedGames(1L) } throws
                InvalidDisciplineStateException("출장 정지 징계만 경기를 소화할 수 있습니다.")

            // when & then
            mockMvc
                .perform(put("/api/backoffice/disciplines/1/increment-served"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_DISCIPLINE_STATE"))

            verify(exactly = 1) { disciplineService.incrementServedGames(1L) }
        }
    }

    // Helper methods
    private fun createWarning(
        id: Long,
        expiresAt: LocalDateTime? = null,
    ): Discipline {
        val discipline =
            Discipline.createWarning(
                player = player,
                competition = competition,
                reason = "과도한 항의",
                issuedBy = "심판장",
                expiresAt = expiresAt,
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
