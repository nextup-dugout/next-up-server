package com.nextup.api.controller.user

import com.nextup.api.dto.user.UpdateProfileRequest
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.team.TeamMemberRole
import com.nextup.core.domain.user.OAuthProvider
import com.nextup.core.domain.user.User
import com.nextup.core.service.game.GameScheduleService
import com.nextup.core.service.game.dto.GameSummaryDto
import com.nextup.core.service.team.TeamMembershipService
import com.nextup.core.service.user.UserService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("ProfileController 테스트")
class ProfileControllerTest {
    private lateinit var userService: UserService
    private lateinit var teamMembershipService: TeamMembershipService
    private lateinit var gameScheduleService: GameScheduleService
    private lateinit var profileController: ProfileController

    @BeforeEach
    fun setUp() {
        userService = mockk()
        teamMembershipService = mockk()
        gameScheduleService = mockk()
        profileController =
            ProfileController(
                userService,
                teamMembershipService,
                gameScheduleService,
            )
    }

    private fun createTestUser(
        id: Long = 1L,
        hasOAuth: Boolean = false,
    ): User {
        val user =
            User.createLocalUser(
                email = "test@example.com",
                encodedPassword = "encoded",
                nickname = "테스터",
            )
        // Use reflection to set ID
        val idField = user.javaClass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(user, id)

        if (hasOAuth) {
            user.addOAuthAccount(OAuthProvider.GOOGLE, "google123", "test@example.com")
        }

        return user
    }

    private fun createMockTeam(
        teamId: Long,
        teamName: String,
        logoUrl: String? = null,
    ): Team {
        val team =
            mockk<Team> {
                every { id } returns teamId
                every { name } returns teamName
                every { this@mockk.logoUrl } returns logoUrl
            }
        return team
    }

    private fun createMockTeamMember(
        team: Team,
        role: TeamMemberRole = TeamMemberRole.MEMBER,
        uniformNumber: Int = 1,
        joinedAt: LocalDateTime = LocalDateTime.of(2026, 1, 1, 0, 0),
    ): TeamMember {
        val member =
            mockk<TeamMember> {
                every { this@mockk.team } returns team
                every { this@mockk.role } returns role
                every { this@mockk.uniformNumber } returns uniformNumber
                every { this@mockk.joinedAt } returns joinedAt
            }
        return member
    }

    @Nested
    @DisplayName("내 프로필 조회")
    inner class GetMyProfile {
        @Test
        fun `should return my profile successfully`() {
            // given
            val userId = 1L
            val user = createTestUser(userId)
            every { userService.getActiveById(userId) } returns user

            // when
            val response = profileController.getMyProfile(userId)

            // then
            assertThat(response.success).isTrue()
            assertThat(response.data?.id).isEqualTo(userId)
            assertThat(response.data?.email).isEqualTo("test@example.com")
            assertThat(response.data?.nickname).isEqualTo("테스터")
            verify { userService.getActiveById(userId) }
        }

        @Test
        fun `should include oauth provider info when linked`() {
            // given
            val userId = 1L
            val user = createTestUser(userId, hasOAuth = true)
            every { userService.getActiveById(userId) } returns user

            // when
            val response = profileController.getMyProfile(userId)

            // then
            assertThat(response.success).isTrue()
            assertThat(response.data?.linkedOAuthProviders).hasSize(1)
            assertThat(
                response.data
                    ?.linkedOAuthProviders
                    ?.first()
                    ?.provider,
            ).isEqualTo("GOOGLE")
        }
    }

    @Nested
    @DisplayName("내 소속 팀 목록 조회")
    inner class GetMyTeams {
        @Test
        fun `should return my teams successfully`() {
            // given
            val userId = 1L
            val team1 = createMockTeam(10L, "팀A", "https://logo.com/a.png")
            val team2 = createMockTeam(20L, "팀B", null)
            val member1 =
                createMockTeamMember(
                    team = team1,
                    role = TeamMemberRole.OWNER,
                    uniformNumber = 7,
                    joinedAt = LocalDateTime.of(2026, 1, 15, 0, 0),
                )
            val member2 =
                createMockTeamMember(
                    team = team2,
                    role = TeamMemberRole.MEMBER,
                    uniformNumber = 3,
                    joinedAt = LocalDateTime.of(2026, 3, 1, 0, 0),
                )

            every { teamMembershipService.getActiveTeamsByUserId(userId) } returns listOf(member1, member2)

            // when
            val response = profileController.getMyTeams(userId)

            // then
            assertThat(response.success).isTrue()
            assertThat(response.data).hasSize(2)

            val first = response.data!![0]
            assertThat(first.teamId).isEqualTo(10L)
            assertThat(first.teamName).isEqualTo("팀A")
            assertThat(first.teamLogoUrl).isEqualTo("https://logo.com/a.png")
            assertThat(first.role).isEqualTo(TeamMemberRole.OWNER)
            assertThat(first.roleDisplayName).isEqualTo("감독")
            assertThat(first.uniformNumber).isEqualTo(7)

            val second = response.data!![1]
            assertThat(second.teamId).isEqualTo(20L)
            assertThat(second.teamName).isEqualTo("팀B")
            assertThat(second.teamLogoUrl).isNull()
            assertThat(second.role).isEqualTo(TeamMemberRole.MEMBER)
            assertThat(second.uniformNumber).isEqualTo(3)

            verify { teamMembershipService.getActiveTeamsByUserId(userId) }
        }

        @Test
        fun `should return empty list when user has no teams`() {
            // given
            val userId = 1L
            every { teamMembershipService.getActiveTeamsByUserId(userId) } returns emptyList()

            // when
            val response = profileController.getMyTeams(userId)

            // then
            assertThat(response.success).isTrue()
            assertThat(response.data).isEmpty()
            verify { teamMembershipService.getActiveTeamsByUserId(userId) }
        }
    }

    @Nested
    @DisplayName("내 upcoming 경기 통합 조회")
    inner class GetMyUpcomingGames {
        @Test
        fun `should return upcoming games for all my teams`() {
            // given
            val userId = 1L
            val team1 = createMockTeam(10L, "팀A")
            val team2 = createMockTeam(20L, "팀B")
            val member1 = createMockTeamMember(team = team1)
            val member2 = createMockTeamMember(team = team2)

            every { teamMembershipService.getActiveTeamsByUserId(userId) } returns listOf(member1, member2)

            val gameSummaries =
                listOf(
                    GameSummaryDto(
                        gameId = 100L,
                        competitionId = 1L,
                        competitionName = "리그A",
                        homeTeamId = 10L,
                        homeTeamName = "팀A",
                        homeTeamLogoUrl = null,
                        awayTeamId = 30L,
                        awayTeamName = "팀C",
                        awayTeamLogoUrl = null,
                        scheduledAt = LocalDateTime.of(2026, 4, 1, 14, 0),
                        status = GameStatus.SCHEDULED,
                        homeScore = 0,
                        awayScore = 0,
                        location = "서울 잠실",
                        fieldName = "잠실구장",
                    ),
                    GameSummaryDto(
                        gameId = 200L,
                        competitionId = 2L,
                        competitionName = "리그B",
                        homeTeamId = 40L,
                        homeTeamName = "팀D",
                        homeTeamLogoUrl = null,
                        awayTeamId = 20L,
                        awayTeamName = "팀B",
                        awayTeamLogoUrl = null,
                        scheduledAt = LocalDateTime.of(2026, 4, 5, 10, 0),
                        status = GameStatus.SCHEDULED,
                        homeScore = 0,
                        awayScore = 0,
                        location = "부산 사직",
                        fieldName = "사직구장",
                    ),
                )

            every {
                gameScheduleService.getUpcomingGamesByTeamIds(listOf(10L, 20L), 10)
            } returns gameSummaries

            // when
            val response = profileController.getMyUpcomingGames(userId, 10)

            // then
            assertThat(response.success).isTrue()
            assertThat(response.data).hasSize(2)
            assertThat(response.data!![0].gameId).isEqualTo(100L)
            assertThat(response.data!![1].gameId).isEqualTo(200L)

            verify { teamMembershipService.getActiveTeamsByUserId(userId) }
            verify { gameScheduleService.getUpcomingGamesByTeamIds(listOf(10L, 20L), 10) }
        }

        @Test
        fun `should return empty list when user has no teams`() {
            // given
            val userId = 1L
            every { teamMembershipService.getActiveTeamsByUserId(userId) } returns emptyList()
            every { gameScheduleService.getUpcomingGamesByTeamIds(emptyList(), 10) } returns emptyList()

            // when
            val response = profileController.getMyUpcomingGames(userId, 10)

            // then
            assertThat(response.success).isTrue()
            assertThat(response.data).isEmpty()
        }

        @Test
        fun `should respect limit parameter`() {
            // given
            val userId = 1L
            val team1 = createMockTeam(10L, "팀A")
            val member1 = createMockTeamMember(team = team1)

            every { teamMembershipService.getActiveTeamsByUserId(userId) } returns listOf(member1)
            every { gameScheduleService.getUpcomingGamesByTeamIds(listOf(10L), 5) } returns emptyList()

            // when
            val response = profileController.getMyUpcomingGames(userId, 5)

            // then
            assertThat(response.success).isTrue()
            verify { gameScheduleService.getUpcomingGamesByTeamIds(listOf(10L), 5) }
        }
    }

    @Nested
    @DisplayName("프로필 수정")
    inner class UpdateMyProfile {
        @Test
        fun `should update profile with new nickname`() {
            // given
            val userId = 1L
            val request = UpdateProfileRequest(nickname = "새닉네임", profileImageUrl = null)
            val updatedUser = createTestUser(userId)

            every { userService.updateProfile(userId, "새닉네임", null) } returns updatedUser

            // when
            val response = profileController.updateMyProfile(userId, request)

            // then
            assertThat(response.success).isTrue()
            verify { userService.updateProfile(userId, "새닉네임", null) }
        }

        @Test
        fun `should update profile with new profile image url`() {
            // given
            val userId = 1L
            val request = UpdateProfileRequest(nickname = null, profileImageUrl = "https://example.com/image.jpg")
            val updatedUser = createTestUser(userId)

            every { userService.updateProfile(userId, null, "https://example.com/image.jpg") } returns updatedUser

            // when
            val response = profileController.updateMyProfile(userId, request)

            // then
            assertThat(response.success).isTrue()
            verify { userService.updateProfile(userId, null, "https://example.com/image.jpg") }
        }
    }

    @Nested
    @DisplayName("회원 탈퇴")
    inner class DeactivateMyAccount {
        @Test
        fun `should deactivate account successfully`() {
            // given
            val userId = 1L
            val user = createTestUser(userId)
            every { userService.deactivate(userId) } returns user

            // when
            val response = profileController.deactivateMyAccount(userId)

            // then
            assertThat(response.success).isTrue()
            verify { userService.deactivate(userId) }
        }
    }
}

@DisplayName("PublicProfileController 테스트")
class PublicProfileControllerTest {
    private lateinit var userService: UserService
    private lateinit var publicProfileController: PublicProfileController

    @BeforeEach
    fun setUp() {
        userService = mockk()
        publicProfileController = PublicProfileController(userService)
    }

    private fun createTestUser(id: Long = 1L): User {
        val user =
            User.createLocalUser(
                email = "test@example.com",
                encodedPassword = "encoded",
                nickname = "테스터",
            )
        val idField = user.javaClass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(user, id)
        return user
    }

    @Nested
    @DisplayName("공개 프로필 조회")
    inner class GetPublicProfile {
        @Test
        fun `should return public profile successfully`() {
            // given
            val userId = 1L
            val user = createTestUser(userId)
            every { userService.getActiveById(userId) } returns user

            // when
            val response = publicProfileController.getPublicProfile(userId)

            // then
            assertThat(response.success).isTrue()
            assertThat(response.data?.id).isEqualTo(userId)
            assertThat(response.data?.nickname).isEqualTo("테스터")
            assertThat(response.data?.hasLinkedPlayer).isFalse()
            verify { userService.getActiveById(userId) }
        }
    }
}
