package com.nextup.api.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Configuration
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.concurrent.TimeUnit

/**
 * HTTP Cache-Control 설정
 *
 * GET 조회 API 응답에 Cache-Control 헤더를 자동 추가합니다.
 * URL 패턴에 따라 적절한 max-age를 설정하여 클라이언트 측 캐싱을 유도합니다.
 *
 * | URL 패턴                  | max-age | scope   | 근거                     |
 * |--------------------------|---------|---------|-------------------------|
 * | /standings               | 300초   | public  | 순위표 (변경 빈도 낮음)    |
 * | /leaderboard             | 300초   | public  | 리더보드 (변경 빈도 낮음)  |
 * | /stats                   | 600초   | public  | 선수/팀 통계             |
 * | /competitions            | 1800초  | public  | 대회 기본 정보            |
 * | /leagues                 | 1800초  | public  | 리그 기본 정보            |
 * | /stadiums (GET only)     | 3600초  | public  | 구장 정보 (정적 데이터)    |
 */
@Configuration
class CacheControlConfig : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(CacheControlInterceptor())
            .addPathPatterns("/api/v1/**")
    }
}

/**
 * GET 요청에 Cache-Control 헤더를 추가하는 인터셉터
 *
 * POST/PUT/PATCH/DELETE 요청에는 적용하지 않습니다.
 * 응답에 이미 Cache-Control 헤더가 있으면 덮어쓰지 않습니다.
 */
class CacheControlInterceptor : HandlerInterceptor {

    companion object {
        private val CACHE_RULES: List<CacheRule> =
            listOf(
                CacheRule("/standings", 300, false),
                CacheRule("/leaderboard", 300, false),
                CacheRule("/stats", 600, false),
                CacheRule("/competitions", 1800, false),
                CacheRule("/leagues", 1800, false),
                CacheRule("/associations", 1800, false),
                CacheRule("/stadiums", 3600, false),
            )
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        // GET 요청만 대상
        if (request.method != "GET") return

        // 이미 Cache-Control 헤더가 설정되어 있으면 건너뜀
        if (response.containsHeader(HttpHeaders.CACHE_CONTROL)) return

        // 에러 응답에는 캐시 적용하지 않음
        if (response.status >= 400) return

        val uri = request.requestURI
        val rule = CACHE_RULES.firstOrNull { uri.contains(it.pathPattern) } ?: return

        val cacheControl =
            if (rule.isPrivate) {
                CacheControl.maxAge(rule.maxAgeSec.toLong(), TimeUnit.SECONDS)
                    .cachePrivate()
            } else {
                CacheControl.maxAge(rule.maxAgeSec.toLong(), TimeUnit.SECONDS)
                    .cachePublic()
            }

        response.setHeader(HttpHeaders.CACHE_CONTROL, cacheControl.headerValue)
    }

    private data class CacheRule(
        val pathPattern: String,
        val maxAgeSec: Int,
        val isPrivate: Boolean,
    )
}
