package com.nextup.core.port.repository

import com.nextup.core.domain.admin.OrganizationAdmin
import com.nextup.core.domain.admin.OrganizationRole
import com.nextup.core.domain.admin.OrganizationType
import com.nextup.core.domain.user.User

/**
 * OrganizationAdmin Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface OrganizationAdminRepositoryPort {
    fun save(organizationAdmin: OrganizationAdmin): OrganizationAdmin

    fun findAll(): List<OrganizationAdmin>

    fun findByIdOrNull(id: Long): OrganizationAdmin?

    fun delete(organizationAdmin: OrganizationAdmin)

    fun deleteById(id: Long)

    /**
     * 특정 사용자의 모든 조직 관리자 권한을 조회합니다.
     */
    fun findByUser(user: User): List<OrganizationAdmin>

    /**
     * 특정 사용자 ID의 모든 조직 관리자 권한을 조회합니다.
     */
    fun findByUserId(userId: Long): List<OrganizationAdmin>

    /**
     * 특정 사용자의 활성화된 조직 관리자 권한만 조회합니다.
     */
    fun findActiveByUserId(userId: Long): List<OrganizationAdmin>

    /**
     * 특정 조직의 모든 관리자를 조회합니다.
     */
    fun findByOrganizationTypeAndOrganizationId(
        organizationType: OrganizationType,
        organizationId: Long,
    ): List<OrganizationAdmin>

    /**
     * 특정 조직의 활성화된 관리자만 조회합니다.
     */
    fun findActiveByOrganizationTypeAndOrganizationId(
        organizationType: OrganizationType,
        organizationId: Long,
    ): List<OrganizationAdmin>

    /**
     * 특정 사용자가 특정 조직의 관리자인지 조회합니다.
     */
    fun findByUserAndOrganizationTypeAndOrganizationId(
        user: User,
        organizationType: OrganizationType,
        organizationId: Long,
    ): OrganizationAdmin?

    /**
     * 특정 사용자 ID가 특정 조직의 관리자인지 조회합니다.
     */
    fun findByUserIdAndOrganizationTypeAndOrganizationId(
        userId: Long,
        organizationType: OrganizationType,
        organizationId: Long,
    ): OrganizationAdmin?

    /**
     * 특정 사용자가 특정 조직의 관리자인지 확인합니다.
     */
    fun existsByUserAndOrganizationTypeAndOrganizationId(
        user: User,
        organizationType: OrganizationType,
        organizationId: Long,
    ): Boolean

    /**
     * 특정 사용자 ID가 특정 조직의 관리자인지 확인합니다.
     */
    fun existsByUserIdAndOrganizationTypeAndOrganizationId(
        userId: Long,
        organizationType: OrganizationType,
        organizationId: Long,
    ): Boolean

    /**
     * 특정 사용자 ID가 특정 조직의 활성화된 관리자인지 확인합니다.
     */
    fun existsActiveByUserIdAndOrganizationTypeAndOrganizationId(
        userId: Long,
        organizationType: OrganizationType,
        organizationId: Long,
    ): Boolean

    /**
     * 특정 사용자 ID의 특정 조직 유형 관리자 권한을 조회합니다.
     */
    fun findByUserIdAndOrganizationType(
        userId: Long,
        organizationType: OrganizationType,
    ): List<OrganizationAdmin>

    /**
     * 특정 사용자 ID의 특정 조직 유형 활성화된 관리자 권한을 조회합니다.
     */
    fun findActiveByUserIdAndOrganizationType(
        userId: Long,
        organizationType: OrganizationType,
    ): List<OrganizationAdmin>

    /**
     * 특정 조직의 특정 역할을 가진 관리자를 조회합니다.
     */
    fun findByOrganizationAndRole(
        organizationType: OrganizationType,
        organizationId: Long,
        role: OrganizationRole,
    ): List<OrganizationAdmin>

    /**
     * 특정 사용자가 관리하는 팀 ID 목록을 조회합니다.
     */
    fun findTeamIdsByUserId(userId: Long): List<Long>

    /**
     * 특정 사용자가 관리하는 리그 ID 목록을 조회합니다.
     */
    fun findLeagueIdsByUserId(userId: Long): List<Long>

    /**
     * 특정 사용자가 관리하는 협회 ID 목록을 조회합니다.
     */
    fun findAssociationIdsByUserId(userId: Long): List<Long>
}
