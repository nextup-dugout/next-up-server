package com.nextup.api.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

@DisplayName("CacheControlInterceptor")
class CacheControlInterceptorTest {

    private lateinit var interceptor: CacheControlInterceptor

    @BeforeEach
    fun setUp() {
        interceptor = CacheControlInterceptor()
    }

    @Nested
    @DisplayName("GET 요청에 Cache-Control 헤더 추가")
    inner class GetRequestCacheControl {

        @Test
        @DisplayName("순위표 URL에 max-age=300, public 헤더가 추가된다")
        fun standingsUrl() {
            val request = MockHttpServletRequest("GET", "/api/v1/competitions/1/standings")
            val response = MockHttpServletResponse()

            interceptor.afterCompletion(request, response, Any(), null)

            val header = response.getHeader(HttpHeaders.CACHE_CONTROL)
            assertThat(header).contains("max-age=300")
            assertThat(header).contains("public")
        }

        @Test
        @DisplayName("리더보드 URL에 max-age=300, public 헤더가 추가된다")
        fun leaderboardUrl() {
            val request = MockHttpServletRequest("GET", "/api/v1/competitions/1/leaderboard/batting")
            val response = MockHttpServletResponse()

            interceptor.afterCompletion(request, response, Any(), null)

            val header = response.getHeader(HttpHeaders.CACHE_CONTROL)
            assertThat(header).contains("max-age=300")
            assertThat(header).contains("public")
        }

        @Test
        @DisplayName("통계 URL에 max-age=600, public 헤더가 추가된다")
        fun statsUrl() {
            val request = MockHttpServletRequest("GET", "/api/v1/players/1/stats/batting/season/2025")
            val response = MockHttpServletResponse()

            interceptor.afterCompletion(request, response, Any(), null)

            val header = response.getHeader(HttpHeaders.CACHE_CONTROL)
            assertThat(header).contains("max-age=600")
            assertThat(header).contains("public")
        }

        @Test
        @DisplayName("대회 URL에 max-age=1800, public 헤더가 추가된다")
        fun competitionsUrl() {
            val request = MockHttpServletRequest("GET", "/api/v1/competitions")
            val response = MockHttpServletResponse()

            interceptor.afterCompletion(request, response, Any(), null)

            val header = response.getHeader(HttpHeaders.CACHE_CONTROL)
            assertThat(header).contains("max-age=1800")
            assertThat(header).contains("public")
        }

        @Test
        @DisplayName("리그 URL에 max-age=1800, public 헤더가 추가된다")
        fun leaguesUrl() {
            val request = MockHttpServletRequest("GET", "/api/v1/leagues")
            val response = MockHttpServletResponse()

            interceptor.afterCompletion(request, response, Any(), null)

            val header = response.getHeader(HttpHeaders.CACHE_CONTROL)
            assertThat(header).contains("max-age=1800")
            assertThat(header).contains("public")
        }

        @Test
        @DisplayName("구장 URL에 max-age=3600, public 헤더가 추가된다")
        fun stadiumsUrl() {
            val request = MockHttpServletRequest("GET", "/api/v1/stadiums/1")
            val response = MockHttpServletResponse()

            interceptor.afterCompletion(request, response, Any(), null)

            val header = response.getHeader(HttpHeaders.CACHE_CONTROL)
            assertThat(header).contains("max-age=3600")
            assertThat(header).contains("public")
        }

        @Test
        @DisplayName("팀 통계 URL에 max-age=600 헤더가 추가된다")
        fun teamStatsUrl() {
            val request = MockHttpServletRequest("GET", "/api/v1/teams/1/stats")
            val response = MockHttpServletResponse()

            interceptor.afterCompletion(request, response, Any(), null)

            val header = response.getHeader(HttpHeaders.CACHE_CONTROL)
            assertThat(header).contains("max-age=600")
            assertThat(header).contains("public")
        }
    }

    @Nested
    @DisplayName("Cache-Control 헤더가 추가되지 않는 경우")
    inner class NoCacheControl {

        @Test
        @DisplayName("POST 요청에는 헤더가 추가되지 않는다")
        fun postRequest() {
            val request = MockHttpServletRequest("POST", "/api/v1/competitions")
            val response = MockHttpServletResponse()

            interceptor.afterCompletion(request, response, Any(), null)

            assertThat(response.getHeader(HttpHeaders.CACHE_CONTROL)).isNull()
        }

        @Test
        @DisplayName("매칭되지 않는 URL에는 헤더가 추가되지 않는다")
        fun unmatchedUrl() {
            val request = MockHttpServletRequest("GET", "/api/v1/auth/login")
            val response = MockHttpServletResponse()

            interceptor.afterCompletion(request, response, Any(), null)

            assertThat(response.getHeader(HttpHeaders.CACHE_CONTROL)).isNull()
        }

        @Test
        @DisplayName("4xx 에러 응답에는 헤더가 추가되지 않는다")
        fun errorResponse() {
            val request = MockHttpServletRequest("GET", "/api/v1/competitions/1/standings")
            val response = MockHttpServletResponse()
            response.status = 404

            interceptor.afterCompletion(request, response, Any(), null)

            assertThat(response.getHeader(HttpHeaders.CACHE_CONTROL)).isNull()
        }

        @Test
        @DisplayName("이미 Cache-Control 헤더가 있으면 덮어쓰지 않는다")
        fun existingHeader() {
            val request = MockHttpServletRequest("GET", "/api/v1/competitions/1/standings")
            val response = MockHttpServletResponse()
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache")

            interceptor.afterCompletion(request, response, Any(), null)

            assertThat(response.getHeader(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-cache")
        }
    }
}
