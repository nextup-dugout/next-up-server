package com.nextup.infrastructure.aspect

import com.nextup.common.audit.AuditLog
import com.nextup.common.audit.AuditSeverity
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import org.aspectj.lang.ProceedingJoinPoint
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * AuditLogAspect 단위 테스트
 *
 * SecurityContextHolder와 RequestContextHolder를 실제 구현으로 사용하여
 * 다른 테스트와의 static mock 간섭을 방지합니다.
 */
@DisplayName("AuditLogAspect")
class AuditLogAspectTest {
    private lateinit var aspect: AuditLogAspect

    @BeforeEach
    fun setUp() {
        aspect = AuditLogAspect()
        SecurityContextHolder.clearContext()
        RequestContextHolder.resetRequestAttributes()
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
        RequestContextHolder.resetRequestAttributes()
    }

    private fun auditLog(
        action: String,
        severity: AuditSeverity = AuditSeverity.INFO,
    ): AuditLog {
        val annotation = mockk<AuditLog>()
        every { annotation.action } returns action
        every { annotation.severity } returns severity
        return annotation
    }

    private fun setAuthenticatedUser(userId: Long) {
        val principal =
            mockk<com.nextup.infrastructure.security.userdetails.CustomUserDetails> {
                every { id } returns userId
            }
        val auth =
            UsernamePasswordAuthenticationToken(
                principal,
                null,
                listOf(SimpleGrantedAuthority("ROLE_USER")),
            )
        SecurityContextHolder.getContext().authentication = auth
    }

    private fun setRequestAttributes(
        remoteAddr: String = "127.0.0.1",
        xForwardedFor: String? = null,
        xRealIp: String? = null,
    ) {
        val request = MockHttpServletRequest()
        request.remoteAddr = remoteAddr
        xForwardedFor?.let { request.addHeader("X-Forwarded-For", it) }
        xRealIp?.let { request.addHeader("X-Real-IP", it) }
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
    }

    private fun mockHttpServletRequest(
        remoteAddr: String = "127.0.0.1",
        xForwardedFor: String? = null,
        xRealIp: String? = null,
    ): HttpServletRequest {
        val request = mockk<HttpServletRequest>()
        every { request.getHeader("X-Forwarded-For") } returns xForwardedFor
        every { request.getHeader("X-Real-IP") } returns xRealIp
        every { request.remoteAddr } returns remoteAddr
        return request
    }

    @Nested
    @DisplayName("audit()")
    inner class AuditMethod {
        @Test
        @DisplayName("성공 시 결과를 그대로 반환한다")
        fun `should return result on success`() {
            setAuthenticatedUser(userId = 1L)
            setRequestAttributes()

            val joinPoint = mockk<ProceedingJoinPoint>()
            every { joinPoint.proceed() } returns "result"

            val result = aspect.audit(joinPoint, auditLog("TEST_ACTION"))

            assertThat(result).isEqualTo("result")
        }

        @Test
        @DisplayName("예외 발생 시 예외를 그대로 재던진다")
        fun `should rethrow exception on failure`() {
            setAuthenticatedUser(userId = 1L)
            setRequestAttributes()

            val joinPoint = mockk<ProceedingJoinPoint>()
            every { joinPoint.proceed() } throws RuntimeException("test error")

            assertThatThrownBy {
                aspect.audit(joinPoint, auditLog("TEST_ACTION"))
            }.isInstanceOf(RuntimeException::class.java)
                .hasMessage("test error")
        }

        @Test
        @DisplayName("인증 정보가 없을 때 anonymous 사용자로 기록한다")
        fun `should log as anonymous when no authentication`() {
            // SecurityContext에 인증 정보 없음 (clearContext 상태)
            setRequestAttributes()

            val joinPoint = mockk<ProceedingJoinPoint>()
            every { joinPoint.proceed() } returns Unit

            aspect.audit(joinPoint, auditLog("TEST_ACTION", AuditSeverity.INFO))
        }

        @Test
        @DisplayName("X-Forwarded-For 헤더가 있을 때 해당 IP를 사용한다")
        fun `should use X-Forwarded-For header when present`() {
            setAuthenticatedUser(userId = 1L)
            setRequestAttributes(xForwardedFor = "10.0.0.1, 10.0.0.2")

            val joinPoint = mockk<ProceedingJoinPoint>()
            every { joinPoint.proceed() } returns Unit

            aspect.audit(joinPoint, auditLog("TEST_ACTION"))
        }

        @Test
        @DisplayName("X-Real-IP 헤더가 있을 때 해당 IP를 사용한다")
        fun `should use X-Real-IP header when present`() {
            setAuthenticatedUser(userId = 1L)
            setRequestAttributes(xRealIp = "192.168.1.100")

            val joinPoint = mockk<ProceedingJoinPoint>()
            every { joinPoint.proceed() } returns Unit

            aspect.audit(joinPoint, auditLog("TEST_ACTION"))
        }

        @Test
        @DisplayName("RequestContextHolder에 요청 정보가 없을 때 unknown IP를 사용한다")
        fun `should use unknown IP when no request attributes`() {
            setAuthenticatedUser(userId = 1L)
            // RequestContextHolder에 아무것도 설정하지 않음

            val joinPoint = mockk<ProceedingJoinPoint>()
            every { joinPoint.proceed() } returns Unit

            aspect.audit(joinPoint, auditLog("TEST_ACTION"))
        }

        @Test
        @DisplayName("WARN 심각도로 로깅한다")
        fun `should log with WARN severity`() {
            setAuthenticatedUser(userId = 42L)
            setRequestAttributes(remoteAddr = "10.10.10.10")

            val joinPoint = mockk<ProceedingJoinPoint>()
            every { joinPoint.proceed() } returns Unit

            aspect.audit(joinPoint, auditLog("KICK_MEMBER", AuditSeverity.WARN))
        }

        @Test
        @DisplayName("예외 발생 시 ERROR 심각도로 로깅한다")
        fun `should log with ERROR severity on exception`() {
            setAuthenticatedUser(userId = 1L)
            setRequestAttributes()

            val joinPoint = mockk<ProceedingJoinPoint>()
            every { joinPoint.proceed() } throws IllegalStateException("forbidden")

            assertThatThrownBy {
                aspect.audit(joinPoint, auditLog("ROLE_CHANGE", AuditSeverity.WARN))
            }.isInstanceOf(IllegalStateException::class.java)
        }

        @Test
        @DisplayName("null 반환 메서드도 정상 처리한다")
        fun `should handle null return value`() {
            setAuthenticatedUser(userId = 1L)
            setRequestAttributes()

            val joinPoint = mockk<ProceedingJoinPoint>()
            every { joinPoint.proceed() } returns null

            val result = aspect.audit(joinPoint, auditLog("DELETE_TEAM", AuditSeverity.WARN))

            assertThat(result).isNull()
        }
    }
}
