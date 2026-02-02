package com.nextup.core.domain.admin

import com.nextup.core.domain.user.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("OrganizationAdmin 테스트")
class OrganizationAdminTest {

    @Nested
    @DisplayName("create 팩토리 메서드")
    inner class Create {

        @Test
        fun `should create organization admin successfully`() {
            // given
            val user = createUser()

            // when
            val admin = OrganizationAdmin.create(
                user = user,
                organizationType = OrganizationType.ASSOCIATION,
                organizationId = 1L,
                role = OrganizationRole.ADMIN
            )

            // then
            assertThat(admin.user).isEqualTo(user)
            assertThat(admin.organizationType).isEqualTo(OrganizationType.ASSOCIATION)
            assertThat(admin.organizationId).isEqualTo(1L)
            assertThat(admin.role).isEqualTo(OrganizationRole.ADMIN)
            assertThat(admin.isActive).isTrue()
            assertThat(admin.assignedBy).isNull()
        }

        @Test
        fun `should create organization admin with assignedBy`() {
            // given
            val user = createUser()
            val assignedByUserId = 100L

            // when
            val admin = OrganizationAdmin.create(
                user = user,
                organizationType = OrganizationType.LEAGUE,
                organizationId = 2L,
                role = OrganizationRole.MANAGER,
                assignedBy = assignedByUserId
            )

            // then
            assertThat(admin.assignedBy).isEqualTo(assignedByUserId)
        }

        @Test
        fun `should throw exception when organizationId is zero`() {
            // given
            val user = createUser()

            // when & then
            assertThrows<IllegalArgumentException> {
                OrganizationAdmin.create(
                    user = user,
                    organizationType = OrganizationType.TEAM,
                    organizationId = 0L,
                    role = OrganizationRole.SCORER
                )
            }
        }

        @Test
        fun `should throw exception when organizationId is negative`() {
            // given
            val user = createUser()

            // when & then
            assertThrows<IllegalArgumentException> {
                OrganizationAdmin.create(
                    user = user,
                    organizationType = OrganizationType.TEAM,
                    organizationId = -1L,
                    role = OrganizationRole.SCORER
                )
            }
        }
    }

    @Nested
    @DisplayName("createAssociationAdmin 팩토리 메서드")
    inner class CreateAssociationAdmin {

        @Test
        fun `should create association admin with default role`() {
            // given
            val user = createUser()

            // when
            val admin = OrganizationAdmin.createAssociationAdmin(
                user = user,
                associationId = 1L
            )

            // then
            assertThat(admin.organizationType).isEqualTo(OrganizationType.ASSOCIATION)
            assertThat(admin.organizationId).isEqualTo(1L)
            assertThat(admin.role).isEqualTo(OrganizationRole.ADMIN)
        }

        @Test
        fun `should create association admin with custom role`() {
            // given
            val user = createUser()

            // when
            val admin = OrganizationAdmin.createAssociationAdmin(
                user = user,
                associationId = 1L,
                role = OrganizationRole.MANAGER
            )

            // then
            assertThat(admin.role).isEqualTo(OrganizationRole.MANAGER)
        }
    }

    @Nested
    @DisplayName("createLeagueAdmin 팩토리 메서드")
    inner class CreateLeagueAdmin {

        @Test
        fun `should create league admin with default role`() {
            // given
            val user = createUser()

            // when
            val admin = OrganizationAdmin.createLeagueAdmin(
                user = user,
                leagueId = 2L
            )

            // then
            assertThat(admin.organizationType).isEqualTo(OrganizationType.LEAGUE)
            assertThat(admin.organizationId).isEqualTo(2L)
            assertThat(admin.role).isEqualTo(OrganizationRole.ADMIN)
        }

        @Test
        fun `should create league admin with assignedBy`() {
            // given
            val user = createUser()

            // when
            val admin = OrganizationAdmin.createLeagueAdmin(
                user = user,
                leagueId = 2L,
                assignedBy = 99L
            )

            // then
            assertThat(admin.assignedBy).isEqualTo(99L)
        }
    }

    @Nested
    @DisplayName("createTeamAdmin 팩토리 메서드")
    inner class CreateTeamAdmin {

        @Test
        fun `should create team admin with default role`() {
            // given
            val user = createUser()

            // when
            val admin = OrganizationAdmin.createTeamAdmin(
                user = user,
                teamId = 3L
            )

            // then
            assertThat(admin.organizationType).isEqualTo(OrganizationType.TEAM)
            assertThat(admin.organizationId).isEqualTo(3L)
            assertThat(admin.role).isEqualTo(OrganizationRole.ADMIN)
        }

        @Test
        fun `should create team admin with scorer role`() {
            // given
            val user = createUser()

            // when
            val admin = OrganizationAdmin.createTeamAdmin(
                user = user,
                teamId = 3L,
                role = OrganizationRole.SCORER
            )

            // then
            assertThat(admin.role).isEqualTo(OrganizationRole.SCORER)
        }
    }

    @Nested
    @DisplayName("changeRole 메서드")
    inner class ChangeRole {

        @Test
        fun `should change role successfully when active`() {
            // given
            val admin = createAdmin(role = OrganizationRole.ADMIN)

            // when
            admin.changeRole(OrganizationRole.MANAGER)

            // then
            assertThat(admin.role).isEqualTo(OrganizationRole.MANAGER)
        }

        @Test
        fun `should throw exception when changing role of deactivated admin`() {
            // given
            val admin = createAdmin(role = OrganizationRole.ADMIN)
            admin.deactivate()

            // when & then
            assertThrows<IllegalArgumentException> {
                admin.changeRole(OrganizationRole.MANAGER)
            }
        }
    }

    @Nested
    @DisplayName("activate/deactivate 메서드")
    inner class ActivateDeactivate {

        @Test
        fun `should deactivate admin successfully`() {
            // given
            val admin = createAdmin()

            // when
            admin.deactivate()

            // then
            assertThat(admin.isActive).isFalse()
        }

        @Test
        fun `should activate admin successfully`() {
            // given
            val admin = createAdmin()
            admin.deactivate()

            // when
            admin.activate()

            // then
            assertThat(admin.isActive).isTrue()
        }
    }

    @Nested
    @DisplayName("hasRoleOrHigher 메서드")
    inner class HasRoleOrHigher {

        @Test
        fun `should return true when admin has higher role`() {
            // given
            val admin = createAdmin(role = OrganizationRole.ADMIN)

            // when & then
            assertThat(admin.hasRoleOrHigher(OrganizationRole.MANAGER)).isTrue()
            assertThat(admin.hasRoleOrHigher(OrganizationRole.SCORER)).isTrue()
        }

        @Test
        fun `should return true when admin has equal role`() {
            // given
            val admin = createAdmin(role = OrganizationRole.MANAGER)

            // when & then
            assertThat(admin.hasRoleOrHigher(OrganizationRole.MANAGER)).isTrue()
        }

        @Test
        fun `should return false when admin has lower role`() {
            // given
            val admin = createAdmin(role = OrganizationRole.SCORER)

            // when & then
            assertThat(admin.hasRoleOrHigher(OrganizationRole.MANAGER)).isFalse()
            assertThat(admin.hasRoleOrHigher(OrganizationRole.ADMIN)).isFalse()
        }

        @Test
        fun `should return false when admin is inactive`() {
            // given
            val admin = createAdmin(role = OrganizationRole.ADMIN)
            admin.deactivate()

            // when & then
            assertThat(admin.hasRoleOrHigher(OrganizationRole.SCORER)).isFalse()
        }
    }

    @Nested
    @DisplayName("조직 유형 프로퍼티")
    inner class OrganizationTypeProperties {

        @Test
        fun `should return true for isAssociationAdmin when type is ASSOCIATION`() {
            // given
            val admin = createAdmin(organizationType = OrganizationType.ASSOCIATION)

            // when & then
            assertThat(admin.isAssociationAdmin).isTrue()
            assertThat(admin.isLeagueAdmin).isFalse()
            assertThat(admin.isTeamAdmin).isFalse()
        }

        @Test
        fun `should return true for isLeagueAdmin when type is LEAGUE`() {
            // given
            val admin = createAdmin(organizationType = OrganizationType.LEAGUE)

            // when & then
            assertThat(admin.isAssociationAdmin).isFalse()
            assertThat(admin.isLeagueAdmin).isTrue()
            assertThat(admin.isTeamAdmin).isFalse()
        }

        @Test
        fun `should return true for isTeamAdmin when type is TEAM`() {
            // given
            val admin = createAdmin(organizationType = OrganizationType.TEAM)

            // when & then
            assertThat(admin.isAssociationAdmin).isFalse()
            assertThat(admin.isLeagueAdmin).isFalse()
            assertThat(admin.isTeamAdmin).isTrue()
        }
    }

    @Nested
    @DisplayName("equals/hashCode")
    inner class EqualsHashCode {

        @Test
        fun `should be equal when same instance`() {
            // given
            val admin = createAdminWithId(1L)

            // when & then
            assertThat(admin).isEqualTo(admin)
        }

        @Test
        fun `should be equal when same id`() {
            // given
            val admin1 = createAdminWithId(1L)
            val admin2 = createAdminWithId(1L)

            // when & then
            assertThat(admin1).isEqualTo(admin2)
            assertThat(admin1.hashCode()).isEqualTo(admin2.hashCode())
        }

        @Test
        fun `should not be equal when different id`() {
            // given
            val admin1 = createAdminWithId(1L)
            val admin2 = createAdminWithId(2L)

            // when & then
            assertThat(admin1).isNotEqualTo(admin2)
        }

        @Test
        fun `should not be equal when id is zero`() {
            // given
            val admin1 = createAdmin()
            val admin2 = createAdmin()

            // when & then
            assertThat(admin1).isNotEqualTo(admin2)
        }

        @Test
        fun `should not be equal to null`() {
            // given
            val admin = createAdminWithId(1L)

            // when & then
            assertThat(admin).isNotEqualTo(null)
        }

        @Test
        fun `should not be equal to different type`() {
            // given
            val admin = createAdminWithId(1L)

            // when & then
            assertThat(admin).isNotEqualTo("string")
        }
    }

    @Nested
    @DisplayName("toString")
    inner class ToString {

        @Test
        fun `should return readable string representation`() {
            // given
            val admin = createAdminWithId(1L, OrganizationType.ASSOCIATION, 10L, OrganizationRole.ADMIN)

            // when
            val result = admin.toString()

            // then
            assertThat(result).contains("OrganizationAdmin")
            assertThat(result).contains("id=1")
            assertThat(result).contains("organizationType=ASSOCIATION")
            assertThat(result).contains("organizationId=10")
            assertThat(result).contains("role=ADMIN")
            assertThat(result).contains("isActive=true")
        }
    }

    // Helper methods

    private fun createUser(): User {
        return User.createLocalUser(
            email = "test@example.com",
            encodedPassword = "password",
            nickname = "테스터"
        )
    }

    private fun createAdmin(
        organizationType: OrganizationType = OrganizationType.ASSOCIATION,
        organizationId: Long = 1L,
        role: OrganizationRole = OrganizationRole.ADMIN
    ): OrganizationAdmin {
        return OrganizationAdmin.create(
            user = createUser(),
            organizationType = organizationType,
            organizationId = organizationId,
            role = role
        )
    }

    private fun createAdminWithId(
        id: Long,
        organizationType: OrganizationType = OrganizationType.ASSOCIATION,
        organizationId: Long = 1L,
        role: OrganizationRole = OrganizationRole.ADMIN
    ): OrganizationAdmin {
        val admin = createAdmin(organizationType, organizationId, role)
        val idField = OrganizationAdmin::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(admin, id)
        return admin
    }
}
