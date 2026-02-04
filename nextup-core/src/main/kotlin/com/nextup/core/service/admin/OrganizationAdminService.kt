package com.nextup.core.service.admin

import com.nextup.common.exception.*
import com.nextup.core.domain.admin.OrganizationAdmin
import com.nextup.core.domain.admin.OrganizationRole
import com.nextup.core.domain.admin.OrganizationType
import com.nextup.core.port.repository.OrganizationAdminRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.port.repository.UserRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 조직 관리자 서비스
 *
 * 조직-관리자 매핑을 관리하고, 같은 리그 내 여러 팀 관리자 충돌을 검증합니다.
 */
@Service
@Transactional(readOnly = true)
class OrganizationAdminService(
    private val organizationAdminRepository: OrganizationAdminRepositoryPort,
    private val userRepository: UserRepositoryPort,
    private val teamRepository: TeamRepositoryPort,
) {
    /**
     * 관리자를 할당합니다.
     *
     * @param userId 사용자 ID
     * @param organizationType 조직 유형
     * @param organizationId 조직 ID
     * @param role 역할
     * @param assignedBy 할당한 사용자 ID (선택)
     * @return 생성된 OrganizationAdmin
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     * @throws OrganizationAdminAlreadyExistsException 이미 할당된 경우
     * @throws SameLeagueConflictException 같은 리그 내 여러 팀 관리자 충돌 시
     */
    @Transactional
    fun assignAdmin(
        userId: Long,
        organizationType: OrganizationType,
        organizationId: Long,
        role: OrganizationRole,
        assignedBy: Long? = null,
    ): OrganizationAdmin {
        // 사용자 존재 확인
        val user =
            userRepository.findByIdOrNull(userId)
                ?: throw UserNotFoundException(userId)

        // 이미 할당되어 있는지 확인
        val existing =
            organizationAdminRepository.findByUserIdAndOrganizationTypeAndOrganizationId(
                userId,
                organizationType,
                organizationId,
            )
        if (existing != null) {
            throw OrganizationAdminAlreadyExistsException(
                organizationType.name,
                organizationId,
                userId,
            )
        }

        // 팀 관리자인 경우 같은 리그 내 여러 팀 관리자 검증
        if (organizationType == OrganizationType.TEAM) {
            validateSameLeagueConflict(userId, organizationId)
        }

        // 관리자 생성
        val admin =
            OrganizationAdmin.create(
                user = user,
                organizationType = organizationType,
                organizationId = organizationId,
                role = role,
                assignedBy = assignedBy,
            )

        return organizationAdminRepository.save(admin)
    }

    /**
     * 같은 리그 내 여러 팀의 관리자가 될 수 없도록 검증합니다.
     *
     * @param userId 사용자 ID
     * @param newTeamId 새로 할당하려는 팀 ID
     * @throws SameLeagueConflictException 같은 리그 내 다른 팀의 관리자인 경우
     * @throws TeamNotFoundException 팀을 찾을 수 없는 경우
     */
    private fun validateSameLeagueConflict(
        userId: Long,
        newTeamId: Long,
    ) {
        // 새 팀의 리그 ID 조회
        val newTeam =
            teamRepository.findByIdWithLeague(newTeamId)
                ?: throw TeamNotFoundException(newTeamId)
        val newTeamLeagueId = newTeam.league.id

        // 사용자의 활성화된 팀 관리자 권한 조회
        val existingTeamAdmins =
            organizationAdminRepository.findActiveByUserIdAndOrganizationType(
                userId,
                OrganizationType.TEAM,
            )

        // 같은 리그 내 다른 팀의 관리자인지 확인
        for (admin in existingTeamAdmins) {
            val existingTeam =
                teamRepository.findByIdWithLeague(admin.organizationId)
                    ?: continue

            val existingTeamLeagueId = existingTeam.league.id

            if (existingTeamLeagueId == newTeamLeagueId) {
                throw SameLeagueConflictException(
                    leagueId = newTeamLeagueId,
                    existingTeamId = admin.organizationId,
                    newTeamId = newTeamId,
                )
            }
        }
    }

    /**
     * 관리자 권한을 해제합니다.
     *
     * @param userId 사용자 ID
     * @param organizationType 조직 유형
     * @param organizationId 조직 ID
     * @throws OrganizationAdminNotFoundException 관리자를 찾을 수 없는 경우
     */
    @Transactional
    fun removeAdmin(
        userId: Long,
        organizationType: OrganizationType,
        organizationId: Long,
    ) {
        val admin =
            organizationAdminRepository.findByUserIdAndOrganizationTypeAndOrganizationId(
                userId,
                organizationType,
                organizationId,
            ) ?: throw OrganizationAdminNotFoundException(
                organizationType.name,
                organizationId,
                userId,
            )

        admin.deactivate()
    }

    /**
     * 특정 조직의 관리자 목록을 조회합니다.
     *
     * @param organizationType 조직 유형
     * @param organizationId 조직 ID
     * @return 관리자 목록 (활성화된 관리자만)
     */
    fun getAdminsByOrganization(
        organizationType: OrganizationType,
        organizationId: Long,
    ): List<OrganizationAdmin> =
        organizationAdminRepository.findActiveByOrganizationTypeAndOrganizationId(
            organizationType,
            organizationId,
        )

    /**
     * 특정 사용자가 관리하는 모든 조직을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 관리하는 조직 목록 (활성화된 것만)
     */
    fun getOrganizationsByUser(userId: Long): List<OrganizationAdmin> =
        organizationAdminRepository.findActiveByUserId(userId)

    /**
     * 관리자의 역할을 변경합니다.
     *
     * @param userId 사용자 ID
     * @param organizationType 조직 유형
     * @param organizationId 조직 ID
     * @param newRole 새로운 역할
     * @return 수정된 OrganizationAdmin
     * @throws OrganizationAdminNotFoundException 관리자를 찾을 수 없는 경우
     * @throws OrganizationAdminDeactivatedException 비활성화된 관리자인 경우
     */
    @Transactional
    fun changeRole(
        userId: Long,
        organizationType: OrganizationType,
        organizationId: Long,
        newRole: OrganizationRole,
    ): OrganizationAdmin {
        val admin =
            organizationAdminRepository.findByUserIdAndOrganizationTypeAndOrganizationId(
                userId,
                organizationType,
                organizationId,
            ) ?: throw OrganizationAdminNotFoundException(
                organizationType.name,
                organizationId,
                userId,
            )

        if (!admin.isActive) {
            throw OrganizationAdminDeactivatedException(admin.id)
        }

        admin.changeRole(newRole)
        return admin
    }

    /**
     * 사용자가 특정 조직에 대한 권한을 가지고 있는지 확인합니다.
     *
     * @param userId 사용자 ID
     * @param organizationType 조직 유형
     * @param organizationId 조직 ID
     * @return 권한 보유 여부
     */
    fun hasPermission(
        userId: Long,
        organizationType: OrganizationType,
        organizationId: Long,
    ): Boolean =
        organizationAdminRepository.existsActiveByUserIdAndOrganizationTypeAndOrganizationId(
            userId,
            organizationType,
            organizationId,
        )

    /**
     * ID로 관리자를 조회합니다.
     *
     * @param id 관리자 ID
     * @return OrganizationAdmin
     * @throws OrganizationAdminNotFoundByIdException 관리자를 찾을 수 없는 경우
     */
    fun getById(id: Long): OrganizationAdmin =
        organizationAdminRepository.findByIdOrNull(id)
            ?: throw OrganizationAdminNotFoundByIdException(id)
}
