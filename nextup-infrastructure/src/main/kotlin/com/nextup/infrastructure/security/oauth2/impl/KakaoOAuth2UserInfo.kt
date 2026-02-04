package com.nextup.infrastructure.security.oauth2.impl

import com.nextup.core.domain.user.OAuthProvider
import com.nextup.infrastructure.security.oauth2.OAuth2UserInfo

/**
 * 카카오 OAuth2 사용자 정보
 *
 * 카카오 응답 형식:
 * {
 *   "id": 123456789,
 *   "kakao_account": {
 *     "email": "user@kakao.com",
 *     "profile": {
 *       "nickname": "홍길동",
 *       "profile_image_url": "http://..."
 *     }
 *   }
 * }
 */
class KakaoOAuth2UserInfo(
    override val attributes: Map<String, Any>,
) : OAuth2UserInfo {
    override val provider: OAuthProvider = OAuthProvider.KAKAO

    override val id: String
        get() = attributes["id"].toString()

    override val email: String?
        get() {
            val kakaoAccount = attributes["kakao_account"] as? Map<*, *> ?: return null
            return kakaoAccount["email"] as? String
        }

    override val name: String?
        get() {
            val kakaoAccount = attributes["kakao_account"] as? Map<*, *> ?: return null
            val profile = kakaoAccount["profile"] as? Map<*, *> ?: return null
            return profile["nickname"] as? String
        }

    override val profileImageUrl: String?
        get() {
            val kakaoAccount = attributes["kakao_account"] as? Map<*, *> ?: return null
            val profile = kakaoAccount["profile"] as? Map<*, *> ?: return null
            return profile["profile_image_url"] as? String
        }
}
