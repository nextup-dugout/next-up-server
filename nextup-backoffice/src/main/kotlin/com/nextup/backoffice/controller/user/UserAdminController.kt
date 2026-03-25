package com.nextup.backoffice.controller.user

import com.nextup.backoffice.dto.user.CreateUserRequest
import com.nextup.backoffice.dto.user.RoleChangeRequest
import com.nextup.backoffice.dto.user.UpdateUserRequest
import com.nextup.backoffice.dto.user.UserAdminResponse
import com.nextup.backoffice.dto.user.UserListResponse
import com.nextup.backoffice.dto.user.toAdminResponse
import com.nextup.backoffice.dto.user.toListResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.common.PageResult
import com.nextup.core.domain.user.Role
import com.nextup.core.service.audit.AuditService
import com.nextup.core.service.user.UserService
import com.nextup.infrastructure.common.toPageCommand
import com.nextup.infrastructure.security.userdetails.CustomUserDetails
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/backoffice/users")
class UserAdminController(
    private val userService: UserService,
    private val passwordEncoder: PasswordEncoder,
    private val auditService: AuditService,
) {
    @GetMapping
    fun getAllUsers(
        @PageableDefault(size = 20) pageable: Pageable,
        @RequestParam(required = false) isActive: Boolean?,
    ): ApiResponse<PageResult<UserListResponse>> {
        val users =
            if (isActive != null) {
                userService.getAllByStatus(isActive, pageable.toPageCommand())
            } else {
                userService.getAll(pageable.toPageCommand())
            }
        return ApiResponse.success(users.map { it.toListResponse() })
    }

    @GetMapping("/search")
    fun searchUsers(
        @RequestParam keyword: String,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ApiResponse<PageResult<UserListResponse>> =
        ApiResponse.success(userService.search(keyword, pageable.toPageCommand()).map { it.toListResponse() })

    @GetMapping("/by-role/{role}")
    fun getUsersByRole(
        @PathVariable role: String,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ApiResponse<PageResult<UserListResponse>> =
        ApiResponse.success(
            userService.getAllByRole(Role.valueOf(role.uppercase()), pageable.toPageCommand()).map {
                it.toListResponse()
            },
        )

    @GetMapping("/{id}")
    fun getUser(
        @PathVariable id: Long,
    ): ApiResponse<UserAdminResponse> = ApiResponse.success(userService.getById(id).toAdminResponse())

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createUser(
        @Valid @RequestBody request: CreateUserRequest,
        @AuthenticationPrincipal admin: CustomUserDetails,
    ): ApiResponse<UserAdminResponse> {
        val user =
            userService.createLocalUser(
                email = request.email,
                encodedPassword = passwordEncoder.encode(request.password),
                nickname = request.nickname,
            )
        request.roles.forEach { roleName ->
            val role = Role.valueOf(roleName.uppercase())
            if (role != Role.USER) userService.addRole(user.id, role)
        }
        auditService.log(
            adminUserId = admin.id,
            action = "CREATE_USER",
            targetEntity = "User",
            targetId = user.id,
            details = "{\"email\":\"${request.email}\"}",
        )
        return ApiResponse.success(userService.getById(user.id).toAdminResponse())
    }

    @PutMapping("/{id}")
    fun updateUser(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateUserRequest,
        @AuthenticationPrincipal admin: CustomUserDetails,
    ): ApiResponse<UserAdminResponse> {
        val user =
            userService.updateProfile(
                userId = id,
                nickname = request.nickname,
                profileImageUrl = request.profileImageUrl,
            )
        request.roles?.let { newRoles ->
            val currentRoles = user.roles.map { it.name }.toSet()
            val targetRoles = newRoles.map { it.uppercase() }.toSet()
            currentRoles
                .filter { it !in targetRoles && it != "USER" }
                .forEach { userService.removeRole(id, Role.valueOf(it)) }
            targetRoles
                .filter { it !in currentRoles && it != "USER" }
                .forEach { userService.addRole(id, Role.valueOf(it)) }
        }
        auditService.log(
            adminUserId = admin.id,
            action = "UPDATE_USER",
            targetEntity = "User",
            targetId = id,
            details = "{\"nickname\":\"${request.nickname}\"}",
        )
        return ApiResponse.success(userService.getById(id).toAdminResponse())
    }

    @PostMapping("/{id}/roles")
    fun addRole(
        @PathVariable id: Long,
        @Valid @RequestBody request: RoleChangeRequest,
        @AuthenticationPrincipal admin: CustomUserDetails,
    ): ApiResponse<UserAdminResponse> {
        val user = userService.addRole(id, Role.valueOf(request.role.uppercase()))
        auditService.log(
            adminUserId = admin.id,
            action = "ADD_ROLE",
            targetEntity = "User",
            targetId = id,
            details = "{\"role\":\"${request.role}\"}",
        )
        return ApiResponse.success(user.toAdminResponse())
    }

    @DeleteMapping("/{id}/roles/{role}")
    fun removeRole(
        @PathVariable id: Long,
        @PathVariable role: String,
        @AuthenticationPrincipal admin: CustomUserDetails,
    ): ApiResponse<UserAdminResponse> {
        val user = userService.removeRole(id, Role.valueOf(role.uppercase()))
        auditService.log(
            adminUserId = admin.id,
            action = "REMOVE_ROLE",
            targetEntity = "User",
            targetId = id,
            details = "{\"role\":\"$role\"}",
        )
        return ApiResponse.success(user.toAdminResponse())
    }

    @DeleteMapping("/{id}")
    fun deactivateUser(
        @PathVariable id: Long,
        @AuthenticationPrincipal admin: CustomUserDetails,
    ): ApiResponse<UserAdminResponse> {
        val user = userService.deactivate(id)
        auditService.log(adminUserId = admin.id, action = "DEACTIVATE_USER", targetEntity = "User", targetId = id)
        return ApiResponse.success(user.toAdminResponse())
    }

    @PostMapping("/{id}/activate")
    fun activateUser(
        @PathVariable id: Long,
        @AuthenticationPrincipal admin: CustomUserDetails,
    ): ApiResponse<UserAdminResponse> {
        val user = userService.activate(id)
        auditService.log(adminUserId = admin.id, action = "ACTIVATE_USER", targetEntity = "User", targetId = id)
        return ApiResponse.success(user.toAdminResponse())
    }
}
