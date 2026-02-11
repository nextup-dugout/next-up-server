package com.nextup.infrastructure.repository.admin

import com.nextup.core.domain.admin.OrganizationAdmin
import com.nextup.core.domain.admin.OrganizationRole
import com.nextup.core.domain.admin.OrganizationType
import com.nextup.core.domain.user.User
import com.nextup.core.port.repository.OrganizationAdminRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface OrganizationAdminRepository :
    JpaRepository<OrganizationAdmin, Long>,
    OrganizationAdminRepositoryPort {
    /**
     * 특정 사용자의 모든 조직 관리자 권한을 조회합니다.
     */
    override fun findByUser(user: User): List<OrganizationAdmin>

    /**
     * 특정 사용자 ID의 모든 조직 관리자 권한을 조회합니다.
     */
    @Query("SELECT oa FROM OrganizationAdmin oa WHERE oa.user.id = :userId")
    override fun findByUserId(userId: Long): List<OrganizationAdmin>

    /**
     * 특정 사용자의 활성화된 조직 관리자 권한만 조회합니다.
     */
    @Query("SELECT oa FROM OrganizationAdmin oa WHERE oa.user.id = :userId AND oa.isActive = true")
    override fun findActiveByUserId(userId: Long): List<OrganizationAdmin>

    /**
     * 특정 조직의 모든 관리자를 조회합니다.
     */
    override fun findByOrganizationTypeAndOrganizationId(
        organizationType: OrganizationType,
        organizationId: Long,
    ): List<OrganizationAdmin>

    /**
     * 특정 조직의 활성화된 관리자만 조회합니다.
     */
    @Query(
        """
        SELECT oa FROM OrganizationAdmin oa
        WHERE oa.organizationType = :organizationType
        AND oa.organizationId = :organizationId
        AND oa.isActive = true
    """,
    )
    override fun findActiveByOrganizationTypeAndOrganizationId(
        organizationType: OrganizationType,
        organizationId: Long,
    ): List<OrganizationAdmin>

    /**
     * 특정 사용자가 특정 조직의 관리자인지 조회합니다.
     */
    override fun findByUserAndOrganizationTypeAndOrganizationId(
        user: User,
        organizationType: OrganizationType,
        organizationId: Long,
    ): OrganizationAdmin?

    /**
     * 특정 사용자 ID가 특정 조직의 관리자인지 조회합니다.
     */
    @Query(
        """
        SELECT oa FROM OrganizationAdmin oa
        WHERE oa.user.id = :userId
        AND oa.organizationType = :organizationType
        AND oa.organizationId = :organizationId
    """,
    )
    override fun findByUserIdAndOrganizationTypeAndOrganizationId(
        userId: Long,
        organizationType: OrganizationType,
        organizationId: Long,
    ): OrganizationAdmin?

    /**
     * 특정 사용자가 특정 조직의 관리자인지 확인합니다.
     */
    override fun existsByUserAndOrganizationTypeAndOrganizationId(
        user: User,
        organizationType: OrganizationType,
        organizationId: Long,
    ): Boolean

    /**
     * 특정 사용자 ID가 특정 조직의 관리자인지 확인합니다.
     */
    @Query(
        """
        SELECT CASE WHEN COUNT(oa) > 0 THEN true ELSE false END
        FROM OrganizationAdmin oa
        WHERE oa.user.id = :userId
        AND oa.organizationType = :organizationType
        AND oa.organizationId = :organizationId
    """,
    )
    override fun existsByUserIdAndOrganizationTypeAndOrganizationId(
        userId: Long,
        organizationType: OrganizationType,
        organizationId: Long,
    ): Boolean

    /**
     * 특정 사용자 ID가 특정 조직의 활성화된 관리자인지 확인합니다.
     */
    @Query(
        """
        SELECT CASE WHEN COUNT(oa) > 0 THEN true ELSE false END
        FROM OrganizationAdmin oa
        WHERE oa.user.id = :userId
        AND oa.organizationType = :organizationType
        AND oa.organizationId = :organizationId
        AND oa.isActive = true
    """,
    )
    override fun existsActiveByUserIdAndOrganizationTypeAndOrganizationId(
        userId: Long,
        organizationType: OrganizationType,
        organizationId: Long,
    ): Boolean

    /**
     * 특정 사용자 ID의 특정 조직 유형 관리자 권한을 조회합니다.
     */
    @Query(
        """
        SELECT oa FROM OrganizationAdmin oa
        WHERE oa.user.id = :userId
        AND oa.organizationType = :organizationType
    """,
    )
    override fun findByUserIdAndOrganizationType(
        userId: Long,
        organizationType: OrganizationType,
    ): List<OrganizationAdmin>

    /**
     * 특정 사용자 ID의 특정 조직 유형 활성화된 관리자 권한을 조회합니다.
     */
    @Query(
        """
        SELECT oa FROM OrganizationAdmin oa
        WHERE oa.user.id = :userId
        AND oa.organizationType = :organizationType
        AND oa.isActive = true
    """,
    )
    override fun findActiveByUserIdAndOrganizationType(
        userId: Long,
        organizationType: OrganizationType,
    ): List<OrganizationAdmin>

    /**
     * 특정 조직의 특정 역할을 가진 관리자를 조회합니다.
     */
    @Query(
        """
        SELECT oa FROM OrganizationAdmin oa
        WHERE oa.organizationType = :organizationType
        AND oa.organizationId = :organizationId
        AND oa.role = :role
        AND oa.isActive = true
    """,
    )
    override fun findByOrganizationAndRole(
        organizationType: OrganizationType,
        organizationId: Long,
        role: OrganizationRole,
    ): List<OrganizationAdmin>

    /**
     * 특정 사용자가 관리하는 팀 ID 목록을 조회합니다.
     */
    @Query(
        """
        SELECT oa.organizationId FROM OrganizationAdmin oa
        WHERE oa.user.id = :userId
        AND oa.organizationType = 'TEAM'
        AND oa.isActive = true
    """,
    )
    override fun findTeamIdsByUserId(userId: Long): List<Long>

    /**
     * 특정 사용자가 관리하는 리그 ID 목록을 조회합니다.
     */
    @Query(
        """
        SELECT oa.organizationId FROM OrganizationAdmin oa
        WHERE oa.user.id = :userId
        AND oa.organizationType = 'LEAGUE'
        AND oa.isActive = true
    """,
    )
    override fun findLeagueIdsByUserId(userId: Long): List<Long>

    /**
     * 특정 사용자가 관리하는 협회 ID 목록을 조회합니다.
     */
    @Query(
        """
        SELECT oa.organizationId FROM OrganizationAdmin oa
        WHERE oa.user.id = :userId
        AND oa.organizationType = 'ASSOCIATION'
        AND oa.isActive = true
    """,
    )
    override fun findAssociationIdsByUserId(userId: Long): List<Long>
}
