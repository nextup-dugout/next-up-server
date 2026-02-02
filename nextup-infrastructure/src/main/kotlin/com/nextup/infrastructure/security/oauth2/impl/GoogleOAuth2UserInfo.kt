package com.nextup.infrastructure.security.oauth2.impl

import com.nextup.core.domain.user.OAuthProvider
import com.nextup.infrastructure.security.oauth2.OAuth2UserInfo

/**
 * 구글 OAuth2 사용자 정보
 *
 * 구글 응답 형식:
 * {
 *   "sub": "123456789",
 *   "email": "user@gmail.com",
 *   "name": "홍길동",
 *   "picture": "http://..."
 * }
 */
class GoogleOAuth2UserInfo(
    override val attributes: Map<String, Any>
) : OAuth2UserInfo {

    override val provider: OAuthProvider = OAuthProvider.GOOGLE

    override val id: String
        get() = attributes["sub"] as? String ?: ""

    override val email: String?
        get() = attributes["email"] as? String

    override val name: String?
        get() = attributes["name"] as? String

    override val profileImageUrl: String?
        get() = attributes["picture"] as? String
}
