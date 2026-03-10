package com.nextup.infrastructure.repository

import com.nextup.core.domain.user.User
import org.springframework.data.jpa.repository.JpaRepository

/**
 * User JPA Repository
 */
interface UserJpaRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?

    fun existsByEmail(email: String): Boolean
}
