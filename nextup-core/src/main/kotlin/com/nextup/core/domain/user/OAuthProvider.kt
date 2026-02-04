package com.nextup.core.domain.user

/**
 * OAuth 제공자
 */
enum class OAuthProvider(
    val displayName: String,
) {
    /** 일반 회원가입 (OAuth 미사용) */
    LOCAL("일반"),

    /** 카카오 로그인 */
    KAKAO("카카오"),

    /** 구글 로그인 */
    GOOGLE("구글"),

    /** 네이버 로그인 */
    NAVER("네이버"),

    /** 애플 로그인 */
    APPLE("애플"),
}
