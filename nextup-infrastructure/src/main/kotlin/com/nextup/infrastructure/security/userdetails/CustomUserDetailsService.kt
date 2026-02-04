package com.nextup.infrastructure.security.userdetails

import com.nextup.core.domain.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

/**
 * User JPA Repository
 */
interface UserJpaRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?

    fun existsByEmail(email: String): Boolean
}

/**
 * Spring Security UserDetailsService 구현체
 *
 * 이메일을 기반으로 사용자를 조회하여 UserDetails로 변환합니다.
 */
@Service
class CustomUserDetailsService(
    private val userJpaRepository: UserJpaRepository,
) : UserDetailsService {
    /**
     * 이메일로 사용자를 조회합니다.
     *
     * @param username 사용자 이메일
     * @return UserDetails
     * @throws UsernameNotFoundException 사용자를 찾을 수 없는 경우
     */
    override fun loadUserByUsername(username: String): UserDetails {
        val user =
            userJpaRepository.findByEmail(username)
                ?: throw UsernameNotFoundException("User not found with email: $username")

        return CustomUserDetails.from(user)
    }

    /**
     * 사용자 ID로 사용자를 조회합니다.
     *
     * @param userId 사용자 ID
     * @return CustomUserDetails
     * @throws UsernameNotFoundException 사용자를 찾을 수 없는 경우
     */
    fun loadUserById(userId: Long): CustomUserDetails {
        val user =
            userJpaRepository.findByIdOrNull(userId)
                ?: throw UsernameNotFoundException("User not found with id: $userId")

        return CustomUserDetails.from(user)
    }
}
