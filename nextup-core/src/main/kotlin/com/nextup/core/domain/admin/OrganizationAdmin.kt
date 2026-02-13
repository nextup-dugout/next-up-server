package com.nextup.core.domain.admin

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.user.User
import jakarta.persistence.*
import java.time.Instant

/**
 * 조직-관리자 매핑 엔티티
 *
 * 다형적 관계(Polymorphic Association)를 사용하여
 * 협회, 리그, 팀 등 다양한 조직과 관리자를 매핑합니다.
 *
 * - organizationType: 조직의 유형 (ASSOCIATION, LEAGUE, TEAM)
 * - organizationId: 해당 조직의 ID
 *
 * 복합 유니크 제약: (user_id, organization_type, organization_id)
 */
@Entity
@Table(
    name = "organization_admins",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_organization_admin_user_org",
            columnNames = ["user_id", "organization_type", "organization_id"],
        ),
    ],
    indexes = [
        Index(name = "idx_org_admin_user", columnList = "user_id"),
        Index(name = "idx_org_admin_org", columnList = "organization_type, organization_id"),
        Index(name = "idx_org_admin_active", columnList = "is_active"),
    ],
)
class OrganizationAdmin private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    @Enumerated(EnumType.STRING)
    @Column(name = "organization_type", nullable = false, length = 20)
    val organizationType: OrganizationType,
    @Column(name = "organization_id", nullable = false)
    val organizationId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    var role: OrganizationRole,
    @Column(name = "assigned_at", nullable = false)
    val assignedAt: Instant = Instant.now(),
    @Column(name = "assigned_by")
    val assignedBy: Long? = null,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    /**
     * 관리자 역할을 변경합니다.
     *
     * @param newRole 새로운 역할
     * @throws IllegalStateException 비활성화된 관리자인 경우
     */
    fun changeRole(newRole: OrganizationRole) {
        require(isActive) { "비활성화된 관리자의 역할은 변경할 수 없습니다." }
        this.role = newRole
    }

    /**
     * 관리자를 비활성화합니다.
     */
    fun deactivate() {
        this.isActive = false
    }

    /**
     * 관리자를 활성화합니다.
     */
    fun activate() {
        this.isActive = true
    }

    /**
     * 특정 역할 이상의 권한을 가지고 있는지 확인합니다.
     *
     * @param requiredRole 필요한 최소 역할
     * @return 권한 보유 여부
     */
    fun hasRoleOrHigher(requiredRole: OrganizationRole): Boolean = isActive && role.isHigherOrEqual(requiredRole)

    /**
     * 협회 관리자인지 확인합니다.
     */
    val isAssociationAdmin: Boolean
        get() = organizationType == OrganizationType.ASSOCIATION

    /**
     * 리그 관리자인지 확인합니다.
     */
    val isLeagueAdmin: Boolean
        get() = organizationType == OrganizationType.LEAGUE

    /**
     * 팀 관리자인지 확인합니다.
     */
    val isTeamAdmin: Boolean
        get() = organizationType == OrganizationType.TEAM

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OrganizationAdmin) return false
        if (id == 0L) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String =
        "OrganizationAdmin(id=$id, userId=${user.id}, organizationType=$organizationType, " +
            "organizationId=$organizationId, role=$role, isActive=$isActive)"

    companion object {
        /**
         * 조직 관리자를 생성합니다.
         *
         * @param user 관리자로 지정할 사용자
         * @param organizationType 조직 유형
         * @param organizationId 조직 ID
         * @param role 부여할 역할
         * @param assignedBy 관리자를 할당한 사용자 ID (선택)
         * @return 생성된 OrganizationAdmin
         */
        fun create(
            user: User,
            organizationType: OrganizationType,
            organizationId: Long,
            role: OrganizationRole,
            assignedBy: Long? = null,
        ): OrganizationAdmin {
            require(organizationId > 0) { "조직 ID는 0보다 커야 합니다." }

            return OrganizationAdmin(
                user = user,
                organizationType = organizationType,
                organizationId = organizationId,
                role = role,
                assignedBy = assignedBy,
            )
        }

        /**
         * 협회 관리자를 생성합니다.
         */
        fun createAssociationAdmin(
            user: User,
            associationId: Long,
            role: OrganizationRole = OrganizationRole.ADMIN,
            assignedBy: Long? = null,
        ): OrganizationAdmin =
            create(
                user = user,
                organizationType = OrganizationType.ASSOCIATION,
                organizationId = associationId,
                role = role,
                assignedBy = assignedBy,
            )

        /**
         * 리그 관리자를 생성합니다.
         */
        fun createLeagueAdmin(
            user: User,
            leagueId: Long,
            role: OrganizationRole = OrganizationRole.ADMIN,
            assignedBy: Long? = null,
        ): OrganizationAdmin =
            create(
                user = user,
                organizationType = OrganizationType.LEAGUE,
                organizationId = leagueId,
                role = role,
                assignedBy = assignedBy,
            )

        /**
         * 팀 관리자를 생성합니다.
         */
        fun createTeamAdmin(
            user: User,
            teamId: Long,
            role: OrganizationRole = OrganizationRole.ADMIN,
            assignedBy: Long? = null,
        ): OrganizationAdmin =
            create(
                user = user,
                organizationType = OrganizationType.TEAM,
                organizationId = teamId,
                role = role,
                assignedBy = assignedBy,
            )
    }
}
