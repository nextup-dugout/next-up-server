package com.nextup.api.controller.team

import com.nextup.api.dto.team.CreateTeamRequest
import com.nextup.api.dto.team.UpdateTeamRequest
import com.nextup.common.exception.InvalidInputException
import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import com.nextup.core.service.team.TeamMembershipService
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("TeamController")
class TeamControllerTest {
    private lateinit var teamMembershipService: TeamMembershipService
    private lateinit var controller: TeamController

    private val league =
        mockk<League> {
            every { id } returns 1L
            every { name } returns "1부 리그"
        }
    private val team =
        mockk<Team> {
            every { id } returns 10L
            every { name } returns "타이거즈"
            every { city } returns "서울"
            every { abbreviation } returns "TGR"
            every { logoUrl } returns null
            every { this@mockk.league } returns this@TeamControllerTest.league
            every { foundedYear } returns 2026
            every { isActive } returns true
        }

    @BeforeEach
    fun setUp() {
        teamMembershipService = mockk()
        controller = TeamController(teamMembershipService)
    }

    @Nested
    @DisplayName("POST /api/v1/teams")
    inner class CreateTeam {
        @Test
        fun `should create team and register owner`() {
            // given
            val request =
                CreateTeamRequest(
                    name = "타이거즈",
                    city = "서울",
                    leagueId = 1L,
                    abbreviation = "TGR",
                    uniformNumber = 1,
                )
            every {
                teamMembershipService.createTeamWithOwner(
                    userId = 100L,
                    leagueId = 1L,
                    name = "타이거즈",
                    city = "서울",
                    abbreviation = "TGR",
                    foundedYear = any(),
                    uniformNumber = 1,
                )
            } returns team
            every { teamMembershipService.getTeamMemberCount(10L) } returns 1

            // when
            val response = controller.createTeam(request, 100L)

            // then
            assertThat(response.success).isTrue()
            assertThat(response.data?.teamId).isEqualTo(10L)
            assertThat(response.data?.name).isEqualTo("타이거즈")
            assertThat(response.data?.city).isEqualTo("서울")
            assertThat(response.data?.leagueName).isEqualTo("1부 리그")
            assertThat(response.data?.memberCount).isEqualTo(1)
        }

        @Test
        fun `should throw when leagueId is null`() {
            // given
            val request =
                CreateTeamRequest(
                    name = "타이거즈",
                    city = "서울",
                    leagueId = null,
                )

            // when & then
            assertThrows<InvalidInputException> {
                controller.createTeam(request, 100L)
            }
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/teams/{teamId}")
    inner class UpdateTeam {
        @Test
        fun `should update team`() {
            // given
            val request = UpdateTeamRequest(name = "뉴타이거즈")
            every {
                teamMembershipService.updateTeam(
                    teamId = 10L,
                    name = "뉴타이거즈",
                    city = null,
                    abbreviation = null,
                )
            } returns team
            every { teamMembershipService.getTeamMemberCount(10L) } returns 5

            // when
            val response = controller.updateTeam(10L, request, 100L)

            // then
            assertThat(response.success).isTrue()
            assertThat(response.data?.teamId).isEqualTo(10L)
        }

        @Test
        fun `should throw when team not found`() {
            // given
            val request = UpdateTeamRequest(name = "뉴타이거즈")
            every {
                teamMembershipService.updateTeam(
                    teamId = 10L,
                    name = "뉴타이거즈",
                    city = null,
                    abbreviation = null,
                )
            } throws TeamNotFoundException(10L)

            // when & then
            assertThrows<TeamNotFoundException> {
                controller.updateTeam(10L, request, 100L)
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/teams/{teamId}")
    inner class GetTeam {
        @Test
        fun `should return team detail`() {
            // given
            every { teamMembershipService.getTeamWithLeague(10L) } returns team
            every { teamMembershipService.getTeamMemberCount(10L) } returns 15

            // when
            val response = controller.getTeam(10L)

            // then
            assertThat(response.success).isTrue()
            assertThat(response.data?.teamId).isEqualTo(10L)
            assertThat(response.data?.name).isEqualTo("타이거즈")
            assertThat(response.data?.memberCount).isEqualTo(15)
        }

        @Test
        fun `should throw when team not found`() {
            // given
            every { teamMembershipService.getTeamWithLeague(999L) } throws TeamNotFoundException(999L)

            // when & then
            assertThrows<TeamNotFoundException> {
                controller.getTeam(999L)
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/teams")
    inner class GetTeams {
        @Test
        fun `should return all active teams`() {
            // given
            val team2 =
                mockk<Team> {
                    every { id } returns 20L
                    every { name } returns "이글스"
                    every { city } returns "부산"
                    every { abbreviation } returns "EGL"
                    every { logoUrl } returns null
                    every { isActive } returns true
                }
            every { teamMembershipService.getActiveTeamsByFilter(null, null) } returns listOf(team, team2)
            every { teamMembershipService.getTeamMemberCounts(listOf(10L, 20L)) } returns mapOf(10L to 15, 20L to 12)

            // when
            val response = controller.getTeams(null, null)

            // then
            assertThat(response.success).isTrue()
            assertThat(response.data).hasSize(2)
        }

        @Test
        fun `should filter by name`() {
            // given
            every { teamMembershipService.getActiveTeamsByFilter("타이거", null) } returns listOf(team)
            every { teamMembershipService.getTeamMemberCounts(listOf(10L)) } returns mapOf(10L to 15)

            // when
            val response = controller.getTeams(name = "타이거", city = null)

            // then
            assertThat(response.data).hasSize(1)
            assertThat(response.data?.first()?.name).isEqualTo("타이거즈")
        }

        @Test
        fun `should filter by city`() {
            // given
            every { teamMembershipService.getActiveTeamsByFilter(null, "서울") } returns listOf(team)
            every { teamMembershipService.getTeamMemberCounts(listOf(10L)) } returns mapOf(10L to 15)

            // when
            val response = controller.getTeams(name = null, city = "서울")

            // then
            assertThat(response.data).hasSize(1)
        }

        @Test
        fun `should default memberCount to zero when not in counts map`() {
            // given
            val team2 =
                mockk<Team> {
                    every { id } returns 20L
                    every { name } returns "이글스"
                    every { city } returns "부산"
                    every { abbreviation } returns "EGL"
                    every { logoUrl } returns null
                    every { isActive } returns true
                }
            every {
                teamMembershipService.getActiveTeamsByFilter(null, null)
            } returns listOf(team, team2)
            every {
                teamMembershipService.getTeamMemberCounts(listOf(10L, 20L))
            } returns mapOf(10L to 15)

            // when
            val response = controller.getTeams(null, null)

            // then
            assertThat(response.success).isTrue()
            assertThat(response.data).hasSize(2)
            val team2Response = response.data?.find { it.teamId == 20L }
            assertThat(team2Response?.memberCount).isEqualTo(0)
        }

        @Test
        fun `should return empty when no match`() {
            // given
            every { teamMembershipService.getActiveTeamsByFilter("없는팀", null) } returns emptyList()
            every { teamMembershipService.getTeamMemberCounts(emptyList()) } returns emptyMap()

            // when
            val response = controller.getTeams(name = "없는팀", city = null)

            // then
            assertThat(response.data).isEmpty()
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/teams/{teamId}")
    inner class DeleteTeam {
        @Test
        fun `should delete team when owner and single member`() {
            // given
            justRun { teamMembershipService.deleteTeam(10L, 100L) }

            // when
            val response = controller.deleteTeam(10L, 100L)

            // then
            assertThat(response.success).isTrue()
            verify { teamMembershipService.deleteTeam(10L, 100L) }
        }

        @Test
        fun `should throw when team not found on delete`() {
            // given
            every {
                teamMembershipService.deleteTeam(999L, 100L)
            } throws TeamNotFoundException(999L)

            // when & then
            assertThrows<TeamNotFoundException> {
                controller.deleteTeam(999L, 100L)
            }
        }
    }
}
