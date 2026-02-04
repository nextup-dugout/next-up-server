package com.nextup.infrastructure.security.oauth2.impl

import com.nextup.core.domain.user.OAuthProvider
import com.nextup.infrastructure.security.oauth2.OAuth2UserInfo

/**
 * 네이버 OAuth2 사용자 정보
 *
 * 네이버 응답 형식:
 * {
 *   "resultcode": "00",
 *   "message": "success",
 *   "response": {
 *     "id": "123456789",
 *     "email": "user@naver.com",
 *     "name": "홍길동",
 *     "profile_image": "http://..."
 *   }
 * }
 */
class NaverOAuth2UserInfo(
    override val attributes: Map<String, Any>,
) : OAuth2UserInfo {
    private val response: Map<*, *>?
        get() = attributes["response"] as? Map<*, *>

    override val provider: OAuthProvider = OAuthProvider.NAVER

    override val id: String
        get() = response?.get("id") as? String ?: ""

    override val email: String?
        get() = response?.get("email") as? String

    override val name: String?
        get() = response?.get("name") as? String

    override val profileImageUrl: String?
        get() = response?.get("profile_image") as? String
}
