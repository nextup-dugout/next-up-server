package com.nextup.api.controller.team

import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.service.team.TeamMatchupService
import com.nextup.core.service.team.dto.TeamMatchupDto
import com.nextup.core.service.team.dto.TeamMatchupGameDto
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class TeamMatchupControllerTest {
    private val teamMatchupService: TeamMatchupService = mockk()
    private val controller = TeamMatchupController(teamMatchupService)

    @Test
    fun `should return team matchup successfully`() {
        // given
        val teamId = 1L
        val opponentId = 2L
        val competitionId = 10L

        val matchupDto =
            TeamMatchupDto(
                teamId = teamId,
                teamName = "서울 타이거즈",
                opponentId = opponentId,
                opponentName = "부산 라이온즈",
                wins = 5,
                losses = 3,
                draws = 1,
                totalGames = 9,
                runsScored = 45,
                runsAllowed = 30,
                avgRunsScored = 5.0,
                avgRunsAllowed = 3.33,
            )

        every {
            teamMatchupService.getTeamMatchup(teamId, opponentId, competitionId)
        } returns matchupDto

        // when
        val response = controller.getTeamMatchup(teamId, opponentId, competitionId)

        // then
        assertThat(response.success).isTrue()
        assertThat(response.data).isNotNull
        assertThat(response.data?.teamId).isEqualTo(teamId)
        assertThat(response.data?.opponentId).isEqualTo(opponentId)
        assertThat(response.data?.record?.wins).isEqualTo(5)
        assertThat(response.data?.record?.losses).isEqualTo(3)
        assertThat(response.data?.record?.draws).isEqualTo(1)
        assertThat(response.data?.record?.totalGames).isEqualTo(9)
        assertThat(response.data?.record?.winRate).isEqualTo(5.0 / 9.0)
        assertThat(response.data?.runs?.runsScored).isEqualTo(45)
        assertThat(response.data?.runs?.runsAllowed).isEqualTo(30)
        assertThat(response.data?.runs?.runDifferential).isEqualTo(15)

        verify(exactly = 1) {
            teamMatchupService.getTeamMatchup(teamId, opponentId, competitionId)
        }
    }

    @Test
    fun `should return team matchup without competition filter`() {
        // given
        val teamId = 1L
        val opponentId = 2L

        val matchupDto =
            TeamMatchupDto(
                teamId = teamId,
                teamName = "서울 타이거즈",
                opponentId = opponentId,
                opponentName = "부산 라이온즈",
                wins = 10,
                losses = 5,
                draws = 2,
                totalGames = 17,
                runsScored = 85,
                runsAllowed = 60,
                avgRunsScored = 5.0,
                avgRunsAllowed = 3.53,
            )

        every {
            teamMatchupService.getTeamMatchup(teamId, opponentId, null)
        } returns matchupDto

        // when
        val response = controller.getTeamMatchup(teamId, opponentId, null)

        // then
        assertThat(response.success).isTrue()
        assertThat(response.data).isNotNull
        assertThat(response.data?.record?.totalGames).isEqualTo(17)

        verify(exactly = 1) {
            teamMatchupService.getTeamMatchup(teamId, opponentId, null)
        }
    }

    @Test
    fun `should throw TeamNotFoundException when team does not exist`() {
        // given
        val teamId = 999L
        val opponentId = 2L

        every {
            teamMatchupService.getTeamMatchup(teamId, opponentId, null)
        } throws TeamNotFoundException(teamId)

        // when & then
        assertThrows<TeamNotFoundException> {
            controller.getTeamMatchup(teamId, opponentId, null)
        }
    }

    @Test
    fun `should return recent games successfully`() {
        // given
        val teamId = 1L
        val opponentId = 2L
        val limit = 5

        val games =
            listOf(
                TeamMatchupGameDto(
                    gameId = 1L,
                    date = LocalDate.of(2024, 1, 15),
                    homeTeamId = teamId,
                    awayTeamId = opponentId,
                    homeScore = 5,
                    awayScore = 3,
                    result = "WIN",
                    venue = "서울 야구장",
                ),
                TeamMatchupGameDto(
                    gameId = 2L,
                    date = LocalDate.of(2024, 1, 10),
                    homeTeamId = opponentId,
                    awayTeamId = teamId,
                    homeScore = 4,
                    awayScore = 2,
                    result = "LOSS",
                    venue = "부산 야구장",
                ),
            )

        every {
            teamMatchupService.getRecentGames(teamId, opponentId, limit)
        } returns games

        // when
        val response = controller.getRecentGames(teamId, opponentId, limit)

        // then
        assertThat(response.success).isTrue()
        assertThat(response.data).isNotNull
        assertThat(response.data).hasSize(2)
        assertThat(response.data?.get(0)?.gameId).isEqualTo(1L)
        assertThat(response.data?.get(0)?.result).isEqualTo("WIN")
        assertThat(response.data?.get(1)?.gameId).isEqualTo(2L)
        assertThat(response.data?.get(1)?.result).isEqualTo("LOSS")

        verify(exactly = 1) {
            teamMatchupService.getRecentGames(teamId, opponentId, limit)
        }
    }

    @Test
    fun `should use default limit when not specified`() {
        // given
        val teamId = 1L
        val opponentId = 2L
        val defaultLimit = 10

        every {
            teamMatchupService.getRecentGames(teamId, opponentId, defaultLimit)
        } returns emptyList()

        // when
        val response = controller.getRecentGames(teamId, opponentId, defaultLimit)

        // then
        assertThat(response.success).isTrue()
        assertThat(response.data).isNotNull
        assertThat(response.data).isEmpty()

        verify(exactly = 1) {
            teamMatchupService.getRecentGames(teamId, opponentId, defaultLimit)
        }
    }

    @Test
    fun `should cap limit to maximum of 50`() {
        // given
        val teamId = 1L
        val opponentId = 2L
        val requestedLimit = 100
        val expectedLimit = 50

        every {
            teamMatchupService.getRecentGames(teamId, opponentId, expectedLimit)
        } returns emptyList()

        // when
        val response = controller.getRecentGames(teamId, opponentId, requestedLimit)

        // then
        assertThat(response.success).isTrue()

        verify(exactly = 1) {
            teamMatchupService.getRecentGames(teamId, opponentId, expectedLimit)
        }
    }

    @Test
    fun `should enforce minimum limit of 1`() {
        // given
        val teamId = 1L
        val opponentId = 2L
        val requestedLimit = 0
        val expectedLimit = 1

        every {
            teamMatchupService.getRecentGames(teamId, opponentId, expectedLimit)
        } returns emptyList()

        // when
        val response = controller.getRecentGames(teamId, opponentId, requestedLimit)

        // then
        assertThat(response.success).isTrue()

        verify(exactly = 1) {
            teamMatchupService.getRecentGames(teamId, opponentId, expectedLimit)
        }
    }

    @Test
    fun `should return empty list when no games found`() {
        // given
        val teamId = 1L
        val opponentId = 2L
        val limit = 10

        every {
            teamMatchupService.getRecentGames(teamId, opponentId, limit)
        } returns emptyList()

        // when
        val response = controller.getRecentGames(teamId, opponentId, limit)

        // then
        assertThat(response.success).isTrue()
        assertThat(response.data).isNotNull
        assertThat(response.data).isEmpty()
    }

    @Test
    fun `should calculate win rate correctly for zero games`() {
        // given
        val teamId = 1L
        val opponentId = 2L

        val matchupDto =
            TeamMatchupDto(
                teamId = teamId,
                teamName = "서울 타이거즈",
                opponentId = opponentId,
                opponentName = "부산 라이온즈",
                wins = 0,
                losses = 0,
                draws = 0,
                totalGames = 0,
                runsScored = 0,
                runsAllowed = 0,
                avgRunsScored = 0.0,
                avgRunsAllowed = 0.0,
            )

        every {
            teamMatchupService.getTeamMatchup(teamId, opponentId, null)
        } returns matchupDto

        // when
        val response = controller.getTeamMatchup(teamId, opponentId, null)

        // then
        assertThat(response.success).isTrue()
        assertThat(response.data?.record?.winRate).isEqualTo(0.0)
        assertThat(response.data?.runs?.runDifferential).isEqualTo(0)
    }
}
