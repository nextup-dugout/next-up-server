package com.nextup.infrastructure.security.oauth2

import com.nextup.core.domain.user.OAuthProvider

/**
 * OAuth2 Provider로부터 받은 사용자 정보 인터페이스
 */
interface OAuth2UserInfo {
    val provider: OAuthProvider
    val id: String
    val email: String?
    val name: String?
    val profileImageUrl: String?
    val attributes: Map<String, Any>
}
