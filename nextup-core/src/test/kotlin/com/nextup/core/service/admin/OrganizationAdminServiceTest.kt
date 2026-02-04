package com.nextup.core.service.admin

import com.nextup.common.exception.*
import com.nextup.core.domain.admin.OrganizationAdmin
import com.nextup.core.domain.admin.OrganizationRole
import com.nextup.core.domain.admin.OrganizationType
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.user.User
import com.nextup.core.port.repository.OrganizationAdminRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.port.repository.UserRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

@DisplayName("OrganizationAdminService")
class OrganizationAdminServiceTest {
    private lateinit var organizationAdminRepository: OrganizationAdminRepositoryPort
    private lateinit var userRepository: UserRepositoryPort
    private lateinit var teamRepository: TeamRepositoryPort
    private lateinit var organizationAdminService: OrganizationAdminService

    @BeforeEach
    fun setUp() {
        organizationAdminRepository = mockk()
        userRepository = mockk()
        teamRepository = mockk()
        organizationAdminService =
            OrganizationAdminService(
                organizationAdminRepository,
                userRepository,
                teamRepository,
            )
    }

    @Nested
    @DisplayName("assignAdmin")
    inner class AssignAdmin {
        @Test
        fun `관리자를 할당할 수 있다`() {
            // given
            val userId = 1L
            val user = createUser(userId, "test@example.com")
            val organizationType = OrganizationType.ASSOCIATION
            val organizationId = 1L
            val role = OrganizationRole.ADMIN

            every { userRepository.findByIdOrNull(userId) } returns user
            every {
                organizationAdminRepository.findByUserIdAndOrganizationTypeAndOrganizationId(
                    userId,
                    organizationType,
                    organizationId,
                )
            } returns null
            every { organizationAdminRepository.save(any()) } answers { firstArg() }

            // when
            val admin =
                organizationAdminService.assignAdmin(
                    userId = userId,
                    organizationType = organizationType,
                    organizationId = organizationId,
                    role = role,
                )

            // then
            assertThat(admin.user.id).isEqualTo(userId)
            assertThat(admin.organizationType).isEqualTo(organizationType)
            assertThat(admin.organizationId).isEqualTo(organizationId)
            assertThat(admin.role).isEqualTo(role)
            assertThat(admin.isActive).isTrue()
            verify { organizationAdminRepository.save(any()) }
        }

        @Test
        fun `존재하지 않는 사용자에게는 관리자를 할당할 수 없다`() {
            // given
            val userId = 99999L
            every { userRepository.findByIdOrNull(userId) } returns null

            // when & then
            assertThatThrownBy {
                organizationAdminService.assignAdmin(
                    userId = userId,
                    organizationType = OrganizationType.ASSOCIATION,
                    organizationId = 1L,
                    role = OrganizationRole.ADMIN,
                )
            }.isInstanceOf(UserNotFoundException::class.java)
        }

        @Test
        fun `이미 할당된 관리자는 다시 할당할 수 없다`() {
            // given
            val userId = 1L
            val user = createUser(userId, "test@example.com")
            val organizationType = OrganizationType.ASSOCIATION
            val organizationId = 1L
            val existingAdmin = createOrganizationAdmin(1L, user, organizationType, organizationId)

            every { userRepository.findByIdOrNull(userId) } returns user
            every {
                organizationAdminRepository.findByUserIdAndOrganizationTypeAndOrganizationId(
                    userId,
                    organizationType,
                    organizationId,
                )
            } returns existingAdmin

            // when & then
            assertThatThrownBy {
                organizationAdminService.assignAdmin(
                    userId = userId,
                    organizationType = organizationType,
                    organizationId = organizationId,
                    role = OrganizationRole.MANAGER,
                )
            }.isInstanceOf(OrganizationAdminAlreadyExistsException::class.java)
        }

        @Test
        fun `같은 리그 내 여러 팀의 관리자가 될 수 없다`() {
            // given
            val userId = 1L
            val user = createUser(userId, "test@example.com")
            val leagueId = 100L
            val team1Id = 1L
            val team2Id = 2L

            val association = createAssociation(1L, "Test Association")
            val league = createLeague(leagueId, "Test League", association)
            val team1 = createTeam(team1Id, "Team 1", league)
            val team2 = createTeam(team2Id, "Team 2", league)

            val existingAdmin = createOrganizationAdmin(1L, user, OrganizationType.TEAM, team1Id)

            every { userRepository.findByIdOrNull(userId) } returns user
            every {
                organizationAdminRepository.findByUserIdAndOrganizationTypeAndOrganizationId(
                    userId,
                    OrganizationType.TEAM,
                    team2Id,
                )
            } returns null
            every { teamRepository.findByIdWithLeague(team2Id) } returns team2
            every {
                organizationAdminRepository.findActiveByUserIdAndOrganizationType(
                    userId,
                    OrganizationType.TEAM,
                )
            } returns listOf(existingAdmin)
            every { teamRepository.findByIdWithLeague(team1Id) } returns team1

            // when & then
            assertThatThrownBy {
                organizationAdminService.assignAdmin(
                    userId = userId,
                    organizationType = OrganizationType.TEAM,
                    organizationId = team2Id,
                    role = OrganizationRole.ADMIN,
                )
            }.isInstanceOf(SameLeagueConflictException::class.java)
                .hasMessageContaining("leagueId=$leagueId")
                .hasMessageContaining("existingTeamId=$team1Id")
                .hasMessageContaining("newTeamId=$team2Id")
        }

        @Test
        fun `서로 다른 리그의 여러 팀 관리자가 될 수 있다`() {
            // given
            val userId = 1L
            val user = createUser(userId, "test@example.com")
            val league1Id = 100L
            val league2Id = 200L
            val team1Id = 1L
            val team2Id = 2L

            val association = createAssociation(1L, "Test Association")
            val league1 = createLeague(league1Id, "League 1", association)
            val league2 = createLeague(league2Id, "League 2", association)
            val team1 = createTeam(team1Id, "Team 1", league1)
            val team2 = createTeam(team2Id, "Team 2", league2)

            val existingAdmin = createOrganizationAdmin(1L, user, OrganizationType.TEAM, team1Id)

            every { userRepository.findByIdOrNull(userId) } returns user
            every {
                organizationAdminRepository.findByUserIdAndOrganizationTypeAndOrganizationId(
                    userId,
                    OrganizationType.TEAM,
                    team2Id,
                )
            } returns null
            every { teamRepository.findByIdWithLeague(team2Id) } returns team2
            every {
                organizationAdminRepository.findActiveByUserIdAndOrganizationType(
                    userId,
                    OrganizationType.TEAM,
                )
            } returns listOf(existingAdmin)
            every { teamRepository.findByIdWithLeague(team1Id) } returns team1
            every { organizationAdminRepository.save(any()) } answers { firstArg() }

            // when
            val admin =
                organizationAdminService.assignAdmin(
                    userId = userId,
                    organizationType = OrganizationType.TEAM,
                    organizationId = team2Id,
                    role = OrganizationRole.ADMIN,
                )

            // then
            assertThat(admin.organizationId).isEqualTo(team2Id)
            verify { organizationAdminRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("removeAdmin")
    inner class RemoveAdmin {
        @Test
        fun `관리자 권한을 해제할 수 있다`() {
            // given
            val userId = 1L
            val user = createUser(userId, "test@example.com")
            val organizationType = OrganizationType.ASSOCIATION
            val organizationId = 1L
            val admin = createOrganizationAdmin(1L, user, organizationType, organizationId)

            every {
                organizationAdminRepository.findByUserIdAndOrganizationTypeAndOrganizationId(
                    userId,
                    organizationType,
                    organizationId,
                )
            } returns admin

            // when
            organizationAdminService.removeAdmin(userId, organizationType, organizationId)

            // then
            assertThat(admin.isActive).isFalse()
        }

        @Test
        fun `존재하지 않는 관리자는 해제할 수 없다`() {
            // given
            val userId = 1L
            every {
                organizationAdminRepository.findByUserIdAndOrganizationTypeAndOrganizationId(
                    userId,
                    OrganizationType.ASSOCIATION,
                    1L,
                )
            } returns null

            // when & then
            assertThatThrownBy {
                organizationAdminService.removeAdmin(
                    userId = userId,
                    organizationType = OrganizationType.ASSOCIATION,
                    organizationId = 1L,
                )
            }.isInstanceOf(OrganizationAdminNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("getAdminsByOrganization")
    inner class GetAdminsByOrganization {
        @Test
        fun `특정 조직의 관리자 목록을 조회할 수 있다`() {
            // given
            val organizationType = OrganizationType.ASSOCIATION
            val organizationId = 1L
            val user1 = createUser(1L, "user1@example.com")
            val user2 = createUser(2L, "user2@example.com")
            val admin1 = createOrganizationAdmin(1L, user1, organizationType, organizationId, OrganizationRole.ADMIN)
            val admin2 = createOrganizationAdmin(2L, user2, organizationType, organizationId, OrganizationRole.MANAGER)

            every {
                organizationAdminRepository.findActiveByOrganizationTypeAndOrganizationId(
                    organizationType,
                    organizationId,
                )
            } returns listOf(admin1, admin2)

            // when
            val admins = organizationAdminService.getAdminsByOrganization(organizationType, organizationId)

            // then
            assertThat(admins).hasSize(2)
            assertThat(admins.map { it.user.id }).containsExactlyInAnyOrder(1L, 2L)
        }
    }

    @Nested
    @DisplayName("getOrganizationsByUser")
    inner class GetOrganizationsByUser {
        @Test
        fun `사용자가 관리하는 모든 조직을 조회할 수 있다`() {
            // given
            val userId = 1L
            val user = createUser(userId, "test@example.com")
            val admin1 = createOrganizationAdmin(1L, user, OrganizationType.ASSOCIATION, 1L)
            val admin2 = createOrganizationAdmin(2L, user, OrganizationType.LEAGUE, 2L)

            every { organizationAdminRepository.findActiveByUserId(userId) } returns listOf(admin1, admin2)

            // when
            val organizations = organizationAdminService.getOrganizationsByUser(userId)

            // then
            assertThat(organizations).hasSize(2)
            assertThat(organizations.map { it.organizationType }).containsExactlyInAnyOrder(
                OrganizationType.ASSOCIATION,
                OrganizationType.LEAGUE,
            )
        }
    }

    @Nested
    @DisplayName("changeRole")
    inner class ChangeRole {
        @Test
        fun `관리자 역할을 변경할 수 있다`() {
            // given
            val userId = 1L
            val user = createUser(userId, "test@example.com")
            val organizationType = OrganizationType.ASSOCIATION
            val organizationId = 1L
            val admin = createOrganizationAdmin(1L, user, organizationType, organizationId, OrganizationRole.ADMIN)

            every {
                organizationAdminRepository.findByUserIdAndOrganizationTypeAndOrganizationId(
                    userId,
                    organizationType,
                    organizationId,
                )
            } returns admin

            // when
            val updated =
                organizationAdminService.changeRole(
                    userId,
                    organizationType,
                    organizationId,
                    OrganizationRole.MANAGER,
                )

            // then
            assertThat(updated.role).isEqualTo(OrganizationRole.MANAGER)
        }

        @Test
        fun `비활성화된 관리자의 역할은 변경할 수 없다`() {
            // given
            val userId = 1L
            val user = createUser(userId, "test@example.com")
            val organizationType = OrganizationType.ASSOCIATION
            val organizationId = 1L
            val admin = createOrganizationAdmin(1L, user, organizationType, organizationId)
            admin.deactivate()

            every {
                organizationAdminRepository.findByUserIdAndOrganizationTypeAndOrganizationId(
                    userId,
                    organizationType,
                    organizationId,
                )
            } returns admin

            // when & then
            assertThatThrownBy {
                organizationAdminService.changeRole(
                    userId,
                    organizationType,
                    organizationId,
                    OrganizationRole.MANAGER,
                )
            }.isInstanceOf(OrganizationAdminDeactivatedException::class.java)
        }
    }

    @Nested
    @DisplayName("hasPermission")
    inner class HasPermission {
        @Test
        fun `사용자가 특정 조직에 대한 권한을 가지고 있는지 확인할 수 있다`() {
            // given
            val userId = 1L
            every {
                organizationAdminRepository.existsActiveByUserIdAndOrganizationTypeAndOrganizationId(
                    userId,
                    OrganizationType.ASSOCIATION,
                    1L,
                )
            } returns true
            every {
                organizationAdminRepository.existsActiveByUserIdAndOrganizationTypeAndOrganizationId(
                    userId,
                    OrganizationType.LEAGUE,
                    2L,
                )
            } returns false

            // when
            val hasPermission =
                organizationAdminService.hasPermission(
                    userId,
                    OrganizationType.ASSOCIATION,
                    1L,
                )
            val noPermission =
                organizationAdminService.hasPermission(
                    userId,
                    OrganizationType.LEAGUE,
                    2L,
                )

            // then
            assertThat(hasPermission).isTrue()
            assertThat(noPermission).isFalse()
        }
    }

    @Nested
    @DisplayName("getById")
    inner class GetById {
        @Test
        fun `ID로 관리자를 조회할 수 있다`() {
            // given
            val adminId = 1L
            val user = createUser(1L, "test@example.com")
            val admin = createOrganizationAdmin(adminId, user, OrganizationType.ASSOCIATION, 1L)

            every { organizationAdminRepository.findByIdOrNull(adminId) } returns admin

            // when
            val result = organizationAdminService.getById(adminId)

            // then
            assertThat(result.id).isEqualTo(adminId)
            assertThat(result.user.id).isEqualTo(1L)
        }

        @Test
        fun `존재하지 않는 ID로 조회하면 예외가 발생한다`() {
            // given
            val adminId = 99999L
            every { organizationAdminRepository.findByIdOrNull(adminId) } returns null

            // when & then
            assertThatThrownBy {
                organizationAdminService.getById(adminId)
            }.isInstanceOf(OrganizationAdminNotFoundByIdException::class.java)
        }
    }

    @Nested
    @DisplayName("validateSameLeagueConflict edge cases")
    inner class ValidateSameLeagueConflictEdgeCases {
        @Test
        fun `새 팀을 찾을 수 없으면 TeamNotFoundException 발생`() {
            // given
            val userId = 1L
            val user = createUser(userId, "test@example.com")
            val newTeamId = 99L

            every { userRepository.findByIdOrNull(userId) } returns user
            every {
                organizationAdminRepository.findByUserIdAndOrganizationTypeAndOrganizationId(
                    userId,
                    OrganizationType.TEAM,
                    newTeamId,
                )
            } returns null
            every { teamRepository.findByIdWithLeague(newTeamId) } returns null

            // when & then
            assertThatThrownBy {
                organizationAdminService.assignAdmin(
                    userId = userId,
                    organizationType = OrganizationType.TEAM,
                    organizationId = newTeamId,
                    role = OrganizationRole.ADMIN,
                )
            }.isInstanceOf(TeamNotFoundException::class.java)
        }

        @Test
        fun `기존 관리 팀을 찾을 수 없으면 무시하고 계속 진행한다`() {
            // given
            val userId = 1L
            val user = createUser(userId, "test@example.com")
            val existingTeamId = 1L
            val newTeamId = 2L

            val association = createAssociation(1L, "Test Association")
            val league = createLeague(100L, "Test League", association)
            val newTeam = createTeam(newTeamId, "New Team", league)

            val existingAdmin = createOrganizationAdmin(1L, user, OrganizationType.TEAM, existingTeamId)

            every { userRepository.findByIdOrNull(userId) } returns user
            every {
                organizationAdminRepository.findByUserIdAndOrganizationTypeAndOrganizationId(
                    userId,
                    OrganizationType.TEAM,
                    newTeamId,
                )
            } returns null
            every { teamRepository.findByIdWithLeague(newTeamId) } returns newTeam
            every {
                organizationAdminRepository.findActiveByUserIdAndOrganizationType(
                    userId,
                    OrganizationType.TEAM,
                )
            } returns listOf(existingAdmin)
            every { teamRepository.findByIdWithLeague(existingTeamId) } returns null
            every { organizationAdminRepository.save(any()) } answers { firstArg() }

            // when
            val admin =
                organizationAdminService.assignAdmin(
                    userId = userId,
                    organizationType = OrganizationType.TEAM,
                    organizationId = newTeamId,
                    role = OrganizationRole.ADMIN,
                )

            // then
            assertThat(admin.organizationId).isEqualTo(newTeamId)
            verify { organizationAdminRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("changeRole edge cases")
    inner class ChangeRoleEdgeCases {
        @Test
        fun `존재하지 않는 관리자의 역할을 변경하면 예외가 발생한다`() {
            // given
            val userId = 1L
            every {
                organizationAdminRepository.findByUserIdAndOrganizationTypeAndOrganizationId(
                    userId,
                    OrganizationType.ASSOCIATION,
                    1L,
                )
            } returns null

            // when & then
            assertThatThrownBy {
                organizationAdminService.changeRole(
                    userId,
                    OrganizationType.ASSOCIATION,
                    1L,
                    OrganizationRole.MANAGER,
                )
            }.isInstanceOf(OrganizationAdminNotFoundException::class.java)
        }
    }

    // Helper methods

    private fun createUser(
        id: Long,
        email: String,
    ): User {
        val user =
            User.createLocalUser(
                email = email,
                encodedPassword = "password",
                nickname = "Test User",
            )
        setEntityId(user, id)
        return user
    }

    private fun createAssociation(
        id: Long,
        name: String,
    ): Association {
        val association =
            Association(
                name = name,
                abbreviation = name.substring(0, 2),
                region = "Seoul",
            )
        setEntityId(association, id)
        return association
    }

    private fun createLeague(
        id: Long,
        name: String,
        association: Association,
    ): League {
        val league =
            League(
                association = association,
                name = name,
                abbreviation = name.substring(0, 2),
                foundedYear = 2020,
            )
        setEntityId(league, id)
        return league
    }

    private fun createTeam(
        id: Long,
        name: String,
        league: League,
    ): Team {
        val team =
            Team(
                league = league,
                name = name,
                city = "Seoul",
                foundedYear = 2020,
            )
        setEntityId(team, id)
        return team
    }

    private fun createOrganizationAdmin(
        id: Long,
        user: User,
        organizationType: OrganizationType,
        organizationId: Long,
        role: OrganizationRole = OrganizationRole.ADMIN,
    ): OrganizationAdmin {
        val admin =
            OrganizationAdmin.create(
                user = user,
                organizationType = organizationType,
                organizationId = organizationId,
                role = role,
            )
        setEntityId(admin, id)
        return admin
    }

    private fun setEntityId(
        entity: Any,
        id: Long,
    ) {
        val idField = entity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }
}
