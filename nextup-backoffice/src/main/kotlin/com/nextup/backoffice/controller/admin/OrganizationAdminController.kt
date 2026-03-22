package com.nextup.backoffice.controller.admin

import com.nextup.backoffice.dto.admin.AssignAdminRequest
import com.nextup.backoffice.dto.admin.ChangeRoleRequest
import com.nextup.backoffice.dto.admin.OrganizationAdminResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.domain.admin.OrganizationType
import com.nextup.core.service.admin.OrganizationAdminService
import com.nextup.core.service.audit.AuditService
import com.nextup.infrastructure.security.userdetails.CustomUserDetails
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/backoffice/organizations")
class OrganizationAdminController(
    private val organizationAdminService: OrganizationAdminService,
    private val auditService: AuditService,
) {
    @PostMapping("/{type}/{id}/admins")
    @ResponseStatus(HttpStatus.CREATED)
    fun assignAdmin(
        @PathVariable type: String,
        @PathVariable id: Long,
        @Valid @RequestBody request: AssignAdminRequest,
        @AuthenticationPrincipal admin: CustomUserDetails,
    ): ApiResponse<OrganizationAdminResponse> {
        val organizationType = OrganizationType.fromValue(type)
        val orgAdmin =
            organizationAdminService.assignAdmin(
                userId = request.userId,
                organizationType = organizationType,
                organizationId = id,
                role = request.role,
                assignedBy = admin.id,
            )
        auditService.log(
            adminUserId = admin.id,
            action = "ASSIGN_ADMIN",
            targetEntity = organizationType.name,
            targetId = id,
            details = "{\"userId\":${request.userId},\"role\":\"${request.role}\"}",
        )
        return ApiResponse.success(OrganizationAdminResponse.from(orgAdmin))
    }

    @DeleteMapping("/{type}/{id}/admins/{userId}")
    fun removeAdmin(
        @PathVariable type: String,
        @PathVariable id: Long,
        @PathVariable userId: Long,
        @AuthenticationPrincipal admin: CustomUserDetails,
    ): ApiResponse<Unit> {
        val organizationType = OrganizationType.fromValue(type)
        organizationAdminService.removeAdmin(
            userId = userId,
            organizationType = organizationType,
            organizationId = id,
        )
        auditService.log(
            adminUserId = admin.id,
            action = "REMOVE_ADMIN",
            targetEntity = organizationType.name,
            targetId = id,
            details = "{\"userId\":$userId}",
        )
        return ApiResponse.success(Unit)
    }

    @GetMapping("/{type}/{id}/admins")
    fun getAdminsByOrganization(
        @PathVariable type: String,
        @PathVariable id: Long,
    ): ApiResponse<List<OrganizationAdminResponse>> {
        val organizationType = OrganizationType.fromValue(type)
        val admins =
            organizationAdminService.getAdminsByOrganization(
                organizationType = organizationType,
                organizationId = id,
            )
        return ApiResponse.success(admins.map { OrganizationAdminResponse.from(it) })
    }

    @GetMapping("/by-user/{userId}")
    fun getOrganizationsByUser(
        @PathVariable userId: Long,
    ): ApiResponse<List<OrganizationAdminResponse>> {
        val admins = organizationAdminService.getOrganizationsByUser(userId)
        return ApiResponse.success(admins.map { OrganizationAdminResponse.from(it) })
    }

    @PutMapping("/{type}/{id}/admins/{userId}/role")
    fun changeRole(
        @PathVariable type: String,
        @PathVariable id: Long,
        @PathVariable userId: Long,
        @Valid @RequestBody request: ChangeRoleRequest,
        @AuthenticationPrincipal admin: CustomUserDetails,
    ): ApiResponse<OrganizationAdminResponse> {
        val organizationType = OrganizationType.fromValue(type)
        val orgAdmin =
            organizationAdminService.changeRole(
                userId = userId,
                organizationType = organizationType,
                organizationId = id,
                newRole = request.role,
            )
        auditService.log(
            adminUserId = admin.id,
            action = "CHANGE_ADMIN_ROLE",
            targetEntity = organizationType.name,
            targetId = id,
            details = "{\"userId\":$userId,\"newRole\":\"${request.role}\"}",
        )
        return ApiResponse.success(OrganizationAdminResponse.from(orgAdmin))
    }
}
