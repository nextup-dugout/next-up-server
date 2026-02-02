package com.nextup.infrastructure.security.oauth2

import com.nextup.core.domain.user.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User

/**
 * OAuth2 인증된 사용자 Principal
 *
 * Spring Security OAuth2와 내부 User Entity를 연결합니다.
 */
class OAuth2UserPrincipal(
    private val user: User,
    private val oAuth2UserInfo: OAuth2UserInfo,
    val isNewUser: Boolean = false
) : OAuth2User {

    val userId: Long
        get() = user.id

    val email: String
        get() = user.email

    val nickname: String
        get() = user.nickname

    override fun getName(): String = user.id.toString()

    override fun getAttributes(): Map<String, Any> = oAuth2UserInfo.attributes

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return user.roles.map { role ->
            SimpleGrantedAuthority("ROLE_${role.name}")
        }
    }

    /**
     * 사용자 역할 이름 목록을 반환합니다 (ROLE_ 접두사 없음).
     */
    fun getRoleNames(): Set<String> = user.roles.map { it.name }.toSet()
}
