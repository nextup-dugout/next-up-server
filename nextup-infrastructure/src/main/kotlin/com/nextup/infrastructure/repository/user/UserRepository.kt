package com.nextup.infrastructure.repository.user

import com.nextup.core.common.PageCommand
import com.nextup.core.common.PageResult
import com.nextup.core.domain.user.Role
import com.nextup.core.domain.user.User
import com.nextup.core.port.repository.UserRepositoryPort
import com.nextup.infrastructure.common.toPageResult
import com.nextup.infrastructure.common.toPageable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface UserRepository :
    JpaRepository<User, Long>,
    UserRepositoryPort {
    override fun findByEmail(email: String): User?

    override fun existsByEmail(email: String): Boolean

    @Query("SELECT u FROM User u WHERE u.isActive = true")
    fun findAllActiveByPageable(pageable: Pageable): Page<User>

    override fun findAllActive(pageCommand: PageCommand): PageResult<User> =
        findAllActiveByPageable(pageCommand.toPageable()).toPageResult()

    @Query("SELECT u FROM User u WHERE u.isActive = :isActive")
    fun findAllByIsActiveByPageable(
        isActive: Boolean,
        pageable: Pageable,
    ): Page<User>

    override fun findAllByIsActive(
        isActive: Boolean,
        pageCommand: PageCommand,
    ): PageResult<User> = findAllByIsActiveByPageable(isActive, pageCommand.toPageable()).toPageResult()

    @Query(
        """
        SELECT u FROM User u
        WHERE u.nickname LIKE %:keyword%
        OR u.email LIKE %:keyword%
    """,
    )
    fun searchByKeywordByPageable(
        keyword: String,
        pageable: Pageable,
    ): Page<User>

    override fun searchByKeyword(
        keyword: String,
        pageCommand: PageCommand,
    ): PageResult<User> = searchByKeywordByPageable(keyword, pageCommand.toPageable()).toPageResult()

    @Query(
        """
        SELECT u FROM User u
        JOIN u._roles r
        WHERE r = :role
    """,
    )
    fun findAllByRoleByPageable(
        role: Role,
        pageable: Pageable,
    ): Page<User>

    override fun findAllByRole(
        role: Role,
        pageCommand: PageCommand,
    ): PageResult<User> = findAllByRoleByPageable(role, pageCommand.toPageable()).toPageResult()

    @Query(
        """
        SELECT u FROM User u
        JOIN u._roles r
        WHERE r IN :roles
    """,
    )
    fun findAllByRolesInByPageable(
        roles: Set<Role>,
        pageable: Pageable,
    ): Page<User>

    override fun findAllByRolesIn(
        roles: Set<Role>,
        pageCommand: PageCommand,
    ): PageResult<User> = findAllByRolesInByPageable(roles, pageCommand.toPageable()).toPageResult()

    @Query("SELECT u FROM User u WHERE u.player.id = :playerId")
    override fun findByPlayerId(playerId: Long): User?
}
