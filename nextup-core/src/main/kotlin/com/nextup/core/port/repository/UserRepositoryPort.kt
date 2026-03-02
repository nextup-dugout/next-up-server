package com.nextup.core.port.repository

import com.nextup.core.common.PageCommand
import com.nextup.core.common.PageResult
import com.nextup.core.domain.user.Role
import com.nextup.core.domain.user.User

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

    fun findAllActive(pageCommand: PageCommand): PageResult<User>

    fun findAllByIsActive(
        isActive: Boolean,
        pageCommand: PageCommand,
    ): PageResult<User>

    fun searchByKeyword(
        keyword: String,
        pageCommand: PageCommand,
    ): PageResult<User>

    fun findAllByRole(
        role: Role,
        pageCommand: PageCommand,
    ): PageResult<User>

    fun findAllByRolesIn(
        roles: Set<Role>,
        pageCommand: PageCommand,
    ): PageResult<User>

    fun findByPlayerId(playerId: Long): User?
}
