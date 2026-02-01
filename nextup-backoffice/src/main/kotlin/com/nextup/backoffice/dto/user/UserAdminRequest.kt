package com.nextup.backoffice.dto.user

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 사용자 생성 요청 DTO (관리자용)
 */
data class CreateUserRequest(
    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    val email: String,

    @field:NotBlank(message = "비밀번호는 필수입니다")
    @field:Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
    val password: String,

    @field:NotBlank(message = "닉네임은 필수입니다")
    @field:Size(min = 2, max = 50, message = "닉네임은 2~50자 사이여야 합니다")
    val nickname: String,

    val roles: Set<String> = emptySet()
)

/**
 * 사용자 정보 수정 요청 DTO (관리자용)
 */
data class UpdateUserRequest(
    @field:Size(min = 2, max = 50, message = "닉네임은 2~50자 사이여야 합니다")
    val nickname: String? = null,

    val profileImageUrl: String? = null,

    val roles: Set<String>? = null
)

/**
 * 역할 변경 요청 DTO
 */
data class RoleChangeRequest(
    @field:NotBlank(message = "역할은 필수입니다")
    val role: String
)

/**
 * 사용자 검색 필터
 */
data class UserSearchFilter(
    val keyword: String? = null,
    val role: String? = null,
    val isActive: Boolean? = null
)
