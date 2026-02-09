package com.nextup.api.controller.attendance

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.nextup.api.dto.attendance.UpdateActivityScoreRequest
import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.attendance.ActivityScore
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.user.User
import com.nextup.core.service.attendance.ActivityService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.math.BigDecimal

@DisplayName("TeamActivityController")
class TeamActivityControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var activityService: ActivityService
    private lateinit var controller: TeamActivityController
    private lateinit var objectMapper: ObjectMapper

    private lateinit var team: Team
    private lateinit var member: TeamMember
    private lateinit var activityScore: ActivityScore

    @BeforeEach
    fun setUp() {
        activityService = mockk()
        controller = TeamActivityController(activityService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()

        objectMapper = ObjectMapper().registerKotlinModule()

        val association = Association(name = "테스트 협회", id = 1L)
        val league =
            League(association = association, name = "테스트 리그", foundedYear = 2024, id = 1L)
        team = Team(league = league, name = "테스트 팀", city = "서울", foundedYear = 2024, id = 1L)
        val user = User.createLocalUser(email = "test@test.com", encodedPassword = "password123", nickname = "테스트")
        val player = Player(name = "홍길동", primaryPosition = Position.STARTING_PITCHER, id = 1L)
        member = TeamMember.create(team = team, user = user, player = player, uniformNumber = 10)
        activityScore = ActivityScore.create(team = team, member = member)
    }

    @Nested
    @DisplayName("GET /api/v1/teams/{teamId}/members/{memberId}/activity")
    inner class GetActivity {
        @Test
        fun `팀원의 활동 점수를 조회할 수 있다`() {
            // given
            every { activityService.getActivityScore(1L, 1L) } returns activityScore

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/1/members/1/activity"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.teamId").value(team.id))
                .andExpect(jsonPath("$.data.memberId").value(member.id))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/teams/{teamId}/members/activity")
    inner class ListActivities {
        @Test
        fun `팀의 모든 활동 점수를 조회할 수 있다`() {
            // given
            every { activityService.listActivityScores(1L) } returns listOf(activityScore)

            // when & then
            mockMvc
                .perform(get("/api/v1/teams/1/members/activity"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(1))
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/teams/{teamId}/members/{memberId}/activity/game-participation")
    inner class UpdateGameParticipation {
        @Test
        fun `경기 참여율을 업데이트할 수 있다`() {
            // given
            val request = UpdateActivityScoreRequest(BigDecimal("85.50"))
            activityScore.updateGameParticipationRate(BigDecimal("85.50"))
            every {
                activityService.updateGameParticipationRate(1L, 1L, BigDecimal("85.50"))
            } returns activityScore

            // when & then
            mockMvc
                .perform(
                    put("/api/v1/teams/1/members/1/activity/game-participation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.gameParticipationRate").value(85.50))
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/teams/{teamId}/members/{memberId}/activity/practice-attendance")
    inner class UpdatePracticeAttendance {
        @Test
        fun `연습 참석률을 업데이트할 수 있다`() {
            // given
            val request = UpdateActivityScoreRequest(BigDecimal("90.00"))
            activityScore.updatePracticeAttendanceRate(BigDecimal("90.00"))
            every {
                activityService.updatePracticeAttendanceRate(1L, 1L, BigDecimal("90.00"))
            } returns activityScore

            // when & then
            mockMvc
                .perform(
                    put("/api/v1/teams/1/members/1/activity/practice-attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.practiceAttendanceRate").value(90.00))
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/teams/{teamId}/members/{memberId}/activity/contribution")
    inner class UpdateContribution {
        @Test
        fun `기여도 점수를 업데이트할 수 있다`() {
            // given
            val request = UpdateActivityScoreRequest(BigDecimal("75.00"))
            activityScore.updateContributionScore(BigDecimal("75.00"))
            every {
                activityService.updateContributionScore(1L, 1L, BigDecimal("75.00"))
            } returns activityScore

            // when & then
            mockMvc
                .perform(
                    put("/api/v1/teams/1/members/1/activity/contribution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.contributionScore").value(75.00))
        }
    }
}
