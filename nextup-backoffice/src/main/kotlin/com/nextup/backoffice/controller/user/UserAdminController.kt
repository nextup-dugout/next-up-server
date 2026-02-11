package com.nextup.backoffice.controller.user

import com.nextup.backoffice.dto.common.ApiResponse
import com.nextup.backoffice.dto.user.*
import com.nextup.core.domain.user.Role
import com.nextup.core.service.user.UserService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

/**
 * 사용자 관리 API Controller (관리자용)
 *
 * 관리자가 사용자를 관리하는 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/backoffice/users")
class UserAdminController(
    private val userService: UserService,
) {
    /**
     * 모든 사용자 목록을 페이징으로 조회합니다.
     */
    @GetMapping
    fun getAllUsers(
        @PageableDefault(size = 20) pageable: Pageable,
        @RequestParam(required = false) isActive: Boolean?,
    ): ApiResponse<Page<UserListResponse>> {
        val users =
            if (isActive != null) {
                userService.getAllByStatus(isActive, pageable)
            } else {
                userService.getAll(pageable)
            }
        return ApiResponse.success(users.map { UserListResponse.from(it) })
    }

    /**
     * 사용자를 검색합니다.
     */
    @GetMapping("/search")
    fun searchUsers(
        @RequestParam keyword: String,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ApiResponse<Page<UserListResponse>> {
        val users = userService.search(keyword, pageable)
        return ApiResponse.success(users.map { UserListResponse.from(it) })
    }

    /**
     * 역할별 사용자를 조회합니다.
     */
    @GetMapping("/by-role/{role}")
    fun getUsersByRole(
        @PathVariable role: String,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ApiResponse<Page<UserListResponse>> {
        val roleEnum = Role.valueOf(role.uppercase())
        val users = userService.getAllByRole(roleEnum, pageable)
        return ApiResponse.success(users.map { UserListResponse.from(it) })
    }

    /**
     * 사용자 상세 정보를 조회합니다.
     */
    @GetMapping("/{id}")
    fun getUser(
        @PathVariable id: Long,
    ): ApiResponse<UserAdminResponse> {
        val user = userService.getById(id)
        return ApiResponse.success(UserAdminResponse.from(user))
    }

    /**
     * 사용자를 생성합니다 (관리자용 - 비밀번호 직접 설정).
     *
     * 주의: 실제 운영에서는 PasswordEncoder를 사용해야 합니다.
     * Security 모듈 구현 후 수정 필요.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createUser(
        @Valid @RequestBody request: CreateUserRequest,
    ): ApiResponse<UserAdminResponse> {
        // TODO: PasswordEncoder 적용 필요 (Issue #26)
        val user =
            userService.createLocalUser(
                email = request.email,
                encodedPassword = request.password, // 임시: 실제로는 인코딩 필요
                nickname = request.nickname,
            )

        // 추가 역할 부여
        request.roles.forEach { roleName ->
            val role = Role.valueOf(roleName.uppercase())
            if (role != Role.USER) {
                userService.addRole(user.id, role)
            }
        }

        return ApiResponse.success(UserAdminResponse.from(userService.getById(user.id)))
    }

    /**
     * 사용자 정보를 수정합니다.
     */
    @PutMapping("/{id}")
    fun updateUser(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateUserRequest,
    ): ApiResponse<UserAdminResponse> {
        var user =
            userService.updateProfile(
                userId = id,
                nickname = request.nickname,
                profileImageUrl = request.profileImageUrl,
            )

        // 역할 변경 처리
        request.roles?.let { newRoles ->
            val currentRoles = user.roles.map { it.name }.toSet()
            val targetRoles = newRoles.map { it.uppercase() }.toSet()

            // 제거할 역할
            currentRoles.filter { it !in targetRoles && it != "USER" }.forEach { roleName ->
                userService.removeRole(id, Role.valueOf(roleName))
            }

            // 추가할 역할
            targetRoles.filter { it !in currentRoles && it != "USER" }.forEach { roleName ->
                userService.addRole(id, Role.valueOf(roleName))
            }
        }

        return ApiResponse.success(UserAdminResponse.from(userService.getById(id)))
    }

    /**
     * 사용자에게 역할을 추가합니다.
     */
    @PostMapping("/{id}/roles")
    fun addRole(
        @PathVariable id: Long,
        @Valid @RequestBody request: RoleChangeRequest,
    ): ApiResponse<UserAdminResponse> {
        val role = Role.valueOf(request.role.uppercase())
        val user = userService.addRole(id, role)
        return ApiResponse.success(UserAdminResponse.from(user))
    }

    /**
     * 사용자에게서 역할을 제거합니다.
     */
    @DeleteMapping("/{id}/roles/{role}")
    fun removeRole(
        @PathVariable id: Long,
        @PathVariable role: String,
    ): ApiResponse<UserAdminResponse> {
        val roleEnum = Role.valueOf(role.uppercase())
        val user = userService.removeRole(id, roleEnum)
        return ApiResponse.success(UserAdminResponse.from(user))
    }

    /**
     * 사용자를 비활성화합니다.
     */
    @DeleteMapping("/{id}")
    fun deactivateUser(
        @PathVariable id: Long,
    ): ApiResponse<UserAdminResponse> {
        val user = userService.deactivate(id)
        return ApiResponse.success(UserAdminResponse.from(user))
    }

    /**
     * 사용자를 활성화합니다.
     */
    @PostMapping("/{id}/activate")
    fun activateUser(
        @PathVariable id: Long,
    ): ApiResponse<UserAdminResponse> {
        val user = userService.activate(id)
        return ApiResponse.success(UserAdminResponse.from(user))
    }
}
