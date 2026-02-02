package com.nextup.infrastructure.security.oauth2

import com.nextup.common.exception.UnsupportedOAuth2ProviderException
import com.nextup.core.domain.user.OAuthProvider
import com.nextup.infrastructure.security.oauth2.impl.GoogleOAuth2UserInfo
import com.nextup.infrastructure.security.oauth2.impl.KakaoOAuth2UserInfo
import com.nextup.infrastructure.security.oauth2.impl.NaverOAuth2UserInfo

/**
 * OAuth2 Provider별 UserInfo 객체를 생성하는 팩토리
 */
object OAuth2UserInfoFactory {

    /**
     * registrationId에 맞는 OAuth2UserInfo 구현체를 생성합니다.
     *
     * @param registrationId OAuth2 클라이언트 등록 ID (kakao, google, naver)
     * @param attributes OAuth2 Provider로부터 받은 사용자 속성
     * @return OAuth2UserInfo 구현체
     * @throws UnsupportedOAuth2ProviderException 지원하지 않는 Provider인 경우
     */
    fun create(registrationId: String, attributes: Map<String, Any>): OAuth2UserInfo {
        return when (registrationId.lowercase()) {
            "kakao" -> KakaoOAuth2UserInfo(attributes)
            "google" -> GoogleOAuth2UserInfo(attributes)
            "naver" -> NaverOAuth2UserInfo(attributes)
            else -> throw UnsupportedOAuth2ProviderException(registrationId)
        }
    }

    /**
     * registrationId를 OAuthProvider로 변환합니다.
     *
     * @param registrationId OAuth2 클라이언트 등록 ID
     * @return OAuthProvider enum
     * @throws UnsupportedOAuth2ProviderException 지원하지 않는 Provider인 경우
     */
    fun toOAuthProvider(registrationId: String): OAuthProvider {
        return when (registrationId.lowercase()) {
            "kakao" -> OAuthProvider.KAKAO
            "google" -> OAuthProvider.GOOGLE
            "naver" -> OAuthProvider.NAVER
            else -> throw UnsupportedOAuth2ProviderException(registrationId)
        }
    }
}
