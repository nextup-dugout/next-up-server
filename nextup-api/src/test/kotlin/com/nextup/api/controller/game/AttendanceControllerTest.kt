package com.nextup.api.controller.game

import com.nextup.api.dto.attendance.AttendanceVoteRequest
import com.nextup.core.domain.game.AttendanceStatus
import com.nextup.core.domain.game.AttendanceVote
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.user.User
import com.nextup.core.service.game.AttendanceService
import com.nextup.core.service.game.dto.AttendanceSummaryDto
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

@DisplayName("AttendanceController")
class AttendanceControllerTest {
    private lateinit var attendanceService: AttendanceService
    private lateinit var controller: AttendanceController

    private val position =
        mockk<Position> {
            every { abbreviation } returns "P"
        }
    private val player =
        mockk<Player> {
            every { id } returns 100L
            every { name } returns "김투수"
            every { primaryPosition } returns position
        }
    private val user =
        mockk<User> {
            every { id } returns 10L
            every { nickname } returns "pitcher01"
        }
    private val member =
        mockk<TeamMember> {
            every { id } returns 50L
            every { this@mockk.user } returns this@AttendanceControllerTest.user
            every { this@mockk.player } returns this@AttendanceControllerTest.player
            every { uniformNumber } returns 18
        }
    private val game =
        mockk<Game> {
            every { id } returns 1L
            every { scheduledAt } returns LocalDateTime.of(2026, 3, 1, 14, 0)
        }

    @BeforeEach
    fun setUp() {
        attendanceService = mockk()
        controller = AttendanceController(attendanceService)
    }

    private fun createMockVote(
        id: Long = 1L,
        status: AttendanceStatus = AttendanceStatus.ATTENDING,
    ): AttendanceVote =
        mockk {
            every { this@mockk.id } returns id
            every { this@mockk.game } returns this@AttendanceControllerTest.game
            every { this@mockk.member } returns this@AttendanceControllerTest.member
            every { this@mockk.status } returns status
            every { reason } returns null
            every { respondedAt } returns LocalDateTime.of(2026, 2, 25, 10, 0)
        }

    @Nested
    @DisplayName("POST /api/v1/games/{gameId}/attendance")
    inner class Vote {
        @Test
        fun `should vote successfully`() {
            // given
            val request = AttendanceVoteRequest(status = AttendanceStatus.ATTENDING)
            val vote = createMockVote()
            every {
                attendanceService.findMemberInGame(1L, 10L)
            } returns member
            every {
                attendanceService.vote(
                    gameId = 1L,
                    memberId = 50L,
                    status = AttendanceStatus.ATTENDING,
                    reason = null,
                )
            } returns vote

            // when
            val response = controller.vote(gameId = 1L, request = request, userId = 10L)

            // then
            assertThat(response.data?.voteId).isEqualTo(1L)
            assertThat(response.data?.status).isEqualTo(AttendanceStatus.ATTENDING)
        }

        @Test
        fun `should throw when user is not a team member`() {
            // given
            val request = AttendanceVoteRequest(status = AttendanceStatus.ATTENDING)
            every {
                attendanceService.findMemberInGame(1L, 10L)
            } throws IllegalStateException("User is not a team member")

            // when & then
            assertThrows<IllegalStateException> {
                controller.vote(gameId = 1L, request = request, userId = 10L)
            }
        }

        @Test
        fun `should throw when game not found`() {
            // given
            val request = AttendanceVoteRequest(status = AttendanceStatus.ATTENDING)
            every {
                attendanceService.findMemberInGame(1L, 10L)
            } throws IllegalStateException("Game not found")

            // when & then
            assertThrows<IllegalStateException> {
                controller.vote(gameId = 1L, request = request, userId = 10L)
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/games/{gameId}/attendance")
    inner class GetVotes {
        @Test
        fun `should return votes for game`() {
            // given
            val votes = listOf(createMockVote())
            val summary =
                AttendanceSummaryDto(
                    gameId = 1L,
                    totalMembers = 10,
                    attending = 7,
                    absent = 2,
                    undecided = 1,
                )
            every {
                attendanceService.verifyGameTeamMember(1L, 10L)
            } just Runs
            every {
                attendanceService.getGameScheduledAt(1L)
            } returns LocalDateTime.of(2026, 3, 1, 14, 0)
            every {
                attendanceService.getVotesByGameId(1L)
            } returns votes
            every {
                attendanceService.getVoteSummary(1L)
            } returns summary

            // when
            val response = controller.getVotes(gameId = 1L, userId = 10L)

            // then
            assertThat(response.data?.gameId).isEqualTo(1L)
            assertThat(response.data?.votes).hasSize(1)
            assertThat(response.data?.summary?.attending).isEqualTo(7)
        }

        @Test
        fun `should throw when user is not a game team member`() {
            // given
            every {
                attendanceService.verifyGameTeamMember(1L, 10L)
            } throws IllegalStateException("User is not a game team member")

            // when & then
            assertThrows<IllegalStateException> {
                controller.getVotes(gameId = 1L, userId = 10L)
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/games/{gameId}/attendance/summary")
    inner class GetSummary {
        @Test
        fun `should return attendance summary`() {
            // given
            val summary =
                AttendanceSummaryDto(
                    gameId = 1L,
                    totalMembers = 10,
                    attending = 8,
                    absent = 1,
                    undecided = 1,
                )
            every {
                attendanceService.verifyGameTeamMember(1L, 10L)
            } just Runs
            every {
                attendanceService.getVoteSummary(1L)
            } returns summary

            // when
            val response = controller.getSummary(gameId = 1L, userId = 10L)

            // then
            assertThat(response.data?.gameId).isEqualTo(1L)
            assertThat(response.data?.attending).isEqualTo(8)
            assertThat(response.data?.responseRate).isEqualTo(0.9)
        }
    }

    @Nested
    @DisplayName("GET /api/v1/games/{gameId}/attendance/non-voters")
    inner class GetNonVoters {
        @Test
        fun `should return non-voters list`() {
            // given
            every {
                attendanceService.verifyGameTeamMember(1L, 10L)
            } just Runs
            every {
                attendanceService.getNonVoters(1L)
            } returns listOf(member)

            // when
            val response = controller.getNonVoters(gameId = 1L, userId = 10L)

            // then
            assertThat(response.data).hasSize(1)
            assertThat(response.data?.first()?.memberId).isEqualTo(50L)
            assertThat(response.data?.first()?.nickname).isEqualTo("pitcher01")
            assertThat(response.data?.first()?.uniformNumber).isEqualTo(18)
            assertThat(response.data?.first()?.position).isEqualTo("P")
        }
    }
}
