package com.nextup.core.port.repository

import com.nextup.core.domain.user.Role
import com.nextup.core.domain.user.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * User Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface UserRepositoryPort {
    fun save(user: User): User

    fun findByIdOrNull(id: Long): User?

    fun findByEmail(email: String): User?

    fun existsByEmail(email: String): Boolean

    fun delete(user: User)

    fun deleteById(id: Long)

    fun findAllActive(pageable: Pageable): Page<User>

    fun findAllByIsActive(
        isActive: Boolean,
        pageable: Pageable,
    ): Page<User>

    fun searchByKeyword(
        keyword: String,
        pageable: Pageable,
    ): Page<User>

    fun findAllByRole(
        role: Role,
        pageable: Pageable,
    ): Page<User>

    fun findAllByRolesIn(
        roles: Set<Role>,
        pageable: Pageable,
    ): Page<User>

    fun findByPlayerId(playerId: Long): User?
}
