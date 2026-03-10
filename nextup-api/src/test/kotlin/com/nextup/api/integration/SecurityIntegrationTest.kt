package com.nextup.api.integration

import com.nextup.infrastructure.security.jwt.JwtTokenProvider
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
class SecurityIntegrationTest : IntegrationTestBase() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Test
    fun `인증 없이 보호된 API 호출 시 401이 반환된다`() {
        mockMvc
            .perform(get("/api/v1/teams"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `유효한 JWT로 API 호출 시 정상 응답이 반환된다`() {
        val token =
            jwtTokenProvider.createAccessToken(
                userId = 1L,
                email = "test@test.com",
                roles = setOf("ROLE_USER"),
            )

        mockMvc
            .perform(
                get("/api/v1/notifications/unread-count")
                    .header("Authorization", "Bearer $token"),
            )
            .andExpect(status().isOk)
    }

    @Test
    fun `만료된 JWT로 API 호출 시 401이 반환된다`() {
        val expiredToken =
            "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJ0ZXN0QHRlc3QuY29tIiwi" +
                "cm9sZXMiOlsiUk9MRV9VU0VSIl0sInR5cGUiOiJhY2Nlc3MiLCJpc3MiOiJuZXh0dXAi" +
                "LCJpYXQiOjE3MDAwMDAwMDAsImV4cCI6MTcwMDAwMDAwMX0.invalid"

        mockMvc
            .perform(
                get("/api/v1/teams")
                    .header("Authorization", "Bearer $expiredToken"),
            )
            .andExpect(status().isUnauthorized)
    }
}
