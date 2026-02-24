package com.nextup.api.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.util.AntPathMatcher

/**
 * SecurityConfig의 URL 패턴 규칙을 단위 테스트로 검증합니다.
 *
 * Spring 컨텍스트 없이 AntPathMatcher를 사용하여 패턴 매칭 로직을 검증합니다.
 * 실제 SecurityFilterChain 동작은 통합 테스트에서 검증해야 하지만,
 * URL 패턴 정의 자체의 정확성을 단위 테스트로 빠르게 검증할 수 있습니다.
 */
@DisplayName("SecurityConfig URL 패턴 테스트")
class SecurityConfigUrlPatternTest {
    private val matcher = AntPathMatcher()

    // SecurityConfig에서 정의된 public 패턴 목록
    private val publicPatterns =
        listOf(
            "/api/auth/**",
            "/oauth2/**",
            "/login/oauth2/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/actuator/health",
        )

    // GET 전용 public 패턴
    private val publicGetPatterns =
        listOf(
            "/api/associations/**",
            "/api/stats/**",
        )

    // ADMIN 전용 패턴
    private val adminPatterns =
        listOf(
            "/api/admin/**",
        )

    // SCORER or ADMIN 패턴
    private val scorerAdminPatterns =
        listOf(
            "/api/games/*/records/**",
        )

    private fun matchesAnyPublicPattern(url: String): Boolean = publicPatterns.any { matcher.match(it, url) }

    private fun matchesAnyPublicGetPattern(url: String): Boolean = publicGetPatterns.any { matcher.match(it, url) }

    private fun matchesAnyAdminPattern(url: String): Boolean = adminPatterns.any { matcher.match(it, url) }

    private fun matchesAnyScorerAdminPattern(url: String): Boolean = scorerAdminPatterns.any { matcher.match(it, url) }

    @Nested
    @DisplayName("인증 엔드포인트 - public")
    inner class AuthEndpoints {
        @Test
        fun `api auth 로그인 경로는 public이어야 한다`() {
            assertThat(matchesAnyPublicPattern("/api/auth/login")).isTrue()
        }

        @Test
        fun `api auth 회원가입 경로는 public이어야 한다`() {
            assertThat(matchesAnyPublicPattern("/api/auth/register")).isTrue()
        }

        @Test
        fun `api auth 토큰 갱신 경로는 public이어야 한다`() {
            assertThat(matchesAnyPublicPattern("/api/auth/refresh")).isTrue()
        }
    }

    @Nested
    @DisplayName("OAuth2 엔드포인트 - public")
    inner class OAuth2Endpoints {
        @Test
        fun `oauth2 authorization 경로는 public이어야 한다`() {
            assertThat(matchesAnyPublicPattern("/oauth2/authorization/google")).isTrue()
        }

        @Test
        fun `login oauth2 code 경로는 public이어야 한다`() {
            assertThat(matchesAnyPublicPattern("/login/oauth2/code/kakao")).isTrue()
        }
    }

    @Nested
    @DisplayName("Swagger 엔드포인트 - public")
    inner class SwaggerEndpoints {
        @Test
        fun `swagger ui html은 public이어야 한다`() {
            assertThat(matchesAnyPublicPattern("/swagger-ui.html")).isTrue()
        }

        @Test
        fun `swagger ui 경로는 public이어야 한다`() {
            assertThat(matchesAnyPublicPattern("/swagger-ui/index.html")).isTrue()
        }

        @Test
        fun `v3 api docs 경로는 public이어야 한다`() {
            assertThat(matchesAnyPublicPattern("/v3/api-docs/swagger-config")).isTrue()
        }

        @Test
        fun `swagger resources 경로는 public이어야 한다`() {
            assertThat(matchesAnyPublicPattern("/swagger-resources/configuration/ui")).isTrue()
        }
    }

    @Nested
    @DisplayName("Actuator 엔드포인트")
    inner class ActuatorEndpoints {
        @Test
        fun `actuator health는 public이어야 한다`() {
            assertThat(matchesAnyPublicPattern("/actuator/health")).isTrue()
        }

        @Test
        fun `actuator metrics는 public 패턴에 포함되지 않아야 한다`() {
            // /actuator/health 만 허용, 나머지 actuator는 인증 필요
            assertThat(matchesAnyPublicPattern("/actuator/metrics")).isFalse()
        }
    }

    @Nested
    @DisplayName("공개 조회 엔드포인트 (GET only)")
    inner class PublicGetEndpoints {
        @Test
        fun `api associations 경로는 GET public 패턴에 포함된다`() {
            assertThat(matchesAnyPublicGetPattern("/api/associations/1")).isTrue()
        }

        @Test
        fun `api stats 경로는 GET public 패턴에 포함된다`() {
            assertThat(matchesAnyPublicGetPattern("/api/stats/batting")).isTrue()
        }

        @Test
        fun `api teams 경로는 GET public 패턴에 포함되지 않는다`() {
            // /api/teams/**은 GET public 패턴 아님 - 인증 필요
            assertThat(matchesAnyPublicGetPattern("/api/teams/1")).isFalse()
        }
    }

    @Nested
    @DisplayName("ADMIN 전용 엔드포인트")
    inner class AdminEndpoints {
        @Test
        fun `api admin 경로는 ADMIN 패턴에 포함된다`() {
            assertThat(matchesAnyAdminPattern("/api/admin/users")).isTrue()
        }

        @Test
        fun `api admin 하위 경로도 ADMIN 패턴에 포함된다`() {
            assertThat(matchesAnyAdminPattern("/api/admin/teams/1/promote")).isTrue()
        }

        @Test
        fun `api auth 경로는 ADMIN 패턴에 포함되지 않는다`() {
            assertThat(matchesAnyAdminPattern("/api/auth/login")).isFalse()
        }
    }

    @Nested
    @DisplayName("SCORER or ADMIN 전용 엔드포인트")
    inner class ScorerAdminEndpoints {
        @Test
        fun `api games records 경로는 SCORER ADMIN 패턴에 포함된다`() {
            assertThat(matchesAnyScorerAdminPattern("/api/games/123/records/batting")).isTrue()
        }

        @Test
        fun `api games records 중첩 경로도 SCORER ADMIN 패턴에 포함된다`() {
            assertThat(matchesAnyScorerAdminPattern("/api/games/456/records/pitching/events")).isTrue()
        }

        @Test
        fun `api games 기본 경로는 SCORER ADMIN 패턴에 포함되지 않는다`() {
            // /api/games/123 만으로는 records 패턴에 해당하지 않음
            assertThat(matchesAnyScorerAdminPattern("/api/games/123")).isFalse()
        }
    }

    @Nested
    @DisplayName("보호된 엔드포인트 - 인증 필요")
    inner class ProtectedEndpoints {
        @Test
        fun `api teams 경로는 어떤 public 패턴에도 해당하지 않는다`() {
            val url = "/api/teams/1"
            assertThat(matchesAnyPublicPattern(url)).isFalse()
            assertThat(matchesAnyPublicGetPattern(url)).isFalse()
        }

        @Test
        fun `api players 경로는 어떤 public 패턴에도 해당하지 않는다`() {
            val url = "/api/players/1"
            assertThat(matchesAnyPublicPattern(url)).isFalse()
            assertThat(matchesAnyPublicGetPattern(url)).isFalse()
        }

        @Test
        fun `api notifications 경로는 어떤 public 패턴에도 해당하지 않는다`() {
            val url = "/api/notifications"
            assertThat(matchesAnyPublicPattern(url)).isFalse()
            assertThat(matchesAnyPublicGetPattern(url)).isFalse()
        }
    }
}
