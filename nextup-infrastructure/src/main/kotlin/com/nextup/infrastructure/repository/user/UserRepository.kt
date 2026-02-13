package com.nextup.infrastructure.repository.user

import com.nextup.core.domain.user.Role
import com.nextup.core.domain.user.User
import com.nextup.core.port.repository.UserRepositoryPort
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
    override fun findAllActive(pageable: Pageable): Page<User>

    @Query("SELECT u FROM User u WHERE u.isActive = :isActive")
    override fun findAllByIsActive(
        isActive: Boolean,
        pageable: Pageable,
    ): Page<User>

    @Query(
        """
        SELECT u FROM User u
        WHERE u.nickname LIKE %:keyword%
        OR u.email LIKE %:keyword%
    """,
    )
    override fun searchByKeyword(
        keyword: String,
        pageable: Pageable,
    ): Page<User>

    @Query(
        """
        SELECT u FROM User u
        JOIN u._roles r
        WHERE r = :role
    """,
    )
    override fun findAllByRole(
        role: Role,
        pageable: Pageable,
    ): Page<User>

    @Query(
        """
        SELECT u FROM User u
        JOIN u._roles r
        WHERE r IN :roles
    """,
    )
    override fun findAllByRolesIn(
        roles: Set<Role>,
        pageable: Pageable,
    ): Page<User>

    @Query("SELECT u FROM User u WHERE u.player.id = :playerId")
    override fun findByPlayerId(playerId: Long): User?
}
