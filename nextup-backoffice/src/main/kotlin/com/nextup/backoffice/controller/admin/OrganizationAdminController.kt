package com.nextup.backoffice.controller.admin

import com.nextup.backoffice.dto.admin.AssignAdminRequest
import com.nextup.backoffice.dto.admin.ChangeRoleRequest
import com.nextup.backoffice.dto.admin.OrganizationAdminResponse
import com.nextup.backoffice.dto.common.ApiResponse
import com.nextup.core.domain.admin.OrganizationType
import com.nextup.core.service.admin.OrganizationAdminService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

/**
 * 조직 관리자 API Controller (관리자용)
 *
 * 조직-관리자 매핑을 관리하는 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/backoffice/organizations")
class OrganizationAdminController(
    private val organizationAdminService: OrganizationAdminService
) {

    /**
     * 관리자를 할당합니다.
     *
     * @param type 조직 유형 (ASSOCIATION, LEAGUE, TEAM)
     * @param id 조직 ID
     * @param request 할당 요청 (userId, role)
     * @return 생성된 관리자 정보
     */
    @PostMapping("/{type}/{id}/admins")
    @ResponseStatus(HttpStatus.CREATED)
    fun assignAdmin(
        @PathVariable type: String,
        @PathVariable id: Long,
        @Valid @RequestBody request: AssignAdminRequest
    ): ApiResponse<OrganizationAdminResponse> {
        val organizationType = OrganizationType.fromValue(type)

        val admin = organizationAdminService.assignAdmin(
            userId = request.userId,
            organizationType = organizationType,
            organizationId = id,
            role = request.role,
            assignedBy = null // TODO: 인증된 사용자 ID로 변경
        )

        return ApiResponse.success(OrganizationAdminResponse.from(admin))
    }

    /**
     * 관리자 권한을 해제합니다.
     *
     * @param type 조직 유형
     * @param id 조직 ID
     * @param userId 사용자 ID
     * @return 성공 응답
     */
    @DeleteMapping("/{type}/{id}/admins/{userId}")
    fun removeAdmin(
        @PathVariable type: String,
        @PathVariable id: Long,
        @PathVariable userId: Long
    ): ApiResponse<Unit> {
        val organizationType = OrganizationType.fromValue(type)

        organizationAdminService.removeAdmin(
            userId = userId,
            organizationType = organizationType,
            organizationId = id
        )

        return ApiResponse.success(Unit)
    }

    /**
     * 특정 조직의 관리자 목록을 조회합니다.
     *
     * @param type 조직 유형
     * @param id 조직 ID
     * @return 관리자 목록
     */
    @GetMapping("/{type}/{id}/admins")
    fun getAdminsByOrganization(
        @PathVariable type: String,
        @PathVariable id: Long
    ): ApiResponse<List<OrganizationAdminResponse>> {
        val organizationType = OrganizationType.fromValue(type)

        val admins = organizationAdminService.getAdminsByOrganization(
            organizationType = organizationType,
            organizationId = id
        )

        return ApiResponse.success(
            admins.map { OrganizationAdminResponse.from(it) }
        )
    }

    /**
     * 특정 사용자가 관리하는 모든 조직을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 관리하는 조직 목록
     */
    @GetMapping("/by-user/{userId}")
    fun getOrganizationsByUser(
        @PathVariable userId: Long
    ): ApiResponse<List<OrganizationAdminResponse>> {
        val admins = organizationAdminService.getOrganizationsByUser(userId)

        return ApiResponse.success(
            admins.map { OrganizationAdminResponse.from(it) }
        )
    }

    /**
     * 관리자의 역할을 변경합니다.
     *
     * @param type 조직 유형
     * @param id 조직 ID
     * @param userId 사용자 ID
     * @param request 역할 변경 요청
     * @return 수정된 관리자 정보
     */
    @PutMapping("/{type}/{id}/admins/{userId}/role")
    fun changeRole(
        @PathVariable type: String,
        @PathVariable id: Long,
        @PathVariable userId: Long,
        @Valid @RequestBody request: ChangeRoleRequest
    ): ApiResponse<OrganizationAdminResponse> {
        val organizationType = OrganizationType.fromValue(type)

        val admin = organizationAdminService.changeRole(
            userId = userId,
            organizationType = organizationType,
            organizationId = id,
            newRole = request.role
        )

        return ApiResponse.success(OrganizationAdminResponse.from(admin))
    }
}
