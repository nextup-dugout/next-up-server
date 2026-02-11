package com.nextup.infrastructure.security.filter

import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

@DisplayName("RateLimitFilter 테스트")
class RateLimitFilterTest {
    private lateinit var filter: RateLimitFilter
    private lateinit var request: MockHttpServletRequest
    private lateinit var response: MockHttpServletResponse
    private lateinit var filterChain: FilterChain

    @BeforeEach
    fun setUp() {
        filter = RateLimitFilter()
        request = MockHttpServletRequest()
        response = MockHttpServletResponse()
        filterChain = mockk(relaxed = true)
    }

    @Nested
    @DisplayName("Rate Limit 정상 통과")
    inner class PassUnderRateLimit {
        @Test
        fun `should pass request when under rate limit`() {
            // given
            request.requestURI = "/api/players"
            request.remoteAddr = "192.168.1.1"

            // when
            filter.doFilter(request, response, filterChain)

            // then
            verify { filterChain.doFilter(request, response) }
            assertThat(response.status).isNotEqualTo(429)
        }
    }

    @Nested
    @DisplayName("Rate Limit 초과")
    inner class RateLimitExceeded {
        @Test
        fun `should return 429 when rate limit exceeded for API endpoint`() {
            // given
            request.requestURI = "/api/players"
            request.remoteAddr = "192.168.1.2"

            // when - exhaust 100 requests
            repeat(100) {
                filter.doFilter(request, response, filterChain)
            }

            // then - 101st request should be rate limited
            val limitedResponse = MockHttpServletResponse()
            filter.doFilter(request, limitedResponse, filterChain)

            assertThat(limitedResponse.status).isEqualTo(429)
            assertThat(limitedResponse.contentType).isEqualTo("application/json")
            assertThat(limitedResponse.contentAsString)
                .isEqualTo(
                    """{"success":false,"error":{"code":"RATE_LIMIT_EXCEEDED","message":"Too many requests. Please try again later."}}""",
                )
        }

        @Test
        fun `should return 429 when rate limit exceeded for auth endpoint`() {
            // given
            request.requestURI = "/api/auth/login"
            request.remoteAddr = "192.168.1.3"

            // when - exhaust 10 requests
            repeat(10) {
                filter.doFilter(request, response, filterChain)
            }

            // then - 11th request should be rate limited
            val limitedResponse = MockHttpServletResponse()
            filter.doFilter(request, limitedResponse, filterChain)

            assertThat(limitedResponse.status).isEqualTo(429)
            assertThat(limitedResponse.contentType).isEqualTo("application/json")
            assertThat(limitedResponse.contentAsString)
                .isEqualTo(
                    """{"success":false,"error":{"code":"RATE_LIMIT_EXCEEDED","message":"Too many requests. Please try again later."}}""",
                )
        }

        @Test
        fun `should return correct JSON error body on rate limit`() {
            // given
            request.requestURI = "/api/auth/register"
            request.remoteAddr = "192.168.1.4"

            // when - exhaust auth endpoint limit
            repeat(10) {
                filter.doFilter(request, response, filterChain)
            }
            val limitedResponse = MockHttpServletResponse()
            filter.doFilter(request, limitedResponse, filterChain)

            // then
            assertThat(limitedResponse.status).isEqualTo(429)
            assertThat(limitedResponse.contentType).isEqualTo("application/json")
            val expectedJson =
                """{"success":false,"error":{"code":"RATE_LIMIT_EXCEEDED","message":"Too many requests. Please try again later."}}"""
            assertThat(limitedResponse.contentAsString).isEqualTo(expectedJson)
        }
    }

    @Nested
    @DisplayName("Bucket 분리")
    inner class SeparateBuckets {
        @Test
        fun `should use separate buckets for auth and non-auth`() {
            // given
            val authRequest = MockHttpServletRequest()
            authRequest.requestURI = "/api/auth/login"
            authRequest.remoteAddr = "192.168.1.5"

            val apiRequest = MockHttpServletRequest()
            apiRequest.requestURI = "/api/players"
            apiRequest.remoteAddr = "192.168.1.5"

            // when - exhaust auth endpoint
            repeat(10) {
                filter.doFilter(authRequest, response, filterChain)
            }

            // then - auth endpoint should be limited
            val authLimitedResponse = MockHttpServletResponse()
            filter.doFilter(authRequest, authLimitedResponse, filterChain)
            assertThat(authLimitedResponse.status).isEqualTo(429)

            // but API endpoint should still work
            val apiResponse = MockHttpServletResponse()
            filter.doFilter(apiRequest, apiResponse, filterChain)
            assertThat(apiResponse.status).isNotEqualTo(429)
            verify { filterChain.doFilter(apiRequest, apiResponse) }
        }
    }

    @Nested
    @DisplayName("Client IP 추출")
    inner class ClientIpExtraction {
        @Test
        fun `should use remoteAddr for rate limiting`() {
            // given
            val request1 = MockHttpServletRequest()
            request1.requestURI = "/api/auth/login"
            request1.remoteAddr = "192.168.2.1"

            val request2 = MockHttpServletRequest()
            request2.requestURI = "/api/auth/login"
            request2.remoteAddr = "192.168.2.2"

            // when - exhaust limit for first IP
            repeat(10) {
                filter.doFilter(request1, response, filterChain)
            }

            // then - first IP should be limited
            val limitedResponse = MockHttpServletResponse()
            filter.doFilter(request1, limitedResponse, filterChain)
            assertThat(limitedResponse.status).isEqualTo(429)

            // but second IP should still work
            val successResponse = MockHttpServletResponse()
            filter.doFilter(request2, successResponse, filterChain)
            assertThat(successResponse.status).isNotEqualTo(429)
            verify { filterChain.doFilter(request2, successResponse) }
        }

        @Test
        fun `should ignore X-Forwarded-For header to prevent spoofing`() {
            // given - same remoteAddr with different X-Forwarded-For
            val request1 = MockHttpServletRequest()
            request1.requestURI = "/api/auth/login"
            request1.remoteAddr = "10.0.0.1"
            request1.addHeader("X-Forwarded-For", "203.0.113.1")

            val request2 = MockHttpServletRequest()
            request2.requestURI = "/api/auth/login"
            request2.remoteAddr = "10.0.0.1"
            request2.addHeader("X-Forwarded-For", "203.0.113.2")

            // when - exhaust limit using remoteAddr
            repeat(10) {
                filter.doFilter(request1, response, filterChain)
            }

            // then - second request with same remoteAddr should also be limited
            val limitedResponse = MockHttpServletResponse()
            filter.doFilter(request2, limitedResponse, filterChain)
            assertThat(limitedResponse.status).isEqualTo(429)
        }

        @Test
        fun `should return remoteAddr from getClientIp`() {
            // given
            val req = MockHttpServletRequest()
            req.remoteAddr = "1.2.3.4"
            req.addHeader("X-Forwarded-For", "5.6.7.8")

            // when
            val ip = filter.getClientIp(req)

            // then
            assertThat(ip).isEqualTo("1.2.3.4")
        }
    }
}
