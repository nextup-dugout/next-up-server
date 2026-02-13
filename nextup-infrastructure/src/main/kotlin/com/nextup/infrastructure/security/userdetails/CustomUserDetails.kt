package com.nextup.infrastructure.security.userdetails

import com.nextup.core.domain.user.Role
import com.nextup.core.domain.user.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

/**
 * Spring Security UserDetails 구현체
 *
 * User 엔티티를 Spring Security가 인식할 수 있는 형태로 래핑합니다.
 */
class CustomUserDetails(
    private val user: User,
) : UserDetails {
    val id: Long get() = user.id

    val email: String get() = user.email

    val nickname: String get() = user.nickname

    val roles: Set<Role> get() = user.roles

    override fun getAuthorities(): Collection<GrantedAuthority> =
        user.roles.map { role ->
            SimpleGrantedAuthority("ROLE_${role.name}")
        }

    override fun getPassword(): String? = user.password

    override fun getUsername(): String = user.email

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = user.isActive

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = user.isActive

    /**
     * 권한 이름 목록을 반환합니다 (ROLE_ 접두사 없음).
     */
    fun getRoleNames(): Set<String> = user.roles.map { it.name }.toSet()

    companion object {
        /**
         * User 엔티티로부터 CustomUserDetails를 생성합니다.
         *
         * @param user User 엔티티
         * @return CustomUserDetails
         */
        fun from(user: User): CustomUserDetails = CustomUserDetails(user)
    }
}
